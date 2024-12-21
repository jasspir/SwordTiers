package you.yuli.swordtiers.tiers;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonObject;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonObject;

public class TierFetcher {
    // Concurrent map to store TierInfo objects, and to track ongoing async requests
    private static final ConcurrentHashMap<UUID, TierInfo> tierInfoMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, CompletableFuture<TierInfo>> ongoingRequests = new ConcurrentHashMap<>();

    // Get TierInfo asynchronously, from map or by triggering a new fetch
    public static TierInfo get(UUID uuid, boolean manual) {
        if (uuid == null) return null;

        // Check if the TierInfo is already in the map (cached result)
        if (tierInfoMap.containsKey(uuid)) {
            // If the result exists in the map, return it (even if it's null)
            return tierInfoMap.get(uuid);
        }

        // Check if an asynchronous request is already in progress
        CompletableFuture<TierInfo> ongoingRequest = ongoingRequests.get(uuid);
        if (ongoingRequest != null) {
            // If the request is in progress, return null immediately and don't trigger again
            return null;  // Return null if the async request is in progress
        }

        System.out.println("fetching tier for " + uuid);

        // If it's the first time the request is being made, start the async process
        // Store the CompletableFuture in ongoingRequests to track the request
        ongoingRequests.put(uuid, getFuture(uuid).thenApply(tierInfo -> {
            // Once the async operation completes, store the result in tierInfoMap
            tierInfoMap.put(uuid, tierInfo); // Store null if the result is null
            // Remove from ongoingRequests after completion
            ongoingRequests.remove(uuid);
            return tierInfo;
        }));

        // Return null immediately, as the async process is still running
        return null;
    }

    // Method to asynchronously fetch TierInfo (simulating an external request)
    private static CompletableFuture<TierInfo> getFuture(UUID uuid) {
        // Simulate an async HTTP request or processing
        CompletableFuture<JsonObject> future = Request.safeGetRequest(uuid.toString());

        return future.thenApply(response -> {
            if (response != null) {
                System.out.println("fetched tier for " + uuid);
                // Convert the JsonObject into a TierInfo object
                return new TierInfo(response.getAsJsonObject());
            }
            return null;  // Return null if response is null
        }).exceptionally(ex -> {
            // Handle any errors that occurred during the request
            System.err.println("Error occurred for fetch (" + uuid + ") " + ex.getMessage());
            return null;  // Return null or a fallback value in case of failure
        });
    }
}