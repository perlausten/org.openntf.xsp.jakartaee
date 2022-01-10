package org.openntf.xsp.microprofile.rest.client;

import java.util.Collection;
import java.util.Collections;

import org.jboss.resteasy.microprofile.client.RestClientExtension;
import org.openntf.xsp.cdi.discovery.WeldBeanClassContributor;
import org.openntf.xsp.jakartaee.LibraryUtil;

import com.ibm.xsp.application.ApplicationEx;

import jakarta.enterprise.inject.spi.Extension;

/**
 * @author Jesse Gallagher
 * @since 2.2.0
 */
public class RestClientCDIContributor implements WeldBeanClassContributor {

	@Override
	public Collection<Class<?>> getBeanClasses() {
		return Collections.emptyList();
	}

	@Override
	public Collection<Extension> getExtensions() {
		ApplicationEx app = ApplicationEx.getInstance();
		if(app != null && LibraryUtil.usesLibrary(RestClientLibrary.LIBRARY_ID, app)) {
			return Collections.singleton(new RestClientExtension());
		} else {
			return Collections.emptyList();
		}
	}

}
