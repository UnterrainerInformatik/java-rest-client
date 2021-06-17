package info.unterrainer.commons.restclient;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import info.unterrainer.commons.restclient.jsons.EliteUserJson;
import info.unterrainer.commons.serialization.JsonMapper;

public class KeycloakContextTests {

	private JsonMapper jsonMapper;
	private RestClient restClient;
	private KeycloakContext kcc;

	@BeforeAll
	public void BeforeAll() {
		jsonMapper = JsonMapper.create()
				kcc = new KeycloakContext("https://keycloak.lan.elite-zettl.at/auth/realms/Cms/protocol/openid-connect/token",
						"gerald.unterrainer@cms-building.at", "9BZOx5EBJRjN4azmkhhA", "CMS", null);
				restClient = new RestClient(jsonMapper);
	}

	@Test
	public void correctCredentialsConnect() {

		List<EliteUserJson> users = kcc.get(restClient)
				.url("https://elite-server.lan.elite-zettl.at/users")
				.type(List<EliteUserJson>.class)
				.execute();

		System.out.println(kcc);
	}
}
