/**
 * Copyright (c) 2018-2023 Contributors to the XPages Jakarta EE Support Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openntf.xsp.jakarta.concurrency;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.openntf.xsp.jakarta.concurrency.jndi.DelegatingManagedExecutorService;
import org.openntf.xsp.jakarta.concurrency.jndi.DelegatingManagedScheduledExecutorService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import lotus.domino.NotesThread;

/**
 * This activator is used to try to ensure that all spawned executors
 * are terminated at HTTP stop, even if they were violently flushed out
 * of context by design changes or if incoming HTTP requests are still
 * held up by blocked threads.
 * 
 * <p>This class works reflectively to access
 * {@code lotus.notes.internal.MessageQueue} because Domino's OSGi stack
 * may prevent normal access to it.</p>
 * 
 * @author Jesse Gallagher
 * @since 2.10.0
 */
public class ConcurrencyActivator implements BundleActivator {
	private static final Logger log = Logger.getLogger(ConcurrencyActivator.class.getPackage().getName());

	public static final String ATTR_SCHEDULEDEXECUTORSERVICE = ConcurrencyActivator.class.getPackage().getName() + "_scheduledExec"; //$NON-NLS-1$

	public static final String ATTR_EXECUTORSERVICE = ConcurrencyActivator.class.getPackage().getName() + "_exec"; //$NON-NLS-1$

	public static final String JNDI_SCHEDULEDEXECUTORSERVICE = "java:comp/DefaultManagedScheduledExecutorService"; //$NON-NLS-1$

	public static final String JNDI_EXECUTORSERVICE = "java:comp/DefaultManagedExecutorService"; //$NON-NLS-1$
	
	private ScheduledExecutorService executor;
	private Class<?> mqClass;
	private Method mqOpen;
	private Method isQuitPending;
	private Method mqClose;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		String jvmVersion = AccessController.doPrivileged((PrivilegedAction<String>)() ->
			System.getProperty("java.specification.version") //$NON-NLS-1$
		);
		
		if("1.8".equals(jvmVersion)) { //$NON-NLS-1$
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			while(cl.getParent() != null) {
				cl = cl.getParent();
			}
			this.mqClass = Class.forName("lotus.notes.internal.MessageQueue", true, cl); //$NON-NLS-1$
			this.mqOpen = this.mqClass.getMethod("open", String.class, int.class); //$NON-NLS-1$
			this.isQuitPending = this.mqClass.getMethod("isQuitPending"); //$NON-NLS-1$
			this.mqClose = this.mqClass.getMethod("close", int.class); //$NON-NLS-1$
			
			this.executor = Executors.newScheduledThreadPool(10, NotesThread::new);
			this.executor.scheduleAtFixedRate(() -> {
				if(isHttpQuitting()) {
					ExecutorHolder.INSTANCE.termAll();
				}
			}, 0, 10, TimeUnit.SECONDS);
		}
		
		InitialContext jndi = new InitialContext();
		try {
			jndi.rebind(ConcurrencyActivator.JNDI_EXECUTORSERVICE, new DelegatingManagedExecutorService());
		} catch(NamingException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, "Encountered exception binding ManagedExecutorService in JNDI", e);
			}
		}
		try {
			jndi.rebind(ConcurrencyActivator.JNDI_SCHEDULEDEXECUTORSERVICE, new DelegatingManagedScheduledExecutorService());
		} catch(NamingException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, "Encountered exception binding ManagedScheduledExecutorService in JNDI", e);
			}
		}
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		ExecutorHolder.INSTANCE.termAll();
		if(!(executor.isShutdown() || executor.isTerminated())) {
			try {
				executor.shutdownNow();
				executor.awaitTermination(5, TimeUnit.MINUTES);
			} catch (Exception e) {
			}
		}
	}
	
	private boolean isHttpQuitting() {
		try {
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			while(cl.getParent() != null) {
				cl = cl.getParent();
			}
			Class<?> mqClass = Class.forName("lotus.notes.internal.MessageQueue", true, cl); //$NON-NLS-1$
			Object mq = mqClass.getConstructor().newInstance();
			mqOpen.invoke(mq, "MQ$HTTP", 0); //$NON-NLS-1$
			try {
				if((Boolean)isQuitPending.invoke(mq)) {
					return true;
				}
			} finally {
				mqClose.invoke(mq, 0);
			}
		} catch(Throwable t) {
			// Ignore
		}
		return false;
	}

}
