package com.github.gubejs.bindings;

import com.github.gubejs.Gubejs;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code Platform} global: what is installed, and where.
 *
 * <pre>{@code
 * if (Platform.isLoaded('create')) { ... }
 * // requires: create      // usually better than the check above
 * }</pre>
 */
public final class PlatformWrapper {

    private PlatformWrapper() {
    }

    /**
     * Returns the mod loader's name.
     *
     * @return always {@code "forge"} here; the value exists so a pack shared between loaders can
     *     branch on it
     */
    public static String getName() {
        return "forge";
    }

    /**
     * Reports whether a mod is installed.
     *
     * @param modId the mod id
     * @return {@code true} if it is loaded
     */
    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /**
     * Returns a mod's version.
     *
     * @param modId the mod id
     * @return the version string, or {@code null} if the mod is not installed
     */
    @Nullable
    public static String getVersion(String modId) {
        return ModList.get().getModContainerById(modId)
            .map(c -> c.getModInfo().getVersion().toString()).orElse(null);
    }

    /**
     * Returns every installed mod id.
     *
     * @return the ids
     */
    public static List<String> getMods() {
        var ids = new ArrayList<String>();
        ModList.get().forEachModContainer((id, container) -> ids.add(id));
        return ids;
    }

    /** Whether this side is a physical client. */
    public static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    /** Whether this side is a dedicated server. */
    public static boolean isServer() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }

    /** Whether the game is running from a development workspace rather than a published jar. */
    public static boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }

    /** Whether this launch is a data generation run rather than a game. */
    public static boolean isDataGen() {
        return FMLLoader.launcherHandlerName().contains("data");
    }

    /** The Minecraft version, as a string. */
    public static String getMcVersion() {
        return Gubejs.MC_VERSION_STRING;
    }

    /** The Minecraft version, as the number packs branch on: 1902 for 1.19.2. */
    public static int getMcVersionNumber() {
        return Gubejs.MC_VERSION_NUMBER;
    }

    /** This mod's version. */
    public static String getGubejsVersion() {
        return getVersion(Gubejs.MOD_ID);
    }
}
