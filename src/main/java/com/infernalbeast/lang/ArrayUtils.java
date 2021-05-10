package com.infernalbeast.lang;

import java.lang.reflect.Array;

public class ArrayUtils {
	public static <T> T[] getEmptyArray(final Class<?> arrayClass) {
		@SuppressWarnings("unchecked")
		T[] array = (T[]) Array.newInstance(arrayClass, 0);
		return array;
	}
}
