package com.infernalbeast.artifactory.classloader;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

class GradlePlugin implements Plugin<Project> {
	public static class PluginExtension {
		public Class<ArtifactTask> getArtifactTaskClass() {
			return ArtifactTask.class;
		}

		public Class<LedgerTask> getLedgerTaskClass() {
			return LedgerTask.class;
		}
	}

	@Override
	public void apply(final Project project) {
		project.getTasks().create("extractArtifactoryDependencies", ExtractTask.class);
		project.getExtensions().create("artifactoryClassloader", PluginExtension.class);
	}
}
