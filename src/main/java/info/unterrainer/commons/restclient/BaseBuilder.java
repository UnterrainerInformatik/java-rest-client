package info.unterrainer.commons.restclient;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

import info.unterrainer.commons.httpserver.jsons.ListJson;
import info.unterrainer.commons.restclient.RestClient.HttpGetCall;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class BaseBuilder<T, R extends BaseBuilder<T, R>> {

	protected enum Retry {
		ONCE,
		SHORT,
		ENDURING
	}

	protected final RestClient client;
	protected final Class<?> type;

	protected String url;
	protected JavaType javaType;
	protected TypeReference<?> typeReference;
	protected Map<String, String> headers = new HashMap<>();
	protected Retry retry = Retry.ONCE;

	@SuppressWarnings("unchecked")
	public R url(final String url) {
		this.url = url;
		return (R) this;
	}

	@SuppressWarnings("unchecked")
	public R isListJson() {
		javaType = client.jsonMapper.getTypeFactory().constructParametricType(ListJson.class, type);
		return (R) this;
	}

	@SuppressWarnings("unchecked")
	public R addHeader(final String key, final String value) {
		headers.put(key, value);
		return (R) this;
	}

	@SuppressWarnings("unchecked")
	public R retryShort() {
		retry = Retry.SHORT;
		return (R) this;
	}

	@SuppressWarnings("unchecked")
	public R retryEnduring() {
		retry = Retry.ENDURING;
		return (R) this;
	}

	protected abstract HttpGetCall<T> provideCall(String url, Class<T> type, Map<String, String> headers);

	protected abstract HttpGetCall<T> provideJavaTypeCall(String url, JavaType javaType, Map<String, String> headers);

	public T execute() {
		switch (retry) {
		case SHORT:
			return client.retryShort(provide(url, type, javaType, headers));
		case ENDURING:
			return client.retryEnduring(provide(url, type, javaType, headers));
		default:
			return client.once(provide(url, type, javaType, headers));
		}
	}

	@SuppressWarnings("unchecked")
	private HttpGetCall<T> provide(final String url, final Class<?> type, final JavaType javaType,
			final Map<String, String> headers) {
		if (javaType != null)
			return provideJavaTypeCall(url, javaType, headers);
		return provideCall(url, (Class<T>) type, headers);
	}
}
