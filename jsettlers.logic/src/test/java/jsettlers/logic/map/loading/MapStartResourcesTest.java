package jsettlers.logic.map.loading;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.material.EMaterialType;
import jsettlers.logic.map.loading.data.objects.BuildingMapDataObject;
import jsettlers.logic.map.loading.data.objects.MapDataObject;
import jsettlers.logic.map.loading.data.objects.StackMapDataObject;
import jsettlers.logic.map.loading.newmap.FreshMapData;
import jsettlers.logic.map.loading.newmap.FreshMapLoader;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.testutils.TestUtils;

public class MapStartResourcesTest {
	@BeforeClass public static void resources() throws Exception { TestUtils.setupTempResourceManager(); }

	@Test public void mapDefaultUsesAuthoredStockAndExplicitChoiceOverridesIt() {
		for (EMapStartResources authored : new EMapStartResources[] { EMapStartResources.LOW_GOODS, EMapStartResources.MEDIUM_GOODS, EMapStartResources.HIGH_GOODS }) {
			FixtureLoader loader = new FixtureLoader(authored);
			FreshMapData map = emptyMap();
			loader.prepare(EMapStartResources.MAP_DEFAULT, map);
			assertEquals(expectedStock(authored), stock(map));
			assertTrue(map.hasStartBuildings());
			for (EMapStartResources explicit : new EMapStartResources[] { EMapStartResources.LOW_GOODS, EMapStartResources.MEDIUM_GOODS, EMapStartResources.HIGH_GOODS }) {
				map = emptyMap();
				loader.prepare(explicit, map);
				assertEquals(expectedStock(explicit), stock(map));
			}
		}
	}

	@Test public void preplacedScenarioObjectsSurviveEveryChoice() {
		for (EMapStartResources selection : EMapStartResources.values()) {
			FreshMapData map = emptyMap();
			BuildingMapDataObject tower = new BuildingMapDataObject(EBuildingType.TOWER, (byte) 0);
			StackMapDataObject stock = new StackMapDataObject(EMaterialType.PLANK, 3);
			map.setMapObject(16, 16, tower);
			map.setMapObject(18, 18, stock);
			new FixtureLoader(EMapStartResources.LOW_GOODS).prepare(selection, map);
			assertSame(tower, map.getMapObject(16, 16));
			assertSame(stock, map.getMapObject(18, 18));
			assertEquals(Map.of(EMaterialType.PLANK, 3), stock(map));
			int count = 0;
			for (int x = 0; x < 32; x++) for (int y = 0; y < 32; y++) if (map.getMapObject(x,y) != null) count++;
			assertEquals(2, count);
		}
	}

	@Test public void remakeFallbackAndLegacyReplayOrdinalsRemainStable() {
		assertEquals(EMapStartResources.HIGH_GOODS, new FreshMapLoader(null, null).getDefaultStartResources());
		assertEquals(EMapStartResources.LOW_GOODS, EMapStartResources.values()[0]);
		assertEquals(EMapStartResources.MEDIUM_GOODS, EMapStartResources.values()[1]);
		assertEquals(EMapStartResources.HIGH_GOODS, EMapStartResources.values()[2]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void unresolvedMapDefaultCannotGenerateAnAccidentalHighStock() {
		EMapStartResources.generateStackObjects(EMapStartResources.MAP_DEFAULT);
	}

	private FreshMapData emptyMap() {
		FreshMapData map = new FreshMapData();
		map.setDimension(32, 32, 1);
		map.setPlayerStart((byte) 0, 16, 16);
		return map;
	}

	private Map<EMaterialType, Integer> expectedStock(EMapStartResources selection) {
		Map<EMaterialType, Integer> result = new EnumMap<>(EMaterialType.class);
		for (MapDataObject object : EMapStartResources.generateStackObjects(selection)) {
			StackMapDataObject stack = (StackMapDataObject) object;
			result.merge(stack.getType(), stack.getCount(), Integer::sum);
		}
		return result;
	}

	private Map<EMaterialType, Integer> stock(FreshMapData map) {
		Map<EMaterialType, Integer> result = new EnumMap<>(EMaterialType.class);
		for (int x = 0; x < 32; x++) for (int y = 0; y < 32; y++) {
			MapDataObject object = map.getMapObject(x,y);
			if (object instanceof StackMapDataObject) {
				StackMapDataObject stack = (StackMapDataObject) object;
				result.merge(stack.getType(), stack.getCount(), Integer::sum);
			}
		}
		return result;
	}

	private static class FixtureLoader extends FreshMapLoader {
		private final EMapStartResources authored;
		FixtureLoader(EMapStartResources authored) { super(null, null); this.authored = authored; }
		@Override public EMapStartResources getDefaultStartResources() { return authored; }
		@Override public int getMaxPlayers() { return 1; }
		void prepare(EMapStartResources selection, FreshMapData map) {
			setupStartConditions(new PlayerSetting[] { new PlayerSetting((byte) 0) }, selection, map);
		}
	}
}
