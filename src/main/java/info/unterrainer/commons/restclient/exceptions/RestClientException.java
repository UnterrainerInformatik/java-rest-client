package info.unterrainer.commons.restclient.exceptions;

public class RestClientException extends RuntimeException {

	private static final long serialVersionUID = 9010432923383872522L;

	public RestClientException() {
		super();
	}

	public RestClientException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public RestClientException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public RestClientException(final String message) {
		super(message);
	}

	public RestClientException(final Throwable cause) {
		super(cause);
	}
}
