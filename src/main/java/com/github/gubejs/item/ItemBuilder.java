package com.github.gubejs.item;

import com.github.gubejs.Gubejs;
import com.github.gubejs.registry.BuilderBase;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a plain item — {@code event.create('steel_ingot')}.
 *
 * <p>Everything has a default that produces a working item, so the shortest useful script is one
 * call. A model and a translation are generated unless the pack provides its own, which is what
 * makes a new item show up with a name and a texture slot rather than as a purple cube.
 */
public class ItemBuilder extends BuilderBase<Item> {

    protected int maxStackSize = 64;

    protected int maxDamage = 0;

    protected Rarity rarity = Rarity.COMMON;

    protected boolean fireResistant;

    @Nullable
    protected CreativeModeTab tab = CreativeModeTab.TAB_MISC;

    @Nullable
    protected FoodProperties food;

    @Nullable
    protected ResourceLocation texture;

    protected String parentModel = "minecraft:item/generated";

    public ItemBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Sets how many fit in one slot.
     *
     * @param maxStackSize 1 to 64
     * @return this builder
     */
    public ItemBuilder maxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    /**
     * Makes the item a tool with durability.
     *
     * <p>Also forces the stack size to one: a damageable item that stacks is not something
     * Minecraft supports, and the combination silently breaks the damage bar.
     *
     * @param maxDamage how many uses before it breaks
     * @return this builder
     */
    public ItemBuilder maxDamage(int maxDamage) {
        this.maxDamage = maxDamage;
        this.maxStackSize = 1;
        return this;
    }

    /**
     * Sets the colour the name is shown in.
     *
     * @param rarity {@code common}, {@code uncommon}, {@code rare} or {@code epic}
     * @return this builder
     */
    public ItemBuilder rarity(Rarity rarity) {
        this.rarity = rarity;
        return this;
    }

    /**
     * Stops the item burning up in lava and fire.
     *
     * @param fireResistant whether it survives fire
     * @return this builder
     */
    public ItemBuilder fireResistant(boolean fireResistant) {
        this.fireResistant = fireResistant;
        return this;
    }

    /**
     * Sets which creative tab the item appears in.
     *
     * @param tab the tab, or {@code null} to hide it from creative
     * @return this builder
     */
    public ItemBuilder creativeTab(@Nullable CreativeModeTab tab) {
        this.tab = tab;
        return this;
    }

    /**
     * Makes the item edible.
     *
     * @param nutrition how many half-drumsticks it restores
     * @param saturation the saturation modifier
     * @return this builder
     */
    public ItemBuilder food(int nutrition, double saturation) {
        this.food = new FoodProperties.Builder()
            .nutrition(nutrition).saturationMod((float) saturation).build();
        return this;
    }

    /**
     * Makes the item edible, describing what eating it does.
     *
     * <pre>{@code
     * event.create('nether_apple').food(food => {
     *     food.hunger(6).saturation(1.2).alwaysEdible()
     *     food.effect('minecraft:fire_resistance', 600, 0, 1)
     * })
     * }</pre>
     *
     * <p>Built at the end of the callback rather than kept, so the effect ids are resolved once
     * every registry is filled — which is what lets a food name an effect the same pack creates.
     *
     * @param action describes the food
     * @return this builder
     */
    public ItemBuilder food(java.util.function.Consumer<FoodBuilder> action) {
        var builder = new FoodBuilder();
        action.accept(builder);
        this.foodBuilder = builder;
        return this;
    }

    /** What a script described in {@link #food(java.util.function.Consumer)}, until it is built. */
    @Nullable
    protected FoodBuilder foodBuilder;

    /**
     * Points the generated model at a texture other than the one named after the item.
     *
     * @param texture the texture id, e.g. {@code mypack:item/steel_ingot}
     * @return this builder
     */
    public ItemBuilder texture(Object texture) {
        this.texture = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(texture)));
        return this;
    }

    /**
     * Replaces the generated model's parent, for an item that should be held like a tool.
     *
     * @param parentModel the parent model id, e.g. {@code minecraft:item/handheld}
     * @return this builder
     */
    public ItemBuilder parentModel(String parentModel) {
        this.parentModel = parentModel;
        return this;
    }

    @Override
    public Item createObject() {
        return new Item(createProperties());
    }

    /**
     * Assembles the vanilla properties object from everything the script set.
     *
     * @return the properties
     */
    protected Item.Properties createProperties() {
        var properties = new Item.Properties().stacksTo(maxStackSize).rarity(rarity).tab(tab);

        if (maxDamage > 0) {
            properties.durability(maxDamage);
        }

        if (fireResistant) {
            properties.fireResistant();
        }

        if (foodBuilder != null) {
            properties.food(foodBuilder.build());
        } else if (food != null) {
            properties.food(food);
        }

        return properties;
    }

    @Override
    public Map<String, String> getTranslations() {
        return Map.of("item." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
            getDisplayName());
    }

    @Override
    public Map<String, String> getGeneratedAssets() {
        var assets = new LinkedHashMap<String, String>();
        var layer = texture != null ? texture
            : new ResourceLocation(id.getNamespace(), "item/" + id.getPath());

        assets.put("assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json",
            """
            {
              "parent": "%s",
              "textures": {
                "layer0": "%s"
              }
            }""".formatted(parentModel, layer));
        return assets;
    }

    /** Registers the item types scripts can create. */
    public static void registerTypes() {
        com.github.gubejs.registry.RegistryInfo.ITEM
            .addType("basic", ItemBuilder::new)
            .defaultType("basic");
        Gubejs.LOGGER.debug("Registered item builder types");
    }
}
