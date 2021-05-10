package com.infernalbeast.lang;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DiligentMainClass {
	private static class StaticArguments extends Arguments {
		private String mainClass;

		public StaticArguments(final String[] arguments) {
			super(arguments);
		}

		@Override
		protected Integer process(final String normalized, final String[] arguments, final int position) {
			if (normalized.equals("main-class")) {
				mainClass = arguments[position + 1];
				return 1;
			}
			return super.process(normalized, arguments, position);
		}

		public String getMainClass() {
			return mainClass;
		}
	}

	public static void main(final String[] arguments) throws Exception {
		StaticArguments staticArguments = new StaticArguments(arguments);
		String classPathString = staticArguments.getClassPath();
		String modulePathString = staticArguments.getModulePath();
		List<Path> classPathPaths = Arrays.asList(classPathString == null ? new Path[] {}
				: Arrays.asList(classPathString.split(File.pathSeparator)).stream().map(path -> Paths.get(path))
						.collect(Collectors.toList()).toArray(new Path[] {}));
		List<Path> modulePathPaths = Arrays.asList(modulePathString == null ? new Path[] {}
				: Arrays.asList(modulePathString.split(File.pathSeparator)).stream().map(path -> Paths.get(path))
						.collect(Collectors.toList()).toArray(new Path[] {}));
		String mainClass = staticArguments.getMainClass();
		if (mainClass == null) {
			throw new IllegalArgumentException("Argument main-class not set");
		}
		ClassLoader classLoader = DiligentMainClass.class.getClassLoader();
		URL resourcesUrl = classLoader.getResource("META-INF/resources");
		List<String> resources = new ArrayList<>();
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourcesUrl.openStream()))) {
			for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
				resources.add(line);
			}
		}
		Thread currentThread = Thread.currentThread();
		ClassLoader threadClassLoader = currentThread.getContextClassLoader();
		try {
			ClassLoader parentClassLoader = DiligentMainClass.class.getClassLoader();
			ClassLoader staticClassLoader = new DiligentClassLoader(parentClassLoader, classPathPaths, modulePathPaths,
					staticArguments.getAddExports(), staticArguments.getAddOpens(), staticArguments.getAddReads(),
					resources);
			currentThread.setContextClassLoader(staticClassLoader);
			Class<?> mainClassClass = staticClassLoader.loadClass(mainClass);
			Method mainMethod = mainClassClass.getDeclaredMethod("main", String[].class);
			mainMethod.invoke(null, new Object[] { staticArguments.getOtherArguments().toArray(new String[] {}) });
		} finally {
			currentThread.setContextClassLoader(threadClassLoader);
		}
	}
}
