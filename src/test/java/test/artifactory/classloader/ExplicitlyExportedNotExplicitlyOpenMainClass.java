package test.artifactory.classloader;

public class ExplicitlyExportedNotExplicitlyOpenMainClass {
	public static void main(final String[] args) throws Exception {
		System.err.println("VALUE: " + com.infernalbeast.testmodule3.Test.getModule1ReflectionValue());
	}
}
