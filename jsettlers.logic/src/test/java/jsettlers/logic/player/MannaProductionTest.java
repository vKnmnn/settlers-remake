package jsettlers.logic.player;

import jsettlers.common.movable.ESoldierType;
import jsettlers.common.player.ECivilisation;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MannaProductionTest {
	@Test
	public void cumulativeProductionSurvivesSpendingAndFurtherProduction() {
		MannaInformation manna = new MannaInformation(ECivilisation.ROMAN);
		for (int i = 0; i < 10; i++) manna.increaseManna();
		manna.upgrade(ESoldierType.SWORDSMAN);
		assertEquals(0, manna.getAmountOfManna());
		assertEquals(10, manna.getAmountOfProducedManna());
		manna.increaseManna();
		manna.increaseMannaByBigTemple();
		assertEquals(9, manna.getAmountOfManna());
		assertEquals(19, manna.getAmountOfProducedManna());
		manna.stopFutureManaIncreasingByBigTemple();
		manna.increaseMannaByBigTemple();
		assertEquals(19, manna.getAmountOfProducedManna());
	}
}
