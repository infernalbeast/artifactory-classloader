package com.infernalbeast.lang;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import jdk.internal.module.Modules;

public class DiligentClassLoader extends PathClassLoader {
	private static final String ALL_UNNAMED = "ALL-UNNAMED";
	private static final int VERSION = Runtime.version().feature();
	private static final String VERSIONS_PREFIX = "META-INF/versions/";
	private static final boolean DEBUG = System.getProperty("debug") != null
			|| System.getProperty("debug." + DiligentClassLoader.class.getName()) != null;

	public abstract class Interface extends ClassLoader {
		public Interface(final DiligentClassLoader staticClassLoader) {
			super(staticClassLoader);
		}

		@Override
		abstract public Enumeration<URL> getResources(final String name) throws IOException;
	}

	private final Map<String, Module> moduleByName = new HashMap<>();
	private final Map<Module, List<String>> exportedToAllPackagesByModuleMap = new HashMap<>();
	private final Map<Module, List<String>> openedToAllPackagesByModuleMap = new HashMap<>();
	private final Map<String, Integer> versionByResource;
	private final Collection<String> relocate;
	private final Map<String, Class<?>> classByName = new HashMap<>();
	private final Map<String, List<Exports>> exportsByName = new HashMap<>();
	private final Map<String, List<Opens>> opensByName = new HashMap<>();
	private final Map<String, List<Reads>> readsByName = new HashMap<>();

	public DiligentClassLoader(final ClassLoader parent, final List<Path> classPath, final List<Path> modulePath,
			final Map<String, Map<String, List<String>>> addExports,
			final Map<String, Map<String, List<String>>> addOpens, final Map<String, List<String>> addReads,
			final Collection<String> resources) {
		super(parent, verifyPaths(merge(classPath, modulePath)));
		addExports.forEach((moduleName, readingModuleNamesByPackage) -> {
			readingModuleNamesByPackage.forEach((packageName, readingModuleNames) -> {
				for (String readingModuleName : readingModuleNames) {
					Exports exports = new Exports(moduleName, packageName, readingModuleName);
					addExports(exports);
				}
			});
		});
		addOpens.forEach((moduleName, reflectingModuleNamesByPackage) -> {
			reflectingModuleNamesByPackage.forEach((packageName, reflectingModuleNames) -> {
				for (String reflectingModuleName : reflectingModuleNames) {
					Opens opens = new Opens(moduleName, packageName, reflectingModuleName);
					addOpens(opens);
				}
			});
		});
		addReads.forEach((moduleName, targetModuleNames) -> {
			for (String targetModuleName : targetModuleNames) {
				Reads reads = new Reads(moduleName, targetModuleName);
				addReads(reads);
			}
		});
		versionByResource = getVersionByResource(resources);
		this.relocate = new HashSet<>(resources);
		if (!modulePath.isEmpty()) {
			ModuleFinder moduleFinder = ModuleFinder.of(modulePath.toArray(new Path[] {}));
			moduleFinder.findAll().forEach(moduleReference -> {
				ModuleDescriptor moduleDescriptor = moduleReference.descriptor();
				Module module = Modules.defineModule(this, moduleDescriptor, null);
				configureModule(module);
			});
		}
	}

	protected static Map<String, Integer> getVersionByResource(final Collection<String> resources) {
		Map<String, Integer> versionByResource = new HashMap<>();
		for (String resource : resources) {
			if (resource.startsWith(VERSIONS_PREFIX)) {
				int index = resource.indexOf("/", VERSIONS_PREFIX.length() + 1);
				int version = Integer.valueOf(resource.substring(VERSIONS_PREFIX.length(), index));
				String normalizedResource = resource.substring(index + 1);
				Integer currentVersion = versionByResource.get(normalizedResource);
				if (currentVersion == null || (currentVersion < version && version <= VERSION)) {
					versionByResource.put(normalizedResource, version);
				}
			}
		}
//		System.out.println("VERSIONS");
//		versionByResource.forEach((resource, version) -> {
//			System.out.println("VERSION: " + resource + " " + version);
//		});
		return versionByResource;
	}

	private void addExports(final Exports exports) {
		addExports(exports.getName(), exports);
		addExports(exports.getTarget(), exports);
	}

	private void addExports(final String name, final Exports exports) {
		List<Exports> exportsList = exportsByName.get(name);
		if (exportsList == null) {
			exportsList = new ArrayList<>();
			exportsByName.put(name, exportsList);
		}
		exportsList.add(exports);
	}

	private void addOpens(final Opens opens) {
		addOpens(opens.getName(), opens);
		addOpens(opens.getTarget(), opens);
	}

	private void addOpens(final String name, final Opens opens) {
		List<Opens> opensList = opensByName.get(name);
		if (opensList == null) {
			opensList = new ArrayList<>();
			opensByName.put(name, opensList);
		}
		opensList.add(opens);
	}

	private void addReads(final Reads reads) {
		addReads(reads.getName(), reads);
		addReads(reads.getTarget(), reads);
	}

	private void addReads(final String name, final Reads reads) {
		List<Reads> readsList = readsByName.get(name);
		if (readsList == null) {
			readsList = new ArrayList<>();
			readsByName.put(name, readsList);
		}
		readsList.add(reads);
	}

	protected void configureModule(final Module module) {
		moduleByName.put(module.getName(), module);
		exportedToAllPackagesByModuleMap.forEach((exportedModule, packages) -> {
			for (String pkg : packages) {
				Modules.addExports(exportedModule, pkg, module);
			}
		});
		openedToAllPackagesByModuleMap.forEach((openedModule, packages) -> {
			for (String pkg : packages) {
				Modules.addOpens(openedModule, pkg, module);
			}
		});
		String moduleName = module.getName();
		ModuleDescriptor moduleDescriptor = module.getDescriptor();
		moduleDescriptor.exports().forEach(exports -> {
			String pkg = exports.source();
			if (exports.isQualified()) {
				exports.targets().forEach(targetName -> {
					Module targetModule = getModule(targetName);
					if (targetModule != null) {
						Modules.addExports(module, pkg, targetModule);
					} else {
						if (DEBUG) {
							System.out.println("FAILED EXPORT: " + targetName);
						}
					}
				});
			} else {
				// Exported to all modules
				List<String> packages = exportedToAllPackagesByModuleMap.get(module);
				if (packages == null) {
					packages = new ArrayList<>();
					exportedToAllPackagesByModuleMap.put(module, packages);
				}
				packages.add(pkg);
				Modules.addExportsToAllUnnamed(module, pkg);
				Iterator<Module> allModules = moduleByName.values().iterator();
				while (allModules.hasNext()) {
					Modules.addExports(module, pkg, allModules.next());
				}
			}
		});
		moduleDescriptor.opens().forEach(opens -> {
			String pkg = opens.source();
			if (opens.isQualified()) {
				opens.targets().forEach(targetName -> {
					Module targetModule = getModule(targetName);
					if (targetModule != null) {
						Modules.addOpens(module, pkg, targetModule);
					} else {
						if (DEBUG) {
							System.out.println("FAILED OPENS: " + targetName);
						}
					}
				});
			} else {
				// Opened to all modules
				List<String> packages = openedToAllPackagesByModuleMap.get(module);
				if (packages == null) {
					packages = new ArrayList<>();
					openedToAllPackagesByModuleMap.put(module, packages);
				}
				packages.add(pkg);
				Modules.addOpensToAllUnnamed(module, pkg);
				Iterator<Module> allModules = moduleByName.values().iterator();
				while (allModules.hasNext()) {
					Modules.addOpens(module, pkg, allModules.next());
				}
			}
		});
		moduleDescriptor.requires().forEach(requires -> {
			if (!requires.modifiers().contains(Requires.Modifier.STATIC)) {
				String requiresName = requires.name();
				addReads(new Reads(moduleName, requiresName));
			}
		});
		moduleDescriptor.provides().forEach(provides -> {
			try {
				Class<?> serviceClass = loadClass(provides.service());
				provides.providers().forEach(provider -> {
					try {
						Class<?> providerClass = loadClass(provider);
						Modules.addProvides(module, serviceClass, providerClass);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				});
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		});
		moduleDescriptor.uses().forEach(uses -> {
			try {
				Class<?> serviceClass = loadClass(uses);
				Modules.addUses(module, serviceClass);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		});

		List<Exports> exportsList = exportsByName.get(moduleName);
		if (exportsList != null) {
			for (Exports exports : exportsList) {
				Module exportsModule = getModule(exports.getName());
				if (exportsModule != null) {
					String readingModuleName = exports.getTarget();
					if (readingModuleName.equals(ALL_UNNAMED)) {
						Modules.addExportsToAllUnnamed(exportsModule, exports.getPkg());
					} else {
						Module readingModule = getModule(readingModuleName);
						if (readingModule != null) {
							Modules.addExports(exportsModule, exports.getPkg(), readingModule);
						}
					}
				}
			}
		}
		List<Opens> opensList = opensByName.get(moduleName);
		if (opensList != null) {
			for (Opens opens : opensList) {
				Module opensModule = getModule(opens.getName());
				if (opensModule != null) {
					String reflectingModuleName = opens.getTarget();
					if (reflectingModuleName.equals(ALL_UNNAMED)) {
						Modules.addOpensToAllUnnamed(opensModule, opens.getPkg());
					} else {
						Module reflectingModule = getModule(reflectingModuleName);
						if (reflectingModule != null) {
							Modules.addOpens(opensModule, opens.getPkg(), reflectingModule);
						}
					}
				}
			}
		}
		List<Reads> readsList = readsByName.get(moduleName);
		if (readsList != null) {
			for (Reads reads : readsList) {
				Module readsModule = getModule(reads.getName());
				if (readsModule != null) {
					String targetModuleName = reads.getTarget();
					if (targetModuleName.equals(ALL_UNNAMED)) {
						Modules.addReadsAllUnnamed(readsModule);
					} else {
						Module targetModule = getModule(targetModuleName);
						if (targetModule != null) {
							Modules.addReads(readsModule, targetModule);
						}
					}
				}
			}
		}
	}

	protected String getClassResource(final String name) {
		String classResource = name.replace('.', '/') + ".class";
		Integer version = versionByResource.get(classResource);
		if (version != null) {
			classResource = VERSIONS_PREFIX + version + "/" + classResource;
		}
		return classResource;
	}

	@Override
	protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
		if (DEBUG) {
			System.out.println("LOAD CLASS: " + name);
		}
		synchronized (getClassLoadingLock(name)) {
			Class<?> redefinedClass = classByName.get(name);
			if (redefinedClass == null) {
				String path = getClassResource(name);
				if (relocate.contains(path)) {
					/*
					 * This class is going to be defined in the parent, if we allow that it will be
					 * cut off from the rest of the class path.
					 */
					Logger logger = Logger.getLogger(getClass().getName());
					logger.log(Level.INFO, "Redefining {0}", name);
					try (InputStream inputStream = getResourceAsStream(path)) {
						ProtectionDomain protectionDomain;
						try {
							logger.log(Level.INFO, "Getting protected domain {0}", name);
							protectionDomain = getParent().loadClass(name).getProtectionDomain();
							logger.log(Level.INFO, "Got protected domain {0}", name);
						} catch (Throwable e) {
							logger.log(Level.INFO, "Thrown {0}", new Object[] { e.getMessage(), e });
							protectionDomain = null;
						}
						redefinedClass = defineClass(name, ByteBuffer.wrap(inputStream.readAllBytes()),
								protectionDomain);
						if (resolve) {
							resolveClass(redefinedClass);
						}
						logger.log(Level.INFO, "Redefined {0}", name);
						classByName.put(name, redefinedClass);
						return redefinedClass;
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			} else {
				return redefinedClass;
			}
		}
		return super.loadClass(name, resolve);
	}

	private Module getModule(final String moduleName) {
		Module module = moduleByName.get(moduleName);
		if (module == null) {
			ModuleLayer layer = ModuleLayer.boot();
			return layer.findModule(moduleName).orElse(null);
		}
		return module;
	}

	private static List<Path> verifyPaths(final List<Path> paths) {
		for (Path path : paths) {
			if (!Files.exists(path)) {
				throw new RuntimeException("Library does not exist: " + path);
			}
		}
		return paths;
	}

	@SafeVarargs
	private static <T> List<T> merge(final List<T>... lists) {
		List<T> newList = new ArrayList<T>();
		for (List<T> list : lists) {
			newList.addAll(list);
		}
		return newList;
	}

	public class Exports {
		private final String name;
		private final String pkg;
		private final String target;

		public Exports(final String name, final String pkg, final String readingTarget) {
			this.name = name;
			this.pkg = pkg;
			this.target = readingTarget;
		}

		public String getName() {
			return name;
		}

		public String getPkg() {
			return pkg;
		}

		public String getTarget() {
			return target;
		}

		@Override
		public String toString() {
			return name + "/" + pkg + "=" + target;
		}
	}

	public class Opens {
		private final String name;
		private final String pkg;
		private final String target;

		public Opens(final String name, final String pkg, final String reflectingTarget) {
			this.name = name;
			this.pkg = pkg;
			this.target = reflectingTarget;
		}

		public String getName() {
			return name;
		}

		public String getPkg() {
			return pkg;
		}

		public String getTarget() {
			return target;
		}

		@Override
		public String toString() {
			return name + "/" + pkg + "=" + target;
		}
	}

	public class Reads {
		private final String name;
		private final String target;

		public Reads(final String name, final String target) {
			this.name = name;
			this.target = target;
		}

		public String getName() {
			return name;
		}

		public String getTarget() {
			return target;
		}

		@Override
		public String toString() {
			return name + "=" + target;
		}
	}
}
