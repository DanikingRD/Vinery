package daniking.vinery;

import daniking.vinery.registry.*;
import daniking.vinery.world.VineryConfiguredFeatures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.LootTableEntry;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

public class Vinery implements ModInitializer {

    public static final String MODID = "vinery";
    public static final ItemGroup CREATIVE_TAB = FabricItemGroupBuilder.build(new VineryIdentifier("creative_tab"), () -> new ItemStack(ObjectRegistry.RED_GRAPE));
    public static final TagKey<Block> ALLOWS_COOKING_ON_POT = TagKey.of(Registry.BLOCK_KEY, new VineryIdentifier("allows_cooking_on_pot"));

    @Override
    public void onInitialize() {
        ObjectRegistry.init();
        VineryBlockEntityTypes.init();
        VineryScreenHandlerTypes.init();
        VineryRecipeTypes.init();
        LootTableLoadingCallback.EVENT.register((resourceManager, manager, id, supplier, setter) -> {
            final Identifier resourceLocation = new VineryIdentifier("inject/seeds");
            if (Blocks.GRASS.getLootTableId().equals(id) || Blocks.TALL_GRASS.getLootTableId().equals(id) || Blocks.FERN.getLootTableId().equals(id)) {
                supplier.withPool(LootPool.builder().with(LootTableEntry.builder(resourceLocation).weight(1)).build());
            }
        });
        VineryBoatTypes.init();
        VineryConfiguredFeatures.init();
        VinerySoundEvents.init();

    }
}

