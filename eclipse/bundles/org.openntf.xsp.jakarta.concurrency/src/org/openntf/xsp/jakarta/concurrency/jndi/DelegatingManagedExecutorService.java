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
package org.openntf.xsp.jakarta.concurrency.jndi;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openntf.xsp.jakarta.concurrency.ConcurrencyActivator;
import org.openntf.xsp.jakartaee.module.ComponentModuleLocator;

import jakarta.enterprise.concurrent.ManagedExecutorService;

/**
 * This class is intended to be provided via JNDI, delegating all calls to the
 * app-context service.
 * 
 * @author Jesse Gallagher
 * @since 2.11.0
 */
public class DelegatingManagedExecutorService implements ManagedExecutorService {

	@Override
	public void shutdown() {
		getDelegate().shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return getDelegate().shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return getDelegate().isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return getDelegate().isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return getDelegate().awaitTermination(timeout, unit);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return getDelegate().submit(task);
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return getDelegate().submit(task, result);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return getDelegate().submit(task);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return getDelegate().invokeAll(tasks);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return getDelegate().invokeAll(tasks, timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return getDelegate().invokeAny(tasks);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return getDelegate().invokeAny(tasks, timeout, unit);
	}

	@Override
	public void execute(Runnable command) {
		getDelegate().execute(command);
	}
	
	private ManagedExecutorService getDelegate() {
		return ComponentModuleLocator.getDefault()
			.flatMap(ComponentModuleLocator::getServletContext)
			.flatMap(ctx -> Optional.ofNullable((ManagedExecutorService)ctx.getAttribute(ConcurrencyActivator.ATTR_EXECUTORSERVICE)))
			.orElseThrow(() -> new IllegalStateException("ManagedExecutorService not available"));
	}

}
