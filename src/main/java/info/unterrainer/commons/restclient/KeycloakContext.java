package info.unterrainer.commons.restclient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import info.unterrainer.commons.httpserver.exceptions.UnauthorizedException;
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
