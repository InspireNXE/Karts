/**
 * The MIT License (MIT)
 *
 * Copyright (c) InspireNXE <https://www.inspirenxe.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.inspirenxe.karts.track;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldCreationSettings;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TrackManager {
    private final List<UUID> instances = new ArrayList<UUID>();

    public TrackManager() {
        Tracks.fakeInit();
    }

    public World createTrack(String trackInstanceName, WorldCreationSettings trackArchetype) throws IOException {
        final WorldProperties properties = Sponge.getServer().createWorldProperties(trackInstanceName, trackArchetype);
        final World instance = Sponge.getServer().loadWorld(properties).orElseThrow(() -> new IOException("Failed to create track instance!"));
        instances.add(instance.getUniqueId());
        return instance;
    }

    public boolean isTrackInstance(World world) {
        for (UUID uniqueId : instances) {
            if (world.getUniqueId().equals(uniqueId)) {
                return true;
            }
        }

        return false;
    }

    public void cleanupInstance(WorldProperties trackProperties) {
        final Optional<World> optWorld = Sponge.getServer().getWorld(trackProperties.getWorldName());
        if (optWorld.isPresent()) {
            if (!isTrackInstance(optWorld.get())) {
                return;
            }

            if (!optWorld.get().getPlayers().isEmpty()) {
                return;
            }

            Sponge.getServer().unloadWorld(optWorld.get());
        }

        Sponge.getServer().deleteWorld(trackProperties).whenCompleteAsync(
                (aBoolean, throwable) -> {
                    if (aBoolean) {
                        try {
                            final Path confPath = Sponge.getGame().getGameDirectory().resolve("config").resolve("sponge").resolve("worlds").resolve
                                    (trackProperties.getDimensionType().getId()).resolve(trackProperties.getWorldName()).resolve
                                    ("world.conf");
                            if (Files.deleteIfExists(confPath)) {
                                Files.deleteIfExists(confPath.getParent());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}
