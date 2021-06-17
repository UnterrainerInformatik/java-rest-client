package info.unterrainer.commons.restclient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PostBuilder<T> {

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
	private String mediaType;
	private String body;

	public PostBuilder<T> url(final String url) {
		this.url = url;
		return this;
	}

	public PostBuilder<T> type(final Class<T> type) {
		this.type = type;
		return this;
	}

	public PostBuilder<T> addHeader(final String key, final String value) {
		headers.put(key, value);
		return this;
	}

	public PostBuilder<T> retryShort() {
		retry = Retry.SHORT;
		return this;
	}

	public PostBuilder<T> retryEnduring() {
		retry = Retry.ENDURING;
		return this;
	}

	public PostBuilder<T> mediaType(final String mediaType) {
		this.mediaType = mediaType;
		return this;
	}

	public PostBuilder<T> body(final String body) {
		this.body = body;
		return this;
	}

	public T execute() throws IOException {
		kcc.update(client);
		switch (retry) {
		case SHORT:
			client.retryShort(client -> client.post(url, type,
					StringParam.builder().parameters(headers).build(), mediaType, body));
		case ENDURING:
			client.retryEnduring(client -> client.post(url, type,
					StringParam.builder().parameters(headers).build(), mediaType, body));
		default:
			return client.once(client -> client.post(url, type,
					StringParam.builder().parameters(headers).build(), mediaType, body));
		}
	}
}
