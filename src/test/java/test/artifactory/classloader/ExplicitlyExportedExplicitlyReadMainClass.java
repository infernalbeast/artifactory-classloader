package test.artifactory.classloader;

public class ExplicitlyExportedExplicitlyReadMainClass {
	public static void main(final String[] args) throws Exception {
		System.err.println("VALUE: " + com.infernalbeast.testmodule2.Test.getModule1Value());
	}
}
