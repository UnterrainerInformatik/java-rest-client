package info.unterrainer.commons.restclient;

public class GetBuilder<T> extends BaseGetBuilder<T, GetBuilder<T>> {

	GetBuilder(final RestClient client, final Class<?> type) {
		super(client, type);
	}
}
