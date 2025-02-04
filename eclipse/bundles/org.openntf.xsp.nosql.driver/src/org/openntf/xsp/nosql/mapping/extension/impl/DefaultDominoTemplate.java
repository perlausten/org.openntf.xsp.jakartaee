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
package org.openntf.xsp.nosql.mapping.extension.impl;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.jnosql.mapping.document.AbstractDocumentTemplate;
import org.eclipse.jnosql.mapping.reflection.ClassMappings;
import org.openntf.xsp.nosql.communication.driver.DominoDocumentCollectionManager;
import org.openntf.xsp.nosql.communication.driver.ViewInfo;
import org.openntf.xsp.nosql.mapping.extension.DominoTemplate;
import org.openntf.xsp.nosql.mapping.extension.ViewQuery;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.nosql.mapping.Converters;
import jakarta.nosql.mapping.Entity;
import jakarta.nosql.mapping.Pagination;
import jakarta.nosql.mapping.Sorts;
import jakarta.nosql.mapping.document.DocumentEntityConverter;
import jakarta.nosql.mapping.document.DocumentEventPersistManager;
import jakarta.nosql.mapping.document.DocumentWorkflow;

/**
 * Default implementation of {@link DominoTemplate}.
 * 
 * @author Jesse Gallagher
 * @since 2.5.0
 */
public class DefaultDominoTemplate extends AbstractDocumentTemplate implements DominoTemplate {

	private Instance<DominoDocumentCollectionManager> manager;

    private DocumentEntityConverter converter;

    private DocumentWorkflow flow;

    private DocumentEventPersistManager persistManager;

    private ClassMappings mappings;

    private Converters converters;

    @Inject
    DefaultDominoTemplate(Instance<DominoDocumentCollectionManager> manager,
                             DocumentEntityConverter converter, DocumentWorkflow flow,
                             DocumentEventPersistManager persistManager,
                             ClassMappings mappings,
                             Converters converters) {
        this.manager = manager;
        this.converter = converter;
        this.flow = flow;
        this.persistManager = persistManager;
        this.mappings = mappings;
        this.converters = converters;
    }

    DefaultDominoTemplate() {
    }

    @Override
    protected DocumentEntityConverter getConverter() {
		DocumentEntityConverter converter = this.converter == null ? CDI.current().select(DocumentEntityConverter.class).get() : this.converter;
		Objects.requireNonNull(converter, "Unable to acquire DocumentEntityConverter");
        return converter;
    }

    @Override
    protected DominoDocumentCollectionManager getManager() {
        return manager.get();
    }

    @Override
    protected DocumentWorkflow getWorkflow() {
        return flow;
    }

    @Override
    protected DocumentEventPersistManager getPersistManager() {
        return persistManager;
    }

    @Override
    protected ClassMappings getClassMappings() {
    	return mappings;
    }

    @Override
    protected Converters getConverters() {
        return converters;
    }

	@SuppressWarnings("unchecked")
	@Override
	public <T> Stream<T> viewEntryQuery(String entityName, String viewName, Pagination pagination, Sorts sorts, int maxLevel, boolean docsOnly, ViewQuery viewQuery, boolean singleResult) {
		return getManager().viewEntryQuery(entityName, viewName, pagination, sorts, maxLevel, docsOnly, viewQuery, singleResult)
			.map(getConverter()::toEntity)
			.map(d -> (T)d);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Stream<T> viewDocumentQuery(String entityName, String viewName, Pagination pagination, Sorts sorts, int maxLevel, ViewQuery viewQuery, boolean singleResult, boolean distinct) {
		return getManager().viewDocumentQuery(entityName, viewName, pagination, sorts, maxLevel, viewQuery, singleResult, distinct)
			.map(getConverter()::toEntity)
			.map(d -> (T)d);
	}
	
	@Override
	public void putInFolder(String entityId, String folderName) {
		getManager().putInFolder(entityId, folderName);
	}
	
	@Override
	public void removeFromFolder(String entityId, String folderName) {
		getManager().removeFromFolder(entityId, folderName);
	}
	
	@Override
	public <T> T insert(T entity, boolean computeWithForm) {
		return getWorkflow().flow(entity, documentEntity -> getManager().insert(documentEntity, computeWithForm));
	}
	
	@Override
	public <T> T update(T entity, boolean computeWithForm) {
		return getWorkflow().flow(entity, documentEntity -> getManager().update(documentEntity, computeWithForm));
	}
	
	@Override
	public boolean existsById(String unid) {
		return getManager().existsById(unid);
	}
	
	@Override
	public <T> Optional<T> getByNoteId(String entityName, String noteId) {
		return getManager().getByNoteId(entityName, noteId).map(getConverter()::toEntity);
	}
	
	@Override
	public <T, K> Optional<T> find(Class<T> entityClass, K id) {
		Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
		String entityName = entityAnnotation == null ? "" : entityAnnotation.value(); //$NON-NLS-1$
		if(entityName.isEmpty()) {
			entityName = entityClass.getSimpleName();
		}
		
		return getManager().getById(entityName, String.valueOf(id)).map(getConverter()::toEntity);
	}
	
	@Override
	public Stream<ViewInfo> getViewInfo() {
		return getManager().getViewInfo();
	}
	
	@Override
	public <T> Optional<T> getByName(String entityName, String name, String userName) {
		return getManager().getByName(entityName, name, userName).map(getConverter()::toEntity);
	}
	
	@Override
	public <T> Optional<T> getProfileDocument(String entityName, String profileName, String userName) {
		return getManager().getProfileDocument(entityName, profileName, userName).map(getConverter()::toEntity);
	}

}
