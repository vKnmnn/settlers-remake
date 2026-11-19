package jsettlers.graphics.map.controls.original.panel.content.material.priorities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jsettlers.common.material.EMaterialType;
import jsettlers.graphics.map.controls.original.panel.content.material.priorities.MaterialPriorityContent.MaterialPriorityPanel;
import org.junit.Test;

public class MaterialPriorityPanelTest {
	@Test
	public void everyMaterialHasItsOwnVisibleSlot() throws Exception {
		MaterialPriorityPanel panel = new MaterialPriorityPanel();
		panel.setOrder(EMaterialType.DROPPABLE_MATERIALS);
		Field field = MaterialPriorityPanel.class.getDeclaredField("positions");
		field.setAccessible(true);
		AnimatablePosition[] positions = (AnimatablePosition[]) field.get(panel);
		Set<String> slots = new HashSet<>();
		for (EMaterialType material : EMaterialType.DROPPABLE_MATERIALS) {
			AnimatablePosition pos = positions[material.ordinal];
			assertTrue(material + " outside the priority panel", pos.getX() >= 0 && pos.getX() < 1
					&& pos.getY() >= 0 && pos.getY() < 1);
			assertTrue(material + " overlaps another priority slot", slots.add(pos.getX() + ":" + pos.getY()));
		}
	}

	@Test
	public void repeatedReorderingNeverLosesOrDuplicatesGoods() {
		MaterialPriorityPanel panel = new MaterialPriorityPanel();
		panel.setOrder(EMaterialType.DROPPABLE_MATERIALS);
		Set<EMaterialType> expected = new HashSet<>(Arrays.asList(EMaterialType.DROPPABLE_MATERIALS));
		for (int destination : new int[] { -100, 100, 0, 17, 33, 1 }) {
			for (EMaterialType material : EMaterialType.DROPPABLE_MATERIALS) {
				EMaterialType[] reordered = panel.reorder(material, destination);
				assertEquals(expected.size(), reordered.length);
				assertEquals(expected, new HashSet<>(Arrays.asList(reordered)));
				panel.setOrder(reordered);
				assertEquals(Math.max(0, Math.min(destination, reordered.length - 1)), panel.indexOf(material));
			}
		}
	}
}
