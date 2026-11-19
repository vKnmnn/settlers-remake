package jsettlers.logic.map.grid.partition.manager.materials.requests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.EPriority;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.utils.collections.list.DoubleLinkedList;
import jsettlers.logic.map.grid.partition.manager.settings.MaterialDistributionSettings;
import jsettlers.logic.map.grid.partition.manager.settings.PartitionManagerSettings;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaterialRequestQueueMigrationTest {
	@Test public void oldQueuesRetainRequestsPrioritiesAndDeliveriesWhenWorkshopIsAdded() throws Exception {
		MaterialDistributionSettings settings = new PartitionManagerSettings(ECivilisation.EGYPTIAN).getDistributionSettings(EMaterialType.IRON);
		MaterialsForBuildingsRequestPriorityQueue original = new MaterialsForBuildingsRequestPriorityQueue(settings);
		for (EBuildingType type : new EBuildingType[] { EBuildingType.WEAPONSMITH, EBuildingType.STOCK, EBuildingType.HARBOR, EBuildingType.MARKET_PLACE }) {
			Request request = new Request(type);
			original.insertRequest(request);
			request.updatePriority(EPriority.HIGH);
			request.deliveryAccepted();
		}
		makeLegacy(original, settings);
		MaterialsForBuildingsRequestPriorityQueue loaded = roundTrip(original);
		for (EBuildingType type : new EBuildingType[] { EBuildingType.WEAPONSMITH, EBuildingType.STOCK, EBuildingType.HARBOR, EBuildingType.MARKET_PLACE }) {
			assertEquals(1, loaded.getQueue(EPriority.HIGH, type).size());
			MaterialRequestObject request = loaded.getQueue(EPriority.HIGH, type).getFront();
			assertSame(type, request.getBuildingType());
			assertEquals(1, request.getInDelivery());
			assertSame(loaded, request.requestQueue);
			request.updatePriority(EPriority.LOW);
			assertTrue(loaded.getQueue(EPriority.HIGH, type).isEmpty());
			assertSame(request, loaded.getQueue(EPriority.LOW, type).getFront());
		}
		Request ballista = new Request(EBuildingType.BALLISTA_WORKSHOP);
		loaded.insertRequest(ballista);
		assertSame(ballista, loaded.getQueue(EPriority.DEFAULT, EBuildingType.BALLISTA_WORKSHOP).getFront());
		MaterialsForBuildingsRequestPriorityQueue resaved = roundTrip(loaded);
		assertEquals(1, resaved.getQueue(EPriority.DEFAULT, EBuildingType.BALLISTA_WORKSHOP).size());
		assertEquals(1, resaved.getQueue(EPriority.LOW, EBuildingType.STOCK).size());
	}

	@Test public void oldCoalQueueAcceptsPowderMakerAfterLoadAndPartitionMerge() throws Exception {
		MaterialDistributionSettings settings = new PartitionManagerSettings(ECivilisation.ASIAN).getDistributionSettings(EMaterialType.COAL);
		MaterialsForBuildingsRequestPriorityQueue original = new MaterialsForBuildingsRequestPriorityQueue(settings);
		makeLegacy(original, settings);
		MaterialsForBuildingsRequestPriorityQueue loaded = roundTrip(original);
		Request powder = new Request(EBuildingType.POWDER_MAKER);
		loaded.insertRequest(powder);
		MaterialsForBuildingsRequestPriorityQueue fresh = new MaterialsForBuildingsRequestPriorityQueue(new PartitionManagerSettings(ECivilisation.ASIAN).getDistributionSettings(EMaterialType.COAL));
		loaded.mergeInto(fresh);
		assertSame(powder, fresh.getQueue(EPriority.DEFAULT, EBuildingType.POWDER_MAKER).getFront());
		assertSame(fresh, powder.requestQueue);
	}

	@SuppressWarnings("unchecked")
	private static void makeLegacy(MaterialsForBuildingsRequestPriorityQueue queue, MaterialDistributionSettings settings) throws Exception {
		EBuildingType[] current = settings.getBuildingTypes();
		EBuildingType[] old = Arrays.stream(current).filter(type -> type.ordinal < EBuildingType.POWDER_MAKER.ordinal).toArray(EBuildingType[]::new);
		DoubleLinkedList<MaterialRequestObject>[][] queues = (DoubleLinkedList<MaterialRequestObject>[][]) field(queue, "queues").get(queue);
		DoubleLinkedList<MaterialRequestObject>[][] oldQueues = new DoubleLinkedList[queues.length][];
		for (int priority = 0; priority < queues.length; priority++) {
			oldQueues[priority] = DoubleLinkedList.getArray(old.length + 3);
			for (int i = 0; i < old.length; i++) oldQueues[priority][i] = queues[priority][Arrays.asList(current).indexOf(old[i])];
			System.arraycopy(queues[priority], current.length, oldQueues[priority], old.length, 3);
		}
		field(queue, "queues").set(queue, oldQueues);
		field(queue, "buildingTypes").set(queue, old);
		field(queue, "numberOfConfigurableBuildings").setInt(queue, old.length);
		field(queue, "numberOfAllBuildings").setInt(queue, old.length + 3);
	}

	private static Field field(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	private static MaterialsForBuildingsRequestPriorityQueue roundTrip(MaterialsForBuildingsRequestPriorityQueue value) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(value); }
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			return (MaterialsForBuildingsRequestPriorityQueue) input.readObject();
		}
	}

	private static final class Request extends MaterialRequestObject {
		private final EBuildingType type;
		Request(EBuildingType type) { this.type = type; }
		@Override protected short getStillNeeded() { return 8; }
		@Override protected int getInDeliveryable() { return 8; }
		@Override protected void materialDelivered() { }
		@Override protected boolean isRoundRobinRequest() { return false; }
		@Override protected EBuildingType getBuildingType() { return type; }
		@Override public ShortPoint2D getPosition() { return new ShortPoint2D(2, 2); }
	}
}
