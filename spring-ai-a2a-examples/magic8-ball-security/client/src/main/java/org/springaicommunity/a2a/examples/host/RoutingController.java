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
package org.springaicommunity.a2a.examples.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the routing agent.
 * Accepts user queries and routes them to appropriate remote A2A agents.
 *
 * @author Christian Tzolov
 */
@RestController
public class RoutingController {

	private static final Logger logger = LoggerFactory.getLogger(RoutingController.class);

	private final ChatClient chatClient;

	public RoutingController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	@PostMapping("/chat")
	public String chat(@RequestBody String userMessage) {
		logger.info("Received user message: {}", userMessage);

		String response = this.chatClient.prompt().user(userMessage).call().content();

		logger.info("Response: {}", response);
		return response;
	}

}
