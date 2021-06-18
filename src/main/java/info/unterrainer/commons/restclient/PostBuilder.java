package info.unterrainer.commons.restclient;

public class PostBuilder<T> extends BasePostBuilder<T, PostBuilder<T>> {

	PostBuilder(final RestClient client, final Class<?> type) {
		super(client, type);
	}
}
