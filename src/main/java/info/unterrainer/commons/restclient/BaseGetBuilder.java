package info.unterrainer.commons.restclient;

import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;

import info.unterrainer.commons.restclient.RestClient.HttpGetCall;

public class BaseGetBuilder<T, R> extends BaseBuilder<T, BaseGetBuilder<T, R>> {

	BaseGetBuilder(final RestClient client, final Class<?> type) {
		super(client, type);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected HttpGetCall<T> provideCall(final String url, final Class<T> type, final Map<String, String> headers) {
		return client -> {
			String r = client.getPlain(url, StringParam.builder().parameters(headers).build());
			if (String.class.isAssignableFrom(type))
				return (T) r;
			return client.jsonMapper.fromStringTo(type, r);
		};
	}

	@Override
	protected HttpGetCall<T> provideJavaTypeCall(final String url, final JavaType javaType,
			final Map<String, String> headers) {
		return client -> {
			String r = client.getPlain(url, StringParam.builder().parameters(headers).build());
			return client.jsonMapper.fromStringTo(javaType, r);
		};
	}
}
