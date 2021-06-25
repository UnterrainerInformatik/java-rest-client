package info.unterrainer.commons.restclient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mapstruct.ap.internal.util.Strings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import info.unterrainer.commons.restclient.RestClient.HttpGetCall;
import info.unterrainer.commons.restclient.exceptions.RestClientException;
import info.unterrainer.commons.restclient.jsons.ListJson;
import info.unterrainer.commons.serialization.JsonMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class BaseBuilder<T, R extends BaseBuilder<T, R>> {

	protected enum Retry {
		ONCE,
		SHORT,
		ENDURING
	}

	protected final RestClient client;
	protected final Class<?> type;

	protected List<String> url = new ArrayList<>();
	protected JavaType javaType;
	protected TypeReference<?> typeReference;
	protected Map<String, String> headers = new HashMap<>();
	protected Map<String, String> parameters = new HashMap<>();
	protected Retry retry = Retry.ONCE;

	/**
	 * Add a string to an URL.
	 * <p>
	 * The URL will be composed of all the parts you add.<br>
	 * The parts will be checked for trailing and leading slashes, which will be
	 * removed before adding the URL-part (separated by slashes).<br>
	 * Null values are ignored. So to add an empty URL-part that should result in
	 * something like this: 'here/we//go' (between here and go), just add an empty
	 * string.
	 *
	 * @param urlPart the part of the URL to add
	 * @return a {@link BaseBuilder} to provide a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public R addUrl(final String urlPart) {
		if (urlPart != null)
			url.add(urlPart);
		return (R) this;
	}

	/**
	 * Sets the return-type to be of {@link ListJson} with the generic type you
	 * provided when starting this builder.
	 *
	 * @return a {@link BaseBuilder} to provide a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public R isListJson() {
		JsonMapper mapper = client.jsonMapper;
		TypeFactory typeFactory = mapper.getTypeFactory();
		javaType = typeFactory.constructParametricType(ListJson.class, type);
		return (R) this;
	}

	/**
	 * Sets the return-type to be of {@link List} with the generic type you provided
	 * when starting this builder.
	 *
	 * @return a {@link BaseBuilder} to provide a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public R isList() {
		JsonMapper mapper = client.jsonMapper;
		TypeFactory typeFactory = mapper.getTypeFactory();
		javaType = typeFactory.constructParametricType(List.class, type);
		return (R) this;
	}

	/**
	 * Adds a header to the call.
	 * <p>
	 * All the headers are saved to a map and added later on, when you make the
	 * call.<br>
	 * Input containing null as key or value will be ignored.
	 *
	 * @param key   the key of the header-field to add
	 * @param value the value of the header-field to add
	 * @return a {@link BaseBuilder} to provide a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public R addHeader(final String key, final String value) {
		if (key != null && value != null)
			headers.put(key, value);
		return (R) this;
	}

	/**
	 * Add a query-parameter to the call.
	 * <p>
	 * All the parameters are saved in a map and added later on, when you make the
	 * call.<br>
	 * The parameters will be checked for trailing and leading '&amp;' and '?',
	 * which will be removed before adding it to the map.<br>
	 * Input containing null as key or value will be ignored.
	 *
	 * @param key   the key of the parameter to add
	 * @param value the value of the parameter to add
	 * @return a {@link BaseBuilder} to provide a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public R addParam(final String key, final String value) {
		if (key != null && value != null) {
			String k = cutLeadingTrailing("&", key);
			k = cutLeadingTrailing("?", k);
			try {
				k = URLEncoder.encode(k, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.error(String.format("The given key-value %s could not be URL-encoded.", k), e);
			}

			String v = cutLeadingTrailing("&", value);
			v = cutLeadingTrailing("?", v);
			try {
				v = URLEncoder.encode(v, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.error(String.format("The given parameter-value %s could not be URL-encoded.", v), e);
			}
			parameters.put(k, v);
		}
		return (R) this;
	}

	/**
	 * By specifying this, you tell the client to retry this call for some time, if
	 * it fails (see {@link RestClient#retryShort(HttpGetCall)}).
	 * <p>
	 * The default is to just try it once and then give up and return an error.
	 *
	 * @return a {@link BaseBuilder} to provide a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public R retryShort() {
		retry = Retry.SHORT;
		return (R) this;
	}

	/**
	 * By specifying this, you tell the client to retry this call for quite some
	 * time, if it fails (see {@link RestClient#retryEnduring(HttpGetCall)}).
	 * <p>
	 * The default is to just try it once and then give up and return an error.
	 *
	 * @return a {@link BaseBuilder} to provide a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public R retryEnduring() {
		retry = Retry.ENDURING;
		return (R) this;
	}

	protected abstract HttpGetCall<T> provideCall(String url, Class<T> type, Map<String, String> headers);

	protected abstract HttpGetCall<T> provideJavaTypeCall(String url, JavaType javaType, Map<String, String> headers);

	/**
	 * Execute the call.
	 *
	 * @throws RestClientException if an error occurred.
	 * @return the return value of type you specified when you created this builder.
	 */
	public T execute() {
		String url = Strings.join(this.url.stream().map(e -> cutLeadingTrailing("/", e)).collect(Collectors.toList()),
				"/");

		String params = Strings.join(
				parameters.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList()),
				"&");
		if (!params.isBlank())
			url += "?" + params;

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

	private String cutLeadingTrailing(final String s, final String input) {
		String u = input;
		while (u.startsWith(s))
			u = u.substring(1);
		while (u.endsWith(s))
			u = u.substring(0, u.length() - 2);
		return u;
	}

	protected <V> Object castTo(final JsonMapper jsonMapper, final String s) {
		if (s == null)
			return null;

		if (String.class.isAssignableFrom(type))
			return s;
		if (Boolean.class.isAssignableFrom(type))
			return Boolean.parseBoolean(s);
		if (Byte.class.isAssignableFrom(type))
			return Byte.parseByte(s);
		if (Short.class.isAssignableFrom(type))
			return Short.parseShort(s);
		if (Integer.class.isAssignableFrom(type))
			return Integer.parseInt(s);
		if (Long.class.isAssignableFrom(type))
			return Long.parseLong(s);
		if (Float.class.isAssignableFrom(type))
			return Float.parseFloat(s);
		if (Double.class.isAssignableFrom(type))
			return Double.parseDouble(s);
		if (Void.class.isAssignableFrom(type))
			return null;

		return client.jsonMapper.fromStringTo(type, s);
	}
}
