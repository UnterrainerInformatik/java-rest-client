package info.unterrainer.commons.restclient;

public class PutKeycloakBuilder<T> extends BasePutBuilder<T, PutKeycloakBuilder<T>> {

	private KeycloakContext kcc;

	PutKeycloakBuilder(final RestClient client, final Class<?> type, final KeycloakContext kcc) {
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
