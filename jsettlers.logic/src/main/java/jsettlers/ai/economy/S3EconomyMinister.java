package jsettlers.ai.economy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import jsettlers.ai.highlevel.AiStatistics;
import jsettlers.ai.original.S3ConstructionPolicy;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.logic.player.Player;

import static jsettlers.common.buildings.EBuildingType.*;

/**
 * Experimental partial reconstruction. Building-industry requests replace the legacy build list;
 * the remaining economy, placement, expansion and housing still use JSettlers adapters.
 */
public final class S3EconomyMinister implements EconomyMinister {
    private static final Set<EBuildingType> INDUSTRY = EnumSet.of(LUMBERJACK, STONECUTTER, SAWMILL, FORESTER);
    private static final Set<EMovableType> CIVILIANS = civilians();
    private final AiStatistics statistics;
    private final Player player;
    private final BuildingListEconomyMinister remainingEconomy;
    private final List<EBuildingType> plan = new ArrayList<>();

    public S3EconomyMinister(AiStatistics statistics, Player player) {
        this.statistics = statistics;
        this.player = player;
        remainingEconomy = new BuildingListEconomyMinister(statistics, player, 1F, 1F, false);
    }

    @Override
    public void update() {
        remainingEconomy.update();
        plan.clear();
        int population = statistics.getCountOfMovablesOfPlayer(player, CIVILIANS);
        int lumberjacks = finished(LUMBERJACK);
        int wood = stock(EMaterialType.TRUNK) + stock(EMaterialType.PLANK);
        List<Request> requests = new ArrayList<>();
        requests.add(new Request(LUMBERJACK, S3ConstructionPolicy.lumberjackPriority(player.getCivilisation(), population, lumberjacks, wood)));
        requests.add(new Request(STONECUTTER, S3ConstructionPolicy.stonecutterPriority(player.getCivilisation(), population, finished(STONECUTTER), stock(EMaterialType.STONE))));
        requests.add(new Request(SAWMILL, S3ConstructionPolicy.sawmillPriority(lumberjacks, finished(SAWMILL))));
        requests.add(new Request(FORESTER, S3ConstructionPolicy.foresterPriority(player.getCivilisation(), lumberjacks, finished(FORESTER), wood)));
        // Stable sorting preserves original building-ID order on equal priorities (2, 3, 4, 5).
        requests.sort(Comparator.comparingInt((Request request) -> request.priority).reversed());
        for (Request request : requests) {
            // Original agents suppress requests for a type while construction is pending.
            int count = finished(request.type);
            if (request.priority > 0 && total(request.type) == count) {
                for (int i = 0; i <= count; i++) plan.add(request.type);
            }
        }
        for (EBuildingType type : remainingEconomy.getBuildingsToBuild()) {
            if (!INDUSTRY.contains(type)) plan.add(type);
        }
    }

    private int finished(EBuildingType type) { return statistics.getNumberOfBuildingTypeForPlayer(type, player.playerId); }
    private int total(EBuildingType type) { return statistics.getTotalNumberOfBuildingTypeForPlayer(type, player.playerId); }
    private int stock(EMaterialType type) { return statistics.getNumberOfMaterialTypeForPlayer(type, player.playerId); }

    @Override public List<EBuildingType> getBuildingsToBuild() { return plan; }
    @Override public int getNumberOfParallelConstructionSites() { return remainingEconomy.getNumberOfParallelConstructionSites(); }
    @Override public int getMidGameNumberOfStoneCutters() { return total(STONECUTTER); }
    @Override public boolean automaticLivingHousesEnabled() { return true; }
    @Override public boolean isEndGame() { return remainingEconomy.isEndGame(); }

    private static Set<EMovableType> civilians() {
        Set<EMovableType> types = EnumSet.allOf(EMovableType.class);
        types.removeAll(EMovableType.PLAYER_CONTROLLED_HUMAN_MOVABLE_TYPES);
        types.removeAll(EMovableType.SOLDIERS);
        types.removeAll(EMovableType.SHIPS);
        types.removeAll(EMovableType.SIEGE_WEAPONS);
        types.remove(EMovableType.DONKEY);
        types.remove(EMovableType.WHITEFLAGGED_DONKEY);
        return types;
    }

    private static final class Request {
        final EBuildingType type;
        final int priority;
        Request(EBuildingType type, int priority) { this.type = type; this.priority = priority; }
    }
}
