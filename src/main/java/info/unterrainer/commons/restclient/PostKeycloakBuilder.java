package info.unterrainer.commons.restclient;

/**
 * Builds a POST call that carries the bearer token of a {@link KeycloakContext}.
 * <p>
 * If the server answers {@code 401}, the token is discarded, a new one is
 * fetched from keycloak and the call is made once more. If that is refused with
 * {@code 401} as well, the caller receives a
 * {@link info.unterrainer.commons.restclient.exceptions.RestClientException}
 * with status {@code 401}. Any other status is not repeated. See also
 * {@link KeycloakContext#invalidate()}.
 * <p>
 * A call refused with {@code 401} is sent twice. For a non-idempotent POST that
 * is safe only if the server rejects the token before it processes the request.
 * Servers that check the bearer token ahead of their handlers do; one that could
 * partly process a request and then answer {@code 401} would see it twice.
 *
 * @param <T> the type the answer is deserialized into
 */
public class PostKeycloakBuilder<T> extends BasePostBuilder<T, PostKeycloakBuilder<T>> {

	private final KeycloakContext kcc;

	PostKeycloakBuilder(final RestClient client, final Class<?> type, final KeycloakContext kcc) {
		super(client, type);
		this.kcc = kcc;
	}

	@Override
	public T execute() {
		return kcc.execute(client, this, super::execute);
	}
}
