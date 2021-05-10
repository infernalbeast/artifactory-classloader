package com.infernalbeast.lang;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseClassLoader extends ClassLoader {
	private final Logger logger = Logger.getLogger(getClass().getName());

	public BaseClassLoader() {
		super();
	}

	public BaseClassLoader(final ClassLoader parent) {
		super(parent);
	}

	public BaseClassLoader(final String name, final ClassLoader parent) {
		super(name, parent);
	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	protected URL findResource(final String name) {
		logger.log(Level.INFO, "Find resource {0}", new Object[] { name });
		try {
			Enumeration<URL> urls = findResources(name);
			if (urls.hasMoreElements()) {
				URL resource = urls.nextElement();
				logger.log(Level.INFO, "Found resource {0}: {1}", new Object[] { name, resource });
				return resource;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		logger.log(Level.INFO, "Not found resource {0}", new Object[] { name });
		return null;
	}
}
