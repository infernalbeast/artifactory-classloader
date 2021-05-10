package com.infernalbeast.lang;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SystemUtils {
	public static void updateClassPath(final Path... paths) {
		updateClassPath(Arrays.asList(paths));
	}

	public static void updateClassPath(final List<Path> paths) {
		List<String> files = paths.stream().map(path -> path.toFile().getAbsolutePath()).collect(Collectors.toList());
		String appendClassPath = String.join(File.pathSeparator, files);
		String classPath = System.getProperty("java.class.path");
		if (classPath != null && !classPath.isEmpty()) {
			classPath += File.pathSeparator + appendClassPath;
		} else {
			classPath = appendClassPath;
		}
		System.setProperty("java.class.path", classPath);
	}
}
