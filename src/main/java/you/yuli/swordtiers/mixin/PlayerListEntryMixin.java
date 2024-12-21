package you.yuli.swordtiers.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import you.yuli.swordtiers.SwordTiers;
import you.yuli.swordtiers.tiers.TierFetcher;
import you.yuli.swordtiers.tiers.TierInfo;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
    @Shadow public abstract GameProfile getProfile();
    @Shadow private int latency;

    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    public Text addTier(Text text) {
        if (text == null) return null;
        if (!SwordTiers.toggled) return text;
        text = text.copy().append(SwordTiers.formatLatency(latency));
        TierInfo tier = TierFetcher.get(getProfile().getId(), false);
        if (tier == null) return text;
        return tier.formatted.copy().append(text);
    }
}