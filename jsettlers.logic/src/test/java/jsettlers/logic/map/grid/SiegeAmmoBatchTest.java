package jsettlers.logic.map.grid;

import jsettlers.common.ai.EPlayerType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.ESiegeWeaponType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.workers.SiegeWorkshopBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import jsettlers.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class SiegeAmmoBatchTest {
    @Test public void allOriginalRecipesRequireTheirFullInputsBeforeYieldingSixShots() {
        for (ESiegeWeaponType type : ESiegeWeaponType.VALUES) {
            if (!type.usesAmmo()) continue;
            MainGrid grid = createGrid(type);
            try {
                SiegeWorkshopBuilding workshop = workshop(grid, type);
                assertEquals(0, workshop.storeAmmoBatch());
                for (EMaterialType input : type.getAmmoInputs()) {
                    for (int i = 0; i < type.getAmmoBatchInputCount(input); i++) {
                        assertEquals("unfinished recipe must not create shots", 0, workshop.storeAmmoBatch());
                        workshop.ammoMaterialTaken(input);
                    }
                }
                assertEquals(type.toString(), 6, workshop.storeAmmoBatch());
                assertEquals(6, workshop.getAmmoStackSize());
                assertEquals("a paid batch can only be delivered once", 0, workshop.storeAmmoBatch());
            } finally { grid.stopThreads(); JSettlersGame.clearState(); }
        }
    }

    @Test public void unfinishedBatchSurvivesSaveAndDoesNotRepeatPartiallyDeliveredShots() throws Exception {
        MainGrid grid = createGrid(ESiegeWeaponType.BALLISTA);
        MainGrid loaded = null;
        try {
            SiegeWorkshopBuilding workshop = workshop(grid, ESiegeWeaponType.BALLISTA);
            for (int i = 0; i < 6; i++) workshop.ammoMaterialTaken(EMaterialType.TRUNK);
            workshop.ammoMaterialTaken(EMaterialType.IRON);
            loaded = TestUtils.serializeAndDeserialize(grid);
            SiegeWorkshopBuilding restored = (SiegeWorkshopBuilding) loaded.getObjectsGrid().getBuildingAt(32, 32);
            assertEquals(0, restored.storeAmmoBatch());
            restored.ammoMaterialTaken(EMaterialType.IRON);
            restored.ammoMaterialTaken(EMaterialType.IRON);
            // Existing stock leaves room for only part of this already-paid batch.
            assertTrue(restored.storeAmmo());
            assertTrue(restored.storeAmmo());
            assertEquals(4, restored.storeAmmoBatch());
            assertEquals(0, restored.storeAmmoBatch());
            for (int i = 0; i < 6; i++) loaded.movablePathfinderGrid.takeMaterial(restored.getAmmoStackPosition(), EMaterialType.BALLISTA_AMMO);
            assertEquals(2, restored.storeAmmoBatch());
            assertEquals(0, restored.storeAmmoBatch());
        } finally {
            if (loaded != null) loaded.stopThreads();
            grid.stopThreads(); JSettlersGame.clearState();
        }
    }

    private MainGrid createGrid(ESiegeWeaponType type) {
        MatchConstants.init(new NetworkTimer(true), 0L);
        return new MainGrid("ammo-batch", "Ammo batch", (short) 64, (short) 64,
                new PlayerSetting[] { new PlayerSetting(EPlayerType.HUMAN, type.civilisation, (byte) 0) });
    }

    private SiegeWorkshopBuilding workshop(MainGrid grid, ESiegeWeaponType type) {
        return (SiegeWorkshopBuilding) grid.constructBuildingAt(new ShortPoint2D(32, 32), type.workshop,
                grid.getPartitionsGrid().getPlayer((byte) 0), true);
    }
}
