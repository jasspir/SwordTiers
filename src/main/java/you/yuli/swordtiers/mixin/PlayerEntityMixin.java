package you.yuli.swordtiers.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import you.yuli.swordtiers.SwordTiers;
import you.yuli.swordtiers.tiers.TierFetcher;
import you.yuli.swordtiers.tiers.TierInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Shadow @Final private GameProfile gameProfile;

    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    public Text addTier(Text text) {
        if (text == null) return null;
        if (!SwordTiers.toggled) return text;

        int latency = 0;
        ClientPlayNetworkHandler network = MinecraftClient.getInstance().getNetworkHandler();
        if (network != null) {
            PlayerListEntry entry = network.getPlayerListEntry(gameProfile.getId());
            if (entry != null) latency = entry.getLatency();
        }
        text = text.copy().append(SwordTiers.formatLatency(latency));

        TierInfo tier = TierFetcher.get(gameProfile.getId(), false);
        if (tier == null) return text;
        return tier.formatted.copy().append(text);
    }
}