package info.unterrainer.commons.restclient;

public class DelKeycloakBuilder<T> extends BaseDelBuilder<T, DelKeycloakBuilder<T>> {

	private KeycloakContext kcc;

	DelKeycloakBuilder(final RestClient client, final Class<?> type, final KeycloakContext kcc) {
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
