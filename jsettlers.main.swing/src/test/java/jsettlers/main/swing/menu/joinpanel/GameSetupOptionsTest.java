package jsettlers.main.swing.menu.joinpanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.util.Random;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.player.ECivilisation;
import org.junit.Test;

public class GameSetupOptionsTest {
	@Test public void freeForAllAssignsDistinctTeams() {
		for (int i = 0; i < 8; i++) assertEquals(i,
				GameSetupOptions.teamFor(GameSetupOptions.Teams.FREE_FOR_ALL, i, 8, EPlayerType.AI_HARD));
	}

	@Test public void humansAndAllAiDifficultiesUseOpposingTeams() {
		for (EPlayerType type : EPlayerType.VALUES) assertEquals(type == EPlayerType.HUMAN ? 0 : 1,
				GameSetupOptions.teamFor(GameSetupOptions.Teams.HUMANS_VS_AI, 0, 8, type));
	}

	@Test public void twoTeamsStayBalancedForEverySupportedPlayerCount() {
		for (int count = 2; count <= 20; count++) {
			int first = 0, second = 0;
			for (int i = 0; i < count; i++) {
				if (GameSetupOptions.teamFor(GameSetupOptions.Teams.TWO_TEAMS, i, count, EPlayerType.AI_HARD) == 0) first++;
				else second++;
			}
			assertEquals(count / 2, second);
			assertEquals((count + 1) / 2, first);
		}
	}

	@Test public void excludingAmazonsResolvesRandomReproducibly() {
		Random first = new Random(42), second = new Random(42);
		for (int i = 0; i < 100; i++) {
			ECivilisation civilisation = GameSetupOptions.civilisationFor(null, false, first);
			assertNotEquals(ECivilisation.AMAZON, civilisation);
			assertEquals(civilisation, GameSetupOptions.civilisationFor(null, false, second));
		}
	}

	@Test public void defaultRandomChoiceIsPreservedWithoutConsumingRandomness() {
		Random random = new Random(42);
		assertNull(GameSetupOptions.civilisationFor(null, true, random));
		assertEquals(ECivilisation.ROMAN, GameSetupOptions.civilisationFor(ECivilisation.ROMAN, false, random));
		assertEquals(new Random(42).nextLong(), random.nextLong());
	}

	@Test(expected = IllegalArgumentException.class)
	public void disabledAmazonCannotSlipIntoPlayerSettings() {
		GameSetupOptions.civilisationFor(ECivilisation.AMAZON, false, new Random(42));
	}
}
