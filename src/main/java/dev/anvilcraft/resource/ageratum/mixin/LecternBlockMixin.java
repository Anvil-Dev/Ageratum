package dev.anvilcraft.resource.ageratum.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.init.AgeratumDataComponents;
import dev.anvilcraft.resource.ageratum.init.AgeratumItems;
import dev.anvilcraft.resource.ageratum.item.GuideBookItem;
import dev.anvilcraft.resource.ageratum.item.component.Doc;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternBlock.class)
public class LecternBlockMixin {
    @WrapOperation(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"
        )
    )
    private boolean useItemOn(ItemStack instance, TagKey<Item> tagKey, Operation<Boolean> original) {
        return instance.is(AgeratumItems.DEFAULT_GUIDE_ITEM) || original.call(instance, tagKey);
    }

    @Inject(
        method = "openScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;"
        ),
        cancellable = true
    )
    private void openScreen(
        Level level,
        BlockPos pos,
        Player player,
        CallbackInfo ci,
        @Local(name = "blockEntity") BlockEntity blockEntity
    ) {
        LecternBlockEntity be = (LecternBlockEntity) blockEntity;
        ItemStack book = be.getBook();
        if (!(book.getItem() instanceof GuideBookItem)) return;
        if (!level.isClientSide()) {
            Doc doc = book.getOrDefault(
                AgeratumDataComponents.DOC.get(),
                Doc.of(Ageratum.location(AgeratumConstants.Guide.INDEX_FILE))
            );
            if (doc.isGitHub()) {
                Ageratum.openGitHubGuide((ServerPlayer) player, doc.value());
            } else {
                Ageratum.openGuide((ServerPlayer) player, doc.id());
            }
        }
        player.awardStat(Stats.INTERACT_WITH_LECTERN);
        ci.cancel();
    }
}
