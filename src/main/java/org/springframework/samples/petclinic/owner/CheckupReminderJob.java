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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

/**
 * Periodically texts the owners of pets that have not been seen for a year.
 */
@Component
class CheckupReminderJob {

	private static final Logger logger = LoggerFactory.getLogger(CheckupReminderJob.class);

	/**
	 * Each text takes a few seconds to be accepted by the provider, so reminders are sent
	 * in parallel to finish a batch in seconds rather than minutes.
	 */
	private static final int SENDER_THREADS = 16;

	private final OwnerRepository owners;

	private final CheckupReminderService reminderService;

	private final ExecutorService senders;

	CheckupReminderJob(OwnerRepository owners, CheckupReminderService reminderService) {
		this.owners = owners;
		this.reminderService = reminderService;
		CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("checkup-reminder-");
		threadFactory.setDaemon(true);
		this.senders = Executors.newFixedThreadPool(SENDER_THREADS, threadFactory);
	}

	@Scheduled(initialDelayString = "${petclinic.reminders.initial-delay:PT30S}",
			fixedDelayString = "${petclinic.reminders.interval:PT1M}")
	void sendCheckupReminders() {
		LocalDate cutoff = LocalDate.now().minusYears(1);
		List<Reminder> due = new ArrayList<>();
		for (Owner owner : this.owners.findAll()) {
			for (Pet pet : owner.getPets()) {
				if (isDue(pet, cutoff)) {
					due.add(new Reminder(owner.getId(), pet.getId()));
				}
			}
		}
		logger.info("Check-up reminders: {} pet(s) due", due.size());

		long started = System.nanoTime();
		List<Future<?>> sends = new ArrayList<>();
		for (Reminder reminder : due) {
			sends.add(this.senders.submit(() -> this.reminderService.remind(reminder.ownerId(), reminder.petId())));
		}
		int sent = 0;
		int failed = 0;
		for (int i = 0; i < sends.size(); i++) {
			Reminder reminder = due.get(i);
			try {
				sends.get(i).get();
				sent++;
			}
			catch (ExecutionException ex) {
				failed++;
				logger.warn("Check-up reminder for pet {} of owner {} failed", reminder.petId(), reminder.ownerId(),
						ex.getCause());
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		logger.info("Check-up reminders: sent {}, failed {} in {} ms", sent, failed,
				(System.nanoTime() - started) / 1_000_000);
	}

	static boolean isDue(Pet pet, LocalDate cutoff) {
		return pet.getVisits()
			.stream()
			.map(Visit::getDate)
			.filter(Objects::nonNull)
			.max(Comparator.naturalOrder())
			.map(lastVisit -> lastVisit.isBefore(cutoff))
			.orElse(true);
	}

	@PreDestroy
	void shutdown() {
		this.senders.shutdownNow();
	}

	private record Reminder(int ownerId, int petId) {
	}

}
