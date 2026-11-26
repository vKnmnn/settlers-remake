package jsettlers.main.swing.menu.campaign;

import jsettlers.common.ai.EPlayerType;
import jsettlers.common.player.ECivilisation;
import jsettlers.logic.map.loading.MapLoadException;
import jsettlers.logic.map.loading.original.OriginalMapFileContent;
import jsettlers.logic.player.InitialGameState;
import jsettlers.logic.player.PlayerSetting;
import org.junit.Test;
import static org.junit.Assert.*;

public class CampaignMapLoaderTest {
	@Test public void authoredCivilizationsAndHumanSlotArePreserved() throws Exception {
		OriginalMapFileContent map = new OriginalMapFileContent(16);
		map.setPlayerCount(4);
		for (int i = 0; i < 4; i++) map.setPlayer(i, i, i, 3 - i, "player");
		InitialGameState initial = CampaignMapLoader.initialState(map, 1234L);
		assertEquals(0, initial.getPlayerId());
		assertEquals(1234L, initial.getRandomSeed());
		PlayerSetting[] players = initial.getPlayerSettings();
		assertEquals(ECivilisation.AMAZON, players[0].getCivilisation());
		assertEquals(ECivilisation.ASIAN, players[1].getCivilisation());
		assertEquals(ECivilisation.EGYPTIAN, players[2].getCivilisation());
		assertEquals(ECivilisation.ROMAN, players[3].getCivilisation());
		assertEquals(EPlayerType.HUMAN, players[0].getPlayerType());
		for (int i = 1; i < 4; i++) assertTrue(players[i].getPlayerType().isAi());
	}

	@Test public void everyAiProfileIsSelectableWithoutChangingTheHuman() throws Exception {
		OriginalMapFileContent map = new OriginalMapFileContent(16);
		map.setPlayerCount(2);
		map.setPlayer(0, 0, 0, 0, "human");
		map.setPlayer(1, 1, 1, 1, "computer");
		for (EPlayerType type : EPlayerType.VALUES_AI_ONLY) {
			PlayerSetting[] players = CampaignMapLoader.initialState(map, 1L, type).getPlayerSettings();
			assertSame(EPlayerType.HUMAN, players[0].getPlayerType());
			assertSame(type, players[1].getPlayerType());
			assertSame(ECivilisation.EGYPTIAN, players[1].getCivilisation());
		}
	}

	@Test(expected = MapLoadException.class) public void unspecifiedNationIsNotRandomizedInCampaign() throws Exception {
		OriginalMapFileContent map = new OriginalMapFileContent(16);
		map.setPlayerCount(1);
		map.setPlayer(0, 0, 0, 255, "player");
		CampaignMapLoader.initialState(map, 1L);
	}
}
