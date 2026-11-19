package jsettlers.logic.player;

import jsettlers.common.ai.EPlayerType;
import jsettlers.logic.map.loading.EMapStartResources;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

public class InitialGameState implements Cloneable, Serializable {
	private static final long serialVersionUID = 4226604173451490234L;

	private final byte playerId;
	private final PlayerSetting[] playerSettings;
	private final long randomSeed;
	private final EMapStartResources startResources;
	private final MatchRules matchRules;

	private static final byte VERSION = 2;

	public InitialGameState(byte playerId, PlayerSetting[] playerSettings, long randomSeed, EMapStartResources startResources) {
		this(playerId, playerSettings, randomSeed, startResources, MatchRules.DEFAULT);
	}

	public InitialGameState(byte playerId, PlayerSetting[] playerSettings, long randomSeed, EMapStartResources startResources, MatchRules matchRules) {
		this.matchRules = java.util.Objects.requireNonNull(matchRules, "matchRules");
		this.playerId = playerId;
		this.playerSettings = playerSettings;
		this.randomSeed = randomSeed;
		this.startResources = startResources;
	}

	public InitialGameState(byte playerId, PlayerSetting[] playerSettings, long randomSeed) {
		this(playerId, playerSettings, randomSeed, EMapStartResources.HIGH_GOODS);
	}

	public InitialGameState(DataInputStream dis) throws IOException {
		byte readVersion = dis.readByte();
		if(readVersion > VERSION) throw new IllegalStateException("replay version is more recent than this build (" + readVersion + ">" + VERSION + ")");

		randomSeed = dis.readLong();
		playerId = dis.readByte();
		startResources = EMapStartResources.values()[dis.readByte()];

		playerSettings = new PlayerSetting[dis.readInt()];
		for (int i = 0; i < playerSettings.length; i++) {
			playerSettings[i] = PlayerSetting.readFromStream(dis);
		}

		if (readVersion >= 2) {
			int mode = dis.readUnsignedByte();
			if (mode >= MatchRules.Mode.values().length) throw new IOException("Unknown match mode: " + mode);
			matchRules = new MatchRules(MatchRules.Mode.values()[mode]);
		} else {
			matchRules = MatchRules.DEFAULT;
		}
	}

	/** Null occurs when reading Java saves created before match rules were added. */
	public MatchRules getMatchRules() {
		return matchRules == null ? MatchRules.DEFAULT : matchRules;
	}

	public byte getPlayerId() {
		return playerId;
	}

	public PlayerSetting[] getPlayerSettings() {
		return playerSettings;
	}

	public long getRandomSeed() {
		return randomSeed;
	}

	public EMapStartResources getStartResources() {
		return startResources;
	}

	public PlayerSetting[] getReplayablePlayerSettings() {
		PlayerSetting[] playerSettings = new PlayerSetting[this.playerSettings.length];
		for (int i = 0; i < playerSettings.length; i++) {
			PlayerSetting originalSetting = this.playerSettings[i];
			playerSettings[i] = new PlayerSetting(originalSetting.isAvailable(), EPlayerType.HUMAN, originalSetting.getCivilisation(), originalSetting.getTeamId());
		}
		return playerSettings;
	}

	public void serialize(DataOutputStream dos) throws IOException {
		dos.writeByte(VERSION);
		dos.writeLong(randomSeed);
		dos.writeByte(playerId);
		dos.writeByte(startResources.ordinal());

		dos.writeInt(playerSettings.length);
		for (PlayerSetting playerSetting : playerSettings) {
			playerSetting.writeTo(dos);
		}
		dos.writeByte(getMatchRules().getMode().ordinal());
	}

	@Override
	public InitialGameState clone() {
		return new InitialGameState(playerId, getReplayablePlayerSettings(), randomSeed, startResources, getMatchRules());
	}

	@Override
	public String toString() {
		return "InitialGameState{" +
				"playerId=" + playerId +
				", playerSettings=" + Arrays.toString(playerSettings) +
				", randomSeed=" + randomSeed +
				", startResources=" + startResources +
				'}';
	}
}
