package jsettlers.logic.map.loading.original;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Test;
import jsettlers.common.CommonConstants;
import jsettlers.logic.map.loading.EMapStartResources;
import jsettlers.logic.map.loading.list.IListedMap;

public class OriginalStartResourcesTest {
	@Test public void exposesStockAuthoredInOriginalMapInfo() throws Exception {
		boolean previous = CommonConstants.DISABLE_ORIGINAL_MAPS_CHECKSUM;
		try {
			// Synthetic resource sections exercise the normal loader; checksum testing is separate.
			CommonConstants.DISABLE_ORIGINAL_MAPS_CHECKSUM = true;
			for (int stock = 1; stock <= 3; stock++) {
				byte[] map = originalMap(stock);
				OriginalMapLoader loader = new OriginalMapLoader(new IListedMap() {
					@Override public String getFileName() { return "start-stock.map"; }
					@Override public InputStream getInputStream() { return new ByteArrayInputStream(map); }
					@Override public void delete() { throw new UnsupportedOperationException(); }
					@Override public boolean isCompressed() { return false; }
					@Override public File getFile() { throw new UnsupportedOperationException(); }
				});
				assertEquals(EMapStartResources.fromMapValue(stock), loader.getDefaultStartResources());
				assertEquals("Original scenarios retain authored victory conditions", stock != 1, loader.supportsCustomMatchRules());
			}
		} finally {
			CommonConstants.DISABLE_ORIGINAL_MAPS_CHECKSUM = previous;
		}
	}

	private byte[] originalMap(int stock) {
		ByteBuffer file = ByteBuffer.allocate(101).order(ByteOrder.LITTLE_ENDIAN);
		file.putInt(0).putInt(10);
		section(file, 1, ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putInt(stock == 1 ? 1 : 0).putInt(1).putInt(stock).array());
		section(file, 2, ByteBuffer.allocate(45).order(ByteOrder.LITTLE_ENDIAN).putInt(0).putInt(16).putInt(16).array());
		section(file, 6, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(32).array());
		file.putInt(0).putInt(8);
		return file.array();
	}

	private void section(ByteBuffer file, int type, byte[] data) {
		file.putInt(type).putInt(data.length + 8);
		int key = type;
		for (byte value : data) {
			int plain = value & 255;
			file.put((byte) (plain ^ key));
			key = ((key << 1) ^ plain) & 255;
		}
	}
}
