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
package org.openntf.xsp.microprofile.metrics;

import java.util.Collection;
import java.util.Collections;

import org.openntf.xsp.cdi.discovery.WeldBeanClassContributor;
import org.openntf.xsp.jakartaee.util.LibraryUtil;

import io.smallrye.metrics.setup.MetricCdiInjectionExtension;
import jakarta.enterprise.inject.spi.Extension;

public class MetricsExtensionContributor implements WeldBeanClassContributor {

	@Override
	public Collection<Class<?>> getBeanClasses() {
		return Collections.emptyList();
	}

	@Override
	public Collection<Extension> getExtensions() {
		if(!"false".equals(LibraryUtil.getApplicationProperty(MetricsResourceContributor.PROP_ENABLED, "true"))) { //$NON-NLS-1$ //$NON-NLS-2$
			return Collections.singleton(new MetricCdiInjectionExtension());
		} else {
			return Collections.emptySet();
		}
	}

}
