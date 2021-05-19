package com.infernalbeast.artifactory.classloader;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.infernalbeast.artifactory.classloader.Dependencies.Artifact;
import com.infernalbeast.artifactory.classloader.Dependencies.ArtifactModule;
import com.infernalbeast.artifactory.classloader.Dependencies.Artifactory;
import com.infernalbeast.artifactory.classloader.Dependencies.Artifactory.Credentials;
import com.infernalbeast.http.UrlUtils;
import com.infernalbeast.io.PatientFile;

public class ArtifactoryClient {
	private final List<Artifactory> artifactories;
	private final Path classLoaderFile;
	private final int retry;
	private final Duration connectTimeout;
	private final Duration readTimeout;
	private final Logger logger = Logger.getLogger(getClass().getName());

	public ArtifactoryClient(final List<Artifactory> artifactories) {
		this(artifactories, 5, null, null);
	}

	public ArtifactoryClient(final List<Artifactory> artifactories, final int retry, final Duration connectTimeout,
			final Duration readTimeout) {
		this(artifactories, retry, connectTimeout, readTimeout,
				Paths.get(System.getProperty("user.home"), ".classloader"));
	}

	public ArtifactoryClient(final List<Artifactory> artifactories, final int retry, final Duration connectTimeout,
			final Duration readTimeout, final Path classLoaderFile) {
		this.artifactories = artifactories;
		this.retry = retry;
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		this.classLoaderFile = classLoaderFile;
	}

	private static InputStream getUncached(final Logger logger, final String path, final Artifactory artifactory,
			final Duration connectTimeout, final Duration readTimeout) {
		try {
			URL url = new URL(artifactory.getUrl(), path);
			Credentials credentials = artifactory.getCredentials();
			String artifactoryUsername;
			String artifactoryPassword;
			if (credentials != null) {
				artifactoryUsername = credentials.getUsername();
				artifactoryPassword = credentials.getPassword();
				logger.log(Level.INFO, "Connecting to {0}@{1}", new Object[] { artifactoryUsername, url });
			} else {
				artifactoryUsername = null;
				artifactoryPassword = null;
				logger.log(Level.INFO, "Connecting to {0}", new Object[] { url });
			}
			return UrlUtils.get(url, artifactoryUsername, artifactoryPassword, connectTimeout, readTimeout);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Return the location where this item is going to be placed.
	 */
	public Path getVirtual(final Artifact artifact) {
		return classLoaderFile.resolve(getPath(artifact));
	}

	public Path get(final Artifact artifact) {
		String path = getPath(artifact);
		return get(path);
	}

	private Path get(final String path) {
		logger.log(Level.INFO, "Populating {0}", new Object[] { path });
		Path artifactFile = classLoaderFile.resolve(path);
		new PatientFile(artifactFile, Duration.ofSeconds(10), Duration.ofSeconds(60)) {
			@Override
			protected void doPopulate(final Path file) {
				new BackOff(retry).execute(() -> {
					List<Throwable> throwables = new ArrayList<>();
					for (Artifactory artifactory : artifactories) {
						logger.log(Level.INFO, "Trying {0} for {1} with connect timeout {2} and read timeout {3}",
								new Object[] { artifactory, path, connectTimeout, readTimeout });
						try (InputStream artifactInputStream = getUncached(logger, path, artifactory, connectTimeout,
								readTimeout)) {
							logger.log(Level.INFO, "Streaming {0} for {1}", new Object[] { artifactory, path });
							try (OutputStream artifactOutputStream = Files.newOutputStream(file)) {
								artifactInputStream.transferTo(artifactOutputStream);
							}
							return null;
						} catch (Throwable t) {
							throwables.add(t);
							logger.log(Level.INFO, t.getMessage());
							try {
								Files.delete(artifactFile);
							} catch (Exception e) {
							}
						}
					}
					for (Throwable t : throwables) {
						logger.log(Level.SEVERE, "Exception", t);
					}
					throw new RuntimeException("Unable to download " + path);
				});
			}
		}.populate();
		logger.log(Level.INFO, "Populated {0}", new Object[] { artifactFile });
		return artifactFile;
	}

	public static String getPath(final ArtifactModule module) {
		return module.getGroup().replaceAll("\\.", "/") + "/" + module.getName();
	}

	public static String getPath(final Artifact artifact) {
		ArtifactModule module = artifact.getModule();
		String name = module.getName();
		String version = artifact.getVersion();
		String uniqueVersion = artifact.getUniqueVersion();
		StringBuilder path = new StringBuilder();
		path.append(getPath(module) + "/" + version + "/");
		path.append(name + "-" + (uniqueVersion != null ? uniqueVersion : version));
		String classifier = artifact.getClassifier();
		if (classifier != null) {
			path.append("-" + classifier);
		}
		String extension = artifact.getExtension();
		if (extension != null) {
			path.append("." + extension);
		} else {
			path.append(".jar");
		}
		return path.toString();
	}
}
