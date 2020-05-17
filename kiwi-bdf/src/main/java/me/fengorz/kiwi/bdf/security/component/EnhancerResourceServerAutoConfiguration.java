/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.bdf.security.component;

import lombok.SneakyThrows;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * @author lengleng
 * @date 2019/03/08
 */
@ComponentScan("me.fengorz.kiwi.bdf.security")
public class EnhancerResourceServerAutoConfiguration {
	@Bean
	@Primary
	@LoadBalanced
	public RestTemplate lbRestTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			@SneakyThrows
			public void handleError(ClientHttpResponse response) {
				if (response.getRawStatusCode() != HttpStatus.BAD_REQUEST.value()) {
					super.handleError(response);
				}
			}
		});
		return restTemplate;
	}
}
