package jsettlers.logic.map.loading.original;

import org.junit.Test;
import jsettlers.common.movable.EMovableType;
import jsettlers.logic.map.loading.data.objects.MovableObject;
import static org.junit.Assert.*;

public class OriginalSiegeImportTest {
    @Test
    public void resolvesSharedSiegeTypeByAuthoredNation() {
        OriginalMapFileContent map = new OriginalMapFileContent(32);
        map.setPlayerCount(4);
        EMovableType[] expected = { EMovableType.CATAPULT, EMovableType.BALLISTA,
                EMovableType.CANNON, EMovableType.GONG };
        for (int nation = 0; nation < expected.length; nation++) {
            map.setPlayer(nation, 1, 1, nation, "Player");
            map.setSettler(4 + nation, 4, 41, nation);
            MovableObject imported = (MovableObject) map.getMapObject(4 + nation, 4);
            assertEquals(expected[nation], imported.getType());
            assertEquals(nation, imported.getPlayerId());
        }
    }

    @Test
    public void footprintHelpersDoNotBecomeIndependentSettlers() {
        OriginalMapFileContent map = new OriginalMapFileContent(32);
        map.setPlayerCount(1);
        map.setSettler(4, 4, 42, 0);
        assertNull(map.getMapObject(4, 4));
    }
}
