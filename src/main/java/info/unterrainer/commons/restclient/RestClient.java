package info.unterrainer.commons.restclient;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import info.unterrainer.commons.restclient.exceptions.RestClientException;
import info.unterrainer.commons.serialization.JsonMapper;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Accessors(fluent = true)
public class RestClient {

	private final Random random = new Random();

	protected OkHttpClient client;
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
		if (userName != null || password != null)
			c.authenticator((route, response) -> {
				String credential = Credentials.basic(userName, password);
				return response.request().newBuilder().header("Authorization", credential).build();
			});
		client = c.build();
	}

	public String getPlain(final String url, final StringParam headers) throws IOException {
		String r = call("GET", url, headers, null, null, null);
		return r;
	}

	public String delPlain(final String url, final StringParam headers) throws IOException {
		String r = call("DEL", url, headers, null, null, null);
		return r;
	}

	public String postPlain(final String url, final StringParam headers, final String mediaType, final String body,
			final byte[] binary) throws IOException {
		String r = call("POST", url, headers, mediaType, body, binary);
		return r;
	}

	public String putPlain(final String url, final StringParam headers, final String mediaType, final String body,
			final byte[] binary) throws IOException {
		String r = call("PUT", url, headers, mediaType, body, binary);
		return r;
	}

	private String call(final String method, final String url, final StringParam headers, final String mediaType,
			final String body, final byte[] binary) throws IOException {
		Call call = getCall(method, url, headers, mediaType, body, binary);
		Response response = call.execute();

		if (!response.isSuccessful()) {
			response.body().close();
			throw new RestClientException(String.format("HTTP call to url %s failed with %s.", url, response.code()));
		}

		log.debug("HTTP call to url [{}] succeeded with [{}]", url, response.code());
		String r = response.body().string();
		response.body().close();
		return r == null ? "" : r;
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
		if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT"))
			request.method(method, requestBody);

		request.headers(Headers.of(headers.getParameters())).url(url);

		return client.newCall(request.build());
	}

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

	<T> T once(final HttpGetCall<T> call) {
		try {
			return call.execute(this);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Makes an HTTP-call and retries it if it fails.
	 * <p>
	 * Calls {@link #retry(int, double, long, HttpGetCall)} with parameters (10, 2D,
	 * 500L, call).
	 *
	 * @param <T>  the return value of the HTTP-call
	 * @param call the HTTP-call to make
	 * @return the return type of the HTTP-call
	 */
	<T> T retryShort(final HttpGetCall<T> call) {
		return retry(2, 2D, 500L, call);
	}

	/**
	 * Makes an HTTP-call and retries it if it fails.
	 * <p>
	 * Calls {@link #retry(int, double, long, HttpGetCall)} with parameters (40, 2D,
	 * 5000L, call).
	 *
	 * @param <T>  the return value of the HTTP-call
	 * @param call the HTTP-call to make
	 * @return the return type of the HTTP-call
	 */
	<T> T retryEnduring(final HttpGetCall<T> call) {
		return retry(3, 2D, 5000L, call);
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
	 * @return the return type of the HTTP-call
	 */
	<T> T retry(final int retries, final double retryWaitExpBase, final long retryWaitCapAt,
			final HttpGetCall<T> call) {
		int ret = retries;
		do
			try {
				T result = call.execute(this);
				if (result != null)
					return result;
				else
					throw new IOException("Call returned error.");
			} catch (IOException e) {
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
					return null;
				}
			}
		while (ret > 0);
		return null;
	}

	private double getRandomBetween(final Double min, final Double max) {
		return min + random.nextDouble() * (max - min);
	}
}
