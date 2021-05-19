package com.infernalbeast.util.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

public class SimpleFormatterTests {
	@Test
	public void test() {
		Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Message {0}", new Object[] { "parameter" });
		Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception", new RuntimeException("Test"));
	}
}
