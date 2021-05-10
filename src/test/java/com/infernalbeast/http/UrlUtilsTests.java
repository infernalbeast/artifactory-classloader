package com.infernalbeast.http;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.net.URL;

import org.junit.Test;

public class UrlUtilsTests {
	@Test
	public void get() throws Exception {
		try (InputStream inputStream = UrlUtils.get(
				new URL("https://raw.githubusercontent.com/infernalbeast/bom/master/gradle.properties"), null, null,
				null, null)) {
			assertNotNull(inputStream);
		}
	}
}
