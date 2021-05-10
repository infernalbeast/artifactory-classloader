package com.infernalbeast.artifactory.classloader;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.infernalbeast.lang.Arguments;

public class ArtifactoryArguments extends Arguments {
	private Integer retry = null;
	private Path directory = null;

	public ArtifactoryArguments(final String[] arguments) {
		super(arguments);
	}

	@Override
	protected Integer process(final String normalized, final String[] arguments, final int position) {
		switch (normalized) {
		case "artifact-retry":
			if (retry == null) {
				retry = Integer.valueOf(arguments[position + 1]);
				return 1;
			}
			break;
		case "artifact-directory":
			if (directory == null) {
				directory = Paths.get(arguments[position + 1]);
				return 1;
			}
			break;
		}
		return super.process(normalized, arguments, position);
	}

	public Integer getRetry() {
		return retry;
	}

	public Path getDirectory() {
		return directory;
	}
}
