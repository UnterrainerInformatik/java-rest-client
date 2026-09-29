package info.unterrainer.commons.restclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import info.unterrainer.commons.restclient.exceptions.RestClientException;
import info.unterrainer.commons.restclient.jsons.MessageJson;
import info.unterrainer.commons.serialization.jsonmapper.JsonMapper;

/**
 * Covers how a {@link KeycloakContext} obtains, reuses and replaces its token.
 * Keycloak and the resource server are both played by a
 * {@link KeycloakTestServer} on loopback, so nothing here needs the network
 * (unlike the manual smoke tests in {@link KeycloakContextTests}).
 */
public class KeycloakTokenTests {

	private KeycloakTestServer server;
	private RestClient restClient;
	private KeycloakContext kcc;
	private WarningCapture warnings;

	@BeforeEach
	public void before() throws IOException {
		server = new KeycloakTestServer();
		restClient = new RestClient(JsonMapper.create(), 2000L, 2000L, 2000L);
		kcc = server.context();
		warnings = WarningCapture.attach();
	}

	@AfterEach
	public void after() {
		warnings.detach();
		server.close();
	}

	@Test
	public void firstCallFetchesATokenAndCarriesIt() {
		MessageJson response = kcc.<MessageJson>get(restClient, MessageJson.class)
				.addUrl(server.resourceUrl())
				.execute();

		assertEquals("ok", response.getMessage());
		assertEquals(1, server.grants());
		assertEquals(List.of("token-1"), server.receivedTokens());
	}

	@Test
	public void validTokenIsReused() {
		kcc.<MessageJson>get(restClient, MessageJson.class).addUrl(server.resourceUrl()).execute();
		kcc.<MessageJson>get(restClient, MessageJson.class).addUrl(server.resourceUrl()).execute();

		assertEquals(1, server.grants(), "a token that has not expired must be reused");
		assertEquals(List.of("token-1", "token-1"), server.receivedTokens());
	}

	@Test
	public void refusedTokenIsReplacedAndTheCallRepeated() {
		server.refuse("token-1");

		MessageJson response = kcc.<MessageJson>post(restClient, MessageJson.class)
				.addUrl(server.resourceUrl())
				.body("{}")
				.execute();

		assertEquals("ok", response.getMessage());
		assertEquals(2, server.grants(), "the refused token must be replaced");
		assertEquals(List.of("token-1", "token-2"), server.receivedTokens());
		assertTrue(warnings.any(server.resourceUrl()), "the cured 401 must be logged at warn with the URL");
	}

	@Test
	public void secondRefusalReachesTheCaller() {
		server.refuse("token-1", "token-2");

		RestClientException e = assertThrows(RestClientException.class,
				() -> kcc.<MessageJson>post(restClient, MessageJson.class)
						.addUrl(server.resourceUrl())
						.body("{}")
						.execute());

		assertEquals(401, e.getStatusCode());
		assertEquals(2, server.resourceCalls(), "the call must be repeated exactly once");
		assertEquals(2, server.grants());
	}

	@ParameterizedTest
	@ValueSource(ints = { 403, 500 })
	public void otherRefusalsAreNotRepeated(final int status) {
		server.answerWith(status);

		RestClientException e = assertThrows(RestClientException.class,
				() -> kcc.<MessageJson>get(restClient, MessageJson.class).addUrl(server.resourceUrl()).execute());

		assertEquals(status, e.getStatusCode());
		assertEquals(1, server.resourceCalls());
		assertEquals(1, server.grants());
	}

	@Test
	public void invalidatedTokenIsNotSentAgain() {
		kcc.<MessageJson>get(restClient, MessageJson.class).addUrl(server.resourceUrl()).execute();

		kcc.invalidate();
		kcc.<MessageJson>get(restClient, MessageJson.class).addUrl(server.resourceUrl()).execute();

		assertEquals(2, server.grants());
		assertEquals(List.of("token-1", "token-2"), server.receivedTokens());
	}

	enum Method {
		GET((c, k) -> new GetKeycloakBuilder<MessageJson>(c, MessageJson.class, k)),
		POST((c, k) -> new PostKeycloakBuilder<MessageJson>(c, MessageJson.class, k).body("{}")),
		PUT((c, k) -> new PutKeycloakBuilder<MessageJson>(c, MessageJson.class, k).body("{}")),
		DEL((c, k) -> new DelKeycloakBuilder<MessageJson>(c, MessageJson.class, k));

		private final BiFunction<RestClient, KeycloakContext, BaseBuilder<MessageJson, ?>> builder;

		Method(final BiFunction<RestClient, KeycloakContext, BaseBuilder<MessageJson, ?>> builder) {
			this.builder = builder;
		}
	}

	@ParameterizedTest
	@EnumSource(Method.class)
	public void everyMethodRecoversFromARefusedToken(final Method method) {
		server.refuse("token-1");

		BaseBuilder<MessageJson, ?> builder = method.builder.apply(restClient, kcc);
		builder.addUrl(server.resourceUrl());
		MessageJson response = builder.execute();

		assertEquals("ok", response.getMessage());
		assertEquals(List.of("token-1", "token-2"), server.receivedTokens());
	}

	/**
	 * Collects the warnings {@link KeycloakContext} logs while a test runs.
	 */
	private static class WarningCapture extends AbstractAppender {

		private final List<String> messages = new CopyOnWriteArrayList<>();
		private Logger logger;

		private WarningCapture() {
			super("warning-capture", null, null, true, Property.EMPTY_ARRAY);
		}

		static WarningCapture attach() {
			WarningCapture capture = new WarningCapture();
			capture.start();
			LoggerContext context = (LoggerContext) LogManager.getContext(false);
			capture.logger = context.getLogger(KeycloakContext.class.getName());
			capture.logger.addAppender(capture);
			return capture;
		}

		void detach() {
			logger.removeAppender(this);
			stop();
		}

		boolean any(final String containing) {
			return messages.stream().anyMatch(m -> m.contains(containing));
		}

		@Override
		public void append(final LogEvent event) {
			if (event.getLevel() == Level.WARN)
				messages.add(event.getMessage().getFormattedMessage());
		}
	}
}
