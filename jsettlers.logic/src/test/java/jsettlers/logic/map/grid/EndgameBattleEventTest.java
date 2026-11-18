package jsettlers.logic.map.grid;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.other.AttackableMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EndgameBattleEventTest {
	private MainGrid grid;
	private Player defender;
	private Player attacker;

	@Before
	public void setUp() {
		MatchConstants.init(new NetworkTimer(true), 0L);
		grid = new MainGrid("battle-statistics", "Battle statistics", (short) 32, (short) 32,
				new PlayerSetting[] { new PlayerSetting((byte) 0), new PlayerSetting((byte) 1),
						new PlayerSetting((byte) 0) });
		defender = grid.getPartitionsGrid().getPlayer((byte) 0);
		attacker = grid.getPartitionsGrid().getPlayer((byte) 1);
	}

	@After
	public void tearDown() {
		if (grid != null) grid.stopThreads();
		JSettlersGame.clearState();
	}

	@Test
	public void onlyLethalEnemyHitCountsAndRepeatedHitDoesNotCountAgain() {
		AttackableMovable target = target();
		target.receiveHit(1, new ShortPoint2D(15, 16), attacker);
		assertEquals(0, attacker.getEndgameStatistic().getBattlePoints());
		target.receiveHit(10000, new ShortPoint2D(15, 16), attacker);
		assertFalse(target.isAlive());
		assertEquals(1, attacker.getEndgameStatistic().getBattlePoints());
		target.receiveHit(10000, new ShortPoint2D(15, 16), attacker);
		assertEquals(1, attacker.getEndgameStatistic().getBattlePoints());
	}

	@Test
	public void friendlyFireAndNonCombatDeathDoNotAwardBattlePoints() {
		Player ally = grid.getPartitionsGrid().getPlayer((byte) 2);
		target().receiveHit(10000, new ShortPoint2D(15, 16), ally);
		assertEquals(0, ally.getEndgameStatistic().getBattlePoints());
		AttackableMovable target = target();
		target.kill();
		target.receiveHit(10000, new ShortPoint2D(15, 16), attacker);
		assertEquals(0, attacker.getEndgameStatistic().getBattlePoints());
	}

	private AttackableMovable target() {
		return new AttackableMovable(grid.movablePathfinderGrid, EMovableType.SWORDSMAN_L1,
				new ShortPoint2D(16, 16), defender, null);
	}
}
