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

import org.inspirenxe.karts.Karts;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldArchetype;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class TrackManager {
    private static final ArrayList<Title> trackStartTitles = new ArrayList<>(4);
    private final List<UUID> instances = new ArrayList<UUID>();
    private final Map<UUID, UUID> taskByWorld = new HashMap<>();

    static {
        final Title.Builder builder = Title.builder()
                .stay(12)
                .fadeIn(0)
                .fadeOut(8);
        trackStartTitles.add(builder.title(Text.of(TextColors.DARK_RED, "3")).build());
        trackStartTitles.add(builder.title(Text.of(TextColors.RED, "2")).build());
        trackStartTitles.add(builder.title(Text.of(TextColors.GOLD, "1")).build());
        trackStartTitles.add(builder.title(Text.of(TextColors.GREEN, "Go!")).build());
    }

    public TrackManager() {
        Tracks.fakeInit();
    }

    public World createTrack(String trackInstanceName, WorldArchetype trackArchetype) throws IOException {
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

    public boolean startRace(World world) {
        //if (!this.trackManager.isTrackInstance(world)) {
        //    throw new CommandException(Text.of("World is not a track!"));
        //}
        taskByWorld.put(
                world.getUniqueId(),
                Task.builder()
                        .execute(new RaceCountdown(world))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Karts.PLUGIN_ID + " - Countdown - " + world.getName())
                        .submit(Karts.instance)
                        .getUniqueId()
        );
        return true;
    }

    private final class RaceCountdown implements Runnable {
        WeakReference<World> worldRef;
        int seconds = 0;

        public RaceCountdown(World world) {
            this.worldRef = new WeakReference<>(world);
        }

        @Override
        public void run() {
            final World world = worldRef.get();

            // Make sure the world is still around and loaded
            if (world != null && world.isLoaded()) {
                for (Player player : world.getPlayers()) {
                    // Make sure a player ref isn't still here
                    if (player.isOnline()) {
                        switch (seconds) {
                            case 0:
                                player.sendTitle(trackStartTitles.get(0));
                                break;
                            case 1:
                                player.sendTitle(trackStartTitles.get(1));
                                break;
                            case 2:
                                player.sendTitle(trackStartTitles.get(2));
                                break;
                            case 3:
                                player.sendTitle(trackStartTitles.get(3));
                                break;
                            default:
                                break;
                        }
                    }
                }

                seconds++;
                if (seconds > 3) {
                    final UUID taskUniqueId = taskByWorld.get(world.getUniqueId());
                    if (taskUniqueId != null) {
                        Sponge.getScheduler().getTaskById(taskUniqueId).ifPresent(Task::cancel);
                    }
                }
            }
        }
    }
}
