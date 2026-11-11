/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.samples.petclinic.owner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

/**
 * Client for the pet insurance partner's quoting API.
 * <p>
 * The demo has no partner account, so a request waits for the time the partner typically
 * needs to price a policy and the premium is estimated locally.
 */
@Component
public class InsurerClient {

	private static final Duration MIN_PRICING_TIME = Duration.ofSeconds(5);

	private static final Duration MAX_PRICING_TIME = Duration.ofSeconds(9);

	private static final BigDecimal BASE_PREMIUM = new BigDecimal("180.00");

	public BigDecimal quoteAnnualPremium(String species, int ageInYears, int visitsLastYear) {
		long pricingTime = ThreadLocalRandom.current()
			.nextLong(MIN_PRICING_TIME.toMillis(), MAX_PRICING_TIME.toMillis() + 1);
		try {
			Thread.sleep(pricingTime);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Quote request for a " + species + " was interrupted", ex);
		}
		BigDecimal ageLoading = BigDecimal.valueOf(1 + ageInYears * 0.08);
		BigDecimal claimsLoading = BigDecimal.valueOf(1 + visitsLastYear * 0.15);
		return BASE_PREMIUM.multiply(ageLoading).multiply(claimsLoading).setScale(2, RoundingMode.HALF_UP);
	}

}
