package com.linkspark.exception;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException(String alias) {
        super("Link not found for alias: " + alias);
    }
}
