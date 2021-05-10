package com.infernalbeast.artifactory.classloader;

import java.lang.reflect.Method;

public class ReflectionUtils {
	public static Method getFullyQualifiedMethod(final String method) {
		int indexMethod = method.lastIndexOf(".");
		String className = method.substring(0, indexMethod);
		String methodName = method.substring(indexMethod + 1);
		try {
			Class<?> methodClass = ReflectionUtils.class.getClassLoader().loadClass(className);
			return methodClass.getMethod(methodName);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
}
