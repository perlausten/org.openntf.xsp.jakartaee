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
package org.openntf.xsp.jakartaee.discovery;

/**
 * This extension class allows contributions to the process of determining
 * the value of a given application property.
 * 
 * @author Jesse Gallagher
 * @since 2.7.0
 */
public interface ApplicationPropertyLocator {
	/**
	 * @return {@code true} if the locator is in a context where it can run
	 */
	boolean isActive();
	
	/**
	 * @param prop the property to retrieve
	 * @param defaultValue the default value to return when the property is not set
	 * @return {@link prop} if set in the application; {@code defaultValue} otherwise
	 */
	String getApplicationProperty(String prop, String defaultValue);
}
