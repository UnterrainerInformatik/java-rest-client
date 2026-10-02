package info.unterrainer.commons.restclient;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import info.unterrainer.commons.restclient.exceptions.RestClientException;
import info.unterrainer.commons.serialization.jsonmapper.JsonMapper;

/**
 * Covers GET calls for {@code byte[]}: the body must arrive byte for byte,
 * plain and keycloak alike, compressed or not. Other types must still be
 * decoded as text. The server is a {@link KeycloakTestServer} on loopback.
 */
public class BinaryResponseTests {

	private static final String TEXT = "Grüße, 東京 – ok";

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

	private static byte[] allByteValues() {
		byte[] bytes = new byte[256];
		for (int i = 0; i < bytes.length; i++)
			bytes[i] = (byte) i;
		return bytes;
	}

	@Test
	public void everyByteValueSurvivesAPlainGet() {
		server.binaryBody(allByteValues(), "application/octet-stream");

		byte[] response = restClient.<byte[]>get(byte[].class).addUrl(server.binaryUrl()).execute();

		assertArrayEquals(allByteValues(), response);
	}

	@Test
	public void everyByteValueSurvivesAKeycloakGetAfterA401() {
		server.binaryBody(allByteValues(), "application/octet-stream");
		server.refuse("token-1");

		byte[] response = kcc.<byte[]>get(restClient, byte[].class)
				.addUrl(server.binaryUrl())
				.addParam("id", "4711")
				.execute();

		assertArrayEquals(allByteValues(), response);
		assertEquals(List.of("token-1", "token-2"), server.receivedTokens());
		assertEquals(List.of("GET", "GET"), server.receivedMethods());
	}

	@Test
	public void everyByteValueSurvivesACompressedAnswer() {
		server.binaryBody(allByteValues(), "application/octet-stream");
		server.compressBinary();

		byte[] response = restClient.<byte[]>get(byte[].class).addUrl(server.binaryUrl()).execute();

		assertArrayEquals(allByteValues(), response);
	}

	@Test
	public void everyByteValueSurvivesEvenIfDeclaredAsText() {
		server.binaryBody(allByteValues(), "text/plain; charset=utf-8");

		byte[] response = restClient.<byte[]>get(byte[].class).addUrl(server.binaryUrl()).execute();

		assertArrayEquals(allByteValues(), response);
	}

	@Test
	public void anEmptyBodyGivesAnEmptyArray() {
		server.binaryBody(new byte[0], "application/octet-stream");

		var builder = restClient.<byte[]>get(byte[].class).addUrl(server.binaryUrl());
		byte[] response = builder.execute();

		assertNotNull(response, "an empty 2xx body is not a failure");
		assertEquals(0, response.length);
		assertNull(builder.getLastException());
	}

	@Test
	public void aRefusalStillRaisesItsStatus() {
		server.answerWith(404);

		RestClientException e = assertThrows(RestClientException.class,
				() -> restClient.<byte[]>get(byte[].class).addUrl(server.binaryUrl()).execute());

		assertEquals(404, e.getStatusCode());
	}

	@Test
	public void aStringGetIsUnchanged() {
		server.binaryBody(TEXT.getBytes(StandardCharsets.UTF_8), "text/plain; charset=utf-8");

		String response = restClient.<String>get(String.class).addUrl(server.binaryUrl()).execute();

		assertEquals(TEXT, response);
	}

	@Test
	public void aCompressedStringGetIsUnchanged() {
		server.binaryBody(TEXT.getBytes(StandardCharsets.UTF_8), "text/plain");
		server.compressBinary();

		String response = restClient.<String>get(String.class).addUrl(server.binaryUrl()).execute();

		assertEquals(TEXT, response);
	}
}
