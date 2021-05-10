package com.infernalbeast.util;

import java.util.Enumeration;
import java.util.Iterator;

public class EnumerationIterator<T> implements Iterator<T> {
	private final Enumeration<T> enumeration;

	public EnumerationIterator(final Enumeration<T> enumeration) {
		this.enumeration = enumeration;
	}

	@Override
	public boolean hasNext() {
		return enumeration.hasMoreElements();
	}

	@Override
	public T next() {
		return enumeration.nextElement();
	}
}
