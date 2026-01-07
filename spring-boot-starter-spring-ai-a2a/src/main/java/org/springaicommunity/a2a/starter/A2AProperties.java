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

package org.springaicommunity.a2a.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for Spring AI A2A (Agent-to-Agent) protocol support.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@ConfigurationProperties(prefix = "spring.ai.a2a")
public class A2AProperties {

	/**
	 * Server configuration properties.
	 */
	private Server server = new Server();

	/**
	 * Agent configuration properties.
	 */
	private Agent agent = new Agent();

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public Agent getAgent() {
		return agent;
	}

	public void setAgent(Agent agent) {
		this.agent = agent;
	}

	/**
	 * Server-specific properties.
	 */
	public static class Server {

		/**
		 * Base path for A2A endpoints. Defaults to "/a2a".
		 */
		private String basePath = "/a2a";

		/**
		 * Whether to enable the A2A server. Defaults to true.
		 */
		private boolean enabled = true;

		public String getBasePath() {
			return basePath;
		}

		public void setBasePath(String basePath) {
			this.basePath = basePath;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	/**
	 * Agent card configuration properties.
	 */
	public static class Agent {

		/**
		 * Agent name. Required.
		 */
		private String name;

		/**
		 * Agent description. Required.
		 */
		private String description;

		/**
		 * Agent version. Defaults to "1.0.0".
		 */
		private String version = "1.0.0";

		/**
		 * A2A protocol version. Defaults to "0.1.0".
		 */
		private String protocolVersion = "0.1.0";

		/**
		 * Agent capabilities.
		 */
		private Capabilities capabilities = new Capabilities();

		/**
		 * Default input modes. Defaults to ["text"].
		 */
		private List<String> defaultInputModes = List.of("text");

		/**
		 * Default output modes. Defaults to ["text"].
		 */
		private List<String> defaultOutputModes = List.of("text");

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public String getProtocolVersion() {
			return protocolVersion;
		}

		public void setProtocolVersion(String protocolVersion) {
			this.protocolVersion = protocolVersion;
		}

		public Capabilities getCapabilities() {
			return capabilities;
		}

		public void setCapabilities(Capabilities capabilities) {
			this.capabilities = capabilities;
		}

		public List<String> getDefaultInputModes() {
			return defaultInputModes;
		}

		public void setDefaultInputModes(List<String> defaultInputModes) {
			this.defaultInputModes = defaultInputModes;
		}

		public List<String> getDefaultOutputModes() {
			return defaultOutputModes;
		}

		public void setDefaultOutputModes(List<String> defaultOutputModes) {
			this.defaultOutputModes = defaultOutputModes;
		}

		/**
		 * Agent capabilities configuration.
		 */
		public static class Capabilities {

			/**
			 * Whether the agent supports streaming. Defaults to true.
			 */
			private boolean streaming = true;

			/**
			 * Whether the agent supports push notifications. Defaults to false.
			 */
			private boolean pushNotifications = false;

			/**
			 * Whether the agent maintains state transition history. Defaults to false.
			 */
			private boolean stateTransitionHistory = false;

			public boolean isStreaming() {
				return streaming;
			}

			public void setStreaming(boolean streaming) {
				this.streaming = streaming;
			}

			public boolean isPushNotifications() {
				return pushNotifications;
			}

			public void setPushNotifications(boolean pushNotifications) {
				this.pushNotifications = pushNotifications;
			}

			public boolean isStateTransitionHistory() {
				return stateTransitionHistory;
			}

			public void setStateTransitionHistory(boolean stateTransitionHistory) {
				this.stateTransitionHistory = stateTransitionHistory;
			}

		}

	}

}
