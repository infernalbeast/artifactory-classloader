package com.infernalbeast.http;

public enum UrlStatus {
	INFORMATIONAL, SUCCESSFUL, REDIRECTS, CLIENT, SERVER;

	public static UrlStatus getType(final int status) {
		switch (status / 100) {
		case 1:
			return UrlStatus.INFORMATIONAL;
		case 2:
			return UrlStatus.SUCCESSFUL;
		case 3:
			return UrlStatus.REDIRECTS;
		case 4:
			return UrlStatus.CLIENT;
		case 5:
			return UrlStatus.SERVER;
		}
		throw new RuntimeException("Out of bounds");
	}
}
