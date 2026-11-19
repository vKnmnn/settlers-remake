package jsettlers.logic.map.grid;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jsettlers.common.ai.EPlayerType;
import jsettlers.common.map.shapes.MapCircle;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.ESpellType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.movable.military.MageMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/** Exercises actual priest commands and map occupancy, without invoking private spell methods. */
public class MageTeleportTest {
    private static final ShortPoint2D CASTER = new ShortPoint2D(12, 12);
    private static final ShortPoint2D REMOTE = new ShortPoint2D(45, 45);
    private MainGrid grid;

    @After
    public void tearDown() {
        if (grid != null) grid.stopThreads();
        JSettlersGame.clearState();
    }

    @Test
    public void callHelpUsesFreeGroundWithoutReplacingPriestOrAnotherUnit() {
        MageMovable priest = createGame(ECivilisation.ASIAN);
        ShortPoint2D occupied = new ShortPoint2D(13, 12);
        ShortPoint2D landing = new ShortPoint2D(14, 12);
        restrictLandingArea(CASTER, CASTER, occupied, landing);
        Movable guard = spawn(EMovableType.SWORDSMAN_L1, 0, occupied);
        Movable ally = spawn(EMovableType.SWORDSMAN_L1, 1, REMOTE);
        ShortPoint2D bowPosition = new ShortPoint2D(46, 45);
        Movable enemyBow = spawn(EMovableType.BOWMAN_L1, 2, bowPosition);

        priest.moveToCast(REMOTE, ESpellType.CALL_HELP);
        priest.timerEvent();

        assertSame(priest, at(CASTER));
        assertSame(guard, at(occupied));
        assertEquals(landing, ally.getPosition());
        assertSame(ally, at(landing));
        assertNull(at(REMOTE));
        assertFalse(grid.movablePathfinderGrid.isBlockedOrProtected(landing.x, landing.y));
        assertSame(enemyBow, at(bowPosition));
        assertEquals(EMovableType.BOWMAN_L1, enemyBow.getMovableType());
        assertTrue(enemyBow.isAlive());
    }

    @Test
    public void callHelpDoesNotAlsoCastCurseBowmanWhenNoAlliesArePresent() {
        MageMovable priest = createGame(ECivilisation.ASIAN);
        Movable enemyBow = spawn(EMovableType.BOWMAN_L1, 2, REMOTE);

        priest.moveToCast(REMOTE, ESpellType.CALL_HELP);
        priest.timerEvent();

        assertSame(enemyBow, at(REMOTE));
        assertEquals(EMovableType.BOWMAN_L1, enemyBow.getMovableType());
        assertTrue(enemyBow.isAlive());
        assertSame(priest, at(CASTER));
    }

    @Test
    public void callHelpLeavesAlliesAtSourceIfNoLandingSpaceIsAvailable() {
        MageMovable priest = createGame(ECivilisation.ASIAN);
        restrictLandingArea(CASTER, CASTER); // the only unblocked cell is already occupied by the priest
        Movable ally = spawn(EMovableType.SWORDSMAN_L1, 1, REMOTE);

        priest.moveToCast(REMOTE, ESpellType.CALL_HELP);
        priest.timerEvent();

        assertEquals(REMOTE, ally.getPosition());
        assertSame(ally, at(REMOTE));
        assertSame(priest, at(CASTER));
    }

    @Test
    public void sendFoesUsesFreeDestinationWithoutOverwritingItsOccupant() {
        MageMovable priest = createGame(ECivilisation.EGYPTIAN);
        ShortPoint2D landing = new ShortPoint2D(46, 45);
        restrictLandingArea(REMOTE, REMOTE, landing);
        Movable guard = spawn(EMovableType.SWORDSMAN_L1, 0, REMOTE);
        ShortPoint2D enemySource = new ShortPoint2D(13, 12);
        Movable enemy = spawn(EMovableType.SWORDSMAN_L1, 2, enemySource);

        priest.moveToCast(REMOTE, ESpellType.SEND_FOES);
        priest.timerEvent();

        assertSame(priest, at(CASTER));
        assertSame(guard, at(REMOTE));
        assertEquals(landing, enemy.getPosition());
        assertSame(enemy, at(landing));
        assertNull(at(enemySource));
        assertFalse(grid.movablePathfinderGrid.isBlockedOrProtected(landing.x, landing.y));
        assertTrue(enemy.isAlive());
    }

    @Test
    public void divineShieldProtectsOwnAndAlliedSoldiersButNeverEnemies() {
        MageMovable priest = createGame(ECivilisation.ASIAN);
        Movable own = spawn(EMovableType.SWORDSMAN_L1, 0, new ShortPoint2D(14, 12));
        Movable ally = spawn(EMovableType.BOWMAN_L1, 1, new ShortPoint2D(14, 13));
        Movable enemy = spawn(EMovableType.PIKEMAN_L1, 2, new ShortPoint2D(13, 13));

        priest.moveToCast(own.getPosition(), ESpellType.SHIELD);
        priest.timerEvent();

        assertTrue(own.hasEffect(EEffectType.SHIELDED));
        assertTrue(ally.hasEffect(EEffectType.SHIELDED));
        assertFalse(enemy.hasEffect(EEffectType.SHIELDED));
        assertTrue(priest.isAlive());
    }

    private MageMovable createGame(ECivilisation civilisation) {
        MatchConstants.init(new NetworkTimer(true), 0L);
        grid = new MainGrid("mage-teleport-test", "Mage teleport test", (short) 64, (short) 64,
                new PlayerSetting[] {
                        new PlayerSetting(EPlayerType.HUMAN, civilisation, (byte) 0),
                        new PlayerSetting(EPlayerType.HUMAN, ECivilisation.ROMAN, (byte) 0),
                        new PlayerSetting(EPlayerType.HUMAN, ECivilisation.ROMAN, (byte) 1)
                });
        Player player = grid.getPartitionsGrid().getPlayer((byte) 0);
        for (int i = 0; i < 100; i++) player.getMannaInformation().increaseManna();
        return (MageMovable) Movable.createMovable(EMovableType.MAGE, player, CASTER, grid.movablePathfinderGrid);
    }

    private ILogicMovable at(ShortPoint2D position) {
        return grid.movablePathfinderGrid.getMovableAt(position.x, position.y);
    }

    private Movable spawn(EMovableType type, int player, ShortPoint2D position) {
        return Movable.createMovable(type, grid.getPartitionsGrid().getPlayer((byte) player),
                position, grid.movablePathfinderGrid);
    }

    private void restrictLandingArea(ShortPoint2D center, ShortPoint2D... unblocked) {
        Set<ShortPoint2D> allowed = new HashSet<>(Arrays.asList(unblocked));
        new MapCircle(center, Constants.SPELL_EFFECT_RADIUS).stream().forEach((x, y) ->
                grid.flagsGrid.setBlockedAndProtected(x, y, !allowed.contains(new ShortPoint2D(x, y))));
    }
}
