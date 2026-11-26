package jsettlers.logic.map.grid;

import jsettlers.common.ai.EPlayerType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.landscape.EResourceType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.RelativePoint;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.workers.MineBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.IAttackableMovable;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class S3DifficultyIntegrationTest {
    private MainGrid grid;
    @After public void cleanup() {
        if (grid != null) grid.stopThreads();
        JSettlersGame.clearState();
    }

    private void initialise() {
        MatchConstants.init(new NetworkTimer(true), 42L);
        grid = new MainGrid("s3-handicap", "S3 handicap", (short)64, (short)64, new PlayerSetting[]{
            new PlayerSetting(EPlayerType.HUMAN, ECivilisation.ROMAN, (byte)0),
            new PlayerSetting(EPlayerType.AI_S3_NORMAL, ECivilisation.ROMAN, (byte)1),
            new PlayerSetting(EPlayerType.AI_S3_EASY, ECivilisation.ROMAN, (byte)2)});
    }

    @Test public void incomingDamageDependsOnTargetProfile() {
        initialise();
        Player attacker = grid.getPartitionsGrid().getPlayer(0);
        for (int id = 0; id < 3; id++) {
            Movable target = Movable.createMovable(EMovableType.SWORDSMAN_L1,
                    grid.getPartitionsGrid().getPlayer(id), new ShortPoint2D(10 + id, 10), grid.movablePathfinderGrid);
            float before = target.getHealth();
            ((IAttackableMovable)target).receiveHit(10F, new ShortPoint2D(15, 15), attacker);
            assertEquals(id == 2 ? 15F : 10F, before - target.getHealth(), 0.0001F);
        }
    }

    @Test public void easyMiningCanDepleteDepositWithoutProducingOutput() {
        initialise();
        ShortPoint2D position = new ShortPoint2D(32, 32);
        MineBuilding mine = new MineBuilding(EBuildingType.COALMINE, grid.getPartitionsGrid().getPlayer(2), position, grid.buildingsGrid);
        boolean discardedAfterDepletion = false;
        for (int attempt = 0; attempt < 100 && !discardedAfterDepletion; attempt++) {
            for (RelativePoint offset : mine.getBuildingVariant().getBlockedTiles()) {
                ShortPoint2D point = offset.calculatePoint(position);
                grid.landscapeGrid.setResourceAt(point.x, point.y, EResourceType.COAL, (byte)15);
            }
            int before = mine.getRemainingResourceAmount();
            boolean output = mine.tryTakingResource();
            int after = mine.getRemainingResourceAmount();
            discardedAfterDepletion = before > after && !output;
        }
        assertTrue("Easy handicap must consume ore even when output is discarded", discardedAfterDepletion);
    }
}
