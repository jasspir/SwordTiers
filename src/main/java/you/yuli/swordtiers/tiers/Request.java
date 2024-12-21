package you.yuli.swordtiers.tiers;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.HTTP_OK;

public class Request {
    private static final String BASE_API_URL = "https://mctiers.com/api/profile/";  // Base URL
    private static final String USER_AGENT = "Mozilla/5.0"; // Mimic browser request
    private static final String EXPECTED_HOSTNAME = "mctiers.com"; // Hostname to validate against
    private static final HttpClient httpClient;

    static {
        // Initialize HTTP client with a custom SSLContext
        try {
            httpClient = HttpClient.newBuilder()
                    .sslContext(createSSLContext())
                    .version(HttpClient.Version.HTTP_2) // HTTP/2 preferred
                    .followRedirects(HttpClient.Redirect.NEVER) // Prevent automatic redirects
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HttpClient: " + e.getMessage(), e);
        }
    }

    private static SSLContext createSSLContext() throws Exception {
        // Create SSLContext for TLS 1.2 or TLS 1.3
        SSLContext sslContext = SSLContext.getInstance("TLS");

        // Initialize with a custom TrustManager that handles certificate chain validation and expiration check
        sslContext.init(null, new javax.net.ssl.TrustManager[]{new TrustManager()}, new SecureRandom());

        // Ensure that only TLS 1.2 or 1.3 is used by specifying the enabled protocols
        sslContext.getSocketFactory().getDefaultCipherSuites();  // Force initialization of cipher suites
        sslContext.getSupportedSSLParameters().setProtocols(new String[]{"TLSv1.3"});

        return sslContext;
    }

    public static CompletableFuture<JsonObject> safeGetRequest(String uuidWithDashes) {
        String cleanedUuid = uuidWithDashes.replace("-", "");
        String apiUrl = BASE_API_URL + cleanedUuid;

        final int maxRetries = 3; // Maximum number of retries

        // Return a CompletableFuture for async handling
        return CompletableFuture.supplyAsync(() -> {
            int attempts = 0;

            while (attempts < maxRetries) {
                try {
                    attempts++;

                    // Check if the URL's hostname matches the expected hostname before proceeding
                    URI apiUri = URI.create(apiUrl);
                    if (!apiUri.getScheme().equalsIgnoreCase("https") || !apiUri.getHost().equals(EXPECTED_HOSTNAME)) {
                        throw new IOException("Unexpected hostname in URL: " + apiUrl);
                    }

                    // Build the GET request
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(apiUri)
                            .timeout(Duration.ofSeconds(5)) // Set read timeout
                            .header("Accept", "application/json") // Expect JSON
                            .header("User-Agent", USER_AGENT) // Custom user agent
                            .header("Accept-Encoding", "identity") // Avoid automatic decompression
                            .header("Cache-Control", "no-cache") // Prevent caching
                            .GET()
                            .build();

                    // Send the request and get the response
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    // Validate response code
                    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                        System.out.println("§eCould not fetch tier for " + uuidWithDashes + " (" + response.statusCode() + ")");
                        return null;
                    }

                    // Validate & Sanitize headers and JSON
                    ValidateHeaders.validateResponseHeaders(response);
                    return ValidateJson.validateJson(response.body(), cleanedUuid);
                } catch (HttpTimeoutException e) {
                    // Timeout occurred, retry after delay
                    logError("§eAPI Request timed out, retrying... (" + (maxRetries - attempts) + " retries left)");
                } catch (IOException | InterruptedException e) {
                    // Log the exception with detailed information
                    logError("§cAPI Request failed security check, aborting! " + e.getMessage());
                    attempts = maxRetries;
                    e.printStackTrace();
                }
            }

            // Log if failed after all retries
            logError("Request failed after " + maxRetries + " attempts.");
            return null;
        });
    }

    public static void logError(String message) {
        // Log to both system console and Minecraft chat
        System.err.println(message);
        if (MinecraftClient.getInstance().player != null) MinecraftClient.getInstance().player.sendMessage(Text.of(message));
    }
}