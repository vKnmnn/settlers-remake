package jsettlers.logic.map.loading.list;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import jsettlers.logic.map.loading.newmap.MapFileHeader;
import org.junit.Test;
import static org.junit.Assert.*;

public class SaveMapResourceTest {
    @Test public void headerWriteFailureClosesUnderlyingFile() { assertClosedOnWriteFailure(false); }
    @Test public void objectStreamInitializationFailureClosesUnderlyingFile() { assertClosedOnWriteFailure(true); }

    private void assertClosedOnWriteFailure(boolean skipHeader) {
        boolean[] closed = {false};
        OutputStream failing = new OutputStream() {
            @Override public void write(int value) throws IOException { throw new IOException("disk full"); }
            @Override public void close() { closed[0] = true; }
        };
        IMapLister directory = new IMapLister() {
            @Override public void listMaps(IMapListerCallable callable) { fail("Failed save must not refresh map list"); }
            @Override public OutputStream getOutputStream(MapFileHeader header) { return failing; }
        };
        MapFileHeader header = new MapFileHeader(MapFileHeader.MapType.SAVED_SINGLE, "test", "base", "",
                (short) 64, (short) 64, (short) 1, (short) 1, new Date(0), new short[MapFileHeader.PREVIEW_IMAGE_SIZE * MapFileHeader.PREVIEW_IMAGE_SIZE]) {
            @Override public void writeTo(OutputStream stream) throws IOException {
                if (!skipHeader) stream.write(1);
            }
        };
        try {
            new MapList(List.of(directory), directory).saveMap(null, header, null);
            fail("Write failure must propagate");
        } catch (IOException expected) {
            assertEquals("disk full", expected.getMessage());
        }
        assertTrue("The save file must be closed even when its header cannot be written", closed[0]);
    }
}
