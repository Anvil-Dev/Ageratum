package dev.anvilcraft.resource.ageratum.item;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.init.AgeratumDataComponents;
import dev.anvilcraft.resource.ageratum.item.component.Doc;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class GuideBookItem extends Item {
    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        Identifier defaultLoc = Ageratum.location(AgeratumConstants.Guide.INDEX_FILE);
        Doc doc = player.getItemInHand(hand).getOrDefault(AgeratumDataComponents.DOC.get(), new Doc(defaultLoc));
        Ageratum.openGuide(serverPlayer, doc.id());
        level.playSound(null, player, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
