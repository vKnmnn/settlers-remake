package go.graphics.event;

import java.util.EnumSet;
import go.graphics.event.command.EModifier;
import org.junit.Test;
import static org.junit.Assert.*;

public class GOKeyEventTest {
	@Test
	public void legacyConstructorHasNoModifiers() {
		GOKeyEvent event = new GOKeyEvent("1");
		assertFalse(event.isAltDown());
		assertFalse(event.isControlDown());
		assertFalse(event.isShiftDown());
	}

	@Test
	public void modifierSnapshotDoesNotChangeWhenKeysAreReleased() {
		EnumSet<EModifier> modifiers = EnumSet.allOf(EModifier.class);
		GOKeyEvent event = new GOKeyEvent("1", modifiers);
		modifiers.clear();
		assertTrue(event.isAltDown());
		assertTrue(event.isControlDown());
		assertTrue(event.isShiftDown());
	}
}
