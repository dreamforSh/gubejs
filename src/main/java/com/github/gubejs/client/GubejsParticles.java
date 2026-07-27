package com.github.gubejs.client;

import com.github.gubejs.Gubejs;
import com.github.gubejs.misc.ParticleTypeBuilder;
import com.github.gubejs.registry.RegistryInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

/**
 * Draws the particles scripts created.
 *
 * <p>A particle type in the registry is only half of one: the client decides what a type looks like
 * through a factory it is asked for separately, and a type with no factory throws the moment
 * something tries to spawn it. So every particle a script created gets this one, which draws the
 * frames named in the generated {@code particles/<path>.json} and drifts like a vanilla effect
 * particle.
 */
public final class GubejsParticles {

    private GubejsParticles() {
    }

    /**
     * Registers a factory for every particle type a script created.
     *
     * @param event Forge's particle provider registration event
     */
    public static void register(RegisterParticleProvidersEvent event) {
        for (var builder : RegistryInfo.PARTICLE_TYPE.getBuilders()) {
            if (!(builder instanceof ParticleTypeBuilder particle)) {
                continue;
            }

            var type = particle.get();

            if (!(type instanceof SimpleParticleType simple)) {
                continue;
            }

            event.register(simple, sprites -> (options, level, x, y, z, vx, vy, vz) ->
                new ScriptParticle(level, x, y, z, vx, vy, vz, sprites));
            Gubejs.LOGGER.debug("Registered a particle provider for {}", builder.id);
        }
    }

    /**
     * What a script's particle actually is.
     *
     * <p>Deliberately plain: it falls slowly, is slowed by air, and cycles through its frames over
     * its life. Anything more specific would be a guess about a particle this code has never seen.
     */
    private static final class ScriptParticle extends TextureSheetParticle {

        private final SpriteSet sprites;

        private ScriptParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz, SpriteSet sprites) {
            super(level, x, y, z, vx, vy, vz);
            this.sprites = sprites;
            this.friction = 0.96F;
            this.gravity = 0.1F;
            this.lifetime = 20 + this.random.nextInt(20);
            this.quadSize *= 0.75F;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        @Override
        public int getLightColor(float partialTick) {
            // Full brightness rather than the block light where it happens: a particle a script
            // spawned is usually a signal to the player, and one that vanishes in a dark room is
            // not a useful signal.
            return 0xF000F0;
        }
    }
}
