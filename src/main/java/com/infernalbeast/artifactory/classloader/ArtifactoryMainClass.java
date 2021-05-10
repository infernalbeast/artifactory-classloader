package com.infernalbeast.artifactory.classloader;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class ArtifactoryMainClass {
	private static final boolean DEBUG = System.getProperty("debug") != null
			|| System.getProperty("debug." + ArtifactoryMainClass.class.getName()) != null;

	public static void main(final String[] arguments) throws IOException, ClassNotFoundException, NoSuchMethodException,
			InvocationTargetException, IllegalAccessException, InstantiationException {
		List<String> otherArguments = new ArrayList<>();
		Boolean lazy = null;
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
				case "artifact-lazy":
					if (lazy == null) {
						argumentCount++;
						lazy = Boolean.valueOf(arguments[argumentCount]);
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
		invoke(lazy, otherArguments);
	}

	public static void invoke(final Boolean lazy, final List<String> otherArguments)
			throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
			IllegalAccessException, InstantiationException {
		boolean evaluatedLazy;
		try (ObjectInputStream objectInputStream = new ObjectInputStream(
				ArtifactoryMainClass.class.getClassLoader().getResourceAsStream("META-INF/artifact-configuration"))) {
			Configuration configuration = (Configuration) objectInputStream.readObject();
			if (DEBUG) {
				System.out.println("CONFIGURATION: " + configuration);
			}
			if (lazy == null) {
				evaluatedLazy = configuration.isLazy();
			} else {
				evaluatedLazy = lazy;
			}
		}
		if (evaluatedLazy) {
			if (DEBUG) {
				System.out.println("MAIN CLASS: " + LazyArtifactoryMainClass.class.getSimpleName());
			}
			LazyArtifactoryMainClass.main(otherArguments.toArray(new String[] {}));
		} else {
			if (DEBUG) {
				System.out.println("MAIN CLASS: " + DiligentArtifactoryMainClass.class.getSimpleName());
			}
			DiligentArtifactoryMainClass.main(otherArguments.toArray(new String[] {}));
		}
	}

	public static class Configuration implements Serializable {
		private static final long serialVersionUID = 1L;

		public boolean lazy;

		public Configuration() {
		}

		public Configuration(final boolean lazy) {
			this.lazy = lazy;
		}

		public boolean isLazy() {
			return lazy;
		}

		@Override
		public String toString() {
			return "lazy(" + lazy + ")";
		}
	}
}
