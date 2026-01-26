/*
* Copyright 2025 - 2025 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.springaicommunity.a2a.server.autoconfigure;

import java.util.Optional;

import io.a2a.server.config.A2AConfigProvider;
import io.a2a.server.config.DefaultValuesConfigProvider;

import org.springframework.core.env.Environment;

/**
 * Spring Environment based A2A configuration provider. It first checks the Spring
 * Environment for the property. If not found, it falls back to
 * DefaultValuesConfigProvider.
 *
 * This allows overriding default A2A server properties using standard Spring Environment
 * properties.
 *
 * @author Christian Tzolov
 */
public class SpringA2AConfigProvider implements A2AConfigProvider {

	private final Environment env;

	private final DefaultValuesConfigProvider defaultValues;

	SpringA2AConfigProvider(Environment env, DefaultValuesConfigProvider defaultValues) {
		this.env = env;
		this.defaultValues = defaultValues;
	}

	@Override
	public String getValue(String name) {
		if (this.env.containsProperty(name)) {
			return this.env.getProperty(name);
		}
		// Fallback to defaults
		return this.defaultValues.getValue(name);
	}

	@Override
	public Optional<String> getOptionalValue(String name) {
		if (this.env.containsProperty(name)) {
			return Optional.of(this.env.getProperty(name));
		}
		return this.defaultValues.getOptionalValue(name);
	}

}
