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
import java.time.LocalDate;
import java.time.Period;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asks the insurance partner for next year's premium for a pet, based on its species, age
 * and how often it was seen in the last year.
 */
@Service
public class RenewalQuoteService {

	private static final Logger logger = LoggerFactory.getLogger(RenewalQuoteService.class);

	private final OwnerRepository owners;

	private final InsurerClient insurer;

	public RenewalQuoteService(OwnerRepository owners, InsurerClient insurer) {
		this.owners = owners;
		this.insurer = insurer;
	}

	@Async(InsuranceConfiguration.QUOTE_EXECUTOR)
	@Transactional(readOnly = true)
	public CompletableFuture<BigDecimal> requestQuote(int ownerId, int petId) {
		Owner owner = this.owners.findById(ownerId)
			.orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + ownerId));
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new IllegalArgumentException("Pet with id " + petId + " not found for owner with id " + ownerId);
		}
		LocalDate today = LocalDate.now();
		int age = pet.getBirthDate() == null ? 0 : Period.between(pet.getBirthDate(), today).getYears();
		int visitsLastYear = (int) pet.getVisits()
			.stream()
			.filter(visit -> visit.getDate() != null && visit.getDate().isAfter(today.minusYears(1)))
			.count();
		String species = pet.getType() == null ? "pet" : pet.getType().getName();
		BigDecimal premium = this.insurer.quoteAnnualPremium(species, age, visitsLastYear);
		logger.info("Renewal quote for {} {}'s {} {}: {} per year", owner.getFirstName(), owner.getLastName(), species,
				pet.getName(), premium);
		return CompletableFuture.completedFuture(premium);
	}

}
