package com.infernalbeast.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.infernalbeast.util.AppendEnumeration;

import jdk.internal.module.Modules;

public class LazyClassLoader extends DiligentClassLoader {
	private static final boolean DEBUG = System.getProperty("debug") != null
			|| System.getProperty("debug." + LazyClassLoader.class.getName()) != null;

	private enum Type {
		CLASS_PATH, MODULE_PATH
	}

	private class GenerateClassLoader {
		private final GenerateUrl generateUrl;
		private final Type type;
		private ClassLoader classLoader = null;

		public GenerateClassLoader(final GenerateUrl generateUrl, final Type type) {
			if (generateUrl == null) {
				throw new IllegalArgumentException("generateUrl can not be null");
			}
			this.generateUrl = generateUrl;
			this.type = type;
		}

		public ClassLoader get(final Logger logger) {
			if (classLoader == null) {
				logger.log(Level.INFO, "Loading {0}", generateUrl);
				URL url = generateUrl.get();
				logger.log(Level.INFO, "Loaded {0} with {1}", new Object[] { generateUrl, url });
				classLoader = new URLClassLoader(new URL[] { url }, null);
				if (type == Type.MODULE_PATH) {
					try {
						URI uri = url.toURI();
						ModuleFinder moduleFinder = ModuleFinder.of(new Path[] { new File(uri).toPath() });
						moduleFinder.findAll().forEach(moduleReference -> {
							ModuleDescriptor moduleDescriptor = moduleReference.descriptor();
							logger.log(Level.INFO, "Loading module {0}", moduleDescriptor.name());
							Module module = Modules.defineModule(LazyClassLoader.this, moduleDescriptor, uri);
							logger.log(Level.INFO, "Configuring module {0}", module.getName());
							configureModule(module);
						});
					} catch (URISyntaxException e) {
						throw new RuntimeException(e);
					}
				}
			}
			return classLoader;
		}
	}

	private final Map<String, Class<?>> classByName = new HashMap<>();
	private final Map<String, Collection<GenerateClassLoader>> generateClassLoadersByResource = new HashMap<>();

	public LazyClassLoader(final ClassLoader parent, final Map<GenerateUrl, Collection<String>> classPathNameByUrl,
			final Map<GenerateUrl, Collection<String>> modulePathNameByUrl, final Collection<String> resources,
			final List<Path> classPath, final List<Path> modulePath,
			final Map<String, Map<String, List<String>>> addExports,
			final Map<String, Map<String, List<String>>> addOpens, final Map<String, List<String>> addReads) {
		super(parent, classPath, modulePath, addExports, addOpens, addReads,
				joinResources(parent, resources, classPathNameByUrl, modulePathNameByUrl));
		add(classPathNameByUrl, Type.CLASS_PATH);
		add(modulePathNameByUrl, Type.MODULE_PATH);
	}

	private static Set<String> joinResources(final ClassLoader classLoader, final Collection<String> resources,
			final Map<GenerateUrl, Collection<String>> classPathNameByUrl,
			final Map<GenerateUrl, Collection<String>> modulePathNameByUrl) {
		Set<String> allResources = new HashSet<>(resources);
		classPathNameByUrl.values().forEach(classResources -> {
			allResources.addAll(classResources);
		});
		modulePathNameByUrl.values().forEach(moduleResources -> {
			allResources.addAll(moduleResources);
		});
		return allResources;
	}

	private void add(final Map<GenerateUrl, Collection<String>> pathNameByUrl, final Type type) {
		if (DEBUG) {
			System.out.println("ADDING TYPE: " + type);
		}
		pathNameByUrl.forEach((generateUrl, resources) -> {
			if (DEBUG) {
				System.out.println("GENERATE URL: " + generateUrl + " " + resources.size());
			}
			if (generateUrl != null) {
				GenerateClassLoader generateClassLoader = new GenerateClassLoader(generateUrl, type);
				for (String resource : resources) {
					Collection<GenerateClassLoader> generateClassLoaders = generateClassLoadersByResource.get(resource);
					if (generateClassLoaders == null) {
						generateClassLoaders = new ArrayList<>();
						generateClassLoadersByResource.put(resource, generateClassLoaders);
					}
					generateClassLoaders.add(generateClassLoader);
				}
			}
		});
	}

	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException {
		Logger logger = Logger.getLogger(getClass().getName());
		logger.log(Level.INFO, "Finding class {0}", name);
		Class<?> foundClass = classByName == null ? null : classByName.get(name);
		if (foundClass == null) {
			String path = getClassResource(name);
			Collection<GenerateClassLoader> generateClassLoaders = generateClassLoadersByResource.get(path);
			if (generateClassLoaders != null) {
				Iterator<GenerateClassLoader> iterator = generateClassLoaders.iterator();
				GenerateClassLoader generateClassLoader = iterator.next();
				ClassLoader classLoader = generateClassLoader.get(logger);
				try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
					logger.log(Level.INFO, "Defining class {0}", name);
					foundClass = defineClass(name, ByteBuffer.wrap(inputStream.readAllBytes()),
							(ProtectionDomain) null);
					classByName.put(name, foundClass);
					logger.log(Level.INFO, "Defined class {0}", foundClass.getName());
					return foundClass;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			return super.findClass(name);
		} else {
			return foundClass;
		}
	}

	@Override
	public URL findResource(final String name) {
		Logger logger = Logger.getLogger(getClass().getName());
		Collection<GenerateClassLoader> generateClassLoaders = generateClassLoadersByResource == null ? null
				: generateClassLoadersByResource.get(name);
		if (generateClassLoaders != null) {
			Iterator<GenerateClassLoader> iterator = generateClassLoaders.iterator();
			GenerateClassLoader generateClassLoader = iterator.next();
			ClassLoader classLoader = generateClassLoader.get(logger);
			return classLoader.getResource(name);
		} else {
			return super.findResource(name);
		}
	}

	@Override
	public Enumeration<URL> findResources(final String name) throws IOException {
		Logger logger = Logger.getLogger(getClass().getName());
		Collection<GenerateClassLoader> generateClassLoaders = generateClassLoadersByResource == null ? null
				: generateClassLoadersByResource.get(name);
		List<Enumeration<URL>> urlEnumerationList = new ArrayList<>();
		if (generateClassLoaders != null) {
			for (GenerateClassLoader generateClassLoader : generateClassLoaders) {
				urlEnumerationList.add(generateClassLoader.get(logger).getResources(name));
			}
		}
		urlEnumerationList.add(super.findResources(name));
		return new AppendEnumeration<>(urlEnumerationList.toArray(ArrayUtils.getEmptyArray(Enumeration.class)));
	}
}
