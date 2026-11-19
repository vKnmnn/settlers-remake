package jsettlers.logic.map.grid.partition.manager.materials;

import java.util.ArrayList;
import java.util.List;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.partition.manager.materials.interfaces.IJoblessSupplier;
import jsettlers.logic.map.grid.partition.manager.materials.interfaces.IManagerBearer;
import jsettlers.logic.map.grid.partition.manager.materials.interfaces.IMaterialOffer;
import jsettlers.logic.map.grid.partition.manager.materials.interfaces.IMaterialRequest;
import jsettlers.logic.map.grid.partition.manager.materials.offers.EOfferPriority;
import jsettlers.logic.map.grid.partition.manager.materials.offers.OffersList;
import jsettlers.logic.map.grid.partition.manager.materials.requests.MaterialRequestObject;
import jsettlers.logic.map.grid.partition.manager.settings.PartitionManagerSettings;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Scheduler regression only: mock bearers hold deliveries pending, without simulating routes. */
public class CoalDeliveryCapacityTest {
	@Test
	public void moreThan256ConcurrentCoalDeliveriesCompleteAndNewDemandIsServed() {
		MatchConstants.init(null, 0L);
		PendingBearers bearers = new PendingBearers();
		OffersList offers = new OffersList(null);
		MaterialsManager manager = new MaterialsManager(bearers, offers, new PartitionManagerSettings(ECivilisation.ASIAN));
		List<Consumer> consumers = new ArrayList<>();
		EBuildingType[] types = { EBuildingType.IRONMELT, EBuildingType.GOLDMELT, EBuildingType.WEAPONSMITH, EBuildingType.POWDER_MAKER };
		for (int i = 0; i < 40; i++) {
			Consumer request = new Consumer(types[i % types.length], new ShortPoint2D(100 + i * 20, 800));
			consumers.add(request);
			manager.addRequestObject(EMaterialType.COAL, request);
			for (int unit = 0; unit < 8; unit++) {
				offers.addOffer(new ShortPoint2D(10 + i * 20, 10 + unit), EMaterialType.COAL, EOfferPriority.OFFER_TO_ALL);
			}
		}
		// One coal assignment per scheduler pass; all 320 remain simultaneously in flight.
		for (int i = 0; i < 320; i++) manager.distributeJobs();
		assertEquals(320, bearers.pending.size());
		for (Consumer consumer : consumers) {
			assertEquals(8, consumer.getInDelivery());
			assertEquals(0, consumer.delivered);
		}
		bearers.completeAll();
		for (Consumer consumer : consumers) {
			assertEquals(8, consumer.delivered);
			assertEquals(0, consumer.getInDelivery());
			assertTrue(consumer.isFinished());
		}
		Consumer later = new Consumer(EBuildingType.IRONMELT, new ShortPoint2D(900, 900));
		manager.addRequestObject(EMaterialType.COAL, later);
		for (int i = 0; i < 8; i++) offers.addOffer(new ShortPoint2D(1, 1 + i), EMaterialType.COAL, EOfferPriority.OFFER_TO_ALL);
		// Finished requests are removed lazily by scheduler passes.
		for (int i = 0; i < consumers.size() + 8; i++) manager.distributeJobs();
		assertEquals("Fresh demand must still be scheduled after the large batch", 8, bearers.pending.size());
		bearers.completeAll();
		assertEquals(8, later.delivered);
		assertTrue(later.isFinished());
	}

	private static final class Consumer extends MaterialRequestObject {
		private final EBuildingType type;
		private final ShortPoint2D position;
		private int delivered;
		Consumer(EBuildingType type, ShortPoint2D position) { this.type = type; this.position = position; }
		@Override public ShortPoint2D getPosition() { return position; }
		@Override protected short getStillNeeded() { return (short) (8 - delivered - getInDelivery()); }
		@Override protected int getInDeliveryable() { return 8; }
		@Override protected void materialDelivered() { delivered++; }
		@Override protected boolean isRoundRobinRequest() { return false; }
		@Override protected EBuildingType getBuildingType() { return type; }
	}

	private static final class PendingBearers implements IJoblessSupplier {
		private final List<IMaterialRequest> pending = new ArrayList<>();
		@Override public boolean isEmpty() { return false; }
		@Override public IManagerBearer removeJoblessCloseTo(ShortPoint2D position) {
			return new IManagerBearer() {
				@Override public ShortPoint2D getPosition() { return position; }
				@Override public void deliver(EMaterialType material, IMaterialOffer offer, IMaterialRequest request) {
					assertEquals(EMaterialType.COAL, material);
					offer.distributionAccepted();
					offer.offerTaken();
					request.deliveryAccepted();
					pending.add(request);
				}
			};
		}
		void completeAll() {
			pending.forEach(IMaterialRequest::deliveryFulfilled);
			pending.clear();
		}
	}
}
