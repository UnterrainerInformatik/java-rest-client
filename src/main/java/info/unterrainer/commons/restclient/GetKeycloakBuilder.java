package info.unterrainer.commons.restclient;

public class GetKeycloakBuilder<T> extends BaseGetBuilder<T, GetKeycloakBuilder<T>> {

	private KeycloakContext kcc;

	GetKeycloakBuilder(final RestClient client, final Class<?> type, final KeycloakContext kcc) {
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
