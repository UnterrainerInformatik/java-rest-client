package info.unterrainer.commons.restclient;

public class DelBuilder<T> extends BaseDelBuilder<T, DelBuilder<T>> {

	DelBuilder(final RestClient client, final Class<?> type) {
		super(client, type);
	}
}
