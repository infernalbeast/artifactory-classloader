package com.infernalbeast.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UrlUtils {
	private static Logger LOGGER = Logger.getLogger(UrlUtils.class.getName());

	public static InputStream get(final URL url, final String username, final String password,
			final Duration connectTimeout, final Duration readTimeout) {
		InputStream inputStream;
		int statusCode;
		if (true) {
			try {
				URLConnection connection = url.openConnection();
				if (connection instanceof HttpURLConnection) {
					HttpURLConnection httpUrlConnection = (HttpURLConnection) connection;
					httpUrlConnection.setInstanceFollowRedirects(true);
				}
				connection.setAllowUserInteraction(false);
				if (username != null || password != null) {
					String encoded = new String(
							Base64.getEncoder().encode((username + ":" + password).getBytes(StandardCharsets.UTF_8)),
							StandardCharsets.UTF_8);
					connection.addRequestProperty("Authorization", "Basic " + encoded);
				}
				if (connectTimeout != null) {
					connection.setConnectTimeout((int) connectTimeout.toMillis());
				}
				if (readTimeout != null) {
					connection.setReadTimeout((int) readTimeout.toMillis());
				}
				inputStream = connection.getInputStream();
				if (connection instanceof HttpURLConnection) {
					statusCode = ((HttpURLConnection) connection).getResponseCode();
				} else {
					statusCode = 200;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			HttpClient.Builder httpClientBuilder = HttpClient.newBuilder().version(Version.HTTP_2)
					.followRedirects(Redirect.NORMAL);
			try {
				HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder(url.toURI());
				if (username != null && password != null) {
					String encoded = new String(
							Base64.getEncoder().encode((username + ":" + password).getBytes(StandardCharsets.UTF_8)),
							StandardCharsets.UTF_8);
					httpRequestBuilder.setHeader("Authorization", "Basic " + encoded);
				}
				if (connectTimeout != null) {
					httpClientBuilder.connectTimeout(connectTimeout);
				}
				if (readTimeout != null) {
					httpRequestBuilder.timeout(readTimeout);
				}
				HttpResponse<InputStream> inputStreamHttpResponse = httpClientBuilder.build()
						.send(httpRequestBuilder.build(), BodyHandlers.ofInputStream());
				inputStream = inputStreamHttpResponse.body();
				statusCode = inputStreamHttpResponse.statusCode();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		if ((statusCode & 200) == 200) {
			LOGGER.log(Level.INFO, "Downloaded {0}", new Object[] { url });
			return inputStream;
		} else {
			LOGGER.log(Level.INFO, "Status Code {0}", new Object[] { statusCode });
			try {
				inputStream.close();
			} catch (IOException e) {
			}
			throw new HttpException(statusCode);
		}
	}
}
