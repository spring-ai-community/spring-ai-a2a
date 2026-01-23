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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring AI A2A Server.
 *
 * <p>
 * These properties allow customization of basic A2A server behavior.
 *
 * <p>
 * <strong>Example configuration:</strong> <pre>
 * spring:
 *   ai:
 *     a2a:
 *       server:
 *         enabled: true
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @since 0.1.0
 */
@ConfigurationProperties(prefix = A2AServerProperties.CONFIG_PREFIX)
public class A2AServerProperties {

	public static final String CONFIG_PREFIX = "spring.ai.a2a.server";

	/**
	 * Whether the A2A server is enabled.
	 */
	private boolean enabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
