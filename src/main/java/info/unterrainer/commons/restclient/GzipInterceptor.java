package info.unterrainer.commons.restclient;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.GzipSink;
import okio.GzipSource;
import okio.Okio;

public class GzipInterceptor implements Interceptor {
	@Override
	public Response intercept(final Interceptor.Chain chain) throws IOException {

		Request originalRequest = chain.request();

		if (acceptGzipAlreadySetAndNoContentToZip(originalRequest))
			return chain.proceed(originalRequest);

		Builder newRequestBuilder = originalRequest.newBuilder().addHeader("Accept-Encoding", "gzip");
		if (contentToZip(originalRequest))
			newRequestBuilder.method(originalRequest.method(), gzip(originalRequest.body()));

		// Make the call.
		Response response = chain.proceed(newRequestBuilder.build());

		if (isGzipped(response))
			return unzip(response);
		else
			return response;
	}

	private boolean acceptGzipAlreadySetAndNoContentToZip(final Request originalRequest) {
		return originalRequest.header("Accept-Encoding") == "gzip"
				&& (originalRequest.body() == null || originalRequest.header("Content-Encoding") != "gzip");
	}

	private RequestBody gzip(final RequestBody body) {
		return new RequestBody() {
			@Override
			public MediaType contentType() {
				return body.contentType();
			}

			@Override
			public long contentLength() {
				return -1; // We don't know the compressed length in advance!
			}

			@Override
			public void writeTo(final BufferedSink sink) throws IOException {
				BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
				body.writeTo(gzipSink);
				gzipSink.close();
			}
		};
	}

	private Response unzip(final Response response) throws IOException {

		if (response.body() == null)
			return response;

		GzipSource gzipSource = new GzipSource(response.body().source());
		String bodyString = Okio.buffer(gzipSource).readUtf8();

		ResponseBody responseBody = ResponseBody.create(response.body().contentType(), bodyString);

		Headers strippedHeaders = response.headers()
				.newBuilder()
				.removeAll("Content-Encoding")
				.removeAll("Content-Length")
				.build();
		return response.newBuilder().headers(strippedHeaders).body(responseBody).message(response.message()).build();

	}

	private Boolean isGzipped(final Response response) {
		return response.header("Content-Encoding") != null && response.header("Content-Encoding").equals("gzip");
	}

	private boolean contentToZip(final Request originalRequest) {
		return originalRequest.body() != null && originalRequest.header("Content-Encoding") == "gzip";
	}
}
