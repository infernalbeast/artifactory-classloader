package com.infernalbeast.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Arguments {
	private final List<String> otherArguments = new ArrayList<>();
	private String classPath = null;
	private String modulePath = null;
	private final Map<String, Map<String, List<String>>> addExports = new HashMap<>();
	private final Map<String, Map<String, List<String>>> addOpens = new HashMap<>();
	private final Map<String, List<String>> addReads = new HashMap<>();

	public Arguments(final String[] arguments) {
		process(arguments);
	}

	private void process(final String[] arguments) {
		for (int argumentCount = 0; argumentCount < arguments.length; argumentCount++) {
			String argument = arguments[argumentCount];
			if (argument.startsWith("-")) {
				String normalizedArgument;
				if (argument.startsWith("--")) {
					normalizedArgument = argument.substring(2);
				} else {
					normalizedArgument = argument.substring(1);
				}
				Integer skip = process(normalizedArgument, arguments, argumentCount);
				if (skip == null) {
					otherArguments.add(argument);
				} else {
					argumentCount += skip;
				}
			} else {
				otherArguments.add(argument);
			}
		}

	}

	protected Integer process(final String normalized, final String[] arguments, final int position) {
		switch (normalized) {
		case "class-path":
			if (classPath == null) {
				classPath = arguments[position + 1];
				return 1;
			}
			break;
		case "module-path":
			if (modulePath == null) {
				modulePath = arguments[position + 1];
				return 1;
			}
			break;
		case "add-exports": {
			// $module/$package=$readingmodule
			String addExport = arguments[position + 1];
			int equalIndex = addExport.indexOf("=");
			String moduleAndPackage = addExport.substring(0, equalIndex);
			String readingModule = addExport.substring(equalIndex + 1);
			int moduleIndex = moduleAndPackage.indexOf("/");
			String module = moduleAndPackage.substring(0, moduleIndex);
			String modulePackage = moduleAndPackage.substring(moduleIndex + 1);
			Map<String, List<String>> readingModulesByModulePackage = addExports.get(module);
			if (readingModulesByModulePackage == null) {
				readingModulesByModulePackage = new HashMap<>();
				addExports.put(module, readingModulesByModulePackage);
			}
			List<String> readingModules = readingModulesByModulePackage.get(modulePackage);
			if (readingModules == null) {
				readingModules = new ArrayList<>();
				readingModulesByModulePackage.put(modulePackage, readingModules);
			}
			readingModules.add(readingModule);
			return 1;
		}
		case "add-opens": {
			// $module/$package=$reflectingmodule
			String addOpen = arguments[position + 1];
			int equalIndex = addOpen.indexOf("=");
			String moduleAndPackage = addOpen.substring(0, equalIndex);
			String reflectingModule = addOpen.substring(equalIndex + 1);
			int moduleIndex = moduleAndPackage.indexOf("/");
			String module = moduleAndPackage.substring(0, moduleIndex);
			String modulePackage = moduleAndPackage.substring(moduleIndex + 1);
			Map<String, List<String>> readingModulesByModulePackage = addExports.get(module);
			if (readingModulesByModulePackage == null) {
				readingModulesByModulePackage = new HashMap<>();
				addExports.put(module, readingModulesByModulePackage);
			}
			List<String> readingModules = readingModulesByModulePackage.get(modulePackage);
			if (readingModules == null) {
				readingModules = new ArrayList<>();
				readingModulesByModulePackage.put(modulePackage, readingModules);
			}
			readingModules.add(reflectingModule);
			return 1;
		}
		case "add-reads": {
			// $module=$targets
			String addRead = arguments[position + 1];
			int equalIndex = addRead.indexOf("=");
			String module = addRead.substring(0, equalIndex);
			String[] targets = addRead.substring(equalIndex + 1).split(",");
			List<String> targetModules = addReads.get(module);
			if (targetModules == null) {
				targetModules = new ArrayList<>();
				addReads.put(module, targetModules);
			}
			targetModules.addAll(Arrays.asList(targets));
			return 1;
		}
		}
		return null;
	}

	public List<String> getOtherArguments() {
		return otherArguments;
	}

	public String getClassPath() {
		return classPath;
	}

	public String getModulePath() {
		return modulePath;
	}

	public Map<String, Map<String, List<String>>> getAddExports() {
		return addExports;
	}

	public Map<String, Map<String, List<String>>> getAddOpens() {
		return addOpens;
	}

	public Map<String, List<String>> getAddReads() {
		return addReads;
	}
}
