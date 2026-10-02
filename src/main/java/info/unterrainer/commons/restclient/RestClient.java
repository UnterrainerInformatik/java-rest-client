package info.unterrainer.commons.restclient;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.DispatchingAuthenticator;
import com.burgstaller.okhttp.basic.BasicAuthenticator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;

import info.unterrainer.commons.restclient.exceptions.RestClientException;
import info.unterrainer.commons.serialization.jsonmapper.JsonMapper;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
@Accessors(fluent = true)
public class RestClient {

	private final Random random = new Random();

	protected OkHttpClient client;
	protected final Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
	protected final JsonMapper jsonMapper;

	public RestClient(final JsonMapper jsonMapper) {
		this(jsonMapper, null, null, 10000L, 10000L, 10000L);
	}

	public RestClient(final JsonMapper jsonMapper, final Long connectTimeoutInMillis, final Long readTimeoutInMillis,
			final Long writeTimeoutInMillis) {
		this(jsonMapper, null, null, connectTimeoutInMillis, readTimeoutInMillis, writeTimeoutInMillis);
	}

	public RestClient(final JsonMapper jsonMapper, final String userName, final String password) {
		this(jsonMapper, userName, password, 10000L, 10000L, 10000L);
	}

	public RestClient(final JsonMapper jsonMapper, final String userName, final String password,
			final Long connectTimeoutInMillis, final Long readTimeoutInMillis, final Long writeTimeoutInMillis) {
		super();
		this.jsonMapper = jsonMapper;
		okhttp3.OkHttpClient.Builder c = new OkHttpClient.Builder()
				.connectTimeout(connectTimeoutInMillis, TimeUnit.MILLISECONDS)
				.readTimeout(readTimeoutInMillis, TimeUnit.MILLISECONDS)
				.writeTimeout(writeTimeoutInMillis, TimeUnit.MILLISECONDS)
				.addInterceptor(new GzipInterceptor())
				.followRedirects(true);
		if (userName != null || password != null) {
			Credentials credentials = new Credentials(userName, password);
			final BasicAuthenticator basicAuthenticator = new BasicAuthenticator(credentials);
			final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);

			// Note that all authentication schemes should be registered as lower-case!
			DispatchingAuthenticator authenticator = new DispatchingAuthenticator.Builder()
					.with("digest", digestAuthenticator)
					.with("basic", basicAuthenticator)
					.build();

			c.authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
					.addInterceptor(new AuthenticationCacheInterceptor(authCache));
		}
		client = c.build();
	}

	String getPlain(final String url, final StringParam headers) throws IOException {
		String r = call("GET", url, headers, null, null, null);
		return r;
	}

	String delPlain(final String url, final StringParam headers) throws IOException {
		String r = call("DELETE", url, headers, null, null, null);
		return r;
	}

	String postPlain(final String url, final StringParam headers, final String mediaType, final String body,
			final byte[] binary) throws IOException {
		String r = call("POST", url, headers, mediaType, body, binary);
		return r;
	}

	String putPlain(final String url, final StringParam headers, final String mediaType, final String body,
			final byte[] binary) throws IOException {
		String r = call("PUT", url, headers, mediaType, body, binary);
		return r;
	}

	/**
	 * Makes a GET call and returns the body of its answer as it was received,
	 * without decoding it as text. An empty body gives an empty array.
	 */
	byte[] getBytes(final String url, final StringParam headers) throws IOException {
		Response response = send("GET", url, headers, null, null, null);
		try (ResponseBody responseBody = response.body()) {
			return responseBody.bytes();
		}
	}

	private String call(final String method, final String url, final StringParam headers, final String mediaType,
			final String body, final byte[] binary) throws IOException {
		Response response = send(method, url, headers, mediaType, body, binary);
		String r = response.body().string();
		response.body().close();
		return r == null ? "" : r;
	}

	private Response send(final String method, final String url, final StringParam headers, final String mediaType,
			final String body, final byte[] binary) throws IOException {
		Call call = getCall(method, url, headers, mediaType, body, binary);
		Response response = call.execute();

		if (!response.isSuccessful()) {
			response.body().close();
			throw new RestClientException(String.format("HTTP call to url %s failed with %s.", url, response.code()),
					response.code());
		}

		log.debug("HTTP call to url [{}] succeeded with [{}]", url, response.code());
		return response;
	}

	private Call getCall(final String method, final String url, final StringParam headers, final String mediaType,
			final String body, final byte[] binary) {

		String mt = mediaType;
		if (binary != null && mt == null)
			mt = "application/octet-stream";
		RequestBody requestBody;
		if (binary == null) {
			if (body == null)
				requestBody = RequestBody.create("", null);
			else
				requestBody = RequestBody.create(body, MediaType.parse(mt));
		} else
			requestBody = RequestBody.create(binary, MediaType.parse(mt));

		Builder request = new Request.Builder();
		switch (method) {
			case "GET":
				request.get();
				break;
			case "DELETE":
				request.delete();
				break;
			case "POST":
			case "PUT":
				request.method(method, requestBody);
				break;
			default:
				throw new IllegalArgumentException(String.format("Unsupported HTTP method %s.", method));
		}

		if (headers != null)
			request.headers(Headers.of(headers.getParameters()));

		return client.newCall(request.url(url).build());
	}

	/**
	 * Builds a GET call whose answer is converted into {@code type}.
	 * <p>
	 * For {@code byte[].class} the body is returned exactly as it was received,
	 * without decoding it as text (an empty body gives an empty array). Every other
	 * type is decoded from the body as text.
	 *
	 * @param <T>  the type the answer is converted into
	 * @param type the class of {@code T}
	 * @return a {@link GetBuilder} to provide a fluent interface.
	 */
	public <T> GetBuilder<T> get(final Class<?> type) {
		return new GetBuilder<>(this, type);
	}

	public <T> DelBuilder<T> del(final Class<?> type) {
		return new DelBuilder<>(this, type);
	}

	public <T> PostBuilder<T> post(final Class<?> type) {
		return new PostBuilder<>(this, type);
	}

	public <T> PutBuilder<T> put(final Class<?> type) {
		return new PutBuilder<>(this, type);
	}

	@FunctionalInterface
	public interface HttpGetCall<T> {
		T execute(RestClient client) throws IOException;
	}

	<T> T once(final HttpGetCall<T> call, final Consumer<IOException> onError) {
		try {
			return call.execute(this);
		} catch (IOException e) {
			onError.accept(e);
			return null;
		}
	}

	/**
	 * Makes an HTTP-call and retries it if it fails.
	 * <p>
	 * Calls {@link #retry(int, double, long, HttpGetCall, Consumer)} with parameters
	 * (2, 2D, 500L, call).
	 *
	 * @param <T>     the return value of the HTTP-call
	 * @param call    the HTTP-call to make
	 * @param onError receives the last {@link IOException} if every attempt failed
	 * @return the return type of the HTTP-call
	 */
	<T> T retryShort(final HttpGetCall<T> call, final Consumer<IOException> onError) {
		return retry(2, 2D, 500L, call, onError);
	}

	/**
	 * Makes an HTTP-call and retries it if it fails.
	 * <p>
	 * Calls {@link #retry(int, double, long, HttpGetCall, Consumer)} with parameters
	 * (3, 2D, 5000L, call).
	 *
	 * @param <T>     the return value of the HTTP-call
	 * @param call    the HTTP-call to make
	 * @param onError receives the last {@link IOException} if every attempt failed
	 * @return the return type of the HTTP-call
	 */
	<T> T retryEnduring(final HttpGetCall<T> call, final Consumer<IOException> onError) {
		return retry(3, 2D, 5000L, call, onError);
	}

	/**
	 * Makes an HTTP-call and retries it if it fails.<br>
	 * Adds a random number of milliseconds between 0 and 20 for each try.
	 * <p>
	 *
	 * @param <T>              the return value of the HTTP-call
	 * @param retries          number of times to retry if it fails
	 * @param retryWaitExpBase the exponent-base to base number of milliseconds to
	 *                         wait in between retries on (2 will give
	 *                         2,4,8,16,32...).
	 * @param retryWaitCapAt   the value to cap the retry-wait-time at (4 with
	 *                         expBase=2 will give 2,4,4,4,4...)
	 * @param call             the HTTP-call to make
	 * @param onError          receives the last {@link IOException} if every attempt
	 *                         failed; not called on success
	 * @return the return type of the HTTP-call
	 */
	<T> T retry(final int retries, final double retryWaitExpBase, final long retryWaitCapAt,
			final HttpGetCall<T> call, final Consumer<IOException> onError) {
		int ret = retries;
		IOException last = null;
		do
			try {
				T result = call.execute(this);
				if (result != null)
					return result;
				else
					throw new IOException("Call returned error.");
			} catch (IOException e) {
				last = e;
				ret--;
				int retry = retries - ret;
				log.debug("Call threw exception [{}] on retry [{}].", e.getMessage(), retry);
				long sleepTime = (long) Math.pow(retryWaitExpBase, retry);
				if (sleepTime > retryWaitCapAt)
					sleepTime = retryWaitCapAt;
				sleepTime += getRandomBetween(0D, 20D);
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
					onError.accept(last);
					return null;
				}
			}
		while (ret > 0);
		onError.accept(last);
		return null;
	}

	private double getRandomBetween(final Double min, final Double max) {
		return min + random.nextDouble() * (max - min);
	}
}
