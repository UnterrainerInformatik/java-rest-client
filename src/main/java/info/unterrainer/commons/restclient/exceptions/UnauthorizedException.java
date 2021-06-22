package info.unterrainer.commons.restclient.exceptions;

public class UnauthorizedException extends RestClientException {

	private static final long serialVersionUID = -3712694195108136405L;

	public UnauthorizedException() {
		super();
	}

	public UnauthorizedException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UnauthorizedException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public UnauthorizedException(final String message) {
		super(message);
	}

	public UnauthorizedException(final Throwable cause) {
		super(cause);
	}
}
