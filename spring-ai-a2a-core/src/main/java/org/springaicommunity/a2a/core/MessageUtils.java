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

package org.springaicommunity.a2a.core;

import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;

import java.util.List;

/**
 * Utility class for working with A2A SDK Message objects.
 *
 * <p>This class provides convenience methods for creating and extracting content from
 * A2A SDK {@link Message} objects used throughout Spring AI A2A integration.
 *
 * <p><strong>Factory methods:</strong>
 * <ul>
 *   <li>{@link #assistantMessage(List)} - Create an assistant response message</li>
 * </ul>
 *
 * <p><strong>Extraction methods:</strong>
 * <ul>
 *   <li>{@link #extractText(Message)} - Extract text content from a message</li>
 *   <li>{@link #extractText(List)} - Extract text content from parts</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
public final class MessageUtils {

	private MessageUtils() {
		// Utility class - prevent instantiation
	}

	/**
	 * Create an ASSISTANT/AGENT role message from parts.
	 * @param parts the message parts
	 * @return a new Message with ASSISTANT role
	 */
	public static Message assistantMessage(List<Part<?>> parts) {
		return Message.builder()
			.role(Message.Role.AGENT)
			.parts(parts)
			.build();
	}

	/**
	 * Extract text content from a Message.
	 * <p>
	 * This method concatenates all TextPart content from the message, ignoring
	 * other part types. Returns empty string if no text parts are found.
	 * @param message the A2A message
	 * @return the concatenated text content, or empty string if no text parts
	 */
	public static String extractText(Message message) {
		if (message == null || message.parts() == null) {
			return "";
		}
		return extractText(message.parts());
	}

	/**
	 * Extract text content from message parts.
	 * <p>
	 * This method concatenates all TextPart content from the parts list, ignoring
	 * other part types. Returns empty string if no text parts are found.
	 * @param parts the list of A2A parts
	 * @return the concatenated text content, or empty string if no text parts
	 */
	public static String extractText(List<Part<?>> parts) {
		if (parts == null || parts.isEmpty()) {
			return "";
		}

		StringBuilder text = new StringBuilder();
		for (Part<?> part : parts) {
			if (part instanceof TextPart textPart) {
				text.append(textPart.text());
			}
		}
		return text.toString();
	}

}
