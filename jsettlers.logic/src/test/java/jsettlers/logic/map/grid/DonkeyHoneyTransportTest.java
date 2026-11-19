package jsettlers.logic.map.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import jsettlers.common.ai.EPlayerType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.ITradeBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.cargo.CargoMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.logic.trading.TransportationRequest;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Real cargo behavior with a finite two-jar source; no direct cargo-field injection. */
@RunWith(Parameterized.class)
public class DonkeyHoneyTransportTest {
	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> civilisations() {
		return Arrays.asList(new Object[][] { { ECivilisation.ROMAN }, { ECivilisation.EGYPTIAN }, { ECivilisation.ASIAN } });
	}

	private final ECivilisation civilisation;
	private MainGrid grid;

	public DonkeyHoneyTransportTest(ECivilisation civilisation) { this.civilisation = civilisation; }

	@After
	public void cleanup() {
		if (grid != null) grid.stopThreads();
		JSettlersGame.clearState();
	}

	@Test
	public void nonAmazonDonkeyLoadsCarriesAndUnloadsHoney() {
		MatchConstants.init(new NetworkTimer(true), 0L);
		grid = new MainGrid("honey", "Honey", (short) 80, (short) 48,
				new PlayerSetting[] { new PlayerSetting(EPlayerType.HUMAN, civilisation, (byte) 0) });
		grid.landscapeGrid.generateBlockedPartitions();
		Player player = grid.getPartitionsGrid().getPlayer((byte) 0);
		HoneySource source = new HoneySource();
		CargoMovable donkey = (CargoMovable) Movable.createMovable(EMovableType.DONKEY, player, new ShortPoint2D(8, 20), grid.movablePathfinderGrid);
		donkey.moveGoods(new TransportationRequest(source, player.getLandTradeManager(), donkey));
		boolean carriedBasket = false;
		for (int time = 100; time <= 60000 && honeyOnGround() < 2; time += 100) {
			MatchConstants.clock().fastForwardTo(time);
			donkey.timerEvent();
			assertTrue("Honey must not kill a non-Amazon donkey", donkey.isAlive());
			carriedBasket |= donkey.getMaterial() == EMaterialType.BASKET;
		}
		assertTrue("A real loading/carrying cycle must occur", carriedBasket);
		assertEquals(0, source.remaining);
		assertEquals("Both jars must reach destination ground stacks", 2, honeyOnGround());
		assertTrue(donkey.getPosition().getOnGridDistTo(source.getPickUpPosition()) > 20);
		assertEquals(EMaterialType.NO_MATERIAL, donkey.getMaterial());
	}

	private int honeyOnGround() {
		int count = 0;
		for (short y = 0; y < grid.height; y++) for (short x = 0; x < grid.width; x++) {
			count += grid.mapObjectsManager.getStackSize(x, y, EMaterialType.HONEY);
		}
		return count;
	}

	private static class HoneySource implements ITradeBuilding {
		private static final long serialVersionUID = 1L;
		private int remaining = 2;
		@Override public boolean needsTrader() { return remaining > 0; }
		@Override public boolean needsMoreTraders() { return needsTrader(); }
		@Override public void addApproachingTrader() { }
		@Override public void removeApproachingTrader() { }
		@Override public ShortPoint2D getPosition() { return getPickUpPosition(); }
		@Override public ShortPoint2D getPickUpPosition() { return new ShortPoint2D(10, 20); }
		@Override public Iterator<ShortPoint2D> getWaypointsIterator() { return Arrays.asList(new ShortPoint2D(55, 20)).iterator(); }
		@Override public Optional<MaterialTypeWithCount> tryToTakeMaterial(int maxAmount) {
			if (remaining == 0) return Optional.empty();
			int amount = Math.min(remaining, maxAmount);
			remaining -= amount;
			return Optional.of(new MaterialTypeWithCount(EMaterialType.HONEY, amount));
		}
	}
}
