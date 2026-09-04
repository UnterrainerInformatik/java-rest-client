package info.unterrainer.commons.restclient.exceptions;

public class RestClientException extends RuntimeException {

	private static final long serialVersionUID = 9010432923383872522L;

	/**
	 * HTTP status code the remote answered with, or {@link #NO_STATUS_CODE} when
	 * the failure carried no HTTP response at all (transport error, or a failure
	 * raised before a response was received).
	 */
	public static final int NO_STATUS_CODE = 0;

	private final int statusCode;

	public RestClientException() {
		super();
		statusCode = NO_STATUS_CODE;
	}

	/**
	 * Creates an exception that carries the HTTP status code the remote answered
	 * with, so callers can react to a specific status (a rate-limiting {@code 429},
	 * say) without parsing the message text.
	 *
	 * @param message    the detail message
	 * @param statusCode the HTTP status code returned by the remote
	 */
	public RestClientException(final String message, final int statusCode) {
		super(message);
		this.statusCode = statusCode;
	}

	public RestClientException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		statusCode = NO_STATUS_CODE;
	}

	public RestClientException(final String message, final Throwable cause) {
		super(message, cause);
		statusCode = NO_STATUS_CODE;
	}

	public RestClientException(final String message) {
		super(message);
		statusCode = NO_STATUS_CODE;
	}

	public RestClientException(final Throwable cause) {
		super(cause);
		statusCode = NO_STATUS_CODE;
	}

	/**
	 * Returns the HTTP status code the remote answered with, or
	 * {@link #NO_STATUS_CODE} if this failure carried no HTTP response.
	 *
	 * @return the HTTP status code, or {@link #NO_STATUS_CODE} if unknown
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Returns whether this failure carries an HTTP status code from the remote. A
	 * {@code false} result means the call failed without a response (transport
	 * error), which is a materially different failure than any status code.
	 *
	 * @return {@code true} if a status code is present
	 */
	public boolean hasStatusCode() {
		return statusCode != NO_STATUS_CODE;
	}
}
