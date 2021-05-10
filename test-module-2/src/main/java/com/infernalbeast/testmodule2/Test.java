package com.infernalbeast.testmodule2;

public class Test {
	public static String getModule1Value() {
		return com.infernalbeast.testmodule1.Test.getValue();
	}

	public static String getModule1PrivateValue() {
		return com.infernalbeast.testmodule1private.Test.getValue();
	}
}
