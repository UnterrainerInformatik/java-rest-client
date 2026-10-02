package info.unterrainer.commons.restclient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Plays both keycloak and the resource server on loopback, on an ephemeral
 * port.
 * <p>
 * The token endpoint hands out numbered tokens ({@code token-1},
 * {@code token-2}, …) and counts the grants. The resource endpoint records the
 * method and token of every call and answers 401 for every token in the refused
 * set, a fixed status if one is set, and 200 with a JSON body otherwise.
 * <p>
 * Exchanges are handled on a thread pool, so concurrent calls really overlap.
 * The token endpoint can be slowed down, and it records the highest number of
 * grants that were in flight at once. Refusals can be held back until a test
 * releases them.
 */
class KeycloakTestServer implements AutoCloseable {

	static final String OK_BODY = "{\"message\":\"ok\"}";

	private final HttpServer server;
	private final AtomicInteger grants = new AtomicInteger();
	private final AtomicInteger resourceCalls = new AtomicInteger();
	private final Set<String> refusedTokens = ConcurrentHashMap.newKeySet();
	private final List<String> receivedTokens = new CopyOnWriteArrayList<>();
	private final List<String> receivedMethods = new CopyOnWriteArrayList<>();
	private final AtomicInteger grantsInFlight = new AtomicInteger();
	private final AtomicInteger maxGrantsInFlight = new AtomicInteger();
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private volatile int fixedStatus;
	private volatile long expiresIn = 300;
	private volatile long tokenDelayMillis;
	private volatile CountDownLatch refusalGate;

	KeycloakTestServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/token", this::handleToken);
		server.createContext("/resource", this::handleResource);
		server.setExecutor(executor);
		server.start();
	}

	String tokenUrl() {
		return baseUrl() + "/token";
	}

	String resourceUrl() {
		return baseUrl() + "/resource";
	}

	KeycloakContext context() {
		return new KeycloakContext(tokenUrl(), "user", "password", "client", null);
	}

	void refuse(final String... tokens) {
		refusedTokens.addAll(List.of(tokens));
	}

	void answerWith(final int status) {
		fixedStatus = status;
	}

	/**
	 * Sets the lifetime, in seconds, reported for every token issued from now on.
	 */
	void expiresIn(final long seconds) {
		expiresIn = seconds;
	}

	/**
	 * Makes the token endpoint wait this long before it answers.
	 */
	void delayTokens(final long millis) {
		tokenDelayMillis = millis;
	}

	/**
	 * Holds back every 401 until {@link #releaseRefusals()} is called. The call is
	 * recorded as received before it is held.
	 */
	void holdRefusals() {
		refusalGate = new CountDownLatch(1);
	}

	void releaseRefusals() {
		refusalGate.countDown();
	}

	/**
	 * Waits until the resource endpoint has received at least {@code count}
	 * calls.
	 */
	void awaitResourceCalls(final int count) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (resourceCalls.get() < count) {
			if (System.nanoTime() > deadline)
				throw new IllegalStateException(
						"Expected " + count + " resource calls, got " + resourceCalls.get() + ".");
			Thread.sleep(5);
		}
	}

	int grants() {
		return grants.get();
	}

	int maxGrantsInFlight() {
		return maxGrantsInFlight.get();
	}

	int resourceCalls() {
		return resourceCalls.get();
	}

	List<String> receivedTokens() {
		return receivedTokens;
	}

	List<String> receivedMethods() {
		return receivedMethods;
	}

	@Override
	public void close() {
		CountDownLatch gate = refusalGate;
		if (gate != null)
			gate.countDown();
		server.stop(0);
		executor.shutdownNow();
	}

	private String baseUrl() {
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}

	private void handleToken(final HttpExchange exchange) throws IOException {
		exchange.getRequestBody().readAllBytes();
		maxGrantsInFlight.accumulateAndGet(grantsInFlight.incrementAndGet(), Math::max);
		try {
			if (tokenDelayMillis > 0)
				Thread.sleep(tokenDelayMillis);
			int n = grants.incrementAndGet();
			respond(exchange, 200, "{\"access_token\":\"token-" + n
					+ "\",\"token_type\":\"Bearer\",\"refresh_token\":\"refresh-" + n + "\",\"expires_in\":"
					+ expiresIn + "}");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			respond(exchange, 500, "");
		} finally {
			grantsInFlight.decrementAndGet();
		}
	}

	private void handleResource(final HttpExchange exchange) throws IOException {
		exchange.getRequestBody().readAllBytes();
		resourceCalls.incrementAndGet();
		receivedMethods.add(exchange.getRequestMethod());
		String authorization = exchange.getRequestHeaders().getFirst("Authorization");
		String token = authorization == null ? null : authorization.replaceFirst("^Bearer ", "");
		receivedTokens.add(token);

		if (token != null && refusedTokens.contains(token)) {
			awaitRefusalGate();
			respond(exchange, 401, "");
		}
		else if (fixedStatus != 0)
			respond(exchange, fixedStatus, "");
		else
			respond(exchange, 200, OK_BODY);
	}

	private void awaitRefusalGate() {
		CountDownLatch gate = refusalGate;
		if (gate == null)
			return;
		try {
			if (!gate.await(10, TimeUnit.SECONDS))
				throw new IllegalStateException("Refusals were never released.");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void respond(final HttpExchange exchange, final int status, final String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		}
	}
}
