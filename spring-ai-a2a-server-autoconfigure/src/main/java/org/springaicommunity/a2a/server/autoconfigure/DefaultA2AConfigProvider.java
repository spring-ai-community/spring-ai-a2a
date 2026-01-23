/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springaicommunity.a2a.server.autoconfigure;

import io.a2a.server.config.A2AConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Default A2A configuration provider implementation.
 *
 * <p>
 * Loads configuration from {@code META-INF/a2a-defaults.properties} on the classpath. If
 * no configuration files are found, uses hardcoded defaults.
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @since 0.1.0
 */
public class DefaultA2AConfigProvider implements A2AConfigProvider, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(DefaultA2AConfigProvider.class);

	private static final String DEFAULTS_RESOURCE = "classpath*:META-INF/a2a-defaults.properties";

	private final ResourcePatternResolver resourceResolver;

	/**
	 * Cached configuration values, loaded during bean initialization. Guaranteed to be
	 * non-null after {@link #afterPropertiesSet()} completes.
	 */
	private Map<String, String> configCache;

	/**
	 * Create DefaultA2AConfigProvider with default resource resolver.
	 */
	public DefaultA2AConfigProvider() {
		this(new PathMatchingResourcePatternResolver());
	}

	/**
	 * Create DefaultA2AConfigProvider with custom resolver for testing.
	 */
	public DefaultA2AConfigProvider(ResourcePatternResolver resourceResolver) {
		this.resourceResolver = resourceResolver;
	}

	@Override
	public void afterPropertiesSet() {
		this.configCache = Map.copyOf(loadConfiguration());
		logger.debug("DefaultA2AConfigProvider initialization complete: {} configuration values loaded",
				configCache.size());
	}

	@Override
	public String getValue(String key) {
		String value = configCache.get(key);
		if (value == null) {
			throw new IllegalArgumentException("No configuration value found for: " + key);
		}
		return value;
	}

	@Override
	public Optional<String> getOptionalValue(String key) {
		return Optional.ofNullable(configCache.get(key));
	}

	/**
	 * Load configuration from Spring resources.
	 */
	protected Map<String, String> loadConfiguration() {
		Map<String, String> mergedConfig = new HashMap<>();
		Map<String, String> sourceTracker = new HashMap<>();

		try {
			Resource[] resources = resourceResolver.getResources(DEFAULTS_RESOURCE);

			if (resources.length == 0) {
				logger.info("No A2A configuration files found at {}, using hardcoded defaults", DEFAULTS_RESOURCE);
				return getHardcodedDefaults();
			}

			logger.info("Loading A2A configuration from {} resource(s)", resources.length);

			for (Resource resource : resources) {
				if (resource.exists() && resource.isReadable()) {
					try {
						Properties props = new Properties();
						try (InputStream is = resource.getInputStream()) {
							props.load(is);

							for (String key : props.stringPropertyNames()) {
								String value = props.getProperty(key);
								String existingSource = sourceTracker.get(key);

								if (existingSource != null) {
									throw new IllegalStateException(String.format(
											"Duplicate configuration key '%s' found in multiple a2a-defaults.properties files: %s and %s",
											key, existingSource, resource.getDescription()));
								}

								mergedConfig.put(key, value);
								sourceTracker.put(key, resource.getDescription());
								logger.trace("Loaded config: {} = {}", key, value);
							}
						}

						logger.debug("Loaded {} configuration values from {}", props.size(), resource.getDescription());
					}
					catch (IOException e) {
						logger.warn("Failed to read configuration from {}: {}", resource.getDescription(),
								e.getMessage());
					}
				}
			}

			logger.info("Loaded {} A2A default configuration values from {} resource(s)", mergedConfig.size(),
					sourceTracker.values().stream().distinct().count());

			return mergedConfig;

		}
		catch (IOException e) {
			logger.error("Failed to load A2A configuration, using hardcoded defaults", e);
			return getHardcodedDefaults();
		}
	}

	/**
	 * Get hardcoded default configuration values for graceful degradation.
	 */
	protected Map<String, String> getHardcodedDefaults() {
		Map<String, String> defaults = new HashMap<>();

		// Blocking call timeouts (required by DefaultRequestHandler)
		defaults.put("a2a.blocking.agent.timeout.seconds", "30");
		defaults.put("a2a.blocking.consumption.timeout.seconds", "5");

		// Executor configuration (required by AsyncExecutorProducer)
		defaults.put("a2a.executor.core-pool-size", "5");
		defaults.put("a2a.executor.max-pool-size", "50");
		defaults.put("a2a.executor.keep-alive-seconds", "60");

		logger.debug("Using {} hardcoded default configuration values", defaults.size());
		return defaults;
	}

	/**
	 * Clear the configuration cache. For testing purposes only.
	 */
	void clearCache() {
		this.configCache = null;
		logger.debug("Configuration cache cleared");
	}

}
