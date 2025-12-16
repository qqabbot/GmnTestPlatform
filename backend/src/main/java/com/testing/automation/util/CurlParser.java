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
    private static final Pattern URL_PATTERN = Pattern.compile("(?:curl|--location)\\s+['\"](https?://[^'\"\\s]+)['\"]|['\"](https?://[^'\"\\s]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEADER_PATTERN = Pattern.compile("(?:-H|--header)\\s+['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    
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
        
        // Normalize: replace line continuations (backslash + newline) with space, but keep actual newlines for multiline matching
        String normalized = curlCommand.replaceAll("\\\\\\s*\n\\s*", " ").replaceAll("\\\\\\s*\r\\s*\n\\s*", " ");
        
        CurlParseResult result = new CurlParseResult();
        
        // Parse method (default to GET if not specified, but POST if --data is present)
        Matcher methodMatcher = METHOD_PATTERN.matcher(normalized);
        if (methodMatcher.find()) {
            result.setMethod(methodMatcher.group(1).toUpperCase());
        } else if (normalized.contains("--data") || normalized.contains("--data-raw") || normalized.contains("--data-binary")) {
            result.setMethod("POST");
        } else {
            result.setMethod("GET");
        }
        
        // Parse URL - look for URL after curl or --location
        Matcher urlMatcher = URL_PATTERN.matcher(normalized);
        String url = null;
        while (urlMatcher.find()) {
            url = urlMatcher.group(1) != null ? urlMatcher.group(1) : urlMatcher.group(2);
            if (url != null && url.startsWith("http")) {
                break;
            }
        }
        
        if (url == null || url.isEmpty()) {
            // Fallback: try to find any URL pattern
            Pattern fallbackUrlPattern = Pattern.compile("['\"](https?://[^'\"\\s]+)['\"]", Pattern.CASE_INSENSITIVE);
            Matcher fallbackMatcher = fallbackUrlPattern.matcher(normalized);
            if (fallbackMatcher.find()) {
                url = fallbackMatcher.group(1);
            }
        }
        
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("Could not parse URL from cURL command");
        }
        
        result.setUrl(url);
        
        // Parse headers - support both -H and --header, handle multiline
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
        
        // Parse body (check multiple patterns) - handle multiline JSON with escaped quotes
        String body = parseDataBody(normalized);
        result.setBody(body);
        
        return result;
    }
    
    /**
     * Parse data body from cURL command, handling escaped quotes in JSON
     */
    private static String parseDataBody(String normalized) {
        // Try different data patterns
        String[] patterns = {
            "--data-raw\\s+",
            "--data-binary\\s+",
            "--data-ascii\\s+",
            "--data\\s+"
        };
        
        for (String patternPrefix : patterns) {
            Pattern pattern = Pattern.compile(patternPrefix, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(normalized);
            
            if (matcher.find()) {
                int startPos = matcher.end();
                
                // Find the opening quote (single or double)
                if (startPos >= normalized.length()) continue;
                
                char quoteChar = normalized.charAt(startPos);
                if (quoteChar != '\'' && quoteChar != '"') continue;
                
                // Find the matching closing quote, handling escaped quotes
                StringBuilder bodyBuilder = new StringBuilder();
                boolean escaped = false;
                
                for (int i = startPos + 1; i < normalized.length(); i++) {
                    char c = normalized.charAt(i);
                    
                    if (escaped) {
                        bodyBuilder.append(c);
                        escaped = false;
                    } else if (c == '\\') {
                        bodyBuilder.append(c);
                        escaped = true;
                    } else if (c == quoteChar) {
                        // Found matching closing quote
                        break;
                    } else {
                        bodyBuilder.append(c);
                    }
                }
                
                String body = bodyBuilder.toString();
                if (!body.isEmpty()) {
                    return unescapeQuotes(body);
                }
            }
        }
        
        return null;
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

