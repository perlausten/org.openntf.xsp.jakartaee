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
package org.openntf.xsp.jaxrs.security;

import java.security.Principal;
import java.util.Collection;

import org.openntf.xsp.jakartaee.util.LibraryUtil;

import com.ibm.commons.util.StringUtil;
import com.ibm.xsp.extlib.util.ExtLibUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.SecurityContext;
import lotus.domino.Database;
import lotus.domino.NotesException;

public class JAXRSSecurityContext implements SecurityContext {
	public static final String ATTR_ROLES = JAXRSSecurityContext.class.getName() + "_roles"; //$NON-NLS-1$
	
	private final HttpServletRequest req;

	public JAXRSSecurityContext(HttpServletRequest req) {
		this.req = req;
	}

	@Override
	public Principal getUserPrincipal() {
		return req.getUserPrincipal();
	}

	@Override
	public boolean isUserInRole(final String role) {
		if(role == null) {
			return false;
		}
		switch(role) {
		case "login": //$NON-NLS-1$
			Principal user = getUserPrincipal();
			if(user != null) {
				String name = user.getName();
				return StringUtil.isNotEmpty(name) && !"Anonymous".equalsIgnoreCase(name); //$NON-NLS-1$
			} else {
				return false;
			}
		default:
			return getRoles().contains(role);
		}
	}

	@Override
	public boolean isSecure() {
		return req.isSecure();
	}

	@Override
	public String getAuthenticationScheme() {
		// TODO look this up from the active authentication filter
		return FORM_AUTH;
	}
	
	private Collection<String> getRoles() {
		@SuppressWarnings("unchecked")
		Collection<String> roles = (Collection<String>)this.req.getAttribute(ATTR_ROLES);
		if(roles == null) {
			// TODO handle cases when there's no current database
			Database database = ExtLibUtil.getCurrentDatabase();
			try {
				roles = LibraryUtil.getUserNamesList(database);
				this.req.setAttribute(ATTR_ROLES, roles);
			} catch(NotesException e) {
				throw new RuntimeException(e);
			}
		}
		return roles;
	}

}
