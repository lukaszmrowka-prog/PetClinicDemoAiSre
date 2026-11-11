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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RenewalQuoteServiceTests {

	@Mock
	private OwnerRepository owners;

	@Mock
	private InsurerClient insurer;

	private RenewalQuoteService service;

	@BeforeEach
	void setup() {
		this.service = new RenewalQuoteService(this.owners, this.insurer);
	}

	private static Owner george() {
		Owner george = new Owner();
		george.setId(1);
		george.setFirstName("George");
		george.setLastName("Franklin");
		Pet leo = new Pet();
		leo.setName("Leo");
		PetType cat = new PetType();
		cat.setName("cat");
		leo.setType(cat);
		leo.setBirthDate(LocalDate.now().minusYears(3).minusDays(1));
		Visit recent = new Visit();
		recent.setDate(LocalDate.now().minusMonths(2));
		leo.addVisit(recent);
		Visit old = new Visit();
		old.setDate(LocalDate.now().minusYears(2));
		leo.addVisit(old);
		george.addPet(leo);
		leo.setId(1);
		return george;
	}

	@Test
	void quotesBySpeciesAgeAndRecentVisits() {
		given(this.owners.findById(1)).willReturn(Optional.of(george()));
		given(this.insurer.quoteAnnualPremium("cat", 3, 1)).willReturn(new BigDecimal("229.43"));

		assertThat(this.service.requestQuote(1, 1)).isCompletedWithValue(new BigDecimal("229.43"));
	}

	@Test
	void doesNotAskForAQuoteForAnUnknownPet() {
		given(this.owners.findById(1)).willReturn(Optional.of(george()));

		assertThatIllegalArgumentException().isThrownBy(() -> this.service.requestQuote(1, 42));
		verifyNoInteractions(this.insurer);
	}

}
