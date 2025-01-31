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
package org.openntf.xsp.cdi.concurrency;

import jakarta.annotation.Priority;
import org.openntf.xsp.cdi.ext.CDIContainerLocator;

/**
 * Provides the CDI environment from an active ServletContext, if available.
 * 
 * @author Jesse Gallagher
 * @since 2.7.0
 */
@Priority(100)
public class ConcurrencyCDIContainerLocator implements CDIContainerLocator {
	private static final ThreadLocal<Object> THREAD_CDI = new ThreadLocal<>();
	
	public static void setCdi(Object cdi) {
		THREAD_CDI.set(cdi);
	}
	
	@Override
	public Object getContainer() {
		return THREAD_CDI.get();
	}

}
