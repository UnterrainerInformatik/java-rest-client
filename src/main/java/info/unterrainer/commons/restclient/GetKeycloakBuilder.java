package info.unterrainer.commons.restclient;

/**
 * Builds a GET call that carries the bearer token of a {@link KeycloakContext}.
 * <p>
 * If the server answers {@code 401}, the token is discarded, a new one is
 * fetched from keycloak and the call is made once more. If that is refused with
 * {@code 401} as well, the caller receives a
 * {@link info.unterrainer.commons.restclient.exceptions.RestClientException}
 * with status {@code 401}. Any other status is not repeated. See also
 * {@link KeycloakContext#invalidate()}.
 *
 * @param <T> the type the answer is deserialized into
 */
public class GetKeycloakBuilder<T> extends BaseGetBuilder<T, GetKeycloakBuilder<T>> {

	private final KeycloakContext kcc;

	GetKeycloakBuilder(final RestClient client, final Class<?> type, final KeycloakContext kcc) {
		super(client, type);
		this.kcc = kcc;
	}

	@Override
	public T execute() {
		return kcc.execute(client, this, super::execute);
	}
}
