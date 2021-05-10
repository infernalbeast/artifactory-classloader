package test.artifactory.classloader;

public class ExportsUnnamedMainClass {
	public static void main(final String[] args) throws Exception {
		System.err.println("VALUE: " + com.infernalbeast.testmodule1private.Test.getValue());
	}
}
