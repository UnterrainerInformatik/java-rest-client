package info.unterrainer.commons.restclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import info.unterrainer.commons.restclient.jsons.MessageJson;
import info.unterrainer.commons.serialization.jsonmapper.JsonMapper;

/**
 * Covers which HTTP method each builder puts on the wire, plain and keycloak
 * alike. The resource server is a {@link KeycloakTestServer} on loopback, which
 * records the method of every call.
 */
public class HttpMethodTests {

	private KeycloakTestServer server;
	private RestClient restClient;
	private KeycloakContext kcc;

	@BeforeEach
	public void before() throws IOException {
		server = new KeycloakTestServer();
		restClient = new RestClient(JsonMapper.create(), 2000L, 2000L, 2000L);
		kcc = server.context();
	}

	@AfterEach
	public void after() {
		server.close();
	}

	enum Builder {
		PLAIN_GET("GET", (c, k) -> new GetBuilder<MessageJson>(c, MessageJson.class)),
		PLAIN_POST("POST", (c, k) -> new PostBuilder<MessageJson>(c, MessageJson.class).body("{}")),
		PLAIN_PUT("PUT", (c, k) -> new PutBuilder<MessageJson>(c, MessageJson.class).body("{}")),
		PLAIN_DELETE("DELETE", (c, k) -> new DelBuilder<MessageJson>(c, MessageJson.class)),
		KEYCLOAK_GET("GET", (c, k) -> new GetKeycloakBuilder<MessageJson>(c, MessageJson.class, k)),
		KEYCLOAK_POST("POST", (c, k) -> new PostKeycloakBuilder<MessageJson>(c, MessageJson.class, k).body("{}")),
		KEYCLOAK_PUT("PUT", (c, k) -> new PutKeycloakBuilder<MessageJson>(c, MessageJson.class, k).body("{}")),
		KEYCLOAK_DELETE("DELETE", (c, k) -> new DelKeycloakBuilder<MessageJson>(c, MessageJson.class, k));

		private final String method;
		private final BiFunction<RestClient, KeycloakContext, BaseBuilder<MessageJson, ?>> builder;

		Builder(final String method,
				final BiFunction<RestClient, KeycloakContext, BaseBuilder<MessageJson, ?>> builder) {
			this.method = method;
			this.builder = builder;
		}

		boolean isKeycloak() {
			return name().startsWith("KEYCLOAK_");
		}
	}

	@ParameterizedTest
	@EnumSource(Builder.class)
	public void everyBuilderSendsItsMethod(final Builder builder) {
		BaseBuilder<MessageJson, ?> b = builder.builder.apply(restClient, kcc);
		b.addUrl(server.resourceUrl());
		MessageJson response = b.execute();

		assertEquals("ok", response.getMessage());
		assertEquals(List.of(builder.method), server.receivedMethods());
		if (builder.isKeycloak())
			assertEquals(List.of("token-1"), server.receivedTokens());
	}

	@Test
	public void deleteRepeatedAfterARefusedTokenIsStillADelete() {
		server.refuse("token-1");

		MessageJson response = new DelKeycloakBuilder<MessageJson>(restClient, MessageJson.class, kcc)
				.addUrl(server.resourceUrl())
				.execute();

		assertEquals("ok", response.getMessage());
		assertEquals(List.of("token-1", "token-2"), server.receivedTokens());
		assertEquals(List.of("DELETE", "DELETE"), server.receivedMethods());
	}
}
