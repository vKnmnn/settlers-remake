package jsettlers.logic.map.loading.original;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Test;
import jsettlers.logic.map.loading.MapLoadException;
import static org.junit.Assert.*;

public class OriginalGarrisonModeTest {
    @Test
    public void defaultModeIgnoresPackedGarbageInBothVersions() throws Exception {
        for (int version : new int[] {10, 11}) {
            assertArrayEquals(new int[] {1, 0, 0, 0, 0, 0, 0, 0, 0}, read(version, 0, 1, 16));
        }
    }

    @Test
    public void explicitModePreservesAllNineNibblesInBothVersions() throws Exception {
        for (int version : new int[] {10, 11}) {
            assertArrayEquals(new int[] {1, 2, 5, 6, 3, 4, 8, 9, 7}, read(version, 1, 1, 16));
        }
    }

    @Test
    public void otherModesCreateNoSoldiers() throws Exception {
        assertArrayEquals(new int[9], read(10, 2, 1, 16));
    }

    @Test(expected = MapLoadException.class)
    public void rejectsRecordMissingItsLastByte() throws Exception {
        read(10, 1, 1, 15);
    }

    @Test(expected = MapLoadException.class)
    public void rejectsOverflowingRecordCount() throws Exception {
        read(10, 1, Integer.MAX_VALUE, 16);
    }

    private int[] read(int version, int mode, int count, int length) throws Exception {
        ByteBuffer record = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        record.putInt(count).put((byte) 0).put((byte) 18).putShort((short) 4).putShort((short) 5)
                .put((byte) mode).put((byte) 0x12).put((byte) 0x34).put((byte) 0x56).put((byte) 0x70).put((byte) 0x89);
        ByteBuffer file = ByteBuffer.allocate(24 + length).order(ByteOrder.LITTLE_ENDIAN);
        file.putInt(0).putInt(version).putInt(8).putInt(length + 8);
        int key = 8;
        for (int i = 0; i < length; i++) {
            int plain = record.array()[i] & 255;
            file.put((byte) (plain ^ key));
            key = ((key << 1) ^ plain) & 255;
        }
        file.putInt(0).putInt(8);
        OriginalMapFileContentReader reader = new OriginalMapFileContentReader(new ByteArrayInputStream(file.array()));
        final int[][] captured = new int[1][];
        reader.mapData = new OriginalMapFileContent(0) {
            @Override
            public void setBuilding(int x, int y, int type, int party, int s1, int s2, int s3,
                                    int a1, int a2, int a3, int p1, int p2, int p3) {
                assertEquals(4, x);
                assertEquals(5, y);
                captured[0] = new int[] {s1, s2, s3, a1, a2, a3, p1, p2, p3};
            }
        };
        reader.loadMapResources();
        reader.readBuildings();
        return captured[0];
    }
}
