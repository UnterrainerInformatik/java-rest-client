package info.unterrainer.commons.restclient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.function.Supplier;

import info.unterrainer.commons.restclient.exceptions.RestClientException;
import info.unterrainer.commons.restclient.exceptions.UnauthorizedException;
import info.unterrainer.commons.restclient.jsons.TokenResponseJson;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KeycloakContext {

	private final String keycloakUrl;
	private final String userName;
	private final String password;
	private final String clientId;
	private final String clientSecret;
	@Getter
	private String accessToken;
	@Getter
	private String refreshToken;
	@Getter
	private Long refreshTimestamp;

	public <T> GetKeycloakBuilder<T> get(final RestClient client, final Class<?> type) {
		return new GetKeycloakBuilder<>(client, type, this);
	}

	public <T> PostKeycloakBuilder<T> post(final RestClient client, final Class<?> type) {
		return new PostKeycloakBuilder<>(client, type, this);
	}

	public <T> PutKeycloakBuilder<T> put(final RestClient client, final Class<?> type) {
		return new PutKeycloakBuilder<>(client, type, this);
	}

	public <T> DelKeycloakBuilder<T> del(final RestClient client, final Class<?> type) {
		return new DelKeycloakBuilder<>(client, type, this);
	}

	/**
	 * Discards the current token, so the next call made through this context
	 * fetches a new one from keycloak.
	 * <p>
	 * A call made through one of this context's builders does this on its own
	 * when the server answers 401. Use it when you learn about a refusal some
	 * other way, for example from a call you built by hand with
	 * {@code getAccessToken()}.
	 */
	public void invalidate() {
		accessToken = null;
		refreshToken = null;
		refreshTimestamp = null;
	}

	/**
	 * Runs a call with this context's token. If the server refuses it with 401,
	 * the token is discarded, a new one is fetched and the call is made once more.
	 * A second 401 reaches the caller.
	 */
	<T> T execute(final RestClient client, final BaseBuilder<T, ?> builder, final Supplier<T> call) {
		authorize(client, builder);
		try {
			return call.get();
		} catch (RestClientException e) {
			if (e.getStatusCode() != 401)
				throw e;
			invalidate();
			authorize(client, builder);
			T result = call.get();
			log.warn("Call to [{}] was refused with 401 and succeeded with a new token.", builder.joinedUrl());
			return result;
		}
	}

	private void authorize(final RestClient client, final BaseBuilder<?, ?> builder) {
		update(client);
		builder.addHeader("Authorization", "Bearer " + accessToken);
	}

	void update(final RestClient client) {
		if (userName == null) {
			log.error("UserName is null");
			return;
		}
		if (password == null) {
			log.error("Password is null");
			return;
		}

		long now = System.currentTimeMillis();
		log.debug("now: [{}]", now);
		log.debug("accessToken: [{}]", accessToken);
		log.debug("refreshTimestamp: [{}]", refreshTimestamp);

		if (refreshTimestamp != null) {
			long delta = refreshTimestamp - now;
			log.debug("valid for another: [{}]s", (delta - delta % 1000) / 1000);
		}

		if (accessToken == null || refreshTimestamp == null || now > refreshTimestamp) {
			String cs = "";
			if (clientSecret != null)
				try {
					cs = "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8");
				} catch (UnsupportedEncodingException e1) {
					log.error("Could not URLEncode clientSecret: [{}]", clientSecret);
				}

			String encUserName = null;
			try {
				encUserName = URLEncoder.encode(userName, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.error("Could not URLEncode userName: [{}]", userName);
			}

			String encPassword = null;
			try {
				encPassword = URLEncoder.encode(password, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.error("Could not URLEncode password: [{}]", password);
			}

			String body = "client_id=" + clientId + cs + "&grant_type=password&username=" + encUserName + "&password="
					+ encPassword;
			log.debug("body: [{}]", body);

			TokenResponseJson response = null;
			response = client.<TokenResponseJson>post(TokenResponseJson.class)
					.addHeader("Content-Type", "application/x-www-form-urlencoded")
					.addHeader("Accept", "application/json")
					.addUrl(keycloakUrl)
					.retryShort()
					.mediaType("application/x-www-form-urlencoded")
					.body(body)
					.execute();
			if (response == null)
				throw new UnauthorizedException("Getting an access-token from the keycloak instance didn't work out.");

			accessToken = response.getAccessToken();
			refreshToken = response.getRefreshToken();
			refreshTimestamp = now + response.getExpiresIn() * 1000L;
		}
	}
}
