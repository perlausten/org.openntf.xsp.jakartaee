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
package org.openntf.xsp.jakartaee.servlet;

import java.util.Enumeration;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionContext;

@SuppressWarnings("deprecation")
class OldHttpSessionContextWrapper implements HttpSessionContext {
	final javax.servlet.http.HttpSessionContext delegate;
	
	public OldHttpSessionContextWrapper(javax.servlet.http.HttpSessionContext delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public HttpSession getSession(String sessionId) {
		return ServletUtil.oldToNew(delegate.getSession(sessionId));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Enumeration<String> getIds() {
		return delegate.getIds();
	}

}
