package info.unterrainer.commons.restclient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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

	public <T> GetBuilder<T> get(final RestClient client) {
		return new GetBuilder<>(client, this);
	}

	public <T> PostBuilder<T> post(final RestClient client) {
		return new PostBuilder<>(client, this);
	}

	void update(final RestClient client) {
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
			try {
				response = client.post(keycloakUrl, TokenResponseJson.class,
						StringParam.builder()
								.parameter("Content-Type", "application/x-www-form-urlencoded")
								.parameter("Accept", "application/json")
								.build(),
						"application/x-www-form-urlencoded", body);
			} catch (IOException e) {
				log.error("Error getting tokens from keycloak.");
				return;
			}
			accessToken = response.getAccessToken();
			refreshToken = response.getRefreshToken();
			refreshTimestamp = now + response.getExpiresIn() * 1000L;
		}
	}
}
