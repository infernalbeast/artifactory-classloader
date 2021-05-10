package com.infernalbeast.artifactory.classloader;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BackOff {
	private final Duration maxWait;

	private final Duration initialWait;

	private final float ratio;

	private final Integer retries;

	private final Logger logger = Logger.getLogger(getClass().getName());

	public BackOff(final int retries) {
		this(retries, Duration.ofSeconds(1));
	}

	public BackOff(final int retries, final Duration initialWait) {
		this(null, retries, initialWait, 3f / 2f);
	}

	public BackOff(final Duration maxWait) {
		this(maxWait, Duration.ofSeconds(1));
	}

	public BackOff(final Duration maxWait, final Duration initialWait) {
		this(maxWait, null, initialWait, 3f / 2f);
	}

	public BackOff(final Duration maxWait, final Integer retries, final Duration initialWait, final float ratio) {
		this.maxWait = maxWait;
		if (ratio <= 1) {
			throw new IllegalArgumentException("Must be greater than 1");
		}
		this.initialWait = initialWait;
		this.ratio = ratio;
		this.retries = retries;
	}

	public <T> T execute(final Execute<T> execute) {
		Instant start = Instant.now();
		int tries;
		Throwable last = null;
		for (tries = 0; retries == null | tries < retries; tries++) {
			try {
				return execute.execute();
			} catch (Throwable t) {
				logger.log(Level.INFO, "Failed {0}", new Object[] { tries });
				logger.log(Level.INFO, t.getMessage(), t);
				Duration wait = Duration.ofMillis((long) (initialWait.toMillis() * Math.pow(ratio, (tries + 1))));
				if (maxWait != null) {
					Instant now = Instant.now();
					Duration time = Duration.between(start, now);
					if (maxWait.compareTo(time) < 0) {
						logger.log(Level.INFO, "Time exceeded {0} < {1}", new Object[] { maxWait, time });
						break;
					}
					Duration total = wait.plus(time);
					if (maxWait.compareTo(total) < 0) {
						wait = total.minus(time);
					}
				}
				if (retries != null) {
					if (retries <= (tries + 1)) {
						logger.log(Level.INFO, "Retries exceeded {0} <= {1}", new Object[] { retries, tries + 1 });
						break;
					}
				}
				logger.log(Level.INFO, "Sleeping for {0}", new Object[] { wait });
				try {
					Thread.sleep(wait.toMillis());
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				last = t;
			}
		}
		throw new RuntimeException("Timeout", last);
	}

	public interface Execute<T> {
		T execute();
	}
}
