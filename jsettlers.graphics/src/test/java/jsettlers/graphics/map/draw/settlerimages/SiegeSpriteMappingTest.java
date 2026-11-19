package jsettlers.graphics.map.draw.settlerimages;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;

/** Regression checks against verified original sequence metadata; no game assets are required. */
public class SiegeSpriteMappingTest {
	private static final EDirection[] DIRECTIONS = { EDirection.SOUTH_WEST, EDirection.WEST, EDirection.NORTH_WEST,
			EDirection.NORTH_EAST, EDirection.EAST, EDirection.SOUTH_EAST };

	@Test
	public void firingSequencesStayWithinOriginalFrameCounts() throws Exception {
		EMovableType[] weapons = { EMovableType.CATAPULT, EMovableType.BALLISTA, EMovableType.CANNON, EMovableType.GONG };
		EMaterialType[] ammo = { EMaterialType.CATAPULT_AMMO, EMaterialType.BALLISTA_AMMO,
				EMaterialType.CANNON_AMMO, EMaterialType.NO_MATERIAL };
		int[] frameCounts = { 24, 15, 18, 39 };
		for (ECivilisation civilisation : ECivilisation.VALUES) {
			for (int weapon = 0; weapon < weapons.length; weapon++) {
				for (int direction = 0; direction < DIRECTIONS.length; direction++) {
					SettlerImageMapItem item = item(civilisation, weapons[weapon], EMovableAction.ACTION1, ammo[weapon], DIRECTIONS[direction]);
					assertEquals(12 + weapon * 10, item.getFile());
					assertEquals((weapon == 3 ? 1 : 2) + direction, item.getSequenceIndex());
					assertFramesWithin(item, frameCounts[weapon]);
				}
			}
		}
	}

	@Test
	public void gongWalkingUsesDistinctDirectionBlocks() throws Exception {
		for (int direction = 0; direction < DIRECTIONS.length; direction++) {
			SettlerImageMapItem item = item(ECivilisation.AMAZON, EMovableType.GONG, EMovableAction.WALKING,
					EMaterialType.NO_MATERIAL, DIRECTIONS[direction]);
			assertEquals(42, item.getFile());
			assertEquals(0, item.getSequenceIndex());
			assertTrue(item.getStart() >= direction * 12);
			assertTrue(item.getDuration() > 1);
			assertFramesWithin(item, (direction + 1) * 12);
		}
	}

	@Test
	public void powderMakerCarriesTheCorrectMaterials() throws Exception {
		EMaterialType[] materials = { EMaterialType.GUN_POWDER, EMaterialType.COAL, EMaterialType.SULFUR, EMaterialType.NO_MATERIAL };
		int[] sequences = { 163, 165, 167, 169 };
		for (int material = 0; material < materials.length; material++) {
			for (EDirection direction : DIRECTIONS) {
				for (EMovableAction action : new EMovableAction[] { EMovableAction.WALKING, EMovableAction.NO_ACTION }) {
					SettlerImageMapItem item = item(ECivilisation.ASIAN, EMovableType.POWDER_MAKER, action, materials[material], direction);
					assertEquals(31, item.getFile());
					assertEquals(sequences[material], item.getSequenceIndex());
					assertFramesWithin(item, 72);
				}
			}
		}
	}

	@Test
	public void engineersHaveMaterialAndHandlingMappingsForEveryCivilisation() throws Exception {
		for (ECivilisation civilisation : ECivilisation.VALUES) {
			for (EMaterialType material : new EMaterialType[] { EMaterialType.NO_MATERIAL, EMaterialType.PLANK,
					EMaterialType.IRON, EMaterialType.STONE, EMaterialType.GOLD }) {
				for (EDirection direction : DIRECTIONS) {
					for (EMovableAction action : new EMovableAction[] { EMovableAction.NO_ACTION, EMovableAction.WALKING,
							EMovableAction.RAISE_UP, EMovableAction.BEND_DOWN }) {
						SettlerImageMapItem item = item(civilisation, EMovableType.SIEGE_ENGINEER, action, material, direction);
						assertFalse("Must not silently use the fallback bearer", item.getFile() == 10 && item.getSequenceIndex() == 0);
						assertNotEquals(0, item.getDuration());
					}
				}
			}
		}
	}

	private static void assertFramesWithin(SettlerImageMapItem item, int frameCount) {
		int last = item.getStart() + item.getDuration() - Integer.signum(item.getDuration());
		assertTrue(item.getStart() >= 0 && item.getStart() < frameCount);
		assertTrue("Frame " + last + " exceeds original sequence length " + frameCount, last >= 0 && last < frameCount);
	}

	private static SettlerImageMapItem item(ECivilisation civilisation, EMovableType type, EMovableAction action,
			EMaterialType material, EDirection direction) throws Exception {
		// Inspect the parsed map without initializing the proprietary image provider or adding production API for tests.
		Method lookup = SettlerImageMap.class.getDeclaredMethod("getMapItem", ECivilisation.class, EMovableType.class,
				EMovableAction.class, EMaterialType.class, EDirection.class);
		lookup.setAccessible(true);
		return (SettlerImageMapItem) lookup.invoke(SettlerImageMap.getInstance(), civilisation, type, action, material, direction);
	}
}
