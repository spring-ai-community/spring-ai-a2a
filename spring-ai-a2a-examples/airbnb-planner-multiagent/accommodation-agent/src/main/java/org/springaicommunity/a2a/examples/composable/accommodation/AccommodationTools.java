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

package org.springaicommunity.a2a.examples.composable.accommodation;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Accommodation Tools - Mock implementation for accommodation search.
 *
 * @author Ilayaperumal Gopinathan
 */
@Component
public class AccommodationTools {

	@Tool(description = "Search for accommodation in a location")
	public String searchAccommodation(
			@ToolParam(description = "The city and state, e.g. Paris, France") String location,
			@ToolParam(description = "Check-in date (YYYY-MM-DD)") String checkIn,
			@ToolParam(description = "Check-out date (YYYY-MM-DD)") String checkOut,
			@ToolParam(description = "Number of guests") int guests) {
		// Mock implementation - replace with real API call
		return String.format("""
			Found 3 accommodations in %s (%s to %s, %d guests):

			1. **Le Grand Hotel Paris**
			   - Price: €250/night
			   - Rating: 4.5/5
			   - Location: Near Eiffel Tower
			   - Link: https://example.com/hotel1

			2. **Cozy Apartment Marais**
			   - Price: €150/night
			   - Rating: 4.3/5
			   - Location: Le Marais district
			   - Link: https://example.com/hotel2

			3. **Budget Hostel Central**
			   - Price: €50/night
			   - Rating: 4.0/5
			   - Location: Latin Quarter
			   - Link: https://example.com/hotel3
			""", location, checkIn, checkOut, guests);
	}

}
