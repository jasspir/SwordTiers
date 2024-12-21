package you.yuli.swordtiers.tiers;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class ValidateHeaders {
    private static final Logger logger = Logger.getLogger(ValidateHeaders.class.getName());

    public static void validateResponseHeaders(HttpResponse<String> response) throws IOException {
        // Get all response headers
        Map<String, List<String>> headers = response.headers().map();

        //mctiers fails the following checks so well ignore them

//        // Check if the `Strict-Transport-Security` header is missing (should be set for security)
//        Optional<String> hstsHeader = response.headers().firstValue("Strict-Transport-Security");
//        if (hstsHeader.isEmpty()) {
//            logger.warning("Missing Strict-Transport-Security (HSTS) header. Server should enforce HTTPS.");
//            throw new IOException("Missing Strict-Transport-Security (HSTS) header.");
//        }

//        // Check for Content-Security-Policy (CSP) header to ensure browser security
//        Optional<String> cspHeader = response.headers().firstValue("Content-Security-Policy");
//        if (cspHeader.isEmpty()) {
//            logger.warning("Missing Content-Security-Policy (CSP) header. This is critical for preventing XSS attacks.");
//            throw new IOException("Missing Content-Security-Policy (CSP) header.");
//        }

//        // Check for Cache-Control header to prevent sensitive data caching
//        Optional<String> cacheControlHeader = response.headers().firstValue("Cache-Control");
//        if (cacheControlHeader.isEmpty() || !cacheControlHeader.get().contains("no-store")) {
//            logger.warning("Cache-Control header should include 'no-store' to prevent caching of sensitive data.");
//            throw new IOException("Cache-Control header should include 'no-store' to prevent caching.");
//        }

        // ValidateJson Content-Type header
        Optional<String> contentType = response.headers().firstValue("Content-Type");
        if (contentType.isEmpty() || !contentType.get().contains("application/json")) {
            throw new IOException("Expected JSON response, but received: " + contentType.orElse("Unknown"));
        }

        // Check for suspicious headers
        Optional<String> xssProtection = response.headers().firstValue("X-XSS-Protection");
        if (xssProtection.isPresent() && !xssProtection.get().equals("1; mode=block")) {
            logger.warning("Unexpected XSS protection header value. Expected '1; mode=block' but found: " + xssProtection.get());
            throw new IOException("Unexpected XSS protection header value.");
        }

        // Log the total size of the headers
        long headerSize = headers.entrySet().stream()
                .mapToLong(entry -> entry.getKey().length() + entry.getValue().stream().mapToLong(String::length).sum())
                .sum();

        // Abort if headers exceed the 1KB limit
        if (headerSize > 1024) {
            throw new IOException("Header size exceeds 1KB. Aborting request. (" + headerSize + ")");
        }

        // ValidateJson Content-Length header (prevent overly large responses, set a reasonable size limit)
        Optional<String> contentLengthHeader = response.headers().firstValue("Content-Length");
        if (contentLengthHeader.isPresent()) {
            try {
                long contentLength = Long.parseLong(contentLengthHeader.get());
                if (contentLength > 2560) {  // 2.5KB max content size
                    logger.warning("Content-Length exceeds allowed limit of 2.5KB. Found: " + contentLength);
                    throw new IOException("Content-Length exceeds allowed limit of 2.5KB.");
                }

                // Read the actual body content (assuming response body is in String form)
                String body = response.body(); // Assuming the response is of type String
                long actualContentLength = body.getBytes(StandardCharsets.UTF_8).length;

                // Compare the content length with the actual body size
                if (contentLength != actualContentLength) {
                    logger.warning("Mismatch between Content-Length header and actual body size. Header: " + contentLength + ", Actual: " + actualContentLength);
                    throw new IOException("Content-Length mismatch: expected " + contentLength + ", but body size is " + actualContentLength);
                }
            } catch (NumberFormatException e) {
                logger.warning("Invalid Content-Length value: " + contentLengthHeader.get());
                throw new IOException("Invalid Content-Length value.");
            }
        }
    }
}
