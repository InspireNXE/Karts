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
package org.inspirenxe.karts;

import static org.spongepowered.api.command.args.GenericArguments.catalogedElement;
import static org.spongepowered.api.command.args.GenericArguments.doubleNum;
import static org.spongepowered.api.command.args.GenericArguments.onlyOne;
import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.playerOrSource;
import static org.spongepowered.api.command.args.GenericArguments.seq;
import static org.spongepowered.api.command.args.GenericArguments.string;
import static org.spongepowered.api.command.args.GenericArguments.world;

import com.flowpowered.math.vector.Vector3i;
import org.inspirenxe.karts.track.TrackManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldArchetype;
import org.spongepowered.api.world.WorldArchetypes;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.IOException;
import java.util.Optional;

@Plugin(id = Karts.PLUGIN_ID)
public class Karts {
    public static final String PLUGIN_ID = "karts";
    public static Karts instance;

    private TrackManager trackManager;

    @Listener
    public void onGameConstruction(GameConstructionEvent event) {
        instance = this;
    }

    @Listener
    public void onGamePreInitialization(GamePreInitializationEvent event) {
        trackManager = new TrackManager();

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Track management"))
                .permission(Karts.PLUGIN_ID + ".command.track")
                .child(CommandSpec.builder()
                        .description(Text.of("Generates a new track"))
                        .permission(Karts.PLUGIN_ID + ".command.track.generate")
                        .arguments(seq(catalogedElement(Text.of("track"), WorldArchetype.class), string(Text.of("instance")), optional
                                (doubleNum(Text.of("radiusX")), 50.5), optional(doubleNum(Text.of("radiusZ")), 50.5)))
                        .executor((src, args) -> {
                            final WorldArchetype arena = args.<WorldArchetype>getOne("track").get();
                            final String instanceName = args.<String>getOne("instance").get();
                            try {
                                final World world = trackManager.createTrack(instanceName, arena);
                                world.getProperties().setSpawnPosition(new Vector3i(0, 65, 0));
                                final DataContainer settings = world.getProperties().getGeneratorSettings();
                            } catch (IOException e) {
                                throw new CommandException(Text.of(e));
                            }
                            return CommandResult.success();
                        })
                        .build(), "generate", "gen")
                .child(CommandSpec.builder()
                        .description(Text.of("Deletes a track"))
                        .permission(Karts.PLUGIN_ID + ":command.track.delete")
                        .arguments(world(Text.of("track")))
                        .executor((src, args) -> {
                            final WorldProperties track = args.<WorldProperties>getOne("track").orElse(null);
                            trackManager.cleanupInstance(track);
                            return CommandResult.success();
                        }).build(), "delete", "del")
                .child(CommandSpec.builder()
                        .arguments(optional(world(Text.of("world"))))
                        .executor((src, args) -> {
                            final Optional<WorldProperties> optProperties = args.getOne("world");
                            final World world;
                            if (optProperties.isPresent()) {
                                world = Sponge.getServer().getWorld(optProperties.get().getWorldName()).orElseThrow(() -> new
                                        CommandException(Text.of("World provided is not online!")));
                            } else if (src instanceof Player){
                                world = ((Player) src).getWorld();
                            } else {
                                throw new CommandException(Text.of("World was not provided!"));
                            }

                            if (!this.trackManager.startRace(world)) {
                                throw new CommandException(Text.of("Failed to start race!"));
                            }

                            return CommandResult.success();
                        }).build(), "start")
                .child(CommandSpec.builder()
                        .arguments(optional(world(Text.of("world"))))
                        .executor((src, args) -> {
                            final Optional<WorldProperties> optProperties = args.getOne("world");
                            final World world;
                            if (optProperties.isPresent()) {
                                world = Sponge.getServer().getWorld(optProperties.get().getWorldName()).orElseThrow(() -> new
                                        CommandException(Text.of("World provided is not online!")));
                            } else if (src instanceof Player){
                                world = ((Player) src).getWorld();
                            } else {
                                throw new CommandException(Text.of("World was not provided!"));
                            }

                            if (!this.trackManager.endRace(world)) {
                                throw new CommandException(Text.of("Failed to end race!"));
                            }

                            return CommandResult.success();
                        }).build(), "end")
                .child(CommandSpec.builder()
                        .arguments(world(Text.of("world")))
                        .executor((src, args) -> {
                            if (!(src instanceof Player)) {
                                throw new CommandException(Text.of("Console cannot join a race!"));
                            }
                            final Player player = (Player) src;
                            final Optional<WorldProperties> optProperties = args.getOne("world");
                            final World world;
                            if (optProperties.isPresent()) {
                                world = Sponge.getServer().getWorld(optProperties.get().getWorldName()).orElseThrow(() -> new
                                        CommandException(Text.of("World provided is not online!")));
                            } else {
                                throw new CommandException(Text.of("World was not provided!"));
                            }

                            player.transferToWorld(world);
                            player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);
                            player.offer(Keys.INVISIBLE, true);
                            player.offer(Keys.INVISIBILITY_IGNORES_COLLISION, true);
                            player.offer(Keys.INVISIBILITY_PREVENTS_TARGETING, true);

                            return CommandResult.success();
                        }).build(), "join")
                .build(), "track");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                        .description(Text.of("Teleports a player to another world"))
                        .arguments(seq(playerOrSource(Text.of("target")), onlyOne(world(Text.of("world")))))
                        .permission(PLUGIN_ID + ".command.tpworld")
                        .executor((src, args) -> {
                            final Optional<WorldProperties> optWorldProperties = args.getOne("world");
                            final Optional<World> optWorld = Sponge.getServer().getWorld(optWorldProperties.get().getWorldName());
                            if (!optWorld.isPresent()) {
                                throw new CommandException(Text.of("World [", Text.of(TextColors.AQUA, optWorldProperties.get().getWorldName()),
                                        "] was not found."));
                            }
                            for (Player target : args.<Player>getAll("target")) {
                                target.setLocation(new Location<>(optWorld.get(), optWorld.get().getProperties()
                                        .getSpawnPosition()));
                            }
                            return CommandResult.success();
                        })
                        .build()
                , "tpworld");
    }

    @Listener
    public void onGameStartingServer(GameStartingServerEvent event) throws IOException {
        final WorldProperties properties = Sponge.getServer().createWorldProperties(PLUGIN_ID + "-lobby", WorldArchetypes.THE_VOID);
        Sponge.getServer().loadWorld(properties).orElseThrow(() -> new IOException("Failed to create a lobby world!"));
    }

    @Listener
    public void onSpawnEntity(SpawnEntityEvent event) {
        if (trackManager.isTrackInstance(event.getTargetWorld())) {
            event.filterEntities(e -> !(e instanceof Boat));
        }
    }

    @Listener
    public void onInteractInventoryClose(InteractInventoryEvent.Close event) {
        event.setCancelled(true);

    }
}
