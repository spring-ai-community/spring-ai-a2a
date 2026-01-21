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

package org.springaicommunity.a2a.examples.planner;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for travel planning using direct A2A SDK integration.
 *
 * <p><strong>Usage:</strong>
 * <pre>
 * curl -X POST http://localhost:8080/api/travel/plan \
 *   -H 'Content-Type: application/json' \
 *   -d '{
 *     "destination": "Paris",
 *     "startDate": "2024-07-01",
 *     "endDate": "2024-07-05"
 *   }'
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/travel")
public class TravelPlannerController {

	private final TravelPlannerService travelPlannerService;

	public TravelPlannerController(TravelPlannerService travelPlannerService) {
		this.travelPlannerService = travelPlannerService;
	}

	/**
	 * Plan a trip by coordinating with weather and accommodation agents.
	 *
	 * @param request the travel planning request
	 * @return the travel plan response
	 */
	@PostMapping("/plan")
	public ResponseEntity<TravelPlannerService.TravelPlanResponse> planTrip(
			@RequestBody TravelPlannerService.TravelPlanRequest request) {

		TravelPlannerService.TravelPlanResponse response = travelPlannerService.planTrip(request);

		if (response.success()) {
			return ResponseEntity.ok(response);
		}
		else {
			return ResponseEntity.internalServerError().body(response);
		}
	}

}
