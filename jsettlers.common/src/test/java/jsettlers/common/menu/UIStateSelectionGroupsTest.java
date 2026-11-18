package jsettlers.common.menu;

import java.io.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class UIStateSelectionGroupsTest {
	@Test public void groupsAreDeepSnapshotsAndSurviveSerialization() throws Exception {
		UIState state = new UIState(10, 20, 1);
		Set<Integer> ids = new LinkedHashSet<>(Arrays.asList(4, 9));
		Map<Integer, Set<Integer>> groups = new HashMap<>();
		groups.put(1, ids);
		state.setSelectionGroups(groups);
		ids.clear();
		state.getSelectionGroups().get(1).clear();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(state); }
		UIState loaded;
		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			loaded = (UIState) in.readObject();
		}
		assertEquals(new LinkedHashSet<>(Arrays.asList(4, 9)), loaded.getSelectionGroups().get(1));
		assertEquals(10, loaded.getScreenCenterX(), 0);
		assertEquals(20, loaded.getScreenCenterY(), 0);
	}

	@Test public void absentLegacyGroupsAreEmptyAndInvalidSlotsAreIgnored() {
		UIState state = new UIState(10, 20, 1);
		assertTrue(state.getSelectionGroups().isEmpty());
		Map<Integer, Set<Integer>> groups = new HashMap<>();
		groups.put(0, Collections.singleton(1));
		groups.put(10, Collections.singleton(2));
		groups.put(3, null);
		state.setSelectionGroups(groups);
		assertTrue(state.getSelectionGroups().isEmpty());
	}
}
