package jsettlers.graphics.map;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.Consumer;
import go.graphics.event.GOKeyEvent;
import go.graphics.event.GOEvent;
import go.graphics.event.GOEventHandler;

import jsettlers.common.action.SelectGroupAction;
import jsettlers.common.movable.IIDable;
import jsettlers.common.movable.IGraphicsMovable;
import jsettlers.common.selectable.ISelectable;
import jsettlers.common.selectable.ISelectionSet;

/** Local numbered army selections. Only IDs are retained, never simulation objects. */
final class SelectionGroups {
	private final Map<Integer, Set<Integer>> groups = new LinkedHashMap<>();
	private int recalledGroup = -1;

	boolean handleKey(GOKeyEvent key, Supplier<ISelectionSet> selection, byte playerId,
			Consumer<SelectGroupAction> action) {
		String code = key.getKeyCode();
		if (code.length() != 1 || code.charAt(0) < '1' || code.charAt(0) > '9') return false;
		if (key.isControlDown() || key.isShiftDown()) return true;
		int number = code.charAt(0) - '0';
		key.setHandler(new GOEventHandler() {
			@Override public void phaseChanged(GOEvent event) { }
			@Override public void aborted(GOEvent event) { }
			@Override public void finished(GOEvent event) {
				if (key.isAltDown()) save(number, selection.get(), playerId);
				else action.accept(recall(number));
			}
		});
		return true;
	}

	synchronized void save(int number, ISelectionSet selection, byte playerId) {
		checkNumber(number);
		Set<Integer> ids = new LinkedHashSet<>();
		if (selection != null) for (ISelectable selectable : selection) {
			if (selectable instanceof IGraphicsMovable && ((IGraphicsMovable) selectable).getMovableType().playerControllable
					&& selectable.getPlayer().getPlayerId() == playerId) {
				ids.add(((IIDable) selectable).getID());
			}
		}
		if (!ids.isEmpty()) {
			groups.values().forEach(members -> members.removeAll(ids));
			groups.put(number, ids);
			recalledGroup = number;
		}
	}

	synchronized SelectGroupAction recall(int number) {
		checkNumber(number);
		Set<Integer> ids = groups.getOrDefault(number, Collections.emptySet());
		boolean center = recalledGroup == number;
		if (!ids.isEmpty()) recalledGroup = number;
		return new SelectGroupAction(ids, center);
	}

	synchronized void selectionChanged() {
		recalledGroup = -1;
	}

	synchronized Map<Integer, Set<Integer>> snapshot() {
		Map<Integer, Set<Integer>> copy = new LinkedHashMap<>();
		groups.forEach((number, ids) -> copy.put(number, new LinkedHashSet<>(ids)));
		return copy;
	}

	synchronized void restore(Map<Integer, Set<Integer>> saved) {
		groups.clear();
		recalledGroup = -1;
		if (saved != null) saved.forEach((number, ids) -> {
			if (number != null && number >= 1 && number <= 9 && ids != null) {
				Set<Integer> copy = new LinkedHashSet<>(ids);
				copy.remove(null);
				groups.put(number, copy);
			}
		});
	}

	private static void checkNumber(int number) {
		if (number < 1 || number > 9) throw new IllegalArgumentException("Group must be 1..9");
	}
}
