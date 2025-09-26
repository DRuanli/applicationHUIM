// File: src/main/java/com/mining/config/FileFormat.java
package com.mining.config;

/**
 * Enumeration of supported file formats for input/output operations.
 */
public enum FileFormat {
    
    /**
     * Text format with space-separated values.
     */
    TEXT(".txt", "text/plain"),
    
    /**
     * Comma-separated values format.
     */
    CSV(".csv", "text/csv"),
    
    /**
     * JSON format for structured data.
     */
    JSON(".json", "application/json"),
    
    /**
     * Binary format for optimized storage.
     */
    BINARY(".bin", "application/octet-stream");
    
    private final String extension;
    private final String mimeType;
    
    FileFormat(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }
    
    public String getExtension() {
        return extension;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    /**
     * Determines file format from filename.
     * 
     * @param filename The filename to check
     * @return The detected file format
     */
    public static FileFormat fromFilename(String filename) {
        String lowerName = filename.toLowerCase();
        for (FileFormat format : values()) {
            if (lowerName.endsWith(format.extension)) {
                return format;
            }
        }
        return TEXT; // Default format
    }
}