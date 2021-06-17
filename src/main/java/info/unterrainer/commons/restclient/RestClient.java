package info.unterrainer.commons.restclient;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import info.unterrainer.commons.serialization.JsonMapper;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Accessors(fluent = true)
public class RestClient {

	private final Random random = new Random();

	protected OkHttpClient client;
	protected final JsonMapper jsonMapper;

	public RestClient(final JsonMapper jsonMapper, final String userName, final String password) {
		super();
		this.jsonMapper = jsonMapper;
		okhttp3.OkHttpClient.Builder c = new OkHttpClient.Builder().connectTimeout(10000, TimeUnit.MILLISECONDS)
				.readTimeout(10000, TimeUnit.MILLISECONDS)
				.writeTimeout(10000, TimeUnit.MILLISECONDS)
				.followRedirects(true);
		if (userName != null || password != null)
			c.authenticator((route, response) -> {
				String credential = Credentials.basic(userName, password);
				return response.request().newBuilder().header("Authorization", credential).build();
			});
		client = c.build();
	}

	public String get(final String url) throws IOException {
		String r = call("GET", url, StringParam.builder().build());
		return r;
	}

	public <T> T get(final String url, final Class<T> targetJson) throws IOException {
		return get(url, targetJson, StringParam.builder().build());
	}

	public <T> T get(final String url, final Class<T> targetJson, final StringParam headers) throws IOException {
		String r = call("GET", url, headers);
		if (r == null)
			return null;
		return jsonMapper.fromStringTo(targetJson, r);
	}

	public String post(final String url, final String mediaType, final String body) throws IOException {
		String r = call("POST", url, StringParam.builder().build());
		return r;
	}

	public <T> T post(final String url, final Class<T> targetJson, final String mediaType, final String body)
			throws IOException {
		return post(url, targetJson, StringParam.builder().build(), mediaType, body);
	}

	public <T> T post(final String url, final Class<T> targetJson, final StringParam headers, final String mediaType,
			final String body) throws IOException {
		String r = call("POST", url, headers);
		if (r == null)
			return null;
		return jsonMapper.fromStringTo(targetJson, r);
	}

	private String call(final String method, final String url, final StringParam headers) throws IOException {
		return call(method, url, headers, null, null);
	}

	private String call(final String method, final String url, final StringParam headers, final String mediaType,
			final String body) throws IOException {
		Call call = getCall(method, url, headers, mediaType, body);
		Response response = call.execute();

		if (!response.isSuccessful()) {
			log.warn("HTTP call to url [{}] failed with [{}]", url, response.code());
			response.body().close();
			return null;
		}

		log.debug("HTTP call to url [{}] succeeded with [{}]", url, response.code());
		String r = response.body().string();
		response.body().close();
		return r == null ? "" : r;
	}

	private Call getCall(final String method, final String url, final StringParam headers, final String mediaType,
			final String body) {

		RequestBody requestBody;
		if (body == null)
			requestBody = RequestBody.create(null, "");
		else
			requestBody = RequestBody.create(MediaType.parse(mediaType), body);

		Builder request = new Request.Builder();
		if (!method.equalsIgnoreCase("GET"))
			request.method(method, requestBody);

		request.headers(Headers.of(headers.getParameters())).url(url);

		return client.newCall(request.build());
	}

	@FunctionalInterface
	public interface HttpGetCall<T> {
		T execute(RestClient client) throws IOException;
	}

	public <T> T once(final HttpGetCall<T> call) {
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
	public <T> T retryShort(final HttpGetCall<T> call) {
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
	public <T> T retryEnduring(final HttpGetCall<T> call) {
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
	public <T> T retry(final int retries, final double retryWaitExpBase, final long retryWaitCapAt,
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