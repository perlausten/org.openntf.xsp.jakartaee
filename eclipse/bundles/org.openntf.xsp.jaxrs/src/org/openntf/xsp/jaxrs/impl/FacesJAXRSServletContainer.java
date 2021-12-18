/**
 * Copyright © 2018-2021 Martin Pradny and Jesse Gallagher
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
package org.openntf.xsp.jaxrs.impl;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.openntf.xsp.jakartaee.JakartaConstants;
import org.openntf.xsp.jakartaee.servlet.ServletUtil;
import org.openntf.xsp.jaxrs.ServiceParticipant;

import com.ibm.commons.util.NotImplementedException;
import com.ibm.domino.xsp.module.nsf.NotesContext;
import com.ibm.xsp.application.ApplicationEx;
import com.ibm.xsp.context.FacesContextEx;
import com.ibm.xsp.controller.FacesController;
import com.ibm.xsp.controller.FacesControllerFactoryImpl;

/**
 * An {@link ServletContainer} subclass that provides a Faces context to the
 * servlet request.
 * 
 * @author Martin Pradny
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class FacesJAXRSServletContainer extends HttpServletDispatcher {
	private static final long serialVersionUID = 1L;
	
	private ServletConfig config;
	private FacesContextFactory contextFactory;
	private boolean initialized = false;

	public FacesJAXRSServletContainer() {
		
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		this.config = config;
		contextFactory = (FacesContextFactory) FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
	}
	
	private javax.servlet.ServletContext getOldServletContext() {
		return ServletUtil.newToOld(getServletContext());
	}
	
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setAttribute(JakartaConstants.CDI_JAXRS_REQUEST, "true"); //$NON-NLS-1$
		
		NotesContext nc = NotesContext.getCurrentUnchecked();
    	String javaClassValue = "plugin.Activator"; //$NON-NLS-1$
		String str = "WEB-INF/classes/" + javaClassValue.replace('.', '/') + ".class"; //$NON-NLS-1$ //$NON-NLS-2$
		nc.setSignerSessionRights(str);
		FacesContext fc=null;
		try {
			fc = initContext(request, response);
	    	FacesContextEx exc = (FacesContextEx)fc;
	    	ApplicationEx application = exc.getApplicationEx();
	    	if (application.getController() == null) {
	    		FacesController controller = new FacesControllerFactoryImpl().createFacesController(getOldServletContext());
	    		controller.init(null);
	    	}
	    	
	    	@SuppressWarnings("unchecked")
			List<ServiceParticipant> participants = (List<ServiceParticipant>)application.findServices(ServiceParticipant.EXTENSION_POINT);
	    	for(ServiceParticipant participant : participants) {
	    		participant.doBeforeService(request, response);
	    	}
			if (!initialized){ // initialization has do be done after NotesContext is initialized with session to support SessionAsSigner operations
				super.init();
				ClassLoader cl = AccessController.doPrivileged((PrivilegedAction<ClassLoader>)() -> Thread.currentThread().getContextClassLoader());
				try {
					AccessController.doPrivileged((PrivilegedAction<Void>)() -> {
						Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
						return null;
					});
					super.init(config);
				} finally {
					AccessController.doPrivileged((PrivilegedAction<Void>)() -> {
						Thread.currentThread().setContextClassLoader(cl);
						return null;
					});
				}
				
				initialized = true;
			}
	    	
	    	try {
	    		super.service(request, response);
	    	} finally {
	    		for(ServiceParticipant participant : participants) {
		    		participant.doAfterService(request, response);
		    	}
	    	}
		} catch(Throwable t) {
			t.printStackTrace();
			response.sendError(500, "Application failed!"); //$NON-NLS-1$
		} finally {
			if (fc != null) {
				releaseContext(fc);
			}
			
		}
	}
	
	@Override
    public ServletConfig getServletConfig() {
    	return config;
    }
	
	// *******************************************************************************
	// * Internal implementation methods
	// *******************************************************************************
	
	private FacesContext initContext(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Create a temporary FacesContext and make it available
		// TODO consider if it would be better to unwrap these instead of double-wrapping
		javax.servlet.ServletContext context = ServletUtil.newToOld(getServletConfig().getServletContext());
		javax.servlet.http.HttpServletRequest req = ServletUtil.newToOld(request);
		javax.servlet.http.HttpServletResponse resp = ServletUtil.newToOld(response);
        return contextFactory.getFacesContext(context, req, resp, dummyLifeCycle);
    }
	
	private void releaseContext(FacesContext context) throws ServletException, IOException {
		context.release();
    }
	
	private static Lifecycle dummyLifeCycle = new Lifecycle() {
		@Override
		public void render(FacesContext context) throws FacesException {
			throw new NotImplementedException();
		}

		@Override
		public void removePhaseListener(PhaseListener listener) {
			throw new NotImplementedException();
		}

		@Override
		public PhaseListener[] getPhaseListeners() {
			throw new NotImplementedException();
		}

		@Override
		public void execute(FacesContext context) throws FacesException {
			throw new NotImplementedException();
		}

		@Override
		public void addPhaseListener(PhaseListener listener) {
			throw new NotImplementedException();
		}
	};

}
