package daniking.vinery.block.entity;

import daniking.vinery.block.StoveBlock;
import daniking.vinery.client.gui.handler.StoveGuiHandler;
import daniking.vinery.recipe.StoveCookingRecipe;
import daniking.vinery.registry.VineryBlockEntityTypes;
import daniking.vinery.registry.VineryRecipeTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class StoveBlockEntity extends BlockEntity implements BlockEntityTicker<StoveBlockEntity>, Inventory, NamedScreenHandlerFactory {

    private DefaultedList<ItemStack> inventory;

    protected int burnTime;
    protected int burnTimeTotal;
    protected int cookTime;
    protected int cookTimeTotal;

    protected float experience;

    protected static final int FUEL_SLOT = StoveGuiHandler.FUEL_SLOT;
    protected static final int BUCKET_SLOT = StoveGuiHandler.BUCKET_SLOT;
    protected static final int INGREDIENT_SLOT = StoveGuiHandler.INGREDIENT_SLOT;
    protected static final int OUTPUT_SLOT = StoveGuiHandler.OUTPUT_SLOT;

    protected static final int TOTAL_COOKING_TIME = 240;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> StoveBlockEntity.this.burnTime;
                case 1 -> StoveBlockEntity.this.burnTimeTotal;
                case 2 -> StoveBlockEntity.this.cookTime;
                case 3 -> StoveBlockEntity.this.cookTimeTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> StoveBlockEntity.this.burnTime = value;
                case 1 -> StoveBlockEntity.this.burnTimeTotal = value;
                case 2 -> StoveBlockEntity.this.cookTime = value;
                case 3 -> StoveBlockEntity.this.cookTimeTotal = value;
            }
        }

        @Override
        public int size() {
            return 4;
        }
    };
    public StoveBlockEntity(BlockPos pos, BlockState state) {
        super(VineryBlockEntityTypes.STOVE_BLOCK_ENTITY, pos, state);
        this.inventory = DefaultedList.ofSize(4, ItemStack.EMPTY);
    }

    public void dropExperience(ServerWorld world, Vec3d pos) {
        ExperienceOrbEntity.spawn(world, pos, (int) experience);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.inventory);
        this.burnTime = nbt.getShort("BurnTime");
        this.cookTime = nbt.getShort("CookTime");
        this.cookTimeTotal = nbt.getShort("CookTimeTotal");
        this.burnTimeTotal = this.getTotalBurnTime(this.getStack(FUEL_SLOT));
        this.experience = nbt.getFloat("Experience");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putShort("BurnTime", (short)this.burnTime);
        nbt.putShort("CookTime", (short)this.cookTime);
        nbt.putShort("CookTimeTotal", (short)this.cookTimeTotal);
        nbt.putFloat("Experience", this.experience);
        Inventories.writeNbt(nbt, this.inventory);
    }

    protected boolean isBurning() {
        return this.burnTime > 0;
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state, StoveBlockEntity blockEntity) {
        if (world.isClient) {
            return;
        }
        boolean initialBurningState = blockEntity.isBurning();
        boolean dirty = false;
        if (initialBurningState) {
            --this.burnTime;
        }
        final StoveCookingRecipe recipe = world.getRecipeManager().getFirstMatch(VineryRecipeTypes.STOVE_RECIPE_TYPE, blockEntity, world).orElse(null);
        if (!initialBurningState && canCraft(recipe)) {
            this.burnTime = this.burnTimeTotal = this.getTotalBurnTime(this.getStack(FUEL_SLOT));
            if (burnTime > 0) {
                dirty = true;
                final ItemStack fuelStack = this.getStack(FUEL_SLOT);
                if (fuelStack.getItem().hasRecipeRemainder()) {
                    setStack(FUEL_SLOT, new ItemStack(fuelStack.getItem().getRecipeRemainder()));
                } else if (fuelStack.getCount() > 1) {
                    removeStack(FUEL_SLOT, 1);
                } else if (fuelStack.getCount() == 1) {
                    setStack(FUEL_SLOT, ItemStack.EMPTY);
                }
            }
        }
        if (isBurning() && canCraft(recipe)) {
            ++this.cookTime;
            if (this.cookTime == cookTimeTotal) {
                this.cookTime = 0;
                craft(recipe);
                dirty = true;
            }
        } else if (!canCraft(recipe)) {
            this.cookTime = 0;
        }
        if (initialBurningState != isBurning()) {
            if (state.get(StoveBlock.LIT) != (burnTime > 0)) {
                world.setBlockState(pos, state.with(StoveBlock.LIT, burnTime > 0), Block.NOTIFY_ALL);
                dirty = true;
            }
        }
        if (dirty) {
            markDirty();
        }

    }

    protected boolean canCraft(StoveCookingRecipe recipe) {
        if (recipe == null || recipe.getOutput().isEmpty()) {
            return false;
        } else if (this.getStack(BUCKET_SLOT).isEmpty()) {
            return false;
        } else if (!this.getStack(BUCKET_SLOT).isOf(Items.WATER_BUCKET)) {
            return false;
        } else if (this.getStack(INGREDIENT_SLOT).isEmpty()) {
            return false;
            // If the output slot is empty, we don't need more checks
        } else if (this.getStack(OUTPUT_SLOT).isEmpty()) {
            return true;
        } else {
            final ItemStack recipeOutput = recipe.getOutput();
            final ItemStack outputSlotStack = this.getStack(OUTPUT_SLOT);
            final int outputSlotCount = outputSlotStack.getCount();
            if (!outputSlotStack.isItemEqualIgnoreDamage(recipeOutput)) {
                return false;
            } else if (outputSlotCount < this.getMaxCountPerStack() && outputSlotCount < outputSlotStack.getMaxCount()) {
                return true;
            } else {
                return outputSlotCount < recipeOutput.getMaxCount();
            }
        }
    }

    protected void craft(StoveCookingRecipe recipe) {
        if (recipe == null || !canCraft(recipe)) {
            return;
        }
        final ItemStack recipeOutput = recipe.getOutput();
        final ItemStack outputSlotStack = this.getStack(OUTPUT_SLOT);
        if (outputSlotStack.isEmpty()) {
            setStack(OUTPUT_SLOT, recipeOutput.copy());
        } else if (outputSlotStack.isOf(recipeOutput.getItem())) {
            outputSlotStack.increment(recipeOutput.getCount());
        }
        final ItemStack bucket =  this.getStack(BUCKET_SLOT);
        final ItemStack ingredient = this.getStack(INGREDIENT_SLOT);
        this.experience += recipe.getExperience();
        if (bucket.getItem().hasRecipeRemainder()) {
            setStack(BUCKET_SLOT, new ItemStack(bucket.getItem().getRecipeRemainder()));
        }
        ingredient.decrement(1);
    }

    protected int getTotalBurnTime(ItemStack fuel) {
        if (fuel.isEmpty()) {
            return 0;
        } else {
            final Item item = fuel.getItem();
            return AbstractFurnaceBlockEntity.createFuelTimeMap().getOrDefault(item, 0);
        }
    }



    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        final ItemStack stackInSlot = this.inventory.get(slot);
        boolean dirty = !stack.isEmpty() && stack.isItemEqualIgnoreDamage(stackInSlot) && ItemStack.areNbtEqual(stack, stackInSlot);
        this.inventory.set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        if (slot == INGREDIENT_SLOT && !dirty) {
            this.cookTimeTotal = TOTAL_COOKING_TIME;
            this.cookTime = 0;
            this.markDirty();
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (this.world.getBlockEntity(this.pos) != this) {
            return false;
        } else {
            return player.squaredDistanceTo((double)this.pos.getX() + 0.5, (double)this.pos.getY() + 0.5, (double)this.pos.getZ() + 0.5) <= 64.0;
        }
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public Text getDisplayName() {
        return new TranslatableText(this.getCachedState().getBlock().getTranslationKey());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new StoveGuiHandler(syncId, inv, this, this.propertyDelegate);
    }
}
