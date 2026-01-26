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
package org.springaicommunity.a2a.examples.magic8ball.server;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class Magic8BallTools {

	/** All possible Magic 8 Ball responses. */
	private static final String[] RESPONSES = { // @formatter:off
		// Positive responses (10)
		"It is certain",
		"It is decidedly so",
		"Without a doubt",
		"Yes definitely",
		"You may rely on it",
		"As I see it, yes",
		"Most likely",
		"Outlook good",
		"Yes",
		"Signs point to yes",

		// Negative responses (5)
		"Don't count on it",
		"My reply is no",
		"My sources say no",
		"Outlook not so good",
		"Very doubtful",

		// Non-committal responses (5)
		"Better not tell you now",
		"Cannot predict now",
		"Concentrate and ask again",
		"Ask again later",
		"Reply hazy, try again"
	}; // @formatter:on

	@Tool(description = "Get the response to the user's question from the Magic 8 Ball")
	public String shakeMagic8Ball(String question) {
		int index = ThreadLocalRandom.current().nextInt(RESPONSES.length);
		String response = RESPONSES[index];
		System.out
			.println("=== TOOL CALLED === Question: " + question + ", Index: " + index + ", Response: " + response);
		return response;
	}

}
