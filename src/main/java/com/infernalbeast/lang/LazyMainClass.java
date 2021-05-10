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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LazyMainClass {
	private static class DyanmicArguments extends Arguments {
		private String mainClass;
		private String lazyClassPath;
		private String lazyModulePath;

		public DyanmicArguments(final String[] arguments) {
			super(arguments);
		}

		@Override
		protected Integer process(final String normalized, final String[] arguments, final int position) {
			switch (normalized) {
			case "main-class":
				mainClass = arguments[position + 1];
				return 1;
			case "lazy-class-path":
				lazyClassPath = arguments[position + 1];
				return 1;
			case "lazy-module-path":
				lazyModulePath = arguments[position + 1];
				return 1;
			}
			return super.process(normalized, arguments, position);
		}

		public String getMainClass() {
			return mainClass;
		}

		public String getLazyClassPath() {
			return lazyClassPath;
		}

		public String getLazyModulePath() {
			return lazyModulePath;
		}
	}

	public static void main(final String[] arguments) throws Exception {
		DyanmicArguments staticArguments = new DyanmicArguments(arguments);
		ClassLoader classLoader = LazyMainClass.class.getClassLoader();
		URL resourcesUrl = classLoader.getResource("META-INF/resources");
		List<String> resources = new ArrayList<>();
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourcesUrl.openStream()))) {
			for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
				resources.add(line);
			}
		}
		String lazyClassPathString = staticArguments.getLazyClassPath();
		String lazyModulePathString = staticArguments.getLazyModulePath();
		Map<GenerateUrl, Collection<String>> lazyClassPathPaths = new HashMap<>(
				GenerateUtils.getMapping(lazyClassPathString == null ? new Path[] {}
						: Arrays.asList(lazyClassPathString.split(File.pathSeparator)).stream()
								.map(path -> Paths.get(path)).collect(Collectors.toList()).toArray(new Path[] {})));
		lazyClassPathPaths.put(null, resources);
		Map<GenerateUrl, Collection<String>> lazyModulePathPaths = GenerateUtils
				.getMapping(lazyModulePathString == null ? new Path[] {}
						: Arrays.asList(lazyModulePathString.split(File.pathSeparator)).stream()
								.map(path -> Paths.get(path)).collect(Collectors.toList()).toArray(new Path[] {}));
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
		Thread currentThread = Thread.currentThread();
		ClassLoader threadClassLoader = currentThread.getContextClassLoader();
		try {
			ClassLoader parentClassLoader = LazyMainClass.class.getClassLoader();
			ClassLoader dynamicClassLoader = new LazyClassLoader(parentClassLoader, lazyClassPathPaths,
					lazyModulePathPaths, resources, classPathPaths, modulePathPaths, staticArguments.getAddExports(),
					staticArguments.getAddOpens(), staticArguments.getAddReads());
			currentThread.setContextClassLoader(dynamicClassLoader);
			Class<?> mainClassClass = dynamicClassLoader.loadClass(mainClass);
			Method mainMethod = mainClassClass.getDeclaredMethod("main", String[].class);
			mainMethod.invoke(null, new Object[] { staticArguments.getOtherArguments().toArray(new String[] {}) });
		} finally {
			currentThread.setContextClassLoader(threadClassLoader);
		}
	}
}
