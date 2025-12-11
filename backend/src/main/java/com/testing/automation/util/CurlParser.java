package com.testing.automation.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for cURL commands
 * Supports parsing HTTP method, URL, headers, and body from cURL command strings
 */
public class CurlParser {
    
    private static final Pattern METHOD_PATTERN = Pattern.compile("-X\\s+(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN = Pattern.compile("curl\\s+['\"]?([^'\"\\s]+)['\"]?|['\"](https?://[^'\"\\s]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEADER_PATTERN = Pattern.compile("-H\\s+['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_PATTERN = Pattern.compile("--data(?:-raw|-binary|-ascii)?\\s+['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_AT_PATTERN = Pattern.compile("--data-raw\\s+['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_BINARY_PATTERN = Pattern.compile("--data-binary\\s+['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    
    /**
     * Parse cURL command and extract HTTP request details
     * 
     * @param curlCommand The cURL command string
     * @return Parsed result containing method, url, headers, and body
     * @throws IllegalArgumentException if cURL command is invalid
     */
    public static CurlParseResult parse(String curlCommand) {
        if (curlCommand == null || curlCommand.trim().isEmpty()) {
            throw new IllegalArgumentException("cURL command cannot be empty");
        }
        
        // Normalize: remove line breaks and extra spaces
        String normalized = curlCommand.replaceAll("\\s+", " ").trim();
        
        CurlParseResult result = new CurlParseResult();
        
        // Parse method (default to GET if not specified)
        Matcher methodMatcher = METHOD_PATTERN.matcher(normalized);
        if (methodMatcher.find()) {
            result.setMethod(methodMatcher.group(1).toUpperCase());
        } else {
            result.setMethod("GET");
        }
        
        // Parse URL
        Matcher urlMatcher = URL_PATTERN.matcher(normalized);
        if (urlMatcher.find()) {
            String url = urlMatcher.group(1) != null ? urlMatcher.group(1) : urlMatcher.group(2);
            if (url != null) {
                result.setUrl(url);
            }
        }
        
        if (result.getUrl() == null || result.getUrl().isEmpty()) {
            throw new IllegalArgumentException("Could not parse URL from cURL command");
        }
        
        // Parse headers
        Map<String, String> headers = new HashMap<>();
        Matcher headerMatcher = HEADER_PATTERN.matcher(normalized);
        while (headerMatcher.find()) {
            String headerLine = headerMatcher.group(1);
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex > 0) {
                String key = headerLine.substring(0, colonIndex).trim();
                String value = headerLine.substring(colonIndex + 1).trim();
                headers.put(key, value);
            }
        }
        result.setHeaders(headers);
        
        // Parse body (check multiple patterns)
        String body = null;
        
        // Try --data-raw first (most common in modern cURL)
        Matcher dataRawMatcher = DATA_AT_PATTERN.matcher(normalized);
        if (dataRawMatcher.find()) {
            body = unescapeQuotes(dataRawMatcher.group(1));
        } else {
            // Try --data-binary
            Matcher dataBinaryMatcher = DATA_BINARY_PATTERN.matcher(normalized);
            if (dataBinaryMatcher.find()) {
                body = unescapeQuotes(dataBinaryMatcher.group(1));
            } else {
                // Try generic --data
                Matcher dataMatcher = DATA_PATTERN.matcher(normalized);
                if (dataMatcher.find()) {
                    body = unescapeQuotes(dataMatcher.group(1));
                }
            }
        }
        
        result.setBody(body);
        
        return result;
    }
    
    /**
     * Unescape quotes and special characters in cURL data
     */
    private static String unescapeQuotes(String str) {
        if (str == null) return null;
        return str.replace("\\\"", "\"")
                  .replace("\\'", "'")
                  .replace("\\n", "\n")
                  .replace("\\t", "\t")
                  .replace("\\\\", "\\");
    }
    
    /**
     * Check if a string looks like a cURL command
     */
    public static boolean isCurlCommand(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.toLowerCase().startsWith("curl") || 
               (trimmed.contains("-H") && trimmed.contains("http"));
    }
    
    /**
     * Result class for parsed cURL command
     */
    public static class CurlParseResult {
        private String method = "GET";
        private String url;
        private Map<String, String> headers = new HashMap<>();
        private String body;
        
        public String getMethod() {
            return method;
        }
        
        public void setMethod(String method) {
            this.method = method;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public Map<String, String> getHeaders() {
            return headers;
        }
        
        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
        
        public String getBody() {
            return body;
        }
        
        public void setBody(String body) {
            this.body = body;
        }
        
        /**
         * Convert headers map to JSON string
         */
        public String getHeadersAsJson() {
            if (headers == null || headers.isEmpty()) {
                return "{}";
            }
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":\"")
                  .append(entry.getValue().replace("\"", "\\\"")).append("\"");
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
    }
}

