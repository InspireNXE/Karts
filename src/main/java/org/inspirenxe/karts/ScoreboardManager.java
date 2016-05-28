package org.inspirenxe.karts;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.scoreboard.Score;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.critieria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ScoreboardManager {
    private static final Map<UUID, Scoreboard> scoreboardByWorld = new HashMap<>();
    private static final Objective.Builder objectiveBuilder = Objective.builder()
            .criterion(Criteria.DUMMY)
            .displayName(Text.of("Leaderboards"))
            .name("Leaderboards");

    public boolean create(UUID uniqueId) {
        if (scoreboardByWorld.get(uniqueId) != null) {
            //return false;
        }

        final Scoreboard scoreboard = Scoreboard.builder().build();
        final Objective objective = objectiveBuilder.build();

        final Optional<World> optWorld = Sponge.getServer().getWorld(uniqueId);
        if (optWorld.isPresent()) {
            for (Player player : optWorld.get().getPlayers()) {
                objective.getOrCreateScore(Text.of(TextColors.GRAY, player.getName())).setScore(3);
            }

            scoreboard.addObjective(objective);

            scoreboard.updateDisplaySlot(objective, DisplaySlots.SIDEBAR);
            for (Player player : optWorld.get().getPlayers()) {
                player.setScoreboard(scoreboard);
            }
        }
        scoreboardByWorld.put(uniqueId, scoreboard);
        return true;
    }

    public void update(UUID uniqueId, Score score) {
        final Scoreboard scoreboard = scoreboardByWorld.get(uniqueId);
    }

    // TODO Boats do not fire collision?
    @Listener
    public void onCollideEntity(CollideEntityEvent event, @Root Boat boat) {
        final Set<Player> players = new HashSet<>(boat.getPassengers().stream().filter(entity -> entity instanceof Player).map(entity -> (Player) entity)
                .collect(Collectors.toList()));
        if (boat.getPassengers().isEmpty()) {
            return;
        }

        final Scoreboard scoreboard = scoreboardByWorld.get(boat.getWorld().getUniqueId());
        for (Player player : players) {
            final Text scoreText = Text.of(TextColors.GRAY, player.getName());

            if (scoreboard != null) {
                final Optional<Objective> optObjective = scoreboard.getObjective("Leaderboards");
                if (optObjective.isPresent()) {
                    final Optional<Score> optScore = optObjective.get().getScore(scoreText);
                    if (optScore.isPresent()) {
                        final int score = optScore.get().getScore();
                        optObjective.get().removeScore(scoreText);
                        if (score <= 1) {
                            return;
                        }
                        optObjective.get().getOrCreateScore(scoreText).setScore(score - 1);
                    }
                }
            }
        }
    }
}
