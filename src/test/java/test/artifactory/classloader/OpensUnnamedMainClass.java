package test.artifactory.classloader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.infernalbeast.testmodule3.Test;

public class OpensUnnamedMainClass {
	public static void main(final String[] args) throws Exception {
		try {
			Class<?> testClass = Test.class.getClassLoader().loadClass("com.infernalbeast.testmodule1private.Test");
			Method method = testClass.getDeclaredMethod("getValue");
			System.out.println("VALUE: " + (String) method.invoke(null));
		} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
				| IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
