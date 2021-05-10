package test.artifactory.classloader;

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import jdk.internal.module.Modules;

public class Test {
	public static void main(final String[] args) throws Exception {
		Path library1 = new File(
				"/home/bakslashr/repositories/git/clone/sources/artifactory-classloader/test-module-1/build/libs/test-module-1.jar")
						.toPath();
		Path library2 = new File(
				"/home/bakslashr/repositories/git/clone/sources/artifactory-classloader/test-module-2/build/libs/test-module-2.jar")
						.toPath();
//		ClassLoader classLoader = Test.class.getClassLoader();
		URLClassLoader classLoader = new URLClassLoader(
				new URL[] { library1.toUri().toURL(), library2.toUri().toURL() });
		System.err.println("CLASS LOADER: " + classLoader);
		ModuleFinder moduleFinder = ModuleFinder.of(library1, library2);
		moduleFinder.findAll().forEach(moduleReference -> {
			ModuleDescriptor moduleDescriptor = moduleReference.descriptor();
			System.err.println("REQUIRES: " + moduleDescriptor.requires());
			System.err.println("MD: " + moduleDescriptor);
			Module module = Modules.defineModule(classLoader, moduleDescriptor, null);
			module.getPackages().forEach(pkg -> Modules.addExportsToAllUnnamed(module, pkg));
		});
		Class<?> clazz = classLoader.loadClass("com.infernalbeast.testmodule2.Test");
		System.err.println("????? " + clazz.getModule());
		System.err.println("?????2 " + clazz.getModule().getLayer());
		System.err
				.println("?????3 " + clazz.getModule().getResourceAsStream("com/infernalbeast/testmodule2/Test.class"));

		ModuleLayer layer = ModuleLayer.boot();
		layer.modules().forEach(module -> {
			ClassLoader moduleClassLoader = module.getClassLoader();
			String classLoaderName = moduleClassLoader == null ? "bootstrap" : moduleClassLoader.getName();
			System.out.println(classLoaderName + ": " + module.getName());
		});
		Method method = clazz.getDeclaredMethod("getModule1Value");
		method.invoke(null);
		System.err.println("DONE");
	}
}
