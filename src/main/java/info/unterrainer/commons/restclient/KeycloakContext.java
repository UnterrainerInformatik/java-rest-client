package info.unterrainer.commons.restclient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import info.unterrainer.commons.restclient.exceptions.RestClientException;
import info.unterrainer.commons.restclient.exceptions.UnauthorizedException;
import info.unterrainer.commons.restclient.jsons.TokenResponseJson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Holds a keycloak access token, fetched with the password grant, and sends it
 * as a bearer token on every call made through its builders.
 * <p>
 * The token is renewed shortly before it expires: once less than 30 seconds of
 * the lifetime keycloak reported are left, or less than half of it if that is
 * shorter. {@link #getRefreshTimestamp()} still returns the reported expiry.
 * <p>
 * A context is safe to use from several threads at once. Threads that need a
 * new token at the same time share a single fetch. A call refused with 401
 * discards the token only if the context still holds the token that call sent.
 */
@Slf4j
@RequiredArgsConstructor
public class KeycloakContext {

	private static final long RENEWAL_MARGIN_MILLIS = 30_000L;

	private final String keycloakUrl;
	private final String userName;
	private final String password;
	private final String clientId;
	private final String clientSecret;
	private final Object lock = new Object();
	private String accessToken;
	private String refreshToken;
	private Long refreshTimestamp;
	private Long renewAt;
	LongSupplier clock = System::currentTimeMillis;

	/**
	 * Returns the access token this context currently holds.
	 *
	 * @return the current access token, or {@code null} if none has been fetched
	 *         or it was discarded
	 */
	public String getAccessToken() {
		synchronized (lock) {
			return accessToken;
		}
	}

	/**
	 * Returns the refresh token keycloak issued with the current access token.
	 *
	 * @return the refresh token issued with the current access token, or
	 *         {@code null}
	 */
	public String getRefreshToken() {
		synchronized (lock) {
			return refreshToken;
		}
	}

	/**
	 * Returns the expiry keycloak reported for the current access token.
	 *
	 * @return the expiry of the current access token as reported by keycloak, in
	 *         epoch milliseconds, or {@code null}. The token is renewed before
	 *         this moment.
	 */
	public Long getRefreshTimestamp() {
		synchronized (lock) {
			return refreshTimestamp;
		}
	}

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
		synchronized (lock) {
			clear();
		}
	}

	/**
	 * Discards the current token only if it is still {@code sentToken}. If
	 * another call has already replaced it, the newer token is kept.
	 */
	void invalidate(final String sentToken) {
		synchronized (lock) {
			if (accessToken != null && accessToken.equals(sentToken))
				clear();
		}
	}

	private void clear() {
		accessToken = null;
		refreshToken = null;
		refreshTimestamp = null;
		renewAt = null;
	}

	/**
	 * Runs a call with this context's token. If the server refuses it with 401,
	 * the refused token is discarded (unless another call has already replaced
	 * it), a new one is obtained and the call is made once more. A second 401
	 * reaches the caller.
	 */
	<T> T execute(final RestClient client, final BaseBuilder<T, ?> builder, final Supplier<T> call) {
		String sent = authorize(client, builder);
		try {
			return call.get();
		} catch (RestClientException e) {
			if (e.getStatusCode() != 401)
				throw e;
			invalidate(sent);
			authorize(client, builder);
			T result = call.get();
			log.warn("Call to [{}] was refused with 401 and succeeded with a new token.", builder.joinedUrl());
			return result;
		}
	}

	private String authorize(final RestClient client, final BaseBuilder<?, ?> builder) {
		String token = update(client);
		builder.addHeader("Authorization", "Bearer " + token);
		return token;
	}

	/**
	 * Makes sure this context holds a token that is not yet due for renewal,
	 * fetching one if necessary, and returns it. The lock is held across the
	 * fetch, so threads that need a token at the same time share one fetch.
	 */
	String update(final RestClient client) {
		synchronized (lock) {
			return ensureToken(client);
		}
	}

	private String ensureToken(final RestClient client) {
		if (userName == null) {
			log.error("UserName is null");
			return accessToken;
		}
		if (password == null) {
			log.error("Password is null");
			return accessToken;
		}

		long now = clock.getAsLong();
		log.debug("now: [{}]", now);
		log.debug("accessToken: [{}]", accessToken);
		log.debug("refreshTimestamp: [{}]", refreshTimestamp);

		if (refreshTimestamp != null) {
			long delta = refreshTimestamp - now;
			log.debug("valid for another: [{}]s", (delta - delta % 1000) / 1000);
		}

		if (accessToken == null || renewAt == null || now >= renewAt) {
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
			long lifetime = response.getExpiresIn() * 1000L;
			refreshTimestamp = now + lifetime;
			renewAt = refreshTimestamp - Math.min(RENEWAL_MARGIN_MILLIS, lifetime / 2);
		}
		return accessToken;
	}
}
