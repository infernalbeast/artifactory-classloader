package com.infernalbeast.util;

import java.util.Iterator;

public abstract class MapIterator<T, S> implements Iterator<S> {
	private final Iterator<T> iterator;

	public MapIterator(final Iterator<T> iterator) {
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public S next() {
		return map(iterator.next());
	}

	abstract public S map(T object);
}
