package jsettlers.logic.map.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.cargo.CargoMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Exercises real behavior trees, movement orders and occupied cells for every declared unit type. */
@RunWith(Parameterized.class)
public class AllMovableMovementTest {
	private static final Set<EMovableType> CIVILIANS = EnumSet.of(
			EMovableType.BEARER, EMovableType.SMITH, EMovableType.LUMBERJACK, EMovableType.STONECUTTER,
			EMovableType.SAWMILLER, EMovableType.FORESTER, EMovableType.MELTER, EMovableType.MINER,
			EMovableType.FISHERMAN, EMovableType.FARMER, EMovableType.MILLER, EMovableType.BAKER,
			EMovableType.PIG_FARMER, EMovableType.DONKEY_FARMER, EMovableType.SLAUGHTERER,
			EMovableType.CHARCOAL_BURNER, EMovableType.WATERWORKER, EMovableType.WINEGROWER,
			EMovableType.HEALER, EMovableType.DOCKWORKER, EMovableType.BRICKLAYER, EMovableType.DIGGER,
			EMovableType.BREWER, EMovableType.RICE_FARMER, EMovableType.BEEKEEPER, EMovableType.DISTILLER,
			EMovableType.ALCHEMIST, EMovableType.MEAD_BREWER, EMovableType.POWDER_MAKER, EMovableType.SIEGE_ENGINEER);
	private static final Set<EMovableType> ORDERED = EnumSet.of(
			EMovableType.THIEF, EMovableType.PIONEER, EMovableType.GEOLOGIST, EMovableType.MAGE,
			EMovableType.SWORDSMAN_L1, EMovableType.SWORDSMAN_L2, EMovableType.SWORDSMAN_L3,
			EMovableType.PIKEMAN_L1, EMovableType.PIKEMAN_L2, EMovableType.PIKEMAN_L3,
			EMovableType.BOWMAN_L1, EMovableType.BOWMAN_L2, EMovableType.BOWMAN_L3,
			EMovableType.FERRY, EMovableType.CATAPULT, EMovableType.BALLISTA, EMovableType.CANNON, EMovableType.GONG);
	private static final Set<EMovableType> TRADERS = EnumSet.of(
			EMovableType.DONKEY, EMovableType.WHITEFLAGGED_DONKEY, EMovableType.CARGO_SHIP);
	private static final ShortPoint2D START = new ShortPoint2D(20, 20);
	private static final ShortPoint2D TARGET = new ShortPoint2D(26, 20);

	@Parameterized.Parameters(name = "{0}: {1}")
	public static Collection<Object[]> scenarios() {
		Set<EMovableType> covered = EnumSet.noneOf(EMovableType.class);
		for (Set<EMovableType> category : Arrays.asList(CIVILIANS, ORDERED, TRADERS)) {
			for (EMovableType type : category) assertTrue("Duplicate movement scenario: " + type, covered.add(type));
		}
		assertEquals("Every new unit must receive a meaningful movement scenario", EnumSet.allOf(EMovableType.class), covered);
		Collection<Object[]> cases = new ArrayList<>();
		for (EMovableType type : EMovableType.VALUES) {
			cases.add(new Object[] { type, CIVILIANS.contains(type) ? "neutral escape" : "destination order" });
			if (CIVILIANS.contains(type)) cases.add(new Object[] { type, "hostile escape" });
		}
		return cases;
	}

	private final EMovableType type;
	private final String scenario;
	private MainGrid grid;
	private Player player;
	private int time;

	public AllMovableMovementTest(EMovableType type, String scenario) {
		this.type = type;
		this.scenario = scenario;
	}

	@Before
	public void setup() {
		MatchConstants.init(new NetworkTimer(true), 0L);
		grid = new MainGrid("movement", "Movement", (short) 48, (short) 48,
				new PlayerSetting[] { new PlayerSetting((byte) 0), new PlayerSetting((byte) 1) });
		player = grid.getPartitionsGrid().getPlayer((byte) 0);
		if (type.isShip()) {
			for (int y = 0; y < grid.height; y++) for (int x = 0; x < grid.width; x++) {
				grid.landscapeGrid.setLandscapeTypeAt(x, y, ELandscapeType.WATER8, false);
			}
		}
		grid.landscapeGrid.generateBlockedPartitions();
		if (scenario.equals("hostile escape")) {
			for (int y = 1; y < grid.height - 1; y++) for (int x = 1; x < grid.width - 1; x++) {
				grid.partitionsGrid.changePlayerAt(x, y, (byte) 1);
			}
		}
	}

	@After
	public void cleanup() {
		if (grid != null) grid.stopThreads();
		JSettlersGame.clearState();
	}

	@Test
	public void followsItsMovementBehaviorAndSurvives() {
		assertTrue("Fixture must provide the unit's full terrain/footprint", grid.movablePathfinderGrid.canSpawnAt(type, player, START));
		Movable unit = Movable.createMovable(type, player, START, grid.movablePathfinderGrid);
		if (CIVILIANS.contains(type)) {
			assertNotEquals(player, grid.movablePathfinderGrid.getPlayerAt(START));
			if (scenario.equals("hostile escape")) {
				assertEquals((byte) 1, grid.movablePathfinderGrid.getPlayerAt(START).getPlayerId());
				assertNotEquals(player.getTeamId(), grid.movablePathfinderGrid.getPlayerAt(START).getTeamId());
			} else assertNull(grid.movablePathfinderGrid.getPlayerAt(START));
			ShortPoint2D previous = START;
			for (int step = 0; step < 3; step++) {
				advanceUntilMoved(unit, previous);
				assertEquals("Escape must walk between adjacent cells", 1, previous.getOnGridDistTo(unit.getPosition()));
				assertFalse("Old cell must be released", grid.movableGrid.getMovableAt(previous.x, previous.y) == unit);
				previous = unit.getPosition();
			}
		} else {
			assertNotNull("Fixture must allow a complete route", grid.movablePathfinderGrid.calculatePathTo(unit, TARGET));
			if (TRADERS.contains(type)) ((CargoMovable) unit).goToTradeBuilding(TARGET);
			else unit.moveTo(TARGET, EMoveToType.FORCED);
			boolean walked = false;
			for (int step = 0; step < 100 && !TARGET.equals(unit.getPosition()); step++) {
				advance(unit);
				walked |= unit.getAction() == EMovableAction.WALKING;
				assertAliveAndOccupied(unit);
			}
			assertTrue("The behavior must animate actual walking", walked);
			assertEquals("Movement order must reach its destination", TARGET, unit.getPosition());
		}
		assertAliveAndOccupied(unit);
	}

	private void advanceUntilMoved(Movable unit, ShortPoint2D previous) {
		for (int attempt = 0; attempt < 20 && previous.equals(unit.getPosition()); attempt++) {
			advance(unit);
			assertAliveAndOccupied(unit);
		}
		assertNotEquals("The escape behavior must make progress", previous, unit.getPosition());
	}

	private void advance(Movable unit) {
		MatchConstants.clock().fastForwardTo(time += 200);
		unit.timerEvent();
	}

	private void assertAliveAndOccupied(Movable unit) {
		assertTrue(type + " died while " + scenario, unit.isAlive());
		ShortPoint2D position = unit.getPosition();
		assertSame(unit, grid.movableGrid.getMovableAt(position.x, position.y));
		assertEquals(type.isShip(), grid.movablePathfinderGrid.isWater(position.x, position.y));
		if (type.isSiegeWeapon()) {
			for (EDirection direction : EDirection.VALUES) {
				ShortPoint2D ring = direction.getNextHexPoint(position);
				assertSame(unit, grid.movableGrid.getOccupantAt(ring.x, ring.y));
			}
		}
	}
}
