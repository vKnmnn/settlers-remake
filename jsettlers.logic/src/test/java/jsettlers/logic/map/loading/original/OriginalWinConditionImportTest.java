package jsettlers.logic.map.loading.original;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import jsettlers.common.material.EMaterialType;
import jsettlers.logic.map.loading.MapLoadException;
import jsettlers.logic.map.loading.original.data.OriginalProduceGoodsWinCondition;
import static org.junit.Assert.*;

public class OriginalWinConditionImportTest {
    @Test
    public void readsFullAmountAndFollowingMaterialByte() throws Exception {
        byte[] conditions = new byte[119];
        conditions[109] = 1;
        // 300 gold, 200 gems, 200 mead: synthetic values confirmed against local map format.
        ByteBuffer.wrap(conditions).order(ByteOrder.LITTLE_ENDIAN).position(110)
                .putShort((short) 300).put((byte) 10)
                .putShort((short) 200).put((byte) 28)
                .putShort((short) 200).put((byte) 34);
        Set<OriginalProduceGoodsWinCondition> goals = goals(read(conditions));
        assertEquals(3, goals.size());
        assertEquals(java.util.Map.of(EMaterialType.GOLD, 300, EMaterialType.GEMS, 200, EMaterialType.MEAD, 200),
                goals.stream().collect(Collectors.toMap(OriginalProduceGoodsWinCondition::getType,
                        OriginalProduceGoodsWinCondition::getAmount)));
    }

    @Test(expected = MapLoadException.class)
    public void rejectsUnsupportedActiveMaterialGoal() throws Exception {
        byte[] conditions = new byte[119];
        conditions[109] = 1;
        conditions[110] = 1;
        conditions[112] = (byte) 255;
        read(conditions);
    }

    @Test
    public void ignoresUnusedMaterialSlots() throws Exception {
        byte[] conditions = new byte[119];
        conditions[109] = 1;
        conditions[112] = (byte) 255;
        assertTrue(goals(read(conditions)).isEmpty());
    }

    @Test(expected = MapLoadException.class)
    public void rejectsUnsupportedActiveBuildingGoal() throws Exception {
        byte[] conditions = buildingConditions();
        conditions[22] = 1;
        conditions[23] = 31; // unsupported catapult workshop
        read(conditions);
    }

    @Test
    public void ignoresUnusedBuildingSlots() throws Exception {
        read(buildingConditions());
    }

    @Test(expected = MapLoadException.class)
    public void rejectsTruncatedConditions() throws Exception {
        read(new byte[118]);
    }

    private byte[] buildingConditions() {
        byte[] conditions = new byte[119];
        conditions[21] = 1;
        for (int i = 0; i < 10; i++) conditions[22 + 2 * i] = (byte) 255;
        return conditions;
    }

    private OriginalSinglePlayerWinCondition read(byte[] clear) throws Exception {
        ByteBuffer file = ByteBuffer.allocate(8 + 8 + clear.length + 8).order(ByteOrder.LITTLE_ENDIAN);
        file.putInt(0).putInt(10); // checksum is not involved in this parser unit test
        file.putInt(10).putInt(clear.length + 8); // WIN_COND resource
        int key = 10;
        for (byte value : clear) {
            int plain = value & 255;
            file.put((byte) (plain ^ key));
            key = ((key << 1) ^ plain) & 255;
        }
        file.putInt(0).putInt(8); // EOF
        OriginalMapFileContentReader reader = new OriginalMapFileContentReader(new ByteArrayInputStream(file.array()));
        reader.loadMapResources();
        return reader.readWinCondition(null);
    }

    @SuppressWarnings("unchecked")
    private Set<OriginalProduceGoodsWinCondition> goals(OriginalSinglePlayerWinCondition condition) throws Exception {
        // Inspect the parsed data without coupling this format test to gameplay or defeat rules.
        Field goals = OriginalSinglePlayerWinCondition.class.getDeclaredField("produceToWin");
        goals.setAccessible(true);
        return (Set<OriginalProduceGoodsWinCondition>) goals.get(condition);
    }
}
