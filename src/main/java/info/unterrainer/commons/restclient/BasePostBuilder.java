package info.unterrainer.commons.restclient;

import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;

import info.unterrainer.commons.restclient.RestClient.HttpGetCall;

public class BasePostBuilder<T, R> extends BaseBuilder<T, BasePostBuilder<T, R>> {

	protected String mediaType;
	protected String body;

	BasePostBuilder(final RestClient client, final Class<?> type) {
		super(client, type);
	}

	@SuppressWarnings("unchecked")
	public R mediaType(final String mediaType) {
		this.mediaType = mediaType;
		return (R) this;
	}

	@SuppressWarnings("unchecked")
	public R body(final String body) {
		this.body = body;
		return (R) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected HttpGetCall<T> provideCall(final String url, final Class<T> type, final Map<String, String> headers) {
		return client -> {
			String r = client.postPlain(url, StringParam.builder().parameters(headers).build(), mediaType, body);
			if (String.class.isAssignableFrom(type))
				return (T) r;
			return client.jsonMapper.fromStringTo(type, r);
		};
	}

	@Override
	protected HttpGetCall<T> provideJavaTypeCall(final String url, final JavaType javaType,
			final Map<String, String> headers) {
		return client -> {
			String r = client.postPlain(url, StringParam.builder().parameters(headers).build(), mediaType, body);
			return client.jsonMapper.fromStringTo(javaType, r);
		};
	}
}
