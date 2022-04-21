package info.unterrainer.commons.restclient;

public class PutBuilder<T> extends BasePutBuilder<T, PutBuilder<T>> {

	PutBuilder(final RestClient client, final Class<?> type) {
		super(client, type);
	}
}
