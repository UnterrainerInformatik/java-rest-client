package info.unterrainer.commons.restclient;

public abstract class BaseKeycloakBuilder<T, R extends BaseKeycloakBuilder<T, R>> extends BaseBuilder<T, R> {

	private final KeycloakContext kcc;

	BaseKeycloakBuilder(final RestClient client, final Class<?> type, final KeycloakContext kcc) {
		super(client, type);
		this.kcc = kcc;
	}

	@Override
	public T execute() {
		kcc.update(client);
		return super.execute();
	}
}
