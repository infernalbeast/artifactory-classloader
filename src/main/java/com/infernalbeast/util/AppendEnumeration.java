package com.infernalbeast.util;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;

public class AppendEnumeration<T> implements Enumeration<T> {
	private final Iterator<Enumeration<T>> enumerations;
	private Enumeration<T> enumeration;
	private boolean set = false;
	private boolean done = false;
	private T next;

	@SafeVarargs
	public AppendEnumeration(final Enumeration<T>... enumerations) {
		this(Arrays.asList(enumerations).iterator());
	}

	public AppendEnumeration(final Iterator<Enumeration<T>> enumerations) {
		this.enumerations = enumerations;
	}

	private void prime() {
		while (!done && !set) {
			if (enumeration == null || !enumeration.hasMoreElements()) {
				if (enumerations.hasNext()) {
					enumeration = enumerations.next();
				} else {
					done = true;
					break;
				}
			} else {
				set = true;
				next = enumeration.nextElement();
				break;
			}
		}
	}

	@Override
	public boolean hasMoreElements() {
		prime();
		return !done;
	}

	@Override
	public T nextElement() {
		prime();
		try {
			return next;
		} finally {
			next = null;
			set = false;
		}
	}
}
