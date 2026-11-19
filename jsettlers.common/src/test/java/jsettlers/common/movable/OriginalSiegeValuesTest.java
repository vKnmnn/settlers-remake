package jsettlers.common.movable;

import jsettlers.common.material.EMaterialType;
import org.junit.Test;
import static org.junit.Assert.*;

public class OriginalSiegeValuesTest {
    @Test public void originalRangesRecoveryHealthAndMovementArePreserved() {
        assertEquals(5120, ESiegeWeaponType.MATERIAL_WORK_DURATION_MS);
        int[] exclusiveRange = {28, 32, 36, 17};
        int[] recoveryTicks = {24, 15, 18, 48};
        for (int i = 0; i < ESiegeWeaponType.VALUES.length; i++) {
            ESiegeWeaponType type = ESiegeWeaponType.VALUES[i];
            assertEquals(exclusiveRange[i] - 1, type.maxRange);
            assertEquals(recoveryTicks[i] * 64, type.cooldownMs);
            assertEquals(type.cooldownMs, type.fireDurationMs);
            assertEquals(250, type.movableType.health, 0);
            assertEquals(768, type.movableType.getStepDurationMs());
        }
    }

    @Test public void originalCostsIncludeInitialAmmunitionAndGongUsesIron() {
        assertCosts(ESiegeWeaponType.CATAPULT, 6, 3, EMaterialType.STONE, 6);
        assertCosts(ESiegeWeaponType.BALLISTA, 5, 6, EMaterialType.TRUNK, 6);
        assertCosts(ESiegeWeaponType.CANNON, 3, 8, EMaterialType.GUN_POWDER, 6);
        assertCosts(ESiegeWeaponType.GONG, 8, 4, EMaterialType.GOLD, 6);
        assertEquals(3, ESiegeWeaponType.BALLISTA.getAmmoBatchInputCount(EMaterialType.IRON));
        assertEquals(6, ESiegeWeaponType.BALLISTA.getAmmoBatchInputCount(EMaterialType.TRUNK));
        assertEquals(0, ESiegeWeaponType.BALLISTA.getAmmoBatchInputCount(EMaterialType.PLANK));
    }

    private void assertCosts(ESiegeWeaponType type, int planks, int iron, EMaterialType third, int amount) {
        assertEquals(planks, type.getWeaponCost(EMaterialType.PLANK));
        assertEquals(iron, type.getWeaponCost(EMaterialType.IRON));
        assertEquals(amount, type.getWeaponCost(third));
    }
}
