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
import org.inspirenxe.karts.track.modifier.Modifiers;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.critieria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.scoreboard.objective.displaymode.ObjectiveDisplayModes;
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
    private final List<UUID> instances = new ArrayList<UUID>();
    // World Id -> Start Task Id
    private final Map<UUID, UUID> startTaskByWorld = new HashMap<>();
    // World Id -> End Task Id
    private final Map<UUID, UUID> endTaskByWorld = new HashMap<>();

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
//        for (UUID uniqueId : instances) {
//            if (world.getUniqueId().equals(uniqueId)) {
//                return true;
//            }
//        }
        return world.getProperties().getGeneratorModifiers().contains(Modifiers.Arenas.ICE);
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
        startTaskByWorld.put(
                world.getUniqueId(),
                Task.builder()
                        .execute(new RaceCountdown.Start(this, world))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Karts.PLUGIN_ID + " - Start Countdown - " + world.getName())
                        .submit(Karts.instance)
                        .getUniqueId()
        );
        return true;
    }

    public boolean endRace(World world) {
        //if (!this.trackManager.isTrackInstance(world)) {
        //    throw new CommandException(Text.of("World is not a track!"));
        //}
        endTaskByWorld.put(
                world.getUniqueId(),
                Task.builder()
                        .execute(new RaceCountdown.End(this, world))
                        .intervalTicks(1)
                        .name(Karts.PLUGIN_ID + " - End Countdown - " + world.getName())
                        .submit(Karts.instance)
                        .getUniqueId()
        );
        return true;
    }

    public Optional<UUID> getStartTaskUniqueIdFor(World world) {
        return Optional.ofNullable(this.startTaskByWorld.get(world.getUniqueId()));
    }

    public Optional<UUID> getEndTaskUniqueIdFor(World world) {
        return Optional.ofNullable(this.endTaskByWorld.get(world.getUniqueId()));
    }
}

abstract class RaceCountdown implements Runnable {

    final TrackManager trackManager;
    final WeakReference<World> worldRef;

    public RaceCountdown(TrackManager trackManager, World world) {
        this.trackManager = trackManager;
        this.worldRef = new WeakReference<>(world);
    }

    public static final class Start extends RaceCountdown {

        private static final ArrayList<Title> trackTitles = new ArrayList<>(4);

        static {
            final Title.Builder builder = Title.builder()
                    .stay(12)
                    .fadeIn(0)
                    .fadeOut(8);
            trackTitles.add(builder.title(Text.of(TextColors.DARK_RED, "3")).build());
            trackTitles.add(builder.title(Text.of(TextColors.RED, "2")).build());
            trackTitles.add(builder.title(Text.of(TextColors.GOLD, "1")).build());
            trackTitles.add(builder.title(Text.of(TextColors.GREEN, "Go!")).build());
        }

        int seconds = 0;

        public Start(TrackManager trackManager, World world) {
            super(trackManager, world);
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
                                player.sendTitle(trackTitles.get(0));
                                break;
                            case 1:
                                player.sendTitle(trackTitles.get(1));
                                break;
                            case 2:
                                player.sendTitle(trackTitles.get(2));
                                break;
                            case 3:
                                player.sendTitle(trackTitles.get(3));
                                break;
                        }
                    }
                }

                seconds++;
                if (seconds > 3) {
                    final UUID taskUniqueId = this.trackManager.getStartTaskUniqueIdFor(world).orElseThrow(() -> new RuntimeException("Task is "
                            + "executing when track has no knowledge of it!"));
                    if (taskUniqueId != null) {
                        Sponge.getScheduler().getTaskById(taskUniqueId).ifPresent(Task::cancel);
                    }
                }
            }
        }
    }

    public static final class End extends RaceCountdown {

        final ServerBossBar bossBar = ServerBossBar.builder()
                .color(BossBarColors.GREEN)
                .playEndBossMusic(false)
                .visible(true)
                .overlay(BossBarOverlays.PROGRESS)
                .name(Text.of("Game ending soon!"))
                .build();
        final int maxTicks = 300;
        int ticks = 300;

        public End(TrackManager trackManager, World world) {
            super(trackManager, world);
        }

        @Override
        public void run() {
            final World world = worldRef.get();

            // Make sure the world is still around and loaded
            if (world != null && world.isLoaded()) {
                bossBar.setPercent((float) ticks * 1f / maxTicks);
                switch (ticks) {
                    case 200:
                        bossBar.setColor(BossBarColors.YELLOW);
                        break;
                    case 100:
                        bossBar.setColor(BossBarColors.RED);
                        break;
                }
                // Make sure a player ref isn't still here
                world.getPlayers().stream().filter(player -> player.isOnline() && !bossBar.getPlayers().contains(player)).forEach(bossBar::addPlayer);

                ticks--;
                if (ticks < 0) {
                    final UUID taskUniqueId = this.trackManager.getEndTaskUniqueIdFor(world).orElseThrow(() -> new RuntimeException("Task is "
                            + "executing when track has no knowledge of it!"));
                    if (taskUniqueId != null) {
                        bossBar.getPlayers().forEach(bossBar::removePlayer);
                        Sponge.getScheduler().getTaskById(taskUniqueId).ifPresent(Task::cancel);
                    }
                }
            }
        }
    }
}