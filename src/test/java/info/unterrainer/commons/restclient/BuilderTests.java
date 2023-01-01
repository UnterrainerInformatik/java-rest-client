package info.unterrainer.commons.restclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import info.unterrainer.commons.restclient.jsons.MessageJson;
import info.unterrainer.commons.serialization.jsonmapper.JsonMapper;

public class BuilderTests {

	private JsonMapper jsonMapper;
	private RestClient restClient;

	@BeforeEach
	public void BeforeAll() {
		jsonMapper = JsonMapper.create();
		restClient = new RestClient(jsonMapper);
	}

	@Test
	public void correctCredentialsConnect() {
		String response = restClient.<String>get(String.class)
				.addUrl("https://elite-server.lan.elite-zettl.at/")
				.execute();

		System.out.println(response);
	}

	@Test
	public void jsonDeserializationWorks() {
		MessageJson response = restClient.<MessageJson>get(MessageJson.class)
				.addUrl("https://elite-server.lan.elite-zettl.at/")
				.execute();

		System.out.println(response);
	}

	@Test
	public void stringGetsStringWithoutSerialization() {
		String response = restClient.<String>get(String.class).addUrl("https://www.google.at/").execute();
		System.out.println(response);
	}
}
