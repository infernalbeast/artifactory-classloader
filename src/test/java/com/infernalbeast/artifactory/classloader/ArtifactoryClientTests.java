package com.infernalbeast.artifactory.classloader;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.infernalbeast.artifactory.classloader.Dependencies.Artifact.ArtifactImpl;
import com.infernalbeast.artifactory.classloader.Dependencies.ArtifactModule.ArtifactModuleImpl;
import com.infernalbeast.artifactory.classloader.Dependencies.Artifactory.ArtifactoryImpl;

public class ArtifactoryClientTests {
	Path path = Paths.get(System.getProperty("java.io.tmpdir"), "classloader");

	@Before
	public void setup() throws IOException {
		cleanup();
	}

	@After
	public void cleanup() throws IOException {
		if (Files.exists(path)) {
			Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
	}

	@Test
	public void get() throws Exception {
		ArtifactoryClient artifactoryClient = new ArtifactoryClient(
				Arrays.asList(new ArtifactoryImpl(null, new URL("https://repo1.maven.org/maven2/"), null)), 1, null,
				null, path);
		Path path = artifactoryClient.get(
				new ArtifactImpl(new ArtifactModuleImpl("commons-io", "commons-io"), "2.8.0", null, null, null, null));
		assertNotNull(path);
	}

	@Test
	public void get_snapshot() throws Exception {
		ArtifactoryClient artifactoryClient = new ArtifactoryClient(
				Arrays.asList(new ArtifactoryImpl(null,
						new URL("https://repository.apache.org/content/repositories/snapshots/"), null)),
				1, null, null, path);
		Path snapshotPath = artifactoryClient.get(new ArtifactImpl(new ArtifactModuleImpl("org.swssf", "bindings"),
				"2.0-SNAPSHOT", "2.0-20120816.090617-2", null, "pom", null));
		assertNotNull(snapshotPath);
	}
}