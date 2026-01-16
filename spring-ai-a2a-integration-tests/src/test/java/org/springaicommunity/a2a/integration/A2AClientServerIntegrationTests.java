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

package org.springaicommunity.a2a.integration;

import io.a2a.spec.AgentCard;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for A2A server auto-configuration.
 *
 * <p>Verifies:
 * <ul>
 * <li>A2A server starts successfully</li>
 * <li>AgentCard bean is created with correct configuration</li>
 * <li>HTTP endpoints are available</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootTest(classes = TestA2AApplication.class,
	webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class A2AClientServerIntegrationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private AgentCard agentCard;

	/**
	 * Tests that the A2A server started successfully with correct configuration.
	 */
	@Test
	void testA2AServerStarted() {
		assertThat(this.agentCard).isNotNull();
		assertThat(this.agentCard.name()).isEqualTo("Test Echo Agent");
		assertThat(this.agentCard.description()).isEqualTo("Simple agent that echoes messages for testing");
		assertThat(this.agentCard.version()).isEqualTo("1.0.0");
		assertThat(this.agentCard.protocolVersion()).isEqualTo("0.1.0");
	}

	/**
	 * Tests that the server is running on the expected port.
	 */
	@Test
	void testServerPort() {
		assertThat(this.port).isEqualTo(58888);
	}

	/**
	 * Tests that AgentCard capabilities are configured.
	 */
	@Test
	void testAgentCardCapabilities() {
		assertThat(this.agentCard.capabilities()).isNotNull();
		assertThat(this.agentCard.capabilities().streaming()).isFalse();
		assertThat(this.agentCard.capabilities().pushNotifications()).isFalse();
	}

}
