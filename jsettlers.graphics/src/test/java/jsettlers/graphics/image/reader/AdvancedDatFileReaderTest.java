package jsettlers.graphics.image.reader;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class AdvancedDatFileReaderTest {
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void acceptsEmptyAndPopulatedHeaderIndexesInBothColorFormats() throws Exception {
		for (DatFileType type : DatFileType.values()) {
			assertEquals(1, guiCount(archive(type, 0, 0), type));
			assertEquals(1, guiCount(archive(type, 2, 8), type));
		}
	}

	@Test
	public void rejectsUnknownHeaderIndexType() throws Exception {
		ByteBuffer bytes = archive(DatFileType.RGB555, 2, 8);
		bytes.putInt(84, 0x1905);
		assertEquals(0, guiCount(bytes, DatFileType.RGB555));
	}

	@Test
	public void rejectsInconsistentHeaderIndexLength() throws Exception {
		ByteBuffer bytes = archive(DatFileType.RGB555, 2, 8);
		bytes.putInt(88, 12);
		assertEquals(0, guiCount(bytes, DatFileType.RGB555));
	}

	@Test
	public void rejectsHeaderIndexBeyondFileEnd() throws Exception {
		ByteBuffer bytes = archive(DatFileType.RGB555, 2, 8);
		bytes.putInt(88, 12 + 100 * 8 * 4);
		bytes.putShort(92, (short) 100);
		assertEquals(0, guiCount(bytes, DatFileType.RGB555));
	}

	@Test
	public void rejectsOverflowingHeaderIndexSize() throws Exception {
		ByteBuffer bytes = archive(DatFileType.RGB555, 2, 8);
		bytes.putInt(88, (int) (12L + 4L * 65535 * 65535));
		bytes.putShort(92, (short) 65535);
		bytes.putShort(94, (short) 65535);
		assertEquals(0, guiCount(bytes, DatFileType.RGB555));
	}

	@Test
	public void rejectsTruncatedHeader() throws Exception {
		ByteBuffer bytes = ByteBuffer.allocate(90).order(ByteOrder.LITTLE_ENDIAN);
		bytes.put(archive(DatFileType.RGB555, 0, 0).array(), 0, 90);
		bytes.putInt(48, 90);
		assertEquals(0, guiCount(bytes, DatFileType.RGB555));
	}

	@Test
	public void rejectsExecutableInsteadOfGraphicsArchive() throws Exception {
		ByteBuffer bytes = archive(DatFileType.RGB555, 0, 0);
		bytes.put(0, (byte) 'M');
		bytes.put(1, (byte) 'Z');
		assertEquals(0, guiCount(bytes, DatFileType.RGB555));
	}

	private int guiCount(ByteBuffer bytes, DatFileType type) throws Exception {
		File file = temporaryFolder.newFile();
		Files.write(file.toPath(), bytes.array());
		return new AdvancedDatFileReader(file, type, "test").getGuis().length();
	}

	/** Creates an independent synthetic archive; no original game data is used. */
	private ByteBuffer archive(DatFileType type, int entries, int pointersPerEntry) {
		int headerIndexLength = 12 + entries * pointersPerEntry * 4;
		int sequenceStart = 84 + headerIndexLength;
		ByteBuffer bytes = ByteBuffer.allocate(sequenceStart + 56).order(ByteOrder.LITTLE_ENDIAN);
		bytes.putInt(0x41304).putInt(12).putInt(0).putInt(84);
		bytes.putInt(32).putInt(64).putInt(0).putInt(16).put((byte) 0);
		bytes.put(type.getFileStartMagic());
		bytes.put(new byte[]{0, 0, 31, 0, 0, 0, 0, 0, 0, 0});
		bytes.putInt(bytes.capacity()).putInt(84);
		for (int i = 0; i < 6; i++) {
			bytes.putInt(sequenceStart + i * 8);
		}
		bytes.putInt(0);
		bytes.putInt(0x1904).putInt(headerIndexLength);
		bytes.putShort((short) entries).putShort((short) pointersPerEntry);
		for (int i = 0; i < entries * pointersPerEntry; i++) {
			bytes.putInt(sequenceStart + 52);
		}
		int[] sequenceTypes = {0x2412, 0x106, 0x3112, 0x5982, 0x21702, 0x11306};
		for (int i = 0; i < sequenceTypes.length; i++) {
			boolean gui = i == sequenceTypes.length - 1;
			bytes.putInt(sequenceTypes[i]).putShort((short) (gui ? 12 : 8)).putShort((short) (gui ? 1 : 0));
		}
		// Retaining this GUI entry proves that parsing reached the sequence lists.
		bytes.putInt(sequenceStart + 52).putInt(0);
		return bytes;
	}
}
