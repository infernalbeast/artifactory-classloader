package com.infernalbeast.artifactory.classloader;

import java.io.Console;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public interface Dependencies {
	List<Artifactory> getArtifactories();

	List<Artifact> getModulePathArtifacts();

	List<Artifact> getClassPathArtifacts();

	List<String> getResources();

	Integer getRetry();

	String getDirectory();

	public class DependenciesImpl implements Dependencies, Serializable {
		private static final long serialVersionUID = 1L;
		private List<Artifactory> artifactories;
		private List<Artifact> modulePathArtifacts;
		private List<Artifact> classPathArtifacts;
		private List<String> resources;
		private Integer retry;
		private String directory;

		public DependenciesImpl() {
		}

		public DependenciesImpl(final List<Artifactory> artifactories, final List<Artifact> classPathArtifacts,
				final List<Artifact> modulePathArtifacts, final List<String> resources, final Integer retries,
				final String directory) {
			this.artifactories = artifactories;
			this.modulePathArtifacts = modulePathArtifacts;
			this.classPathArtifacts = classPathArtifacts;
			this.resources = resources;
			this.retry = retries;
			this.directory = directory;
		}

		@Override
		public List<Artifactory> getArtifactories() {
			return artifactories;
		}

		@Override
		public List<Artifact> getModulePathArtifacts() {
			return modulePathArtifacts;
		}

		@Override
		public List<Artifact> getClassPathArtifacts() {
			return classPathArtifacts;
		}

		@Override
		public List<String> getResources() {
			return resources;
		}

		@Override
		public Integer getRetry() {
			return retry;
		}

		@Override
		public String getDirectory() {
			return directory;
		}

		@Override
		public String toString() {
			return "artifactories(" + artifactories + ") modulePathArtifacts(" + modulePathArtifacts
					+ ") classPathArtifacts(" + classPathArtifacts + ") retries(" + retry + ") directory(" + directory
					+ ")";
		}
	}

	public interface Artifactory {
		public interface Credentials {
			String getUsername();

			String getPassword();

			public class CredentialsImpl implements Credentials, Serializable {
				private static final long serialVersionUID = 1L;
				private String name;
				private String environmentUsername;
				private String environmentPassword;
				private String systemUsername;
				private String systemPassword;
				private String literalUsername;
				private String literalPassword;
				private String methodUsername;
				private String methodPassword;

				public void setName(final String name) {
					this.name = name;
				}

				public CredentialsImpl setLiteralUsername(final String literalUsername) {
					this.literalUsername = literalUsername;
					return this;
				}

				public CredentialsImpl setEnvironmentUsername(final String environmentUsername) {
					this.environmentUsername = environmentUsername;
					return this;
				}

				public CredentialsImpl setSystemUsername(final String systemUsername) {
					this.systemUsername = systemUsername;
					return this;
				}

				public CredentialsImpl setMethodUsername(final String methodUsername) {
					this.methodUsername = methodUsername;
					return this;
				}

				@Override
				public String getUsername() {
					if (systemUsername != null) {
						String username = System.getProperty(systemUsername);
						if (username != null) {
							return username;
						}
					}
					if (environmentUsername != null) {
						String username = System.getenv(environmentUsername);
						if (username != null) {
							return username;
						}
					}
					if (literalUsername != null) {
						return literalUsername;
					}
					if (methodUsername != null) {
						Method staticMethod = ReflectionUtils.getFullyQualifiedMethod(methodUsername);
						try {
							String username = (String) staticMethod.invoke(null);
							return username;
						} catch (InvocationTargetException | IllegalAccessException e) {
							throw new RuntimeException(e);
						}
					}
					Console console = System.console();
					if (console != null) {
						System.out.print("Enter username for " + name + ": ");
						literalUsername = console.readLine();
					}
					return literalUsername;
				}

				public CredentialsImpl setLiteralPassword(final String literalPassword) {
					this.literalPassword = literalPassword;
					return this;
				}

				public CredentialsImpl setEnvironmentPassword(final String environmentPassword) {
					this.environmentPassword = environmentPassword;
					return this;
				}

				public CredentialsImpl setSystemPassword(final String systemPassword) {
					this.systemPassword = systemPassword;
					return this;
				}

				public CredentialsImpl setMethodPassword(final String methodPassword) {
					this.methodPassword = methodPassword;
					return this;
				}

				@Override
				public String getPassword() {
					if (systemPassword != null) {
						String password = System.getProperty(systemPassword);
						if (password != null) {
							return password;
						}
					}
					if (environmentPassword != null) {
						String password = System.getenv(environmentPassword);
						if (password != null) {
							return password;
						}
					}
					if (literalPassword != null) {
						return literalPassword;
					}
					if (methodPassword != null) {
						Method staticMethod = ReflectionUtils.getFullyQualifiedMethod(methodPassword);
						try {
							String password = (String) staticMethod.invoke(null);
							return password;
						} catch (InvocationTargetException | IllegalAccessException e) {
							throw new RuntimeException(e);
						}
					}
					Console console = System.console();
					if (console != null) {
						System.out.print("Enter password for " + name + ": ");
						literalPassword = new String(console.readPassword());
					}
					return literalPassword;
				}

				@Override
				public String toString() {
					return "environmentUsername(" + environmentUsername + ") environmentPassword(" + environmentPassword
							+ ") systemUsername(" + systemUsername + ") systemPassword(" + systemPassword
							+ ") literalUsername(" + literalUsername + ") literalPassword(" + literalPassword
							+ ") methodUsername(" + methodUsername + ") methodPassword(" + methodPassword + ")";
				}
			}
		}

		String getName();

		URL getUrl();

		Credentials getCredentials();

		public class ArtifactoryImpl implements Artifactory, Serializable {
			private static final long serialVersionUID = 1L;
			private String name;
			private URL url;
			private Credentials credentials;

			public ArtifactoryImpl() {
			}

			public ArtifactoryImpl(final String name, final URL url, final Credentials credentials) {
				this.name = name;
				this.url = url;
				this.credentials = credentials;
			}

			@Override
			public URL getUrl() {
				return url;
			}

			@Override
			public Credentials getCredentials() {
				return credentials;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String toString() {
				return "name(" + name + ") url(" + url + ") credentials(" + credentials + ")";
			}
		}
	}

	public interface ArtifactModule {
		String getGroup();

		String getName();

		public class ArtifactModuleImpl implements ArtifactModule, Serializable {
			private static final long serialVersionUID = 1L;
			private String group;
			private String name;

			public ArtifactModuleImpl() {
			}

			public ArtifactModuleImpl(final String group, final String name) {
				this.group = group;
				this.name = name;
			}

			@Override
			public String getGroup() {
				return group;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String toString() {
				return "group(" + group + ") name(" + name + ")";
			}

			@Override
			public int hashCode() {
				return Objects.hash(group, name);
			}

			@Override
			public boolean equals(final Object object) {
				if (object instanceof ArtifactModule) {
					ArtifactModule artifactModule = (ArtifactModule) object;
					return group.equals(artifactModule.getGroup()) && name.equals(artifactModule.getName());
				}
				return false;
			}
		}
	}

	public interface Artifact {
		ArtifactModule getModule();

		String getVersion();

		String getUniqueVersion();

		String getClassifier();

		String getExtension();

		List<String> getResources();

		public class ArtifactImpl implements Artifact, Serializable {
			private static final long serialVersionUID = 1L;
			private ArtifactModule module;
			private String version;
			private String uniqueVersion;
			private String classifier;
			private String extension;
			private List<String> resources;

			public ArtifactImpl() {
			}

			public ArtifactImpl(final ArtifactModule module, final String version, final String uniqueVersion,
					final String classifier, final String extension, final List<String> resources) {
				this.module = module;
				this.version = version;
				this.uniqueVersion = uniqueVersion;
				this.classifier = classifier;
				this.extension = extension;
				this.resources = resources;
			}

			@Override
			public ArtifactModule getModule() {
				return module;
			}

			@Override
			public String getVersion() {
				return version;
			}

			@Override
			public String getUniqueVersion() {
				return uniqueVersion;
			}

			@Override
			public String getExtension() {
				return extension;
			}

			@Override
			public String getClassifier() {
				return classifier;
			}

			@Override
			public List<String> getResources() {
				return resources;
			}

			@Override
			public String toString() {
				return "module(" + module + ") version(" + version + ") uniqueVersion(" + uniqueVersion + ") extension("
						+ extension + ") classifier(" + classifier + ")";
			}
		}
	}
}
