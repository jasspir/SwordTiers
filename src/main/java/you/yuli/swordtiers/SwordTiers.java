package you.yuli.swordtiers;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class SwordTiers implements ModInitializer {
    public static boolean toggled = true;

    @Override
    public void onInitialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, a) ->
                dispatcher.register(ClientCommandManager.literal("tiers").executes(context -> toggle())));
    }

    public int toggle() {
        toggled = !toggled;
        MinecraftClient.getInstance().player.sendMessage(Text.of("§7Tiers toggled"));
        return 1;
    }

    public static Text formatLatency(int latency) {
        // Determine the color based on the latency value
        String color = switch (latency / 50) {
            case 3 -> "§c"; // Red (150+)
            case 2 -> "§6"; // Orange (100+)
            case 1 -> "§e"; // Yellow (50+)
            case 0 -> latency > 25 ? "§a" : "§b"; // Lime (>25) or Aqua (<25)
            default -> "§4"; // Dark Red (200+)
        };

        // Set the latency with the appropriate color
        return Text.of(" " + color + latency + "ms");
    }
}
