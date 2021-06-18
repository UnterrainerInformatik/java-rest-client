package info.unterrainer.commons.restclient;

public class PostKeycloakBuilder<T> extends BasePostBuilder<T, PostKeycloakBuilder<T>> {

	private KeycloakContext kcc;

	PostKeycloakBuilder(final RestClient client, final Class<?> type, final KeycloakContext kcc) {
		super(client, type);
		this.kcc = kcc;
	}

	@Override
	public T execute() {
		kcc.update(client);
		addHeader("Authorization", "Bearer " + kcc.getAccessToken());
		return super.execute();
	}
}
