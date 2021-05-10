package com.infernalbeast.artifactory.classloader;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.infernalbeast.artifactory.classloader.DiligentArtifactoryMainClass;

public class DiligentArtifactoryMainClassTests {
	Path libraries = Paths.get(System.getProperty("java.io.tmpdir")).resolve("libraries");

	@Before
	public void setup() {
		cleanup();
	}

	@After
	public void cleanup() {
		try {
			Files.delete(libraries);
		} catch (IOException e) {
		}
	}

	@Test
	public void test() throws Exception {
		DiligentArtifactoryMainClass.main(new String[] { libraries.toAbsolutePath().toString() });
		boolean found = false;
		try (BufferedReader reader = Files.newBufferedReader(libraries)) {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (Paths.get(line).getFileName().toString().equals("commons-io-2.8.0.jar")) {
					found = true;
					break;
				}
			}
		}
		assertTrue(found);
	}
}
