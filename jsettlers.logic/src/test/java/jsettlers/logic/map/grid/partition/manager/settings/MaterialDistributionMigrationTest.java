package jsettlers.logic.map.grid.partition.manager.settings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.ECivilisation;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaterialDistributionMigrationTest {
	@Test public void oldSaveRetainsPreferencesAndEnablesNewConsumers() throws Exception {
		MaterialDistributionSettings original = new MaterialDistributionSettings(EMaterialType.IRON, ECivilisation.EGYPTIAN);
		original.setUserConfiguredDistributionValue(EBuildingType.WEAPONSMITH, 0.25f);
		original.setUserConfiguredDistributionValue(EBuildingType.TOOLSMITH, 0f);
		original.setUserConfiguredDistributionValue(EBuildingType.DOCKYARD, 0.75f);
		makeLegacy(original);
		MaterialDistributionSettings loaded = roundTrip(original);
		assertEquals(0.25f, loaded.getUserConfiguredDistributionValue(EBuildingType.WEAPONSMITH), 0f);
		assertEquals(0f, loaded.getUserConfiguredDistributionValue(EBuildingType.TOOLSMITH), 0f);
		assertEquals(1f, loaded.getUserConfiguredDistributionValue(EBuildingType.BALLISTA_WORKSHOP), 0f);
		assertEquals(0.5f, loaded.getDistributionProbability(EBuildingType.BALLISTA_WORKSHOP), 0f);
		assertEquals(0f, loaded.getUserConfiguredDistributionValue(EBuildingType.CANNON_WORKSHOP), 0f);
		loaded.setUserConfiguredDistributionValue(EBuildingType.BALLISTA_WORKSHOP, 0f);
		assertEquals(0f, roundTrip(loaded).getUserConfiguredDistributionValue(EBuildingType.BALLISTA_WORKSHOP), 0f);
	}

	@Test public void allMaterialAndCivilizationTablesExpandWithoutInvalidConsumers() throws Exception {
		for (ECivilisation civilisation : ECivilisation.VALUES) {
			for (EMaterialType material : EMaterialType.VALUES) {
				MaterialDistributionSettings original = new MaterialDistributionSettings(material, civilisation);
				makeLegacy(original);
				MaterialDistributionSettings loaded = roundTrip(original);
				for (EBuildingType building : EBuildingType.VALUES) {
					float expected = Arrays.asList(loaded.getBuildingTypes()).contains(building) ? 1f : 0f;
					assertEquals(civilisation + " " + material + " " + building, expected, loaded.getUserConfiguredDistributionValue(building), 0f);
				}
			}
		}
	}

	/** Reproduce the pre-siege serialized field shape without requiring a binary save fixture. */
	private static void makeLegacy(MaterialDistributionSettings settings) throws Exception {
		int oldCount = EBuildingType.POWDER_MAKER.ordinal;
		for (EBuildingType building : EBuildingType.VALUES) {
			if (building.ordinal >= oldCount) settings.setUserConfiguredDistributionValue(building, 0f);
		}
		Field relativeField = MaterialDistributionSettings.class.getDeclaredField("distributionSettings");
		relativeField.setAccessible(true);
		Object relative = relativeField.get(settings);
		Field values = RelativeSettings.class.getDeclaredField("userValues");
		values.setAccessible(true);
		values.set(relative, Arrays.copyOf((float[]) values.get(relative), oldCount));
	}

	private static MaterialDistributionSettings roundTrip(MaterialDistributionSettings value) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(value); }
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			return (MaterialDistributionSettings) input.readObject();
		}
	}
}
