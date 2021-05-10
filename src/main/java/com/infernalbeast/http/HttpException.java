package com.infernalbeast.http;

public class HttpException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final int status;

	public HttpException(final int status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return UrlStatus.getType(status) + ":" + status;
	}
}
