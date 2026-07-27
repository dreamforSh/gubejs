package com.github.gubejs.gui;

import com.github.gubejs.event.EventExit;
import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/**
 * One click on a scripted screen.
 *
 * <pre>{@code
 * gui.onClick(11, click => {
 *     if (click.rightClick) {
 *         click.player.tell('Right')
 *     }
 *     click.gui.set(11, 'minecraft:emerald')
 *     click.refresh()
 * })
 * }</pre>
 *
 * <p>Always on the server, which is what makes a scripted screen safe to build a shop out of: the
 * client only ever sent "I clicked slot 11", and everything the callback decides happens where the
 * items actually are.
 */
public class ChestGuiClickEventJS extends EventJS {

    private final ChestGuiJS gui;

    private final GubejsChestMenu menu;

    private final Player player;

    private final int slot;

    private final int button;

    private final ClickType clickType;

    ChestGuiClickEventJS(ChestGuiJS gui, GubejsChestMenu menu, Player player, int slot, int button,
                         ClickType clickType) {
        this.gui = gui;
        this.menu = menu;
        this.player = player;
        this.slot = slot;
        this.button = button;
        this.clickType = clickType;
    }

    /** The screen that was clicked, so a callback can change what is on it. */
    public ChestGuiJS getGui() {
        return gui;
    }

    /** Who clicked. */
    public Player getPlayer() {
        return player;
    }

    /** Which slot, counted from the top-left across each row. */
    public int getSlot() {
        return slot;
    }

    /** Which column the slot is in, 0 to 8. */
    public int getX() {
        return slot % ChestGuiJS.COLUMNS;
    }

    /** Which row the slot is in, counted from the top. */
    public int getY() {
        return slot / ChestGuiJS.COLUMNS;
    }

    /** What is in the slot. */
    public ItemStack getItem() {
        return gui.get(slot);
    }

    /** {@code true} for a left click. */
    public boolean isLeftClick() {
        return button == 0 && clickType != ClickType.QUICK_MOVE;
    }

    /** {@code true} for a right click. */
    public boolean isRightClick() {
        return button == 1 && clickType != ClickType.QUICK_MOVE;
    }

    /** {@code true} when the player was holding shift. */
    public boolean isShiftClick() {
        return clickType == ClickType.QUICK_MOVE;
    }

    /**
     * Returns what kind of click this was, as the game names it.
     *
     * @return {@code 'pickup'}, {@code 'quick_move'}, {@code 'swap'}, {@code 'clone'},
     *     {@code 'throw'}, {@code 'quick_craft'} or {@code 'pickup_all'}
     */
    public String getType() {
        return clickType.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Sends the screen's contents to the player again.
     *
     * <p>Needed after a callback changes what is in a slot: the client drew what was there before
     * the click, and nothing else tells it otherwise.
     */
    public void refresh() {
        menu.broadcastFullState();
    }

    /** Closes the screen. */
    public void close() {
        player.closeContainer();
    }

    /**
     * Calls one listener, reporting a failure rather than letting it reach the click packet.
     *
     * <p>A callback that throws would otherwise travel up through the network handler, where Forge
     * turns it into a disconnection — which is a great deal more than the pack author asked for by
     * making a typo in a shop menu.
     */
    void run(Consumer<ChestGuiClickEventJS> callback) {
        var manager = gui.owner == null ? null : gui.owner.getManager();

        try {
            if (manager == null) {
                callback.accept(this);
            } else {
                manager.inContext(() -> {
                    callback.accept(this);
                    return null;
                });
            }
        } catch (Throwable ex) {
            // event.cancel() means "stop here", and here is the end of the callback anyway.
            if (EventExit.unwrap(ex) == null) {
                ConsoleJS.getCurrent(ConsoleJS.SERVER)
                    .handleError(ex, "Error handling a click on a scripted screen");
            }
        }
    }
}
