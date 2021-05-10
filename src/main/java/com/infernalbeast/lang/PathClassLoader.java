package com.infernalbeast.lang;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class PathClassLoader extends URLClassLoader {
	public PathClassLoader(final ClassLoader parent, final List<Path> classPath) {
		super(getUrls(classPath), parent);
	}

	private static URL[] getUrls(final List<Path> path) {
		URL[] urls = new URL[path.size()];
		Iterator<Path> iterator = path.iterator();
		for (int i = 0; iterator.hasNext(); i++) {
			try {
				urls[i] = iterator.next().toUri().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		return urls;
	}

	@Override
	public URL findResource(final String name) {
		try {
			Enumeration<URL> urls = findResources(name);
			if (urls.hasMoreElements()) {
				URL resource = urls.nextElement();
				return resource;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}
}
