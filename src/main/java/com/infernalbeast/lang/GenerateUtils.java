package com.infernalbeast.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.infernalbeast.artifactory.classloader.ArtifactoryClient;
import com.infernalbeast.artifactory.classloader.Dependencies;
import com.infernalbeast.artifactory.classloader.Dependencies.Artifact;
import com.infernalbeast.artifactory.classloader.Dependencies.Artifactory;

public class GenerateUtils {
	private static final boolean DEBUG = System.getProperty("debug") != null
			|| System.getProperty("debug." + GenerateUtils.class.getName()) != null;

	public interface ModuleAndClassPathResources {
		Map<GenerateUrl, Collection<String>> getModulePath();

		Map<GenerateUrl, Collection<String>> getClassPath();

		Collection<String> getResources();
	}

	public static ModuleAndClassPathResources getResourcesByLazyUrl(final ClassLoader classLoader, final Integer retry,
			final Path directory) {
		String artifactRetry = System.getProperty("artifact-retry");
		Integer evaluatedRetry;
		if (artifactRetry != null) {
			evaluatedRetry = Integer.valueOf(artifactRetry);
		} else {
			evaluatedRetry = retry;
		}
		String artifactDirectory = System.getProperty("artifact-directory");
		Path evaluatedDirectory;
		if (artifactDirectory != null) {
			evaluatedDirectory = Paths.get(artifactDirectory);
		} else {
			evaluatedDirectory = directory;
		}
		return getEvaluatedResourcesByUrl(classLoader, evaluatedRetry, evaluatedDirectory);
	}

	private static ModuleAndClassPathResources getEvaluatedResourcesByUrl(final ClassLoader classLoader,
			final Integer retry, final Path classLoaderFile) {
		try {
			Collection<String> allResources = new ArrayList<>();
			String resource = "META-INF/artifacts";
			Enumeration<URL> artifactorResources = classLoader.getResources(resource);
			final List<Path> paths = new ArrayList<>();
			final Map<GenerateUrl, Collection<String>> modulePathResourcesByUrl = new HashMap<>();
			final Map<GenerateUrl, Collection<String>> classPathResourcesByUrl = new HashMap<>();
			if (DEBUG) {
				System.out.println("ARTIFACT RESOURCES: " + artifactorResources.hasMoreElements());
			}
			if (artifactorResources.hasMoreElements()) {
				Set<URL> processed = new HashSet<>();
				do {
					URL artifactoryUrl = artifactorResources.nextElement();
					if (!processed.contains(artifactoryUrl)) {
						processed.add(artifactoryUrl);
						try (ObjectInputStream objectInputStream = new ObjectInputStream(artifactoryUrl.openStream())) {
							Dependencies dependencies = (Dependencies) objectInputStream.readObject();
							final int evaluatedRetry;
							if (retry == null) {
								Integer definedRetry = dependencies.getRetry();
								if (definedRetry == null || definedRetry == -1) {
									evaluatedRetry = 1;
								} else {
									evaluatedRetry = definedRetry;
								}
							} else {
								evaluatedRetry = retry;
							}
							Duration evaluatedConnectTimeout = Duration.ofSeconds(5);
							Duration evaluatedReadTimeout = Duration.ofSeconds(10);
							final Path evaluatedClassLoaderFile;
							if (classLoaderFile == null) {
								Path userHomeFile = Paths.get(System.getProperty("user.home"));
								if (Files.exists(userHomeFile)) {
									String name = dependencies.getDirectory();
									if (name == null) {
										name = ".artifacts";
									}
									evaluatedClassLoaderFile = userHomeFile.resolve(name);
								} else {
									evaluatedClassLoaderFile = Files.createTempDirectory("artifacts");
								}
							} else {
								evaluatedClassLoaderFile = classLoaderFile;
							}
							if (!Files.exists(evaluatedClassLoaderFile)) {
								Files.createDirectory(evaluatedClassLoaderFile);
							}
							List<Artifactory> artifactories = dependencies.getArtifactories();
							ArtifactoryClient artifactoryClient = new ArtifactoryClient(artifactories, evaluatedRetry,
									evaluatedConnectTimeout, evaluatedReadTimeout, evaluatedClassLoaderFile);
							add(artifactoryClient, paths, classPathResourcesByUrl,
									dependencies.getClassPathArtifacts());
							add(artifactoryClient, paths, modulePathResourcesByUrl,
									dependencies.getModulePathArtifacts());
							List<String> resources = dependencies.getResources();
							if (resources != null) {
								allResources.addAll(resources);
							}
						}
					}
				} while (artifactorResources.hasMoreElements());
			} else {
				System.out.println(String.format("%s not found", resource));
			}
			try {
				Enumeration<URL> urls = classLoader.getResources("META-INF/resources");
				while (urls.hasMoreElements()) {
					URL url = urls.nextElement();
					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(url.openConnection().getInputStream()))) {
						for (String line = reader.readLine(); line != null; line = reader.readLine()) {
							allResources.add(line);
						}
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			SystemUtils.updateClassPath(paths);
			return new ModuleAndClassPathResources() {
				@Override
				public Map<GenerateUrl, Collection<String>> getModulePath() {
					return modulePathResourcesByUrl;
				}

				@Override
				public Map<GenerateUrl, Collection<String>> getClassPath() {
					return classPathResourcesByUrl;
				}

				@Override
				public Collection<String> getResources() {
					return allResources;
				}
			};
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void add(final ArtifactoryClient artifactoryClient, final List<Path> paths,
			final Map<GenerateUrl, Collection<String>> classPathResourcesByUrl,
			final List<Artifact> classPathArtifacts) {
		for (Artifact artifact : classPathArtifacts) {
			if (DEBUG) {
				System.out.println("ARTIFACT: " + artifact.getModule());
			}
			paths.add(artifactoryClient.getVirtual(artifact));
			List<String> resources = artifact.getResources();
			GenerateUrl generateUrl = new GenerateUrl() {
				private URL url;

				@Override
				public URL get() {
					if (url == null) {
						try {
							url = artifactoryClient.get(artifact).toUri().toURL();
						} catch (MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
					return url;
				}

				@Override
				public String toString() {
					return artifact.toString();
				}
			};
			classPathResourcesByUrl.put(generateUrl, resources);
		}
	}

	public static Map<GenerateUrl, Collection<String>> getMapping(final Path[] libraries) {
		Map<GenerateUrl, Collection<String>> map = new HashMap<>();
		for (Path library : libraries) {
			map.put(new GenerateUrl() {
				@Override
				public URL get() {
					try {
						return library.toUri().toURL();
					} catch (MalformedURLException e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				public String toString() {
					return library.toString();
				}
			}, getResources(library));
		}
		return map;
	}

	public static List<String> getResources(final Path library) {
		List<String> resources = new ArrayList<>();
		try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(library))) {
			for (ZipEntry zipEntry = zipInputStream.getNextEntry(); zipEntry != null; zipEntry = zipInputStream
					.getNextEntry()) {
				if (!zipEntry.isDirectory()) {
					String resource = zipEntry.getName();
					resources.add(resource);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return resources;
	}
}
