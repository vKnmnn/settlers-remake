package jsettlers.logic.player;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectStreamClass;
import org.junit.Test;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.player.ECivilisation;
import jsettlers.logic.map.loading.EMapStartResources;

public class InitialGameStateRulesTest {
	@Test public void readsLegacyVersionOneWithoutConsumingFollowingReplayData() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		out.writeByte(1);
		out.writeLong(12345L);
		out.writeByte(0);
		out.writeByte(2); // old HIGH_GOODS ordinal
		out.writeInt(1);
		new PlayerSetting((byte) 0).writeTo(out);
		out.writeInt(0x12345678);
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
		InitialGameState restored = new InitialGameState(in);
		assertEquals(MatchRules.DEFAULT, restored.getMatchRules());
		assertEquals(12345L, restored.getRandomSeed());
		assertEquals(EMapStartResources.HIGH_GOODS, restored.getStartResources());
		assertEquals(0x12345678, in.readInt());
	}

	@Test public void versionTwoAndReplayClonePreserveRulesAndMapDefault() throws Exception {
		MatchRules rules = new MatchRules(MatchRules.Mode.FREEPLAY);
		InitialGameState state = new InitialGameState((byte) 0,
			new PlayerSetting[] { new PlayerSetting(EPlayerType.AI_HARD, ECivilisation.AMAZON, (byte) 0) },
			987L, EMapStartResources.MAP_DEFAULT, rules);
		InitialGameState restored = read(write(state));
		assertEquals(rules, restored.getMatchRules());
		assertEquals(EMapStartResources.MAP_DEFAULT, restored.getStartResources());
		assertEquals(987L, restored.getRandomSeed());
		assertEquals(ECivilisation.AMAZON, restored.getPlayerSettings()[0].getCivilisation());
		InitialGameState replay = restored.clone();
		assertEquals(rules, replay.getMatchRules());
		assertEquals(EPlayerType.AI_HARD, replay.getPlayerSettings()[0].getPlayerType());
		org.junit.Assert.assertFalse(replay.getPlayerSettings()[0].shouldRunAi());
		org.junit.Assert.assertTrue(restored.getPlayerSettings()[0].shouldRunAi());
		assertEquals(EMapStartResources.MAP_DEFAULT, replay.getStartResources());
	}

	@Test public void constructorsAndJavaSerializationKeepLegacyDefaults() {
		assertEquals(MatchRules.DEFAULT, new InitialGameState((byte) 0, new PlayerSetting[0], 1L).getMatchRules());
		assertEquals(4226604173451490234L, ObjectStreamClass.lookup(InitialGameState.class).getSerialVersionUID());
	}

	@Test(expected = IOException.class) public void rejectsUnknownSerializedMode() throws Exception {
		byte[] data = write(new InitialGameState((byte) 0, new PlayerSetting[0], 1L));
		data[data.length - 1] = (byte) 255;
		read(data);
	}


	private byte[] write(InitialGameState state) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		state.serialize(new DataOutputStream(bytes));
		return bytes.toByteArray();
	}
	private InitialGameState read(byte[] bytes) throws IOException {
		return new InitialGameState(new DataInputStream(new ByteArrayInputStream(bytes)));
	}
}
