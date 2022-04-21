package info.unterrainer.commons.restclient;

import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;

import info.unterrainer.commons.restclient.RestClient.HttpGetCall;

public class BaseDelBuilder<T, R> extends BaseBuilder<T, BaseDelBuilder<T, R>> {

	BaseDelBuilder(final RestClient client, final Class<?> type) {
		super(client, type);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected HttpGetCall<T> provideCall(final String url, final Class<T> type, final Map<String, String> headers) {
		return client -> {
			String r = client.delPlain(url, StringParam.builder().parameters(headers).build());
			return (T) castTo(client.jsonMapper, r);
		};
	}

	@Override
	protected HttpGetCall<T> provideJavaTypeCall(final String url, final JavaType javaType,
			final Map<String, String> headers) {
		return client -> {
			String r = client.delPlain(url, StringParam.builder().parameters(headers).build());
			return client.jsonMapper.fromStringTo(javaType, r);
		};
	}
}
