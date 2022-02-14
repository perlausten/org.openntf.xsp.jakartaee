/**
 * Copyright © 2018-2022 Martin Pradny and Jesse Gallagher
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
package org.openntf.xsp.jaxrs;

import java.util.Collection;
import java.util.Collections;

import jakarta.ws.rs.core.Application;

/**
 * Extension interface to contribute additional resource classes
 * at {@link Application} init.
 * 
 * @author Jesse Gallagher
 * @since 2.1.0
 */
public interface JAXRSClassContributor {
	Collection<Class<?>> getClasses();
	
	/**
	 * @since 2.3.0
	 */
	default Collection<Object> getSingletons() {
		return Collections.emptySet();
	}
}
