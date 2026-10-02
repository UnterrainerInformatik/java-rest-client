package info.unterrainer.commons.restclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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

	private static final long START = 1_000_000L;
	private static final int THREADS = 8;

	private final AtomicLong clock = new AtomicLong(START);
	private KeycloakTestServer server;
	private RestClient restClient;
	private KeycloakContext kcc;
	private WarningCapture warnings;

	@BeforeEach
	public void before() throws IOException {
		server = new KeycloakTestServer();
		restClient = new RestClient(JsonMapper.create(), 2000L, 2000L, 2000L);
		kcc = server.context();
		kcc.clock = clock::get;
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

	@Test
	public void tokenCloseToItsExpiryIsRenewedBeforeItIsSent() {
		get();

		clock.set(START + 271_000L);
		MessageJson response = get();

		assertEquals("ok", response.getMessage());
		assertEquals(2, server.grants(), "a token inside its 30 s renewal margin must be replaced before the call");
		assertEquals(List.of("token-1", "token-2"), server.receivedTokens());
		assertEquals(2, server.resourceCalls(), "the renewed call must not be refused and repeated");
		assertEquals(START + 271_000L + 300_000L, kcc.getRefreshTimestamp(),
				"the refresh timestamp must still be the expiry keycloak reported");
	}

	@Test
	public void shortLivedTokenIsReusedWithinHalfItsLifetime() {
		server.expiresIn(2);
		get();

		clock.set(START + 900L);
		get();

		assertEquals(1, server.grants());
		assertEquals(List.of("token-1", "token-1"), server.receivedTokens());
	}

	@Test
	public void shortLivedTokenIsRenewedAtHalfItsLifetime() {
		server.expiresIn(2);
		get();

		clock.set(START + 1_100L);
		get();

		assertEquals(2, server.grants(), "a 2 s token must be renewed once less than 1 s of it is left");
		assertEquals(List.of("token-1", "token-2"), server.receivedTokens());
	}

	@Test
	public void simultaneousFirstCallsFetchOneToken() throws Exception {
		server.delayTokens(200);

		List<MessageJson> responses = getConcurrently();

		assertEquals(THREADS, responses.size());
		assertEquals(1, server.grants(), "threads that need a token at the same time must share one fetch");
		assertEquals(1, server.maxGrantsInFlight());
		assertEquals(Collections.nCopies(THREADS, "token-1"), server.receivedTokens());
	}

	@Test
	public void simultaneousRefusalsFetchOneNewToken() throws Exception {
		kcc.update(restClient);
		server.refuse("token-1");
		server.holdRefusals();
		server.delayTokens(200);

		List<Future<MessageJson>> futures = startConcurrently();
		server.awaitResourceCalls(THREADS);
		server.releaseRefusals();
		List<MessageJson> responses = collect(futures);

		responses.forEach(r -> assertEquals("ok", r.getMessage()));
		assertEquals(2, server.grants(), "one refused token must be replaced once, not once per thread");
		List<String> tokens = server.receivedTokens();
		assertEquals(Collections.nCopies(THREADS, "token-1"), tokens.subList(0, THREADS));
		assertEquals(Collections.nCopies(THREADS, "token-2"), tokens.subList(THREADS, tokens.size()));
	}

	@Test
	public void lateRefusalDoesNotDiscardANewerToken() throws Exception {
		server.refuse("token-1");
		server.holdRefusals();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			Future<MessageJson> late = executor.submit(this::get);
			server.awaitResourceCalls(1);

			kcc.invalidate();
			get();
			server.releaseRefusals();

			assertEquals("ok", late.get(10, TimeUnit.SECONDS).getMessage());
		} finally {
			executor.shutdownNow();
		}

		assertEquals(2, server.grants(), "a refusal of a token that was already replaced must not discard its successor");
		assertEquals(List.of("token-1", "token-2", "token-2"), server.receivedTokens());
		assertEquals("token-2", kcc.getAccessToken());
	}

	private MessageJson get() {
		return kcc.<MessageJson>get(restClient, MessageJson.class).addUrl(server.resourceUrl()).execute();
	}

	private List<MessageJson> getConcurrently() throws Exception {
		return collect(startConcurrently());
	}

	/**
	 * Starts {@value #THREADS} calls that are released together by a latch.
	 */
	private List<Future<MessageJson>> startConcurrently() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(THREADS);
		CountDownLatch ready = new CountDownLatch(THREADS);
		CountDownLatch go = new CountDownLatch(1);
		List<Future<MessageJson>> futures = new ArrayList<>();
		for (int i = 0; i < THREADS; i++)
			futures.add(executor.submit(() -> {
				ready.countDown();
				go.await();
				return get();
			}));
		executor.shutdown();
		ready.await(10, TimeUnit.SECONDS);
		go.countDown();
		return futures;
	}

	private List<MessageJson> collect(final List<Future<MessageJson>> futures) throws Exception {
		List<MessageJson> responses = new ArrayList<>();
		for (Future<MessageJson> future : futures)
			responses.add(future.get(10, TimeUnit.SECONDS));
		return responses;
	}

	enum Method {
		GET((c, k) -> k.<MessageJson>get(c, MessageJson.class)),
		POST((c, k) -> k.<MessageJson>post(c, MessageJson.class).body("{}")),
		PUT((c, k) -> k.<MessageJson>put(c, MessageJson.class).body("{}")),
		DEL((c, k) -> k.<MessageJson>del(c, MessageJson.class));

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
