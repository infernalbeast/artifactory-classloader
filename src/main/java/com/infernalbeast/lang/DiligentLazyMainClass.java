package com.infernalbeast.lang;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class serves as proxy for the main class.
 *
 * By default libraries are lazy loaded, this primes libraries up front.
 *
 * java
 * -Djava.system.class.loader=com.infernalbeast.lang.ArtifactoryDynamicClassLoader
 * com.infernalbeast.lang.DiligentMainClass --main-class <MyMainClass>
 **/
public class DiligentLazyMainClass {
	private static final Logger LOGGER = Logger.getLogger(DiligentLazyMainClass.class.getName());

	public static void main(final String[] arguments) throws Exception {
		LOGGER.log(Level.INFO, "Diligent main class loaded");
		List<String> otherArguments = new ArrayList<>();
		String mainClass = null;
		Integer threads = null;
		for (int argumentCount = 0; argumentCount < arguments.length; argumentCount++) {
			String argument = arguments[argumentCount];
			if (argument.startsWith("-")) {
				String normalizedArgument;
				if (argument.startsWith("--")) {
					normalizedArgument = argument.substring(2);
				} else {
					normalizedArgument = argument.substring(1);
				}
				switch (normalizedArgument) {
				case "artifact-threads":
					if (threads == null) {
						argumentCount++;
						threads = Integer.valueOf(arguments[argumentCount]);
					} else {
						otherArguments.add(argument);
					}
					break;
				case "main-class":
					if (mainClass == null) {
						argumentCount++;
						mainClass = arguments[argumentCount];
					} else {
						otherArguments.add(argument);
					}
					break;
				default:
					otherArguments.add(argument);
				}
			} else {
				otherArguments.add(argument);
			}
		}
		ClassLoader classLoader = DiligentLazyMainClass.class.getClassLoader();
		LOGGER.log(Level.INFO, "Class loader: {0}", classLoader.getClass().getName());
		Method loadAllMethod = classLoader.getClass().getDeclaredMethod("loadAll", Integer.class);
		loadAllMethod.invoke(classLoader, threads);
		if (mainClass == null) {
			throw new IllegalArgumentException("Argument main-class not set");
		}
		Class<?> mainClassClass = classLoader.loadClass(mainClass);
		Method mainMethod = mainClassClass.getDeclaredMethod("main", String[].class);
		mainMethod.invoke(null, new Object[] { otherArguments.toArray(new String[] {}) });
	}
}
