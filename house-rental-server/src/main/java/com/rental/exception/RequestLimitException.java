package com.rental.exception;

public class RequestLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public RequestLimitException(long retryAfterSeconds) {
        super("请求过于频繁，请稍后重试");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
