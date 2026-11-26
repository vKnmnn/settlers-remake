package jsettlers.main.swing.menu.campaign;

import java.io.File;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.player.ECivilisation;
import jsettlers.logic.map.loading.EMapStartResources;
import jsettlers.logic.map.loading.MapLoadException;
import jsettlers.logic.map.loading.data.IMutableMapData;
import jsettlers.logic.map.loading.list.DirectoryMapLister.ListedMapFile;
import jsettlers.logic.map.loading.original.OriginalMapFileContent;
import jsettlers.logic.map.loading.original.OriginalMapLoader;
import jsettlers.logic.player.InitialGameState;
import jsettlers.logic.player.PlayerSetting;

/** Campaigns use their authored settlers, buildings and goods, not skirmish starting resources. */
public final class CampaignMapLoader extends OriginalMapLoader {
	@Override
	public boolean supportsCustomMatchRules() { return false; }

	public CampaignMapLoader(File file) throws MapLoadException { super(new ListedMapFile(file)); }

	public InitialGameState initialState(long seed) throws MapLoadException {
		return initialState(seed, EPlayerType.AI_HARD);
	}

	public InitialGameState initialState(long seed, EPlayerType computerType) throws MapLoadException {
		return initialState((OriginalMapFileContent) getMapData(), seed, computerType);
	}

	static InitialGameState initialState(OriginalMapFileContent data, long seed) throws MapLoadException {
		return initialState(data, seed, EPlayerType.AI_HARD);
	}

	static InitialGameState initialState(OriginalMapFileContent data, long seed, EPlayerType computerType) throws MapLoadException {
		if (computerType == null || !computerType.isAi()) throw new IllegalArgumentException("Campaign opponents must use an AI profile");
		PlayerSetting[] players = new PlayerSetting[data.getPlayerCount()];
		for (byte player = 0; player < players.length; player++) {
			ECivilisation civilisation;
			switch (data.getPlayerCivilization(player)) {
			case ROMANS: civilisation = ECivilisation.ROMAN; break;
			case EGYPTIANS: civilisation = ECivilisation.EGYPTIAN; break;
			case ASIANS: civilisation = ECivilisation.ASIAN; break;
			case AMAZONS: civilisation = ECivilisation.AMAZON; break;
			default: throw new MapLoadException("Campaign player has no fixed civilization: " + player);
			}
			players[player] = new PlayerSetting(player == 0 ? EPlayerType.HUMAN : computerType, civilisation, player);
		}
		return new InitialGameState((byte) 0, players, seed);
	}

	@Override
	protected PlayerSetting[] setupStartConditions(PlayerSetting[] settings, EMapStartResources resources, IMutableMapData data) {
		return settings;
	}
}
