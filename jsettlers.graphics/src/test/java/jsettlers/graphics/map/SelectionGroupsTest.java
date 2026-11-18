package jsettlers.graphics.map;

import java.lang.reflect.Proxy;
import java.util.*;
import go.graphics.event.GOKeyEvent;
import go.graphics.event.command.EModifier;
import jsettlers.common.action.SelectGroupAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IGraphicsMovable;
import jsettlers.common.player.IPlayer;
import jsettlers.common.selectable.ISelectionSet;
import org.junit.Test;
import static org.junit.Assert.*;

public class SelectionGroupsTest {
	@Test public void assigningIsExclusiveAndEmptySelectionDoesNotEraseGroup() {
		SelectionGroups groups = new SelectionGroups();
		groups.save(1, selection(unit(10, 0, EMovableType.SWORDSMAN_L1), unit(11, 0, EMovableType.SWORDSMAN_L1)), (byte) 0);
		groups.save(2, selection(unit(11, 0, EMovableType.SWORDSMAN_L1)), (byte) 0);
		assertEquals(Collections.singleton(10), groups.snapshot().get(1));
		groups.save(2, selection(), (byte) 0);
		assertEquals(Collections.singleton(11), groups.snapshot().get(2));
		assertTrue(groups.recall(2).isCenter());
		assertFalse(groups.recall(1).isCenter());
		assertTrue(groups.recall(1).isCenter());
		groups.selectionChanged();
		assertFalse(groups.recall(1).isCenter());
	}

	@Test public void storesOnlyOwnControllableUnitsAndSnapshotsAreIndependent() {
		SelectionGroups groups = new SelectionGroups();
		groups.save(3, selection(unit(1, 0, EMovableType.MAGE), unit(2, 0, EMovableType.GEOLOGIST),
				unit(3, 1, EMovableType.SWORDSMAN_L1), unit(4, 0, EMovableType.BEARER)), (byte) 0);
		Map<Integer, Set<Integer>> snapshot = groups.snapshot();
		assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2)), snapshot.get(3));
		snapshot.get(3).clear();
		assertEquals(2, groups.snapshot().get(3).size());
		Map<Integer, Set<Integer>> saved = groups.snapshot();
		SelectionGroups restored = new SelectionGroups();
		restored.restore(saved);
		saved.get(3).clear();
		assertEquals(Arrays.asList(1, 2), restored.recall(3).getIds());
		assertTrue(restored.recall(3).isCenter());
		assertTrue(restored.recall(9).getIds().isEmpty());
	}

	@Test public void altDigitSavesPlainDigitRecallsAndModifiersNeverRecallAccidentally() {
		SelectionGroups groups = new SelectionGroups();
		List<SelectGroupAction> actions = new ArrayList<>();
		ISelectionSet selection = selection(unit(12, 0, EMovableType.SWORDSMAN_L1));
		GOKeyEvent save = new GOKeyEvent("4", EnumSet.of(EModifier.ALT));
		assertTrue(groups.handleKey(save, () -> selection, (byte) 0, actions::add));
		save.started(); save.released();
		assertTrue(actions.isEmpty());
		GOKeyEvent recall = new GOKeyEvent("4");
		groups.handleKey(recall, () -> selection, (byte) 0, actions::add);
		recall.started(); recall.released();
		assertEquals(Collections.singletonList(12), actions.get(0).getIds());
		assertTrue(actions.get(0).isCenter());
		GOKeyEvent ctrl = new GOKeyEvent("4", EnumSet.of(EModifier.CTRL));
		assertTrue(groups.handleKey(ctrl, () -> selection, (byte) 0, actions::add));
		ctrl.started(); ctrl.released();
		assertEquals(1, actions.size());
		assertFalse(groups.handleKey(new GOKeyEvent("0"), () -> selection, (byte) 0, actions::add));
	}

	private static ISelectionSet selection(IGraphicsMovable... units) {
		return (ISelectionSet) Proxy.newProxyInstance(ISelectionSet.class.getClassLoader(), new Class<?>[]{ISelectionSet.class},
				(proxy, method, args) -> {
					if (method.getName().equals("iterator")) return Arrays.asList(units).iterator();
					throw new AssertionError(method.getName());
				});
	}

	private static IGraphicsMovable unit(int id, int owner, EMovableType type) {
		IPlayer player = (IPlayer) Proxy.newProxyInstance(IPlayer.class.getClassLoader(), new Class<?>[]{IPlayer.class},
				(proxy, method, args) -> (byte) owner);
		return (IGraphicsMovable) Proxy.newProxyInstance(IGraphicsMovable.class.getClassLoader(), new Class<?>[]{IGraphicsMovable.class},
				(proxy, method, args) -> {
					switch (method.getName()) {
						case "getID": return id;
						case "getPlayer": return player;
						case "getMovableType": return type;
						default: throw new AssertionError(method.getName());
					}
				});
	}
}
