package info.unterrainer.commons.restclient.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RestClientExceptionTests {

	@Test
	public void statusCodeIsCarriedThrough() {
		RestClientException e = new RestClientException("HTTP call failed with 429.", 429);

		assertEquals(429, e.getStatusCode());
		assertTrue(e.hasStatusCode());
	}

	@Test
	public void exceptionWithoutStatusCodeReportsNone() {
		RestClientException e = new RestClientException("connection refused");

		assertEquals(RestClientException.NO_STATUS_CODE, e.getStatusCode());
		assertFalse(e.hasStatusCode());
	}

	@Test
	public void causeOnlyConstructorReportsNoStatusCode() {
		RestClientException e = new RestClientException(new IllegalStateException("boom"));

		assertEquals(RestClientException.NO_STATUS_CODE, e.getStatusCode());
		assertFalse(e.hasStatusCode());
	}

	@Test
	public void messageAndCauseConstructorReportsNoStatusCode() {
		RestClientException e = new RestClientException("wrapped", new IllegalStateException("boom"));

		assertEquals(RestClientException.NO_STATUS_CODE, e.getStatusCode());
		assertFalse(e.hasStatusCode());
	}

	@Test
	public void unauthorizedExceptionRemainsARestClientException() {
		UnauthorizedException e = new UnauthorizedException("no token");

		assertTrue(e instanceof RestClientException);
		assertEquals(RestClientException.NO_STATUS_CODE, e.getStatusCode());
	}
}
