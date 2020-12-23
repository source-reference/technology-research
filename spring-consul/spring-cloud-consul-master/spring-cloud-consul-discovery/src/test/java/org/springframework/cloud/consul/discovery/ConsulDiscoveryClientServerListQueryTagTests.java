/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.consul.discovery;

import java.util.Collections;
import java.util.List;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.consul.test.ConsulTestcontainers;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.cloud.consul.discovery.ConsulDiscoveryClientServerListQueryTagTests.NAME;

/**
 * Integration test to verify the
 * {@link ConsulDiscoveryProperties#getServerListQueryTags()} is respected.
 *
 * @author Chris Bono
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = MOCK, classes = ConsulDiscoveryClientServerListQueryTagTests.TestConfig.class,
		properties = { "spring.application.name=" + NAME,
				"spring.cloud.consul.discovery.catalogServicesWatch.enabled=false",
				"spring.cloud.consul.discovery.server-list-query-tags[" + NAME + "]=uat",
				"spring.cloud.consul.discovery.defaultQueryTag=intg" })
@DirtiesContext
@ContextConfiguration(initializers = ConsulTestcontainers.class)
public class ConsulDiscoveryClientServerListQueryTagTests {

	public static final String NAME = "consulServiceServerListQueryTags";

	@Autowired
	private ConsulDiscoveryClient discoveryClient;

	@Autowired
	private ConsulClient consulClient;

	private NewService intgService = serviceForEnvironment("intg", 9081);

	private NewService uatService = serviceForEnvironment("uat", 9080);

	@Before
	public void setUp() {
		consulClient.agentServiceRegister(intgService);
		consulClient.agentServiceRegister(uatService);
	}

	@After
	public void tearDown() {
		consulClient.agentServiceDeregister(intgService.getId());
		consulClient.agentServiceDeregister(uatService.getId());
	}

	@Test
	public void shouldReturnInstanceWithMatchingServerListQueryTags() {
		List<ServiceInstance> instances = discoveryClient.getInstances(NAME);
		assertThat(instances).as("instances was wrong size").hasSize(1);
		ServiceInstance serviceInstance = instances.get(0);
		assertThat(serviceInstance.getPort()).isEqualTo(uatService.getPort());
		assertThat(serviceInstance.getServiceId()).isEqualTo(uatService.getName());
		assertThat(serviceInstance.getInstanceId()).isEqualTo(uatService.getId());
	}

	private NewService serviceForEnvironment(String env, int port) {
		NewService service = new NewService();
		service.setAddress("localhost");
		service.setId(NAME + env);
		service.setName(NAME);
		service.setPort(port);
		service.setTags(Collections.singletonList(env));
		return service;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@Import({ ConsulDiscoveryClientConfiguration.class })
	protected static class TestConfig {

	}

}
