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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically asks the insurance partner for next year's premium for every pet, so the
 * clinic can offer owners a renewal.
 */
@Component
class RenewalQuoteJob {

	private static final Logger logger = LoggerFactory.getLogger(RenewalQuoteJob.class);

	private final OwnerRepository owners;

	private final RenewalQuoteService quoteService;

	RenewalQuoteJob(OwnerRepository owners, RenewalQuoteService quoteService) {
		this.owners = owners;
		this.quoteService = quoteService;
	}

	@Scheduled(initialDelayString = "${petclinic.insurance.initial-delay:PT45S}",
			fixedDelayString = "${petclinic.insurance.interval:PT2M}")
	void requestRenewalQuotes() {
		long started = System.nanoTime();
		Map<String, CompletableFuture<BigDecimal>> quotes = new LinkedHashMap<>();
		for (Owner owner : this.owners.findAll()) {
			for (Pet pet : owner.getPets()) {
				quotes.put(pet.getName() + " (owner " + owner.getId() + ")",
						this.quoteService.requestQuote(owner.getId(), pet.getId()));
			}
		}
		logger.info("Renewal quotes: requested {} quote(s)", quotes.size());

		int received = 0;
		int failed = 0;
		for (Map.Entry<String, CompletableFuture<BigDecimal>> quote : quotes.entrySet()) {
			try {
				quote.getValue().join();
				received++;
			}
			catch (CompletionException ex) {
				failed++;
				logger.warn("Renewal quote for {} failed", quote.getKey(), ex.getCause());
			}
		}
		logger.info("Renewal quotes: received {}, failed {} in {} ms", received, failed,
				(System.nanoTime() - started) / 1_000_000);
	}

}
