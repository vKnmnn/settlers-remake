package jsettlers.logic.map.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESoldierClass;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.military.occupying.OccupyingBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.loading.data.objects.BuildingMapDataObject;
import jsettlers.logic.map.loading.original.OriginalMapFileContent;
import jsettlers.logic.map.loading.original.data.EOriginalMapBuildingType;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import jsettlers.testutils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class OriginalGarrisonTest {
    private final List<MainGrid> grids = new ArrayList<>();

    @Before
    public void initializeClock() {
        MatchConstants.init(new NetworkTimer(true), 0L);
    }

    @After
    public void tearDown() {
        grids.forEach(MainGrid::stopThreads);
        JSettlersGame.clearState();
    }

    @Test
    public void originalCastleRetainsSoldierTypesAndLevelsAcrossCivilisations() {
        for (ECivilisation civilisation : ECivilisation.values()) {
            OriginalMapFileContent content = new OriginalMapFileContent(64);
            // The Roman remake castle has five infantry slots; the other three have six.
            int sword2 = civilisation == ECivilisation.ROMAN ? 0 : 1;
            content.setBuilding(32, 32, EOriginalMapBuildingType.CASTLE.ordinal(), 0,
                    1, sword2, 1, 1, 1, 1, 1, 1, 1);
            OccupyingBuilding castle = load(content, civilisation);
            List<EMovableType> expected = new ArrayList<>(EMovableType.SOLDIERS);
            if (sword2 == 0) expected.remove(EMovableType.SWORDSMAN_L2);
            assertEquals(civilisation.toString(), counts(expected), counts(castle.getOccupiers().stream()
                    .map(occupier -> occupier.getMovable().getMovableType()).collect(Collectors.toList())));
            assertEquals(0, castle.getSearchedSoldiers(ESoldierClass.INFANTRY));
            assertEquals(0, castle.getComingSoldiers(ESoldierClass.INFANTRY));
            assertEquals(expected.size(), castle.getOccupiers().stream().map(occupier -> occupier.getPlace()).distinct().count());
            // Immediate tower placement must not leave a soldier blocking its door.
            MainGrid grid = grids.get(grids.size() - 1);
            assertNull(grid.movablePathfinderGrid.getMovableAt(castle.getDoor().x, castle.getDoor().y));
        }
    }

    @Test
    public void repeatedArchersAndInfantrySurviveSavingAndLoading() throws Exception {
        OriginalMapFileContent content = new OriginalMapFileContent(64);
        content.setBuilding(32, 32, EOriginalMapBuildingType.TOWER_BIG.ordinal(), 0,
                0, 2, 0, 0, 0, 3, 0, 1, 0);
        OccupyingBuilding tower = load(content, ECivilisation.ROMAN);
        Map<EMovableType, Long> expected = Map.of(EMovableType.SWORDSMAN_L2, 2L,
                EMovableType.BOWMAN_L3, 3L, EMovableType.PIKEMAN_L2, 1L);
        assertEquals(expected, counts(tower.getOccupiers().stream()
                .map(occupier -> occupier.getMovable().getMovableType()).collect(Collectors.toList())));
        MainGrid restored = TestUtils.serializeAndDeserialize(grids.get(0));
        grids.add(restored);
        OccupyingBuilding loaded = (OccupyingBuilding) restored.getObjectsGrid().getBuildingAt(32, 32);
        assertEquals(expected, counts(loaded.getOccupiers().stream()
                .map(occupier -> occupier.getMovable().getMovableType()).collect(Collectors.toList())));
        loaded.getOccupiers().forEach(occupier -> ((jsettlers.logic.movable.Movable) occupier.getMovable()).timerEvent());
        assertEquals(6, loaded.getOccupiers().size());
    }

    @Test
    public void explicitEmptyOriginalTowerDoesNotInventOrRequestSoldiers() {
        OriginalMapFileContent content = new OriginalMapFileContent(64);
        content.setBuilding(32, 32, EOriginalMapBuildingType.TOWER.ordinal(), 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0);
        OccupyingBuilding tower = load(content, ECivilisation.ROMAN);
        assertTrue(tower.getOccupiers().isEmpty());
        assertEquals(0, tower.getSearchedSoldiers(ESoldierClass.INFANTRY));
        assertEquals(0, tower.getSearchedSoldiers(ESoldierClass.BOWMAN));
    }

    @Test
    public void nativeMapWithoutGarrisonRetainsDefaultSwordsman() {
        OriginalMapFileContent content = new OriginalMapFileContent(64);
        content.setMapObject(32, 32, new BuildingMapDataObject(EBuildingType.TOWER, (byte) 0));
        OccupyingBuilding tower = load(content, ECivilisation.ROMAN);
        assertEquals(1, tower.getOccupiers().size());
        assertEquals(EMovableType.SWORDSMAN_L1, tower.getOccupiers().get(0).getMovable().getMovableType());
    }

    @Test
    public void excessInfantryIsRejectedBeforeAnySoldiersAreCreated() {
        MainGrid grid = new MainGrid("garrison", "Garrison", (short) 64, (short) 64,
                new PlayerSetting[] { new PlayerSetting((byte) 0) });
        grids.add(grid);
        OccupyingBuilding tower = (OccupyingBuilding) grid.constructBuildingAt(new ShortPoint2D(32, 32),
                EBuildingType.TOWER, grid.getPartitionsGrid().getPlayer((byte) 0), true);
        try {
            tower.initializeGarrison(List.of(EMovableType.SWORDSMAN_L1, EMovableType.PIKEMAN_L3));
            fail("Two infantry do not fit a small tower");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("capacity"));
        }
        assertTrue(tower.getOccupiers().isEmpty());
        assertEquals(0, tower.getComingSoldiers(ESoldierClass.INFANTRY));
        assertEquals(1, tower.getSearchedSoldiers(ESoldierClass.INFANTRY));
    }

    private OccupyingBuilding load(OriginalMapFileContent content, ECivilisation civilisation) {
        // Real map files have a blocking water border for partition traversal.
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                if (x < 4 || y < 4 || x >= 60 || y >= 60) content.setLandscape(y * 64 + x, 0);
            }
        }
        MainGrid grid = new MainGrid("garrison", "Garrison", content,
                new PlayerSetting[] { new PlayerSetting(null, civilisation, (byte) 0) });
        grids.add(grid);
        return (OccupyingBuilding) grid.getObjectsGrid().getBuildingAt(32, 32);
    }

    private Map<EMovableType, Long> counts(List<EMovableType> types) {
        return types.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
}
