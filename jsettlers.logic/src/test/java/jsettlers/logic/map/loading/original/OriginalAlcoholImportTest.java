package jsettlers.logic.map.loading.original;

import jsettlers.common.material.EMaterialType;
import jsettlers.logic.map.loading.data.objects.StackMapDataObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class OriginalAlcoholImportTest {
    @Test
    public void preservesRiceLiquorAndMeadStacks() {
        OriginalMapFileContent map = new OriginalMapFileContent(8);
        // Original-format IDs, independent of the Java enum's ordinal lookup.
        map.setStack(2, 3, 33, 7);
        map.setStack(4, 5, 34, 5);
        assertStack(map, 2, 3, EMaterialType.LIQUOR, 7);
        assertStack(map, 4, 5, EMaterialType.MEAD, 5);
    }

    private void assertStack(OriginalMapFileContent map, int x, int y, EMaterialType type, int count) {
        assertTrue(map.getMapObject(x, y) instanceof StackMapDataObject);
        StackMapDataObject stack = (StackMapDataObject) map.getMapObject(x, y);
        assertEquals(type, stack.getType());
        assertEquals(count, stack.getCount());
    }
}
