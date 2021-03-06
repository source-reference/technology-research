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

package com.study.consul.config;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.study.consul.properties.ConsulConfigProperties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

import static com.study.consul.properties.ConsulConfigProperties.Format.PROPERTIES;
import static com.study.consul.properties.ConsulConfigProperties.Format.YAML;
import static org.springframework.util.Base64Utils.decodeFromString;

public class ConsulPropertySource extends EnumerablePropertySource<ConsulClient> {

	private final Map<String, Object> properties = new LinkedHashMap<>();

	private String context;

	private ConsulConfigProperties configProperties;

	private Long initialIndex;

	public ConsulPropertySource(String context, ConsulClient source, ConsulConfigProperties configProperties) {
		super(context, source);
		this.context = context;
		this.configProperties = configProperties;

	}

	public void init() {
		Response<List<GetValue>> response = this.source.getKVValues(this.context, this.configProperties.getAclToken(),
				QueryParams.DEFAULT);

		this.initialIndex = response.getConsulIndex();

		final List<GetValue> values = response.getValue();
		ConsulConfigProperties.Format format = this.configProperties.getFormat();
		switch (format) {
		case PROPERTIES:
		case YAML:
			parsePropertiesWithNonKeyValueFormat(values, format);
		}
	}

	public Long getInitialIndex() {
		return this.initialIndex;
	}

	/**
	 * Parses the properties using the format which is not a key value style i.e., either
	 * java properties style or YAML style.
	 * @param values values to parse
	 * @param format format in which the values should be parsed
	 */
	protected void parsePropertiesWithNonKeyValueFormat(List<GetValue> values, ConsulConfigProperties.Format format) {
		if (values == null) {
			return;
		}

		for (GetValue getValue : values) {
			parseValue(getValue, format);
		}
	}

	protected void parseValue(GetValue getValue, ConsulConfigProperties.Format format) {
		String value = getValue.getDecodedValue();
		if (value == null) {
			return;
		}

		Properties props = generateProperties(value, format);

		for (Map.Entry entry : props.entrySet()) {
			this.properties.put(entry.getKey().toString(), entry.getValue());
		}
	}

	protected Properties generateProperties(String value, ConsulConfigProperties.Format format) {
		final Properties props = new Properties();

		if (format == PROPERTIES) {
			try {
				// Must use the ISO-8859-1 encoding because Properties.load(stream)
				// expects it.
				props.load(new ByteArrayInputStream(value.getBytes("ISO-8859-1")));
			}
			catch (IOException e) {
				throw new IllegalArgumentException(value + " can't be encoded using ISO-8859-1");
			}

			return props;
		}
		else if (format == YAML) {
			final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
			yaml.setResources(new ByteArrayResource(value.getBytes(Charset.forName("UTF-8"))));

			return yaml.getObject();
		}

		return props;
	}

	protected Map<String, Object> getProperties() {
		return this.properties;
	}

	protected ConsulConfigProperties getConfigProperties() {
		return this.configProperties;
	}

	protected String getContext() {
		return this.context;
	}

	@Override
	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> strings = this.properties.keySet();
		return strings.toArray(new String[strings.size()]);
	}

}
