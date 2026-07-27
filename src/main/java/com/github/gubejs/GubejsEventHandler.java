package com.github.gubejs;

import com.github.gubejs.bindings.event.BlockEvents;
import com.github.gubejs.bindings.event.EntityEvents;
import com.github.gubejs.bindings.event.ItemEvents;
import com.github.gubejs.bindings.event.LevelEvents;
import com.github.gubejs.bindings.event.PlayerEvents;
import com.github.gubejs.bindings.event.ServerEvents;
import com.github.gubejs.block.BlockBrokenEventJS;
import com.github.gubejs.block.BlockLeftClickedEventJS;
import com.github.gubejs.block.BlockPlacedEventJS;
import com.github.gubejs.block.BlockRightClickedEventJS;
import com.github.gubejs.block.FarmlandTrampledEventJS;
import com.github.gubejs.entity.CheckLivingEntitySpawnEventJS;
import com.github.gubejs.entity.EntitySpawnedEventJS;
import com.github.gubejs.entity.LivingEntityDeathEventJS;
import com.github.gubejs.entity.LivingEntityHurtEventJS;
import com.github.gubejs.item.FoodEatenEventJS;
import com.github.gubejs.item.ItemClickedEventJS;
import com.github.gubejs.item.ItemCraftedEventJS;
import com.github.gubejs.item.ItemDroppedEventJS;
import com.github.gubejs.item.ItemEntityInteractedEventJS;
import com.github.gubejs.item.ItemPickedUpEventJS;
import com.github.gubejs.item.ItemSmeltedEventJS;
import com.github.gubejs.level.ExplosionEventJS;
import com.github.gubejs.level.LevelEventJS;
import com.github.gubejs.player.InventoryEventJS;
import com.github.gubejs.player.PlayerChatEventJS;
import com.github.gubejs.player.PlayerEventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.server.CommandEventJS;
import com.github.gubejs.server.CommandRegistryEventJS;
import com.github.gubejs.server.ScheduledEvents;
import com.github.gubejs.server.ServerEventJS;
import com.github.gubejs.server.ServerScriptManager;
import com.github.gubejs.util.ConsoleJS;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Turns Forge's game events into the events scripts listen to.
 *
 * <p>Every listener checks {@code hasListeners()} first. Several of these fire per entity per
 * tick, and the check is a null test against a field that stays null for the whole game unless a
 * pack asked for it — which is what keeps an unused event free.
 *
 * <p>The id handed to {@code post} is the transformed key, not a {@code ResourceLocation}: the
 * item, block and entity-type events are keyed on the game object itself so that the lookup is a
 * reference comparison. See {@link ItemEvents#SUPPORTS_ITEM} and its siblings.
 */
public final class GubejsEventHandler {

    // --- server lifecycle --------------------------------------------------------------------

    /**
     * Marks server scripts as stale so the reload that is starting picks up edited files.
     *
     * @param event Forge's reload listener registration, which fires at the start of each reload
     */
    @SubscribeEvent
    public void reloadStarted(AddReloadListenerEvent event) {
        ServerScriptManager.markDirty();
    }

    @SubscribeEvent
    public void serverStarted(ServerStartedEvent event) {
        ConsoleJS.SERVER.flush();

        if (ServerEvents.LOADED.hasListeners()) {
            ServerEvents.LOADED.post(ScriptType.SERVER, new ServerEventJS(event.getServer()));
        }
    }

    @SubscribeEvent
    public void serverStopping(ServerStoppingEvent event) {
        if (ServerEvents.UNLOADED.hasListeners()) {
            ServerEvents.UNLOADED.post(ScriptType.SERVER, new ServerEventJS(event.getServer()));
        }

        ConsoleJS.SERVER.flush();
        ScheduledEvents.clear();
        ServerScriptManager.unload();
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return;
        }

        ScheduledEvents.tick(server);

        if (ServerEvents.TICK.hasListeners()) {
            ServerEvents.TICK.post(ScriptType.SERVER, new ServerEventJS(server));
        }
    }

    // --- commands ----------------------------------------------------------------------------

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        GubejsCommands.register(event.getDispatcher());

        if (ServerEvents.COMMAND_REGISTRY.hasListeners()) {
            ServerEvents.COMMAND_REGISTRY.post(ScriptType.SERVER, new CommandRegistryEventJS(
                event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()));
        }
    }

    @SubscribeEvent
    public void command(CommandEvent event) {
        if (!ServerEvents.COMMAND.hasListeners()) {
            return;
        }

        var nodes = event.getParseResults().getContext().getNodes();

        if (nodes.isEmpty()) {
            return;
        }

        var name = nodes.get(0).getNode().getName();

        if (ServerEvents.COMMAND.post(ScriptType.SERVER, name,
            new CommandEventJS(event.getParseResults(), name)).interruptFalse()) {
            event.setCanceled(true);
        }
    }

    // --- levels ------------------------------------------------------------------------------

    @SubscribeEvent
    public void levelLoaded(LevelEvent.Load event) {
        if (LevelEvents.LOADED.hasListeners() && event.getLevel() instanceof Level level) {
            LevelEvents.LOADED.post(new LevelEventJS(level), level.dimension().location());
        }
    }

    @SubscribeEvent
    public void levelUnloaded(LevelEvent.Unload event) {
        if (LevelEvents.UNLOADED.hasListeners() && event.getLevel() instanceof Level level) {
            LevelEvents.UNLOADED.post(new LevelEventJS(level), level.dimension().location());
        }
    }

    @SubscribeEvent
    public void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && LevelEvents.TICK.hasListeners()) {
            LevelEvents.TICK.post(new LevelEventJS(event.level), event.level.dimension().location());
        }
    }

    @SubscribeEvent
    public void explosionStart(ExplosionEvent.Start event) {
        if (!LevelEvents.BEFORE_EXPLOSION.hasListeners()) {
            return;
        }

        if (LevelEvents.BEFORE_EXPLOSION.post(
            new ExplosionEventJS.Before(event.getLevel(), event.getExplosion())).interruptFalse()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void explosionDetonate(ExplosionEvent.Detonate event) {
        if (LevelEvents.AFTER_EXPLOSION.hasListeners()) {
            LevelEvents.AFTER_EXPLOSION.post(new ExplosionEventJS.After(event.getLevel(),
                event.getExplosion(), event.getAffectedBlocks(), event.getAffectedEntities()));
        }
    }

    /**
     * Answers a furnace's "how long does this burn" question for items a script changed.
     *
     * <p>Through the event rather than through the item, because Forge's {@code getBurnTime} is a
     * default method on an interface the item never overrode — there is no implementation to
     * inject into.
     *
     * @param event Forge's fuel burn time event
     */
    @SubscribeEvent
    public void furnaceFuelBurnTime(
        net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent event) {
        if (event.getItemStack().getItem()
            instanceof com.github.gubejs.core.ItemKJS modifiable) {
            var modifications = modifiable.gjs$getModifications();

            if (modifications != null && modifications.burnTime != null) {
                event.setBurnTime(modifications.burnTime);
            }
        }
    }

    // --- players -----------------------------------------------------------------------------

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Before the listeners: one of them may add a stage, and sending afterwards would send the
        // list twice while sending only here would send it without the new one.
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.github.gubejs.core.StageManager.sync(serverPlayer);
        }

        if (PlayerEvents.LOGGED_IN.hasListeners()) {
            PlayerEvents.LOGGED_IN.post(new PlayerEventJS(event.getEntity()));
        }

        GubejsCommands.announceErrors(event.getEntity());
    }

    @SubscribeEvent
    public void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (PlayerEvents.LOGGED_OUT.hasListeners()) {
            PlayerEvents.LOGGED_OUT.post(new PlayerEventJS(event.getEntity()));
        }
    }

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public void playerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        if (PlayerEvents.RESPAWNED.hasListeners()) {
            PlayerEvents.RESPAWNED.post(new PlayerEventJS(event.getEntity()));
        }

        if (PlayerEvents.RESPAWN.hasListeners()) {
            PlayerEvents.RESPAWN.post(new PlayerEventJS(event.getEntity()));
        }
    }

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && PlayerEvents.TICK.hasListeners()) {
            PlayerEvents.TICK.post(new PlayerEventJS(event.player));
        }
    }

    @SubscribeEvent
    public void chat(ServerChatEvent event) {
        if (!PlayerEvents.CHAT.hasListeners() && !PlayerEvents.DECORATE_CHAT.hasListeners()) {
            return;
        }

        var chatEvent = new PlayerChatEventJS(event.getPlayer(), event.getRawText());

        if (PlayerEvents.CHAT.hasListeners()
            && PlayerEvents.CHAT.post(ScriptType.SERVER, chatEvent).interruptFalse()) {
            event.setCanceled(true);
            return;
        }

        if (PlayerEvents.DECORATE_CHAT.hasListeners()) {
            PlayerEvents.DECORATE_CHAT.post(ScriptType.SERVER, chatEvent);
        }

        if (chatEvent.getComponent() != null) {
            event.setMessage(chatEvent.getComponent());
        }
    }

    @SubscribeEvent
    public void containerOpened(PlayerContainerEvent.Open event) {
        if (PlayerEvents.INVENTORY_OPENED.hasListeners()) {
            PlayerEvents.INVENTORY_OPENED.post(
                new InventoryEventJS(event.getEntity(), event.getContainer()), menuTypeOf(event));
        }

        if (PlayerEvents.CHEST_OPENED.hasListeners()
            && event.getContainer() instanceof net.minecraft.world.inventory.ChestMenu) {
            PlayerEvents.CHEST_OPENED.post(new com.github.gubejs.player.ChestEventJS(
                event.getEntity(), event.getContainer()), menuTypeOf(event));
        }
    }

    @SubscribeEvent
    public void containerClosed(PlayerContainerEvent.Close event) {
        if (PlayerEvents.INVENTORY_CLOSED.hasListeners()) {
            PlayerEvents.INVENTORY_CLOSED.post(
                new InventoryEventJS(event.getEntity(), event.getContainer()), menuTypeOf(event));
        }

        if (PlayerEvents.CHEST_CLOSED.hasListeners()
            && event.getContainer() instanceof net.minecraft.world.inventory.ChestMenu) {
            PlayerEvents.CHEST_CLOSED.post(new com.github.gubejs.player.ChestEventJS(
                event.getEntity(), event.getContainer()), menuTypeOf(event));
        }
    }

    /**
     * Reads a menu's type, tolerating the player's own inventory.
     *
     * <p>{@code InventoryMenu.getType()} throws rather than returning null, because vanilla never
     * needs its type — but a pack listening without an id still wants the event.
     */
    @Nullable
    private static Object menuTypeOf(PlayerContainerEvent event) {
        try {
            return event.getContainer().getType();
        } catch (Exception ignored) {
            return null;
        }
    }

    // --- blocks ------------------------------------------------------------------------------

    @SubscribeEvent
    public void blockRightClicked(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var pos = event.getPos();
        var stack = event.getItemStack();

        // The item gets first refusal, so a listener can stop a click before the chest it was
        // aimed at opens.
        if (ItemEvents.FIRST_RIGHT_CLICKED.hasListeners() && !stack.isEmpty()
            && ItemEvents.FIRST_RIGHT_CLICKED.post(
                new ItemClickedEventJS(event.getEntity(), stack, event.getHand()), stack.getItem())
            .interruptFalse()) {
            event.setCanceled(true);
            return;
        }

        if (BlockEvents.RIGHT_CLICKED.hasListeners() && BlockEvents.RIGHT_CLICKED.post(
            new BlockRightClickedEventJS(level, pos, event.getEntity(), event.getHand()),
            level.getBlockState(pos).getBlock()).interruptFalse()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void blockLeftClicked(PlayerInteractEvent.LeftClickBlock event) {
        var level = event.getLevel();
        var pos = event.getPos();
        var stack = event.getItemStack();

        if (ItemEvents.FIRST_LEFT_CLICKED.hasListeners() && !stack.isEmpty()
            && ItemEvents.FIRST_LEFT_CLICKED.post(
                new ItemClickedEventJS(event.getEntity(), stack, event.getHand()), stack.getItem())
            .interruptFalse()) {
            event.setCanceled(true);
            return;
        }

        if (BlockEvents.LEFT_CLICKED.hasListeners() && BlockEvents.LEFT_CLICKED.post(
            new BlockLeftClickedEventJS(level, pos, event.getEntity(), event.getFace()),
            level.getBlockState(pos).getBlock()).interruptFalse()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void blockBroken(BlockEvent.BreakEvent event) {
        if (!BlockEvents.BROKEN.hasListeners() || !(event.getLevel() instanceof Level level)) {
            return;
        }

        if (BlockEvents.BROKEN.post(new BlockBrokenEventJS(
            level, event.getPos(), event.getState(), event.getPlayer()),
            event.getState().getBlock()).interruptFalse()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!BlockEvents.PLACED.hasListeners() || !(event.getLevel() instanceof Level level)) {
            return;
        }

        if (BlockEvents.PLACED.post(new BlockPlacedEventJS(
            level, event.getPos(), event.getPlacedBlock(), event.getEntity()),
            event.getPlacedBlock().getBlock()).interruptFalse()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void farmlandTrampled(BlockEvent.FarmlandTrampleEvent event) {
        if (!BlockEvents.FARMLAND_TRAMPLED.hasListeners()
            || !(event.getLevel() instanceof Level level)) {
            return;
        }

        if (BlockEvents.FARMLAND_TRAMPLED.post(new FarmlandTrampledEventJS(level, event.getPos(),
            event.getState(), event.getEntity(), event.getFallDistance()),
            event.getState().getBlock()).interruptFalse()) {
            event.setCanceled(true);
        }
    }

    // --- items -------------------------------------------------------------------------------

    @SubscribeEvent
    public void itemRightClicked(PlayerInteractEvent.RightClickItem event) {
        if (!ItemEvents.RIGHT_CLICKED.hasListeners()) {
            return;
        }

        var stack = event.getItemStack();

        if (ItemEvents.RIGHT_CLICKED.post(
            new ItemClickedEventJS(event.getEntity(), stack, event.getHand()), stack.getItem())
            .interruptFalse()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void entityInteracted(PlayerInteractEvent.EntityInteract event) {
        if (!ItemEvents.ENTITY_INTERACTED.hasListeners()) {
            return;
        }

        var stack = event.getItemStack();

        if (ItemEvents.ENTITY_INTERACTED.post(new ItemEntityInteractedEventJS(
            event.getEntity(), stack, event.getTarget(), event.getHand()), stack.getItem())
            .interruptFalse()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void itemCanPickUp(EntityItemPickupEvent event) {
        if (!ItemEvents.CAN_PICK_UP.hasListeners()) {
            return;
        }

        var stack = event.getItem().getItem();

        if (ItemEvents.CAN_PICK_UP.post(new ItemPickedUpEventJS(
            event.getEntity(), event.getItem(), stack), stack.getItem()).interruptFalse()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void itemPickedUp(PlayerEvent.ItemPickupEvent event) {
        if (!ItemEvents.PICKED_UP.hasListeners()) {
            return;
        }

        var stack = event.getStack();
        ItemEvents.PICKED_UP.post(
            new ItemPickedUpEventJS(event.getEntity(), event.getOriginalEntity(), stack),
            stack.getItem());
    }

    @SubscribeEvent
    public void itemDropped(ItemTossEvent event) {
        if (!ItemEvents.DROPPED.hasListeners()) {
            return;
        }

        var itemEntity = event.getEntity();

        if (ItemEvents.DROPPED.post(new ItemDroppedEventJS(event.getPlayer(), itemEntity),
            itemEntity.getItem().getItem()).interruptFalse()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void itemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!ItemEvents.CRAFTED.hasListeners()) {
            return;
        }

        var stack = event.getCrafting();
        ItemEvents.CRAFTED.post(
            new ItemCraftedEventJS(event.getEntity(), stack, event.getInventory()), stack.getItem());
    }

    @SubscribeEvent
    public void itemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!ItemEvents.SMELTED.hasListeners()) {
            return;
        }

        var stack = event.getSmelting();
        ItemEvents.SMELTED.post(new ItemSmeltedEventJS(event.getEntity(), stack), stack.getItem());
    }

    @SubscribeEvent
    public void foodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!ItemEvents.FOOD_EATEN.hasListeners() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        var stack = event.getItem();
        var foodEvent = new FoodEatenEventJS(player, stack, event.getResultStack());
        var result = ItemEvents.FOOD_EATEN.post(foodEvent, stack.getItem());

        // Finish is not a cancellable Forge event -- the food has already been swallowed. Handing
        // back the untouched item is the closest the game lets us get to undoing it.
        event.setResultStack(result.interruptFalse() ? stack.copy() : foodEvent.getResultItem());
    }

    // --- entities ----------------------------------------------------------------------------

    @SubscribeEvent
    public void entitySpawned(EntityJoinLevelEvent event) {
        if (!EntityEvents.SPAWNED.hasListeners()) {
            return;
        }

        var entity = event.getEntity();

        if (EntityEvents.SPAWNED.post(new EntitySpawnedEventJS(entity), entity.getType())
            .interruptFalse()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void checkSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (!EntityEvents.CHECK_SPAWN.hasListeners()) {
            return;
        }

        var entity = event.getEntity();
        var result = EntityEvents.CHECK_SPAWN.post(new CheckLivingEntitySpawnEventJS(
            entity, event.getX(), event.getY(), event.getZ(), event.getSpawner(),
            event.getSpawnReason()), entity.getType());

        // A three-state event: DENY refuses, ALLOW forces the spawn past checks that would
        // otherwise refuse it, DEFAULT leaves the decision to the game.
        if (result.override()) {
            event.setResult(result.forge());
        }
    }

    @SubscribeEvent
    public void entityDeath(LivingDeathEvent event) {
        if (!EntityEvents.DEATH.hasListeners()) {
            return;
        }

        var entity = event.getEntity();

        if (EntityEvents.DEATH.post(new LivingEntityDeathEventJS(entity, event.getSource()),
            entity.getType()).interruptFalse()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void entityHurt(LivingHurtEvent event) {
        if (!EntityEvents.HURT.hasListeners()) {
            return;
        }

        var entity = event.getEntity();
        var hurtEvent = new LivingEntityHurtEventJS(entity, event.getSource(), event.getAmount());

        if (EntityEvents.HURT.post(hurtEvent, entity.getType()).interruptFalse()) {
            event.setCanceled(true);
        } else {
            event.setAmount(hurtEvent.getDamage());
        }
    }
}
