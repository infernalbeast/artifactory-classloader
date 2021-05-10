package com.infernalbeast.artifactory.classloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class DiligentArtifactoryMainClass {
	private static class DiligentArtifactoryArguments extends ArtifactoryArguments {
		private Integer threads;

		public DiligentArtifactoryArguments(final String[] arguments) {
			super(arguments);
		}

		@Override
		protected Integer process(final String normalized, final String[] arguments, final int position) {
			if (normalized.equals("artifact-threads")) {
				if (threads == null) {
					threads = Integer.valueOf(arguments[position + 1]);
					return 1;
				}
			}
			return super.process(normalized, arguments, position);
		}

		public Integer getThreads() {
			return threads;
		}
	}

	public static void main(final String[] arguments) throws IOException, ClassNotFoundException, NoSuchMethodException,
			InvocationTargetException, IllegalAccessException, InstantiationException {
		DiligentArtifactoryArguments artifactoryArguments = new DiligentArtifactoryArguments(arguments);
		invoke(artifactoryArguments.getDirectory(), artifactoryArguments.getRetry(),
				artifactoryArguments.getClassPath(), artifactoryArguments.getModulePath(),
				artifactoryArguments.getAddExports(), artifactoryArguments.getAddOpens(),
				artifactoryArguments.getAddReads(), artifactoryArguments.getThreads(),
				artifactoryArguments.getOtherArguments());
	}

	public static void invoke(final Path directory, final Integer retry, final String classPathString,
			final String modulePathString, final Map<String, Map<String, List<String>>> addExports,
			final Map<String, Map<String, List<String>>> addOpens, final Map<String, List<String>> addReads,
			final Integer threads, final List<String> otherArguments) throws IOException, ClassNotFoundException,
			NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
		Thread currentThread = Thread.currentThread();
		ClassLoader classLoader = currentThread.getContextClassLoader();
		List<Path> classPathPaths = Arrays.asList(classPathString == null ? new Path[] {}
				: Arrays.asList(classPathString.split(File.pathSeparator)).stream().map(path -> Paths.get(path))
						.collect(Collectors.toList()).toArray(new Path[] {}));
		List<Path> modulePathPaths = Arrays.asList(modulePathString == null ? new Path[] {}
				: Arrays.asList(modulePathString.split(File.pathSeparator)).stream().map(path -> Paths.get(path))
						.collect(Collectors.toList()).toArray(new Path[] {}));
		ClassLoader diligentClassLoader = new ArtifactoryClassLoader(classLoader, retry, directory, classPathPaths,
				modulePathPaths, addExports, addOpens, addReads);
		Method loadAllMethod = diligentClassLoader.getClass().getDeclaredMethod("loadAll", Integer.class);
		loadAllMethod.invoke(diligentClassLoader, new Object[] { threads });

		currentThread.setContextClassLoader(diligentClassLoader);
		try {
			Enumeration<URL> manifestUrls = classLoader.getResources("META-INF/MANIFEST.MF");
			String classPathClass = null;
			do {
				URL url = manifestUrls.nextElement();
				try (InputStream inputStream = url.openStream()) {
					Manifest manifest = new Manifest(inputStream);
					Attributes attributes = manifest.getMainAttributes();
					classPathClass = attributes.getValue("Diligent-Artifactory-Main-Class");
				}
			} while (classPathClass == null && manifestUrls.hasMoreElements());
			Class<?> mainClass = diligentClassLoader.loadClass(classPathClass);
			Method mainMethod = mainClass.getDeclaredMethod("main", new Class<?>[] { String[].class });
			mainMethod.invoke(null, new Object[] { otherArguments.toArray(new String[] {}) });
		} finally {
			// Reset to previous
			currentThread.setContextClassLoader(classLoader);
		}
	}
}
