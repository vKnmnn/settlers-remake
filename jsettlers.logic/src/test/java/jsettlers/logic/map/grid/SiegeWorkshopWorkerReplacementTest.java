package jsettlers.logic.map.grid;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.stack.IRequestStack;
import jsettlers.logic.buildings.workers.SiegeWorkshopBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableWorker;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.player.Player;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import org.junit.Test;
import static org.junit.Assert.*;

public class SiegeWorkshopWorkerReplacementTest {
	@Test public void replacementEngineerRequestsFreshAmmunitionMaterials() {
		MatchConstants.init(new NetworkTimer(true), 0L);
		MainGrid grid = new MainGrid("worker-replacement", "Worker replacement", (short) 64, (short) 64,
				new PlayerSetting[] { new PlayerSetting((byte) 0) });
		try {
			Player player = grid.getPartitionsGrid().getPlayer((byte) 0);
			InspectableWorkshop workshop = new InspectableWorkshop(player, grid);
			IManageableWorker first = (IManageableWorker) Movable.createMovable(EMovableType.SIEGE_ENGINEER,
					player, new ShortPoint2D(12, 12), grid.movablePathfinderGrid);
			IManageableWorker replacement = (IManageableWorker) Movable.createMovable(EMovableType.SIEGE_ENGINEER,
					player, new ShortPoint2D(14, 12), grid.movablePathfinderGrid);
			workshop.occupyBuilding(first);
			workshop.orderSiegeWeapon();
			IRequestStack oldStoneRequest = stoneRequest(workshop);
			assertTrue(oldStoneRequest.getStillRequired() > 0);
			workshop.leaveBuilding(first);
			assertEquals(0, oldStoneRequest.getStillRequired());
			workshop.occupyBuilding(replacement);
			IRequestStack newStoneRequest = stoneRequest(workshop);
			assertNotSame("released requests must not be reused", oldStoneRequest, newStoneRequest);
			assertTrue("replacement engineer needs active ammunition deliveries", newStoneRequest.getStillRequired() > 0);
			assertEquals("worker replacement must preserve the weapon queue", 1, workshop.getOrderedSiegeWeapons());
		} finally {
			grid.stopThreads();
			JSettlersGame.clearState();
		}
	}

	private static final class InspectableWorkshop extends SiegeWorkshopBuilding {
		InspectableWorkshop(Player player, MainGrid grid) {
			super(EBuildingType.CATAPULT_WORKSHOP, player, new ShortPoint2D(32, 32), grid.buildingsGrid);
		}
		java.util.List<? extends IRequestStack> workStacks() { return getStacks(); }
	}

	private IRequestStack stoneRequest(InspectableWorkshop workshop) {
		return workshop.workStacks().stream().filter(stack -> stack.getMaterialType() == EMaterialType.STONE)
				.findFirst().orElseThrow(() -> new AssertionError("missing stone request"));
	}
}
