package info.unterrainer.commons.restclient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class GetBuilder<T> {

	private enum Retry {
		ONCE,
		SHORT,
		ENDURING
	}

	private final RestClient client;
	private final KeycloakContext kcc;

	private String url;
	private Class<T> type;
	private Map<String, String> headers = new HashMap<>();
	private Retry retry = Retry.ONCE;

	public GetBuilder<T> url(final String url) {
		this.url = url;
		return this;
	}

	public GetBuilder<T> type(final Class<T> type) {
		this.type = type;
		return this;
	}

	public GetBuilder<T> addHeader(final String key, final String value) {
		headers.put(key, value);
		return this;
	}

	public GetBuilder<T> retryShort() {
		retry = Retry.SHORT;
		return this;
	}

	public GetBuilder<T> retryEnduring() {
		retry = Retry.ENDURING;
		return this;
	}

	public T execute() throws IOException {
		kcc.update(client);
		switch (retry) {
		case SHORT:
			client.retryShort(
					client -> client.get(url, type, StringParam.builder().parameters(headers).build()));
		case ENDURING:
			client.retryEnduring(
					client -> client.get(url, type, StringParam.builder().parameters(headers).build()));
		default:
			return client
					.once(client -> client.get(url, type, StringParam.builder().parameters(headers).build()));
		}
	}
}
