package com.infernalbeast.artifactory.classloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;

import com.infernalbeast.artifactory.classloader.ArtifactoryMainClass.Configuration;

public class ArtifactTask extends DefaultTask {
	private Jar jar;
	private boolean lazy = true;
	private final Project project = getProject();
	private final File outputDirectory = new File(project.getBuildDir(), getName());
	private final File configurationFile = new File(outputDirectory, "configuration");
	private final ExtractTask extractTask = (ExtractTask) project.getTasks()
			.getByName("extractArtifactoryDependencies");
	{
		dependsOn(extractTask);
	}

	@TaskAction
	public void action() {
		outputDirectory.mkdir();
		Manifest manifest = jar.getManifest();
		String mainClass = (String) manifest.getAttributes().get("Main-Class");
		if (mainClass != null) {
			project.getLogger().lifecycle("Updating manifest: " + jar.getName());
			Map<String, String> attributes = new HashMap<>();
			attributes.put("Main-Class", ArtifactoryMainClass.class.getCanonicalName());
			attributes.put("Diligent-Artifactory-Main-Class", mainClass);
			attributes.put("Lazy-Artifactory-Main-Class", mainClass);
			manifest.attributes(attributes);
		}
		project.getLogger().lifecycle("Adding artifactory files: " + jar.getName());
		for (File file : getLibraryDirectory().listFiles()) {
			jar.from(project.zipTree(file));
		}
		try {
			try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(
					new FileOutputStream(configurationFile))) {
				Configuration configuration = new Configuration(lazy);
				objectOutputStream.writeObject(configuration);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		jar.into("META-INF", copySpec -> {
			copySpec.from(configurationFile).rename(configurationFile.getName(), "artifact-configuration");
		});
	}

	public void setJar(final Jar jar) {
		if (this.jar != null) {
			throw new RuntimeException("Already Set");
		}
		this.jar = jar;
		Project jarProject = jar.getProject();
		Project thisProject = this.getProject();
		if (jarProject != thisProject) {
			throw new RuntimeException("Should not happen " + jarProject.getName() + " == " + thisProject.getName());
		}
		jar.dependsOn(this);
	}

	@Internal
	public Jar getJar() {
		return jar;
	}

	public void setLazy(final boolean lazy) {
		this.lazy = lazy;
	}

	@Input
	public boolean isLazy() {
		return lazy;
	}

	@InputDirectory
	public File getLibraryDirectory() {
		return extractTask.getLibrariesDirectory();
	}
}
