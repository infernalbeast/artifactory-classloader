package test.artifactory.classloader;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.channels.NetworkChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Stack;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.io.comparator.SizeFileComparator;

import com.infernalbeast.artifactory.classloader.ArtifactoryClassLoader;
import com.infernalbeast.lang.DiligentClassLoader;
import com.infernalbeast.lang.LazyClassLoader;
import com.infernalbeast.testsnapshot.Snapshot;
import com.sun.management.GcInfo;
import com.sun.management.ThreadMXBean;
import com.sun.security.auth.NTSid;
import com.sun.security.auth.UserPrincipal;

import jdk.internal.module.ServicesCatalog;
import jdk.internal.module.ServicesCatalog.ServiceProvider;
import jdk.net.NetworkPermission;

public class MainClass extends Parent {
	public static void main(final String[] args) throws Exception {
		ClassLoader mainClassLoader = MainClass.class.getClassLoader();
		if (!(mainClassLoader instanceof LazyClassLoader || mainClassLoader instanceof DiligentClassLoader
				|| mainClassLoader.getClass().getName().equals(LazyClassLoader.class.getName())
				|| mainClassLoader.getClass().getName().equals(DiligentClassLoader.class.getName())
				|| mainClassLoader.getClass().getName().equals(ArtifactoryClassLoader.class.getName()))) {
			throw new RuntimeException("Wrong class loader: " + mainClassLoader);
		}
		// List all of the libraries on the class path to the output
		if (args.length > 0) {
			try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(args[0])))) {
				String javaClassPath = System.getProperty("java.class.path");
				String[] classPath = javaClassPath.split(File.pathSeparator);
				for (String library : classPath) {
					writer.println(Paths.get(library).toString());
				}
			}
		}
		ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
		System.out.println("SYSTEM CLASS LOADER: " + systemClassLoader.getClass().getName());

		Class<Sibling> sibling = Sibling.class;
		System.out.println("SIBLING: " + sibling);

		for (ClassLoader cl = systemClassLoader; cl != null; cl = cl.getParent()) {
			ServicesCatalog servicesCatalog = ServicesCatalog.getServicesCatalogOrNull(cl);
			ClassLoader serviceClassLoader = cl;
			if (servicesCatalog != null) {
				List<ServiceProvider> serviceProviders = servicesCatalog.findServices("java.security.Provider");
				serviceProviders.forEach(serviceProvider -> System.out.println("SERVICE PROVIDER: "
						+ serviceClassLoader.getClass().getName() + " " + serviceProvider.providerName()));
			}
			Enumeration<URL> urls = cl.getResources("META-INF/services/java.security.Provider");
			while (urls.hasMoreElements()) {
				System.out.println("PROVIDER URL: " + cl.getClass().getName() + " " + urls.nextElement());
			}
		}

		ServiceLoader<java.security.Provider> serviceLoader = ServiceLoader.load(java.security.Provider.class,
				systemClassLoader);
		serviceLoader.forEach(provider -> System.out.println("PROVIDER: " + provider.getName()));
		Class<?> clazz = MainClass.class.getClassLoader().loadClass("sun.security.ssl.CipherSuite");
		Method method = clazz.getDeclaredMethod("defaultCipherSuites", new Class<?>[] {});
		method.setAccessible(true);
		System.out.println("CIPHER SUITES " + method.invoke(null, new Object[] {}));
		System.out.println("TEST CIPHERS");
		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		String[] defaultCiphers = ssf.getDefaultCipherSuites();
		String[] availableCiphers = ssf.getSupportedCipherSuites();
		TreeMap<String, Boolean> ciphers = new TreeMap<>();
		for (int i = 0; i < availableCiphers.length; ++i) {
			ciphers.put(availableCiphers[i], Boolean.FALSE);
		}

		for (int i = 0; i < defaultCiphers.length; ++i) {
			ciphers.put(defaultCiphers[i], Boolean.TRUE);
		}
		System.out.println("Default\tCipher");
		for (Iterator<Map.Entry<String, Boolean>> i = ciphers.entrySet().iterator(); i.hasNext();) {
			Map.Entry<String, Boolean> cipher = i.next();
			if (Boolean.TRUE.equals(cipher.getValue())) {
				System.out.print('*');
			} else {
				System.out.print(' ');
			}

			System.out.print('\t');
			System.out.println(cipher.getKey());
		}
		System.out.println("PROVIDER LIST "
				+ sun.security.jca.Providers.getProviderList()/* .getService("SSLContext", "Default") */);

		// Test the ssl ciphers
		System.out.println("TEST SSL");
		new URL("https://www.google.com").getContent();

		System.out.println("TEST SEPARATE THREADS");
		Callable<Void> callable = () -> {
			List<Class<?>> classList = new ArrayList<>();
			// java classes
			classList.add(StringBuilder.class);
			classList.add(StringBuffer.class);
			classList.add(TreeMap.class);
			classList.add(TimeZone.class);
			classList.add(Stack.class);
			classList.add(ListIterator.class);
			classList.add(Formatter.class);
			classList.add(Currency.class);

			// com.sun classes
			classList.add(ThreadMXBean.class);
			classList.add(GcInfo.class);

			classList.add(NetworkChannel.class);
			classList.add(NetworkPermission.class);

			classList.add(UserPrincipal.class);
			classList.add(NTSid.class);

			System.out.println(classList);
			return null;
		};
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
		System.out.println("STARTING THREADS");
		for (int i = 0; i < 5; i++) {
			executor.submit(callable);
		}
		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.DAYS);

		System.out.println("TEST OPTIONS");
		Options options = new Options();
		System.out.println("OPTIONS: " + options);
		Class<CommandLine> commandLineClass = CommandLine.class;
		System.out.println("COMMAND LINE CLASS: " + commandLineClass);

		System.out.println("TEST CLASS PATH");
		String[] classPath = System.getProperty("java.class.path").split(File.pathSeparator);
		for (String library : classPath) {
			System.out.println("LIBRARY: " + library);
		}
		System.out.println("TEST JSON");
		// Try to access the json library
		Class<Json> jsonClass = Json.class;
		System.out.println("JSON CLASS: " + jsonClass);
		// Try to access the snapshot
		Class<Snapshot> snapshotClass = Snapshot.class;
		System.out.println("SNAPSHOT CLASS: " + snapshotClass);
//		// Try to access the snapshot compress library
//		Class<ZipEncoding> zipEncodingClass = ZipEncoding.class;
//		System.out.println("SNAPSHOT ZIP ENCODING CLASS: " + zipEncodingClass);
		// Try to load a class from the library
		Class<?> sizeFileComparatorClass = SizeFileComparator.class;
		System.out.println("APACHE IO CLASS: " + sizeFileComparatorClass);

		Enumeration<URL> enumeration = systemClassLoader
				.getResources(SizeFileComparator.class.getName().replaceAll("\\.", "/") + ".class");
		while (enumeration.hasMoreElements()) {
			System.err.println("URL: " + enumeration.nextElement());
		}
	}
}
