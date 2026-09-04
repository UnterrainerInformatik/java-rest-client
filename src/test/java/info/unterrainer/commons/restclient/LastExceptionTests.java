package info.unterrainer.commons.restclient;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import info.unterrainer.commons.serialization.jsonmapper.JsonMapper;

/**
 * Covers the transport-failure reporting on {@link BaseBuilder}. Port 1 on
 * loopback is not listening, so the connection is refused immediately — no
 * external host and no waiting for a timeout.
 */
public class LastExceptionTests {

	private static final String UNREACHABLE_URL = "http://127.0.0.1:1/";

	private RestClient restClient;

	@BeforeEach
	public void before() {
		restClient = new RestClient(JsonMapper.create(), 500L, 500L, 500L);
	}

	@Test
	public void transportFailureIsReported() {
		var builder = restClient.<String>get(String.class).addUrl(UNREACHABLE_URL);

		String response = builder.execute();

		assertNull(response, "a refused connection must still answer null");
		assertNotNull(builder.getLastException(), "the swallowed transport failure must be retrievable");
		assertInstanceOf(IOException.class, builder.getLastException());
	}

	@Test
	public void executeResetsThePreviousFailure() {
		var builder = restClient.<String>get(String.class).addUrl(UNREACHABLE_URL);

		builder.execute();
		assertNotNull(builder.getLastException());

		builder.execute();
		assertNotNull(builder.getLastException(), "a second failing call reports its own failure");
	}

	@Test
	public void freshBuilderReportsNoFailureBeforeExecute() {
		var builder = restClient.<String>get(String.class).addUrl(UNREACHABLE_URL);

		assertNull(builder.getLastException(), "nothing has been executed yet");
	}

	@Test
	public void buildersDoNotShareFailureState() {
		var failing = restClient.<String>get(String.class).addUrl(UNREACHABLE_URL);
		var untouched = restClient.<String>get(String.class).addUrl(UNREACHABLE_URL);

		failing.execute();

		assertNotNull(failing.getLastException());
		assertNull(untouched.getLastException(), "each call gets its own builder and its own failure state");
	}
}
