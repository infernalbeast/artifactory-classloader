package com.infernalbeast.artifactory.classloader;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.infernalbeast.lang.Diligent;
import com.infernalbeast.lang.GenerateUrl;
import com.infernalbeast.lang.GenerateUtils;
import com.infernalbeast.lang.GenerateUtils.ModuleAndClassPathResources;
import com.infernalbeast.lang.LazyClassLoader;

public class ArtifactoryClassLoader extends LazyClassLoader implements Diligent {
	private final Map<GenerateUrl, Collection<String>> classPathByLazyUrl;
	private final Map<GenerateUrl, Collection<String>> modulePathByLazyUrl;

	public ArtifactoryClassLoader(final ClassLoader parent) {
		this(parent, (Integer) null, null, new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>(),
				new HashMap<>());
	}

	public ArtifactoryClassLoader(final ClassLoader parent, final Integer retry, final Path directory,
			final List<Path> classPath, final List<Path> modulePath,
			final Map<String, Map<String, List<String>>> addExports,
			final Map<String, Map<String, List<String>>> addOpens, final Map<String, List<String>> addReads) {
		this(parent, GenerateUtils.getResourcesByLazyUrl(parent, retry, directory), classPath, modulePath, addOpens,
				addExports, addReads);
	}

	private ArtifactoryClassLoader(final ClassLoader parent,
			final ModuleAndClassPathResources moduleAndClassPathResources, final List<Path> classPath,
			final List<Path> modulePath, final Map<String, Map<String, List<String>>> addExports,
			final Map<String, Map<String, List<String>>> addOpens, final Map<String, List<String>> addReads) {
		this(parent, moduleAndClassPathResources.getClassPath(), moduleAndClassPathResources.getModulePath(),
				moduleAndClassPathResources.getResources(), classPath, modulePath, addExports, addOpens, addReads);
	}

	private ArtifactoryClassLoader(final ClassLoader parent,
			final Map<GenerateUrl, Collection<String>> classPathResourcesByLazyUrl,
			final Map<GenerateUrl, Collection<String>> modulePathResourcesByLazyUrl, final Collection<String> resources,
			final List<Path> classPath, final List<Path> modulePath,
			final Map<String, Map<String, List<String>>> addExports,
			final Map<String, Map<String, List<String>>> addOpens, final Map<String, List<String>> addReads) {
		super(parent, classPathResourcesByLazyUrl, modulePathResourcesByLazyUrl, resources, classPath, modulePath,
				addExports, addOpens, addReads);
		this.classPathByLazyUrl = classPathResourcesByLazyUrl;
		this.modulePathByLazyUrl = modulePathResourcesByLazyUrl;
		init();
	}

	protected void init() {
	}

	@Override
	public void loadAll(final Integer threads) {
		Logger logger = Logger.getLogger(getClass().getName());
		logger.log(Level.INFO, "Retrieving all libraries");
		ExecutorService executorService = Executors.newFixedThreadPool(threads == null ? 5 : threads);
		try {
			List<Future<URL>> urlFutures = new ArrayList<>();
			for (GenerateUrl generateUrl : classPathByLazyUrl.keySet()) {
				if (generateUrl != null) {
					urlFutures.add(executorService.submit(() -> generateUrl.get()));
				}
			}
			for (GenerateUrl generateUrl : modulePathByLazyUrl.keySet()) {
				if (generateUrl != null) {
					urlFutures.add(executorService.submit(() -> generateUrl.get()));
				}
			}
			for (Future<URL> urlFuture : urlFutures) {
				try {
					urlFuture.get();
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
		} finally {
			executorService.shutdown();
		}
		logger.log(Level.INFO, "Libraries retrieved");
	}
}
