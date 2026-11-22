package dev.anvilcraft.resource.ageratum.item;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.init.AgeratumDataComponents;
import dev.anvilcraft.resource.ageratum.item.component.Doc;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GuideBookItem extends Item {
    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.success(stack);
        ResourceLocation defaultLoc = Ageratum.location(AgeratumConstants.Guide.INDEX_FILE);
        Doc doc = stack.getOrDefault(AgeratumDataComponents.DOC.get(), Doc.of(defaultLoc));
        if (doc.isGitHub()) {
            Ageratum.openGitHubGuide(serverPlayer, doc.value());
        } else {
            var location = doc.id();
            if (location == null) return InteractionResultHolder.fail(stack);
            Ageratum.openGuide(serverPlayer, location);
        }
        level.playSound(null, player, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.consume(stack);
    }
}
