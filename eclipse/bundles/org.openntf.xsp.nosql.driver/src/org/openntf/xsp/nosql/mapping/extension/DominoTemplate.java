/**
 * Copyright © 2018-2022 Contributors to the XPages Jakarta EE Support Project
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
package org.openntf.xsp.nosql.mapping.extension;

import java.util.stream.Stream;

import jakarta.nosql.mapping.Pagination;
import jakarta.nosql.mapping.document.DocumentTemplate;

/**
 * {@link DocumentTemplate} sub-interface to provide access to
 * Domino-specific extensions.
 * 
 * @author Jesse Gallagher
 * @since 2.5.0
 */
public interface DominoTemplate extends DocumentTemplate {
	/**
	 * Reads entries from the provided view, restricted to the named category.
	 * 
	 * @param <T> the class of object returned
	 * @param entityName the effective entity name returned by this type
	 * @param viewName the name of the view to query
	 * @param category the category to restrict to, or {@code null} to not restrict
	 * @param pagination the pagination settings to use, or {@code null} to skip pagination
	 * @param maxLevel the maximum view entry level to process
	 * @return a {@link Stream} of entities
	 * @sine 2.6.0
	 */
	<T> Stream<T> viewEntryQuery(String entityName, String viewName, String category, Pagination pagination, int maxLevel);

	/**
	 * Reads documents from the provided view, restricted to the named category.
	 * 
	 * @param <T> the class of object returned
	 * @param entityName the effective entity name returned by this type
	 * @param viewName the name of the view to query
	 * @param category the category to restrict to, or {@code null} to not restrict
	 * @param pagination the pagination settings to use, or {@code null} to skip pagination
	 * @param maxLevel the maximum view entry level to process
	 * @return a {@link Stream} of entities
	 * @sine 2.6.0
	 */
	<T> Stream<T> viewDocumentQuery(String entityName, String viewName, String category, Pagination pagination, int maxLevel);

	/**
	 * Adds the entity to the named folder, creating the folder if it doesn't
	 * exist.
	 * 
	 * @param entityId the UNID of the entity to add to a folder
	 * @param folderName the folder to add the entity to
	 * @throws IllegalStateException if the document has not yet been saved
	 * @throws IllegalArgumentException if {@code folderName} is empty
	 * @since 2.6.0
	 */
	void putInFolder(String entityId, String folderName);
	
	/**
	 * Removes the entity from the named folder.
	 * 
	 * @param entityId the UNID of the entity to remove from a folder
	 * @param folderName the folder to remove the entity from
	 * @throws IllegalArgumentException if {@code folderName} is empty
	 * @since 2.6.0
	 */
	void removeFromFolder(String entityId, String folderName);
}
