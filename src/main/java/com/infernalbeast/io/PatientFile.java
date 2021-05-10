package com.infernalbeast.io;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is used to avoid the same file being downloaded multiple times when
 * there is more than one client.
 **/
public abstract class PatientFile {
	// Time to wait for an update if file is already there an has not updated
	private final Duration initialMax;
	// Time to wait for another update if file is there and has been updated
	private final Duration continualMax;
	private final Logger logger = Logger.getLogger(getClass().getName());

	private final Path file;

	public PatientFile(final Path file, final Duration initialMax, final Duration continualMax) {
		this.file = file;
		this.initialMax = initialMax;
		this.continualMax = continualMax;
	}

	public void populate() {
		if (!Files.exists(file)) {
			logger.log(Level.INFO, "File does not exist {0}", new Object[] { file });
			try {
				Files.createDirectories(file.getParent());
				String name = file.getFileName().toString();
				// In case we exit prematurely use an intermediary file
				Path tmpFile = file.getParent().resolve(name + ".tmp");
				tmpFile.toFile().deleteOnExit();
				// If this file already exists it may mean another process is downloading it
				try {
					Files.createFile(tmpFile);
				} catch (FileAlreadyExistsException e) {
					logger.log(Level.INFO, "Intermediate file exist {0}", new Object[] { tmpFile });
					Instant last = Instant.now();
					long size = Files.size(tmpFile);
					int updates = 0;
					do {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException ex) {
							throw new RuntimeException(ex);
						}
						try {
							// Will throw a NoSuchFileException if it has been moved
							long currentSize = Files.size(tmpFile);
							Instant currentLast = Instant.now();
							if (currentSize == size) {
								// Has it been too long?
								Duration duration = Duration.between(last, currentLast);
								if (updates == 0) {
									if (duration.compareTo(initialMax) > 0) {
										logger.log(Level.INFO, "Timeout {0}", new Object[] { tmpFile });
										break;
									}
								} else {
									if (duration.compareTo(continualMax) > 0) {
										logger.log(Level.INFO, "Timeout {0}", new Object[] { tmpFile });
										break;
									}
								}
							} else {
								last = currentLast;
								size = currentSize;
								updates++;
							}
						} catch (NoSuchFileException ex) {
							if (Files.exists(file)) {
								logger.log(Level.INFO, "Populated by other {0}", new Object[] { file });
								return;
							} else {
								logger.log(Level.INFO, "Continuing {0}", new Object[] { file });
								// Other aborted, so try to download
								break;
							}
						}
					} while (true);
				}

				try {
					logger.log(Level.INFO, "Populating by self {0}", new Object[] { file });
					doPopulate(tmpFile);
					try {
						Files.move(tmpFile, file);
					} catch (FileAlreadyExistsException e) {
						Files.delete(tmpFile);
					}
				} catch (Exception e) {
					try {
						Files.delete(tmpFile);
					} catch (Throwable t) {
					}
					throw new RuntimeException(e);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	abstract protected void doPopulate(Path file);
}
