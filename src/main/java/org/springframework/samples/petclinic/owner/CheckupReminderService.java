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

import org.springframework.stereotype.Service;

/**
 * Sends a pet's owner a text reminding them that the pet is due for its annual check-up.
 * <p>
 * Not transactional on purpose: the owner is loaded in the repository's own short
 * transaction, so no database connection is held while the SMS provider responds.
 */
@Service
public class CheckupReminderService {

	private final OwnerRepository owners;

	private final SmsGateway smsGateway;

	public CheckupReminderService(OwnerRepository owners, SmsGateway smsGateway) {
		this.owners = owners;
		this.smsGateway = smsGateway;
	}

	public void remind(int ownerId, int petId) {
		Owner owner = this.owners.findById(ownerId)
			.orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + ownerId));
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new IllegalArgumentException("Pet with id " + petId + " not found for owner with id " + ownerId);
		}
		String text = "Hi " + owner.getFirstName() + ", " + pet.getName()
				+ " is due for an annual check-up. Book a visit at PetClinic.";
		this.smsGateway.send(owner.getTelephone(), text);
	}

}
