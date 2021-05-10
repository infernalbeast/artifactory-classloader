package com.infernalbeast.io;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import org.junit.After;
import org.junit.Test;

public class PatientFileTest {
	Path testFile;
	{
		try {
			testFile = Files.createTempFile("test", ".txt");
			Files.delete(testFile);
			testFile.toFile().deleteOnExit();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@After
	public void cleanup() {
		try {
			Files.delete(testFile);
		} catch (IOException e) {
		}
	}

	@Test
	public void populate_multiple() throws Exception {
		Duration initial = Duration.ofSeconds(2);
		Duration continuous = Duration.ofSeconds(4);
		Thread thread = new Thread() {
			@Override
			public void run() {
				new PatientFile(testFile, initial, continuous) {
					@Override
					protected void doPopulate(final Path file) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						try (Writer writer = Files.newBufferedWriter(file)) {
							writer.write("This is a test");
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}.populate();
			}
		};
		thread.start();
		Thread.sleep(1000);
		new PatientFile(testFile, initial, continuous) {
			@Override
			protected void doPopulate(final Path file) {
				throw new RuntimeException("Should not get here");
			}
		}.populate();
		String actual = Files.readString(testFile);
		assertThat(actual, equalTo("This is a test"));
	}

	/**
	 * Surpass the initial timeout but not the continuous one.
	 **/
	@Test
	public void populate_multiple_initialTimeout() throws Exception {
		Duration initial = Duration.ofSeconds(2);
		Duration continuous = Duration.ofSeconds(4);
		Thread thread = new Thread() {
			@Override
			public void run() {
				new PatientFile(testFile, initial, continuous) {
					@Override
					protected void doPopulate(final Path file) {
						try {
							Thread.sleep(60000);
						} catch (InterruptedException e) {
							return;
						}
					}
				}.populate();
			}
		};
		thread.start();
		Thread.sleep(1000);
		Instant start = Instant.now();
		new PatientFile(testFile, initial, continuous) {
			@Override
			protected void doPopulate(final Path file) {
				try (Writer writer = Files.newBufferedWriter(file)) {
					writer.write("This is a test");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}.populate();
		Duration length = Duration.between(start, Instant.now());
		assertTrue(initial.compareTo(length) < 0);
		assertTrue(length.compareTo(continuous) < 0);
		String actual = Files.readString(testFile);
		assertThat(actual, equalTo("This is a test"));
		thread.interrupt();
	}

	/**
	 * Write every second for 10 seconds as to prevent a timeout
	 **/
	@Test
	public void populate_multiple_10seconds() throws Exception {
		Duration initial = Duration.ofSeconds(2);
		Duration continuous = Duration.ofSeconds(4);
		Thread thread = new Thread() {
			@Override
			public void run() {
				new PatientFile(testFile, initial, continuous) {
					@Override
					protected void doPopulate(final Path file) {
						try (Writer writer = Files.newBufferedWriter(file)) {
							for (int i = 0; i < 10; i++) {
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									throw new RuntimeException(e);
								}
								writer.write(String.valueOf(i));
								writer.flush();
							}
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}.populate();
			}
		};
		thread.start();
		Thread.sleep(1000);
		new PatientFile(testFile, initial, continuous) {
			@Override
			protected void doPopulate(final Path file) {
				throw new RuntimeException("Should not get here");
			}
		}.populate();
		String actual = Files.readString(testFile);
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			stringBuilder.append(String.valueOf(i));
		}
		assertThat(actual, equalTo(stringBuilder.toString()));
	}

	/**
	 * Surpass the continuous timeout.
	 **/
	@Test
	public void populate_multiple_continuousTimeout() throws Exception {
		Duration initial = Duration.ofSeconds(2);
		Duration continuous = Duration.ofSeconds(4);
		Thread thread = new Thread() {
			@Override
			public void run() {
				new PatientFile(testFile, initial, continuous) {
					@Override
					protected void doPopulate(final Path file) {
						try (Writer writer = Files.newBufferedWriter(file)) {
							for (int i = 0; i < 2; i++) {
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									throw new RuntimeException(e);
								}
								writer.write(String.valueOf(i));
								writer.flush();
							}
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						try {
							Thread.sleep(60000);
						} catch (InterruptedException e) {
							return;
						}
					}
				}.populate();
			}
		};
		thread.start();
		Thread.sleep(1000);
		Instant start = Instant.now();
		new PatientFile(testFile, initial, continuous) {
			@Override
			protected void doPopulate(final Path file) {
				try (Writer writer = Files.newBufferedWriter(file)) {
					writer.write("This is a test");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}.populate();
		Duration length = Duration.between(start, Instant.now());
		assertTrue(continuous.compareTo(length) < 0);
		String actual = Files.readString(testFile);
		assertThat(actual, equalTo("This is a test"));
		thread.interrupt();
	}
}
