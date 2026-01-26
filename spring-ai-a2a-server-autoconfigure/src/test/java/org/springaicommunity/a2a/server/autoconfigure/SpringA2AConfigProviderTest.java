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

import java.util.Optional;

import io.a2a.server.config.DefaultValuesConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SpringA2AConfigProvider}.
 *
 * @author Christian Tzolov
 */
@ExtendWith(MockitoExtension.class)
class SpringA2AConfigProviderTest {

	private MockEnvironment environment;

	@Mock
	private DefaultValuesConfigProvider defaultValues;

	private SpringA2AConfigProvider provider;

	@BeforeEach
	void setUp() {
		this.environment = new MockEnvironment();
		this.provider = new SpringA2AConfigProvider(this.environment, this.defaultValues);
	}

	@Test
	void getValueReturnsFromEnvironmentWhenPresent() {
		this.environment.setProperty("a2a.blocking.agent.timeout.seconds", "60");

		assertThat(this.provider.getValue("a2a.blocking.agent.timeout.seconds")).isEqualTo("60");
	}

	@Test
	void getValueFallsBackToDefaultWhenNotInEnvironment() {
		when(this.defaultValues.getValue("a2a.blocking.agent.timeout.seconds")).thenReturn("30");

		assertThat(this.provider.getValue("a2a.blocking.agent.timeout.seconds")).isEqualTo("30");
	}

	@Test
	void getOptionalValueReturnsFromEnvironmentWhenPresent() {
		this.environment.setProperty("custom.property", "custom-value");

		assertThat(this.provider.getOptionalValue("custom.property")).hasValue("custom-value");
	}

	@Test
	void getOptionalValueFallsBackToDefaultWhenNotInEnvironment() {
		when(this.defaultValues.getOptionalValue("a2a.executor.core-pool-size")).thenReturn(Optional.of("5"));

		assertThat(this.provider.getOptionalValue("a2a.executor.core-pool-size")).hasValue("5");
	}

	@Test
	void getOptionalValueReturnsEmptyForUnknownProperty() {
		when(this.defaultValues.getOptionalValue("unknown.property")).thenReturn(Optional.empty());

		assertThat(this.provider.getOptionalValue("unknown.property")).isEmpty();
	}

	@Test
	void environmentTakesPrecedenceOverDefaults() {
		this.environment.setProperty("a2a.executor.max-pool-size", "100");

		assertThat(this.provider.getValue("a2a.executor.max-pool-size")).isEqualTo("100");
	}

}
