// File: src/main/java/com/mining/api/exception/MiningException.java
package com.mining.api.exception;

public class MiningException extends RuntimeException {
    
    public MiningException(String message) {
        super(message);
    }
    
    public MiningException(String message, Throwable cause) {
        super(message, cause);
    }
}