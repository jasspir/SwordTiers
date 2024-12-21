package you.yuli.swordtiers.tiers;

import com.google.gson.JsonObject;
import net.minecraft.text.Text;

public class TierInfo {
    public int tier = -1;
    public String rank = "";
    public boolean retired = false;
    public String region = "";
    public Text formatted = Text.empty();

    public TierInfo(JsonObject json) {
        if (json == null) {
            System.out.println("Tier info is null");
            formatted = Text.empty();
            return;
        }

        // Extract basic information
        region = json.has("region") ? json.get("region").getAsString() : "??";

        // Extract sword rankings
        if (json.has("rankings")) {
            JsonObject rankings = json.getAsJsonObject("rankings");
            if (rankings.has("sword")) {
                JsonObject sword = rankings.getAsJsonObject("sword");
                // Extract tier and position, fallback to peak if necessary
                tier = sword.has("tier") ? sword.get("tier").getAsInt() : -1;
                int pos = sword.has("pos") ? sword.get("pos").getAsInt() : -1;
                int peakTier = sword.has("peak_tier") ? sword.get("peak_tier").getAsInt() : -1;
                int peakPos = sword.has("peak_pos") ? sword.get("peak_pos").getAsInt() : -1;
                retired = sword.has("retired") && sword.get("retired").getAsBoolean();

                // If no current tier or pos, fall back to peak values
                if (tier == -1 || pos == -1) {
                    tier = peakTier;
                    pos = peakPos;
                    retired = true; // Automatically set retired to true if using peak values
                }

                // Determine rank based on position
                rank = (pos == 0) ? "HIGH" : (pos > 0) ? "LOW" : "UNKNOWN";
            }
        }

        formatted = format();
    }

    @Override
    public String toString() {
        return formatted.getString();
    }

    private Text format() {
        // Mapping for region colors with default as §0
        String regionColor = switch (region) {
            case "NA" -> "§c";  // North America -> color c (light red)
            case "EU" -> "§a";  // Europe -> color a (lime)
            case "AS" -> "§b";  // Asia -> color b (aqua)
            case "AU" -> "§e";  // Australia -> color e (yellow)
            case "SA" -> "§6";  // South America -> color 6 (gold)
            case "ME" -> "§3";  // Middle East -> color 3 (dark aqua)
            default -> "§0";    // Anything else -> color 0 (black)
        };

        // Mapping for ranking colors with default as §0
        String color = switch (rank) {
            case "HIGH" -> switch (tier) {
                case 1 -> "§4";  // HT1 -> color 4 (red)
                case 2 -> "§9";  // HT2 -> color 9 (blue)
                case 3 -> "§2";  // HT3 -> color 2 (green)
                case 4 -> "§6";  // HT4 -> color 6 (yellow)
                case 5 -> "§5";  // HT5 -> color 5 (purple)
                default -> "§0"; // Default to color 0 (black) if tier is invalid
            };
            case "LOW" -> switch (tier) {
                case 1 -> "§c";  // LT1 -> color c (light red)
                case 2 -> "§b";  // LT2 -> color b (aqua)
                case 3 -> "§a";  // LT3 -> color a (lime)
                case 4 -> "§e";  // LT4 -> color e (yellow)
                case 5 -> "§3";  // LT5 -> color 3 (dark aqua)
                default -> "§0"; // Default to color 0 (black) if tier is invalid
            };
            default -> "§0"; // Default to color 0 (black) for unrecognized rank
        };

        // Color before the username
        String endColor = "§7";  // Light gray color

        // Retired flag with gold color for the "R"
        String retiredFlag = retired ? "§6R" : "";  // "R" is gold if retired, empty if not

        // Format
        if (tier != -1) {
            String rankInitial = rank.isEmpty() ? "T" : rank.substring(0, 1).toUpperCase() + "T";
            return Text.of(String.format("%s %s%s%d %s", regionColor + region, retiredFlag, color + rankInitial, tier, endColor));
        } else {
            return Text.of(String.format("%s%s ", regionColor + region, endColor));
        }
    }
}