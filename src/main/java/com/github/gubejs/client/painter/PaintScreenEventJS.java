package com.github.gubejs.client.painter;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import com.github.gubejs.util.NbtHelper;
import com.github.gubejs.util.ValueUtils;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The screen being drawn, once per frame — {@code ClientEvents.paintScreen(event => ...)}.
 *
 * <pre>{@code
 * ClientEvents.paintScreen(event => {
 *     if (!event.player) return
 *     event.draw({ type: 'text', text: `${event.player.health | 0} HP`, x: 10, y: 10, shadow: true })
 *     event.draw({ type: 'rectangle', x: event.width - 30, y: 10, w: 20, h: 20, color: '#80000000' })
 * })
 * }</pre>
 *
 * <p>Same description format as {@code Client.paint(...)} and {@code player.paint(...)} — there is
 * one shape to learn, not two. The difference is who owns the drawing: the named painter keeps what
 * it was given until something changes it, and this draws only what the callback drew this frame.
 * A bar that follows a value every frame belongs here; a quest marker that sits there until the
 * quest is done belongs in the painter, which costs nothing between changes.
 *
 * <p>Fires only while the game's own interface is being drawn — in a world, not on the title screen
 * or behind a menu, which is the same rule the named painter follows.
 *
 * <p>This runs on the render thread, sixty or more times a second. Work done here is work done that
 * often — read a value, draw it, and leave anything expensive to a tick listener.
 */
public final class PaintScreenEventJS extends EventJS implements ScriptTypeHolder {

    private final PoseStack pose;

    private final Window window;

    private final float partialTick;

    public PaintScreenEventJS(PoseStack pose, Window window, float partialTick) {
        this.pose = pose;
        this.window = window;
        this.partialTick = partialTick;
    }

    /**
     * Returns how wide the screen is, in the pixels a description's coordinates are measured in.
     *
     * @return the GUI-scaled width
     */
    public int getWidth() {
        return window.getGuiScaledWidth();
    }

    /**
     * Returns how tall the screen is, in the pixels a description's coordinates are measured in.
     *
     * @return the GUI-scaled height
     */
    public int getHeight() {
        return window.getGuiScaledHeight();
    }

    /**
     * Returns where the mouse is, in the same pixels as {@link #getWidth}.
     *
     * @return the horizontal position
     */
    public int getMouseX() {
        var screen = window.getScreenWidth();
        return screen == 0 ? 0
            : (int) (Minecraft.getInstance().mouseHandler.xpos() * getWidth() / screen);
    }

    /**
     * Returns where the mouse is, in the same pixels as {@link #getHeight}.
     *
     * @return the vertical position
     */
    public int getMouseY() {
        var screen = window.getScreenHeight();
        return screen == 0 ? 0
            : (int) (Minecraft.getInstance().mouseHandler.ypos() * getHeight() / screen);
    }

    /**
     * Returns how far through the current tick this frame is.
     *
     * <p>Between {@code 0} and {@code 1}. What a value read once a tick is interpolated with when
     * it should move smoothly rather than in twenty steps a second.
     *
     * @return the fraction of a tick elapsed
     */
    public float getPartialTick() {
        return partialTick;
    }

    /**
     * Returns the player at the keyboard.
     *
     * @return the player, or {@code null} on a screen with no world behind it
     */
    @Nullable
    public Player getPlayer() {
        return Minecraft.getInstance().player;
    }

    /**
     * Returns the transform stack the frame is being drawn with.
     *
     * <p>For a script reaching past {@link #draw} into the game's own drawing methods.
     *
     * @return the pose stack
     */
    public PoseStack getPoseStack() {
        return pose;
    }

    /**
     * Draws one thing.
     *
     * @param description the same shape {@code player.paint} takes for one of its values —
     *     {@code { type: 'text', text: 'hello', x: 10, y: 10 }}
     */
    public void draw(Object description) {
        var tag = NbtHelper.compound(ValueUtils.unwrap(description));

        if (tag == null || tag.isEmpty()) {
            return;
        }

        var object = new PaintObject(tag);

        if (object.isVisible()) {
            object.draw(pose, getWidth(), getHeight());
        }
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return ScriptType.CLIENT;
    }
}
