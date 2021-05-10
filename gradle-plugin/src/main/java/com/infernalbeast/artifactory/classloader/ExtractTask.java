package com.infernalbeast.artifactory.classloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.yaml.snakeyaml.Yaml;

public class ExtractTask extends DefaultTask {
	private final Project project = getProject();
	private final File outputDirectory = new File(project.getBuildDir(), getName());
	private final File librariesDirectory = new File(outputDirectory, "libraries");

	@TaskAction
	public void action() {
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream("META-INF/index.yaml");
		Yaml yaml = new Yaml();
		List<String> libraries = yaml.load(inputStream);
		outputDirectory.mkdir();
		librariesDirectory.mkdir();
		for (String library : libraries) {
			InputStream libraryInputStream = classLoader.getResourceAsStream("META-INF/lib/" + library);
			if (libraryInputStream == null) {
				throw new RuntimeException("Not found: " + library);
			}
			try {
				File file = new File(librariesDirectory, library);
				libraryInputStream.transferTo(new FileOutputStream(file));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@OutputDirectory
	public File getLibrariesDirectory() {
		return librariesDirectory;
	}
}
