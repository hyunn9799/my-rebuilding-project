package com.aicc.silverlink.global.exception;

public class LuxiaHttpException extends RuntimeException{

    private final String body;
    private final int status;

    public LuxiaHttpException(int status, String body) {
        super("LUXIA HTTP " + status + " body=" + body);
        this.status = status;
        this.body = body;
    }
    public int status() { return status; }
    public String body() { return body; }
}
