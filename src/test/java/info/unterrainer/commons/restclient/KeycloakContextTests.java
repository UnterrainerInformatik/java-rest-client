package info.unterrainer.commons.restclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import info.unterrainer.commons.restclient.jsons.EliteUserJson;
import info.unterrainer.commons.restclient.jsons.ListJson;
import info.unterrainer.commons.serialization.JsonMapper;

public class KeycloakContextTests {

	private JsonMapper jsonMapper;
	private RestClient restClient;
	private KeycloakContext kcc;

	@BeforeEach
	public void BeforeAll() {
		jsonMapper = JsonMapper.create();
		kcc = new KeycloakContext("https://keycloak.lan.elite-zettl.at/auth/realms/Cms/protocol/openid-connect/token",
				"gerald.unterrainer@cms-building.at", "9BZOx5EBJRjN4azmkhhA", "CMS", null);
		restClient = new RestClient(jsonMapper);
	}

	@Test
	public void listJsonDeserializationWorks() {
		ListJson<EliteUserJson> users = kcc.<ListJson<EliteUserJson>>get(restClient, EliteUserJson.class)
				.isListJson()
				.addUrl("https://elite-server.lan.elite-zettl.at")
				.addUrl("users")
				.execute();

		System.out.println(users);
	}
}
