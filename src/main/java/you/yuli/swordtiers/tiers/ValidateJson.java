package you.yuli.swordtiers.tiers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

public class ValidateJson {

    // Method to filter and return a new JSON object excluding the 'uuid' field
    public static JsonObject filterJsonFields(JsonObject jsonResponse) {
        JsonObject filteredJson = new JsonObject();

        // Add fields to filteredJson (except 'uuid')
        if (jsonResponse.has("rankings")) {
            filteredJson.add("rankings", jsonResponse.get("rankings"));
        }

        if (jsonResponse.has("region")) {
            filteredJson.add("region", jsonResponse.get("region"));
        }

        return filteredJson;
    }

    public static boolean containsNonAlphanumeric(String response) {
        String allowedJsonChars = "{}:\",_[]+";

        for (int i = 0; i < response.length(); i++) {
            char c = response.charAt(i);

            if (!(Character.isLetterOrDigit(c) || Character.isSpaceChar(c) || allowedJsonChars.indexOf(c) >= 0)) {
                return true;
            }
        }

        return false;
    }

    public static JsonObject validateJson(String response, String expectedUuid) throws IOException {
        if (response == null || response.isEmpty()) {
            throw new IOException("Received empty response.");
        }

        if (containsNonAlphanumeric(response)) {
            throw new IOException("Invalid character detected in the response. Only alphanumeric characters and spaces are allowed.");
        }

        try {
            // Parse the JSON response
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

            // ValidateJson 'uuid' field
            if (!jsonResponse.has("uuid") || !jsonResponse.get("uuid").isJsonPrimitive() || !jsonResponse.get("uuid").getAsJsonPrimitive().isString()) {
                throw new IOException("Missing or invalid 'uuid' field.");
            }

            String uuid = jsonResponse.get("uuid").getAsString();
            if (!uuid.equals(expectedUuid)) {
                throw new IOException("Expected 'uuid' to be: " + expectedUuid + ", but got: " + uuid);
            }

            // Return the filtered JSON object
            return filterJsonFields(jsonResponse);
        } catch (Exception e) {
            throw new IOException("Invalid JSON response: " + e.getMessage(), e);
        }
    }
}