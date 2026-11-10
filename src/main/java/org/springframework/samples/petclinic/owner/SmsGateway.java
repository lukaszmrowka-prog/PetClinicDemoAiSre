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

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Client for the SMS provider used for owner notifications.
 * <p>
 * The demo has no provider account, so a send waits for the round-trip time the provider
 * typically needs to accept a single message.
 */
@Component
public class SmsGateway {

	private static final Logger logger = LoggerFactory.getLogger(SmsGateway.class);

	private static final Duration MIN_ROUND_TRIP = Duration.ofSeconds(4);

	private static final Duration MAX_ROUND_TRIP = Duration.ofSeconds(8);

	public void send(String telephone, String text) {
		long roundTrip = ThreadLocalRandom.current().nextLong(MIN_ROUND_TRIP.toMillis(), MAX_ROUND_TRIP.toMillis() + 1);
		try {
			Thread.sleep(roundTrip);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("SMS to " + telephone + " was interrupted", ex);
		}
		logger.info("SMS accepted by provider for {} after {} ms: {}", telephone, roundTrip, text);
	}

}
