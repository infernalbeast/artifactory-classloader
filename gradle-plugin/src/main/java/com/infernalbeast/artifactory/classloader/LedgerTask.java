package com.infernalbeast.artifactory.classloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.LenientConfiguration;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.yaml.snakeyaml.Yaml;

import com.infernalbeast.artifactory.classloader.Dependencies.Artifact;
import com.infernalbeast.artifactory.classloader.Dependencies.Artifact.ArtifactImpl;
import com.infernalbeast.artifactory.classloader.Dependencies.ArtifactModule.ArtifactModuleImpl;
import com.infernalbeast.artifactory.classloader.Dependencies.Artifactory;
import com.infernalbeast.artifactory.classloader.Dependencies.Artifactory.ArtifactoryImpl;
import com.infernalbeast.artifactory.classloader.Dependencies.Artifactory.Credentials;
import com.infernalbeast.artifactory.classloader.Dependencies.Artifactory.Credentials.CredentialsImpl;
import com.infernalbeast.artifactory.classloader.Dependencies.DependenciesImpl;
import com.infernalbeast.lang.GenerateUtils;

public class LedgerTask extends DefaultTask {
	private Configuration configuration;
	private SourceSet sourceSet;
	private Map<String, Map<String, Map<String, String>>> credentials;
	private Integer retry;
	private String artifactDirectory = null;
	private final File outputDirectory = new File(getProject().getBuildDir(), getName());
	private File outputFile = new File(new File(outputDirectory, "META-INF"), "artifacts");

	@TaskAction
	public void action() {
		Project project = getProject();
		Logger logger = project.getLogger();
		/*
		 * If using snapshots and sub projects are uploaded we need to be able to get
		 * the unique version.
		 */
		Configuration uniqueVersionResolveConfiguration = project.getConfigurations().create("unique-version-resolve");
		for (Project subproject : project.getAllprojects()) {
			Dependency dependency = project.getDependencies()
					.create(subproject.getGroup() + ":" + subproject.getName() + ":" + subproject.getVersion());
			uniqueVersionResolveConfiguration.getDependencies().add(dependency);
		}
		LenientConfiguration uniqueConfiguration = uniqueVersionResolveConfiguration.getResolvedConfiguration()
				.getLenientConfiguration();
		Map<String, String> unqiueMap = new HashMap<>();
		for (ResolvedArtifact resolvedArtifact : uniqueConfiguration.getArtifacts()) {
			ComponentIdentifier componentIdentifier = resolvedArtifact.getId().getComponentIdentifier();
			if (componentIdentifier instanceof MavenUniqueSnapshotComponentIdentifier) {
				MavenUniqueSnapshotComponentIdentifier mavenUniqueSnapshotComponentIdentifier = (MavenUniqueSnapshotComponentIdentifier) componentIdentifier;
				unqiueMap.put(resolvedArtifact.getName(),
						mavenUniqueSnapshotComponentIdentifier.getTimestampedVersion());
			}
		}
		project.getConfigurations().remove(uniqueVersionResolveConfiguration);

		logger.debug("Configuration {}", configuration.getName());
		List<Artifactory> artifactories = new ArrayList<>();
		project.getRepositories().forEach(repository -> {
			if (repository instanceof MavenArtifactRepository) {
				MavenArtifactRepository mavenArtifactRepository = (MavenArtifactRepository) repository;
				String artifactoryUrl = mavenArtifactRepository.getUrl().toString();
				if (!artifactoryUrl.endsWith("/")) {
					artifactoryUrl += "/";
				}
				String name = mavenArtifactRepository.getName();
				Credentials credentials = null;
				if (name != null) {
					if (this.credentials != null) {
						Map<String, Map<String, String>> artifactory = this.credentials.get(name);
						if (artifactory != null) {
							CredentialsImpl credentialsImpl = new CredentialsImpl();
							credentialsImpl.setName(name);
							Map<String, String> username = artifactory.get("username");
							if (username != null) {
								String literalUsername = username.get("literal");
								if (literalUsername != null) {
									credentialsImpl.setLiteralUsername(literalUsername);
								}
								String environmentUsername = username.get("environment");
								if (environmentUsername != null) {
									credentialsImpl.setEnvironmentUsername(environmentUsername);
								}
								String systemUsername = username.get("system");
								if (systemUsername != null) {
									credentialsImpl.setSystemUsername(systemUsername);
								}
								String methodUsername = username.get("method");
								if (methodUsername != null) {
									credentialsImpl.setMethodUsername(methodUsername);
								}
							}
							Map<String, String> password = artifactory.get("password");
							if (password != null) {
								String literalPassword = password.get("literal");
								if (literalPassword != null) {
									credentialsImpl.setLiteralPassword(literalPassword);
								}
								String environmentPassword = password.get("environment");
								if (environmentPassword != null) {
									credentialsImpl.setEnvironmentPassword(environmentPassword);
								}
								String systemPassword = password.get("system");
								if (systemPassword != null) {
									credentialsImpl.setSystemPassword(systemPassword);
								}
								String methodPassword = password.get("method");
								if (methodPassword != null) {
									credentialsImpl.setMethodPassword(methodPassword);
								}
							}
							credentials = credentialsImpl;
						}
					}
				}
				try {
					artifactories.add(new ArtifactoryImpl(name, new URL(artifactoryUrl), credentials));
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
			}
		});
		List<Artifact> classPathArtifacts = new ArrayList<>();
		List<Artifact> modulePathArtifacts = new ArrayList<>();
		configuration.getResolvedConfiguration().getResolvedArtifacts().forEach(resolvedArtifact -> {
			ModuleVersionIdentifier moduleVersionIdentifier = resolvedArtifact.getModuleVersion().getId();
			ModuleIdentifier moduleIdentifier = moduleVersionIdentifier.getModule();
			String version = moduleVersionIdentifier.getVersion();
			if (version != null && !version.equals("unspecified")) {
				String groupId = moduleIdentifier.getGroup();
				String artifactId = moduleIdentifier.getName();
				String classifier = resolvedArtifact.getClassifier();
				String type = resolvedArtifact.getType();
				ComponentIdentifier componentIdentifier = resolvedArtifact.getId().getComponentIdentifier();
				String uniqueVersion;
				if (componentIdentifier instanceof MavenUniqueSnapshotComponentIdentifier) {
					MavenUniqueSnapshotComponentIdentifier mavenUniqueSnapshotComponentIdentifier = (MavenUniqueSnapshotComponentIdentifier) componentIdentifier;
					uniqueVersion = mavenUniqueSnapshotComponentIdentifier.getTimestampedVersion();
				} else {
					uniqueVersion = null;
				}
				if (moduleIdentifier.getGroup().equals(project.getGroup())) {
					String mappedUniqueVersion = unqiueMap.get(resolvedArtifact.getName());
					if (mappedUniqueVersion != null) {
						uniqueVersion = mappedUniqueVersion;
					}
				}
				File artifactFile = resolvedArtifact.getFile();
				List<String> resources = GenerateUtils.getResources(artifactFile.toPath());
				Artifact artifact = new ArtifactImpl(new ArtifactModuleImpl(groupId, artifactId), version,
						uniqueVersion, classifier, type, resources);
				logger.debug("Artifact {}", artifact);
				classPathArtifacts.add(artifact);
			}
		});

		List<String> resources = new ArrayList<>();
		sourceSet.getRuntimeClasspath().forEach(file -> {
			if (file.isDirectory()) {
				try {
					Path root = file.toPath();
					Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(final Path file,
								final java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
							StringBuilder stringBuilder = new StringBuilder();
							stringBuilder.append(file.getFileName());
							for (Path current = file.getParent(); !current.equals(root); current = current
									.getParent()) {
								stringBuilder.insert(0, current.getFileName() + "/");
							}
							String resource = stringBuilder.toString();
							logger.info("Resource {}", resource);
							resources.add(resource);
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});

		ClassLoader classLoader = getClass().getClassLoader();
		URL url = classLoader.getResource("META-INF/index.yaml");
		if (url != null) {
			try (InputStream inputStream = url.openStream()) {
				Yaml yaml = new Yaml();
				List<String> libraries = yaml.load(inputStream);
				for (String library : libraries) {
					try (ZipInputStream libraryZipInputStream = new ZipInputStream(
							classLoader.getResourceAsStream("META-INF/lib/" + library))) {
						for (ZipEntry zipEntry = libraryZipInputStream
								.getNextEntry(); zipEntry != null; zipEntry = libraryZipInputStream.getNextEntry()) {
							if (!zipEntry.isDirectory()) {
								String name = zipEntry.getName();
								resources.add(name);
							}
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		Dependencies dependencies = new DependenciesImpl(artifactories, classPathArtifacts, modulePathArtifacts,
				resources, retry, artifactDirectory);

		project.delete(outputDirectory);
		logger.lifecycle("Generating Artifactory Ledger {}", outputDirectory);
		outputFile.getParentFile().mkdirs();
		try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
			objectOutputStream.writeObject(dependencies);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setConfiguration(final Configuration configuration) {
		this.configuration = configuration;
	}

	@Internal
	public Configuration getConfiguration() {
		return configuration;
	}

	public void setSourceSet(final SourceSet sourceSet) {
		if (this.sourceSet != null) {
			throw new RuntimeException("Already set");
		}
		this.sourceSet = sourceSet;

		Project project = getProject();
		// TODO: This can be smarter by looking at the configuration dependencies
		for (Project subproject : project.getAllprojects()) {
			// Run after subproject tasks have been published
			Task publishTask = subproject.getTasks().findByName("publish");
			if (publishTask != null) {
				shouldRunAfter(publishTask);
			}
		}
		Task compileJava = project.getTasks().findByName(sourceSet.getCompileJavaTaskName());
		if (!compileJava.getDidWork()) {
			Project compileJavaProject = compileJava.getProject();
			Project thisProject = this.getProject();
			if (compileJavaProject != thisProject) {
				throw new RuntimeException(
						"Should not happen " + compileJavaProject.getName() + " == " + thisProject.getName());
			}
			compileJava.finalizedBy(this);
		}
		sourceSet.getOutput().dir(Collections.singletonMap("buildBy", getName()), outputDirectory);
	}

	@Internal
	public SourceSet getSourceSet() {
		return sourceSet;
	}

	public void setRetry(final Integer retry) {
		this.retry = retry;
	}

	@Input
	public int getRetry() {
		return retry;
	}

	public void setArtifactDirectory(final String artifactDirectory) {
		this.artifactDirectory = artifactDirectory;
	}

	@Input
	@Optional
	public String getArtifactDirectory() {
		return artifactDirectory;
	}

	public void setCredentials(final Map<String, Map<String, Map<String, String>>> credentials) {
		this.credentials = credentials;
	}

	@Input
	@Optional
	public Map<String, Map<String, Map<String, String>>> getCredentials() {
		return credentials;
	}

	public void setOutputFile(final File outputFile) {
		this.outputFile = outputFile;
	}

	@OutputFile
	@Optional
	public File getOutputFile() {
		return outputFile;
	}
}
