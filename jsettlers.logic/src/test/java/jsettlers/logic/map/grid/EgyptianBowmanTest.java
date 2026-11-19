package jsettlers.logic.map.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jsettlers.common.ai.EPlayerType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.logic.objects.arrow.ArrowObject;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** An Egyptian level-three bow must release a damaging arrow in every direction. */
@RunWith(Parameterized.class)
public class EgyptianBowmanTest {
	@Parameterized.Parameters(name = "{0}")
	public static Object[] directions() { return EDirection.VALUES; }
	private final EDirection direction;
	private MainGrid grid;

	public EgyptianBowmanTest(EDirection direction) { this.direction = direction; }

	@After
	public void cleanup() {
		if (grid != null) grid.stopThreads();
		JSettlersGame.clearState();
	}

	@Test
	public void levelThreeBowDamagesEnemyInEveryDirection() {
		MatchConstants.init(new NetworkTimer(true), 0L);
		grid = new MainGrid("bow", "Bow", (short) 64, (short) 64, new PlayerSetting[] {
				new PlayerSetting(EPlayerType.HUMAN, ECivilisation.EGYPTIAN, (byte) 0),
				new PlayerSetting(EPlayerType.HUMAN, ECivilisation.ROMAN, (byte) 1) });
		grid.landscapeGrid.generateBlockedPartitions();
		ShortPoint2D source = new ShortPoint2D(32, 32);
		Movable archer = Movable.createMovable(EMovableType.BOWMAN_L3, grid.partitionsGrid.getPlayer((byte) 0),
				source, grid.movablePathfinderGrid);
		Movable target = Movable.createMovable(EMovableType.PIKEMAN_L3, grid.partitionsGrid.getPlayer((byte) 1),
				direction.getNextHexPoint(source, Constants.BOWMAN_MIN_ATTACK_DISTANCE + 1), grid.movablePathfinderGrid);
		float initialHealth = target.getHealth();
		boolean releasedArrow = false;
		// Keep the target stationary; run the shooter's real behavior and scheduled arrow impacts.
		for (int time = 200; time <= 10000 && target.getHealth() == initialHealth; time += 200) {
			MatchConstants.clock().fastForwardTo(time);
			archer.timerEvent();
			ArrowObject arrow = (ArrowObject) grid.objectsGrid.getMapObjectAt(target.getPosition().x,
					target.getPosition().y, EMapObjectType.ARROW);
			if (arrow != null) {
				releasedArrow = true;
				assertEquals(direction, arrow.getDirection());
				assertTrue(arrow.getHitStrength() > 0);
			}
			grid.mapObjectsManager.timerEvent();
		}
		assertTrue("A real arrow must be released", releasedArrow);
		assertTrue("Arrow must hit in " + direction, target.getHealth() < initialHealth);
		assertEquals(source, archer.getPosition());
		assertTrue(archer.isAlive());
	}
}
