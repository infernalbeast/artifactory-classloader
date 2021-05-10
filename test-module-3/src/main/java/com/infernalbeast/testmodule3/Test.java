package com.infernalbeast.testmodule3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Test {
	public static String getModule1Value() {
		return com.infernalbeast.testmodule1.Test.getValue();
	}

	public static String getModule1PrivateValue() {
		return com.infernalbeast.testmodule1private.Test.getValue();
	}

	public static String getModule1ReflectionValue() {
		try {
			Class<?> testClass = Test.class.getClassLoader().loadClass("com.infernalbeast.testmodule1private.Test");
			Method method = testClass.getDeclaredMethod("getValue");
			return (String) method.invoke(null);
		} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
				| IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
