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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckupReminderServiceTests {

	@Mock
	private OwnerRepository owners;

	@Mock
	private SmsGateway smsGateway;

	private CheckupReminderService service;

	@BeforeEach
	void setup() {
		this.service = new CheckupReminderService(this.owners, this.smsGateway);
	}

	private static Owner george() {
		Owner george = new Owner();
		george.setId(1);
		george.setFirstName("George");
		george.setTelephone("6085551023");
		Pet leo = new Pet();
		leo.setName("Leo");
		george.addPet(leo);
		leo.setId(1);
		return george;
	}

	@Test
	void textsTheOwnerAboutTheirPet() {
		given(this.owners.findById(1)).willReturn(Optional.of(george()));

		this.service.remind(1, 1);

		verify(this.smsGateway).send(eq("6085551023"), contains("Leo is due for an annual check-up"));
	}

	@Test
	void sendsNothingForAnUnknownPet() {
		given(this.owners.findById(1)).willReturn(Optional.of(george()));

		assertThatIllegalArgumentException().isThrownBy(() -> this.service.remind(1, 42));
		verifyNoInteractions(this.smsGateway);
	}

	@Test
	void petsWithoutARecentVisitAreDue() {
		LocalDate today = LocalDate.of(2026, 6, 1);
		Pet seenLastMonth = new Pet();
		Visit recent = new Visit();
		recent.setDate(today.minusMonths(1));
		seenLastMonth.addVisit(recent);
		Pet seenTwoYearsAgo = new Pet();
		Visit old = new Visit();
		old.setDate(today.minusYears(2));
		seenTwoYearsAgo.addVisit(old);

		assertThat(CheckupReminderJob.isDue(seenLastMonth, today.minusYears(1))).isFalse();
		assertThat(CheckupReminderJob.isDue(seenTwoYearsAgo, today.minusYears(1))).isTrue();
		assertThat(CheckupReminderJob.isDue(new Pet(), today.minusYears(1))).isTrue();
	}

}
