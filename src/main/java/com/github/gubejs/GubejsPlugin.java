package com.github.gubejs;

import com.github.gubejs.event.EventGroup;
import com.github.gubejs.recipe.RecipesEventJS;
import com.github.gubejs.script.BindingsEvent;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.util.ClassFilter;

/**
 * What a mod extends to add to the scripting API.
 *
 * <p>Discovered from a {@code gubejs.plugins.txt} in the mod's jar, one fully qualified class name
 * per line. Every method is a no-op, so an implementation overrides only what it needs.
 *
 * <p>Plugins are constructed during mod loading, before any script runs, and the same instance is
 * reused for every reload — so anything cached in one has to be dropped in
 * {@link #clearCaches()}.
 */
public class GubejsPlugin {

    /**
     * Called once, as soon as the plugin is constructed.
     *
     * <p>Registry types and other things scripts refer to by name belong here, because startup
     * scripts run immediately afterwards.
     */
    public void init() {
    }

    /** Called after startup scripts have run and every registry has been filled. */
    public void afterInit() {
    }

    /** Called on the client only, once the client has finished setting up. */
    public void clientInit() {
    }

    /** Called before server scripts run on each datapack reload. */
    public void onServerReload() {
    }

    /**
     * Publishes this plugin's event groups.
     *
     * <p>Call {@link EventGroup#register()} here rather than in a static initialiser, so that a
     * group belonging to an absent mod is never published.
     */
    public void registerEvents() {
    }

    /**
     * Adjusts which Java classes scripts of one type may look up.
     *
     * @param type the script type being set up
     * @param filter the filter to adjust
     */
    public void registerClasses(ScriptType type, ClassFilter filter) {
    }

    /**
     * Adds globals.
     *
     * @param event collects the globals for one script type
     */
    public void registerBindings(BindingsEvent event) {
    }

    /**
     * Drops anything cached between reloads.
     *
     * <p>Called at the start of every reload, before scripts are read.
     */
    public void clearCaches() {
    }

    /**
     * Adds recipes that this mod generates at runtime.
     *
     * <p>Only needed by a mod whose own runtime recipes are otherwise lost, because this mod takes
     * over recipe loading when a pack listens to {@code ServerEvents.recipes}.
     *
     * @param event the recipe event being built
     */
    public void injectRuntimeRecipes(RecipesEventJS event) {
    }

    @Override
    public String toString() {
        return getClass().getName();
    }
}
