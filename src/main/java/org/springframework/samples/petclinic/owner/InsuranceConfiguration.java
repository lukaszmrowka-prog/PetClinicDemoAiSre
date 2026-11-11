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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Infrastructure for talking to the pet insurance partner.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
class InsuranceConfiguration {

	static final String QUOTE_EXECUTOR = "insuranceQuoteExecutor";

	/**
	 * The partner takes several seconds to price a policy and allows up to a dozen
	 * concurrent requests per client, so quotes are requested in parallel up to that
	 * limit.
	 */
	private static final int PARTNER_CONCURRENCY_LIMIT = 12;

	@Bean(QUOTE_EXECUTOR)
	ThreadPoolTaskExecutor insuranceQuoteExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(PARTNER_CONCURRENCY_LIMIT);
		executor.setMaxPoolSize(PARTNER_CONCURRENCY_LIMIT);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("insurance-quote-");
		return executor;
	}

}
