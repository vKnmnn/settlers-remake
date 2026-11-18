package go.graphics.swing.event.swingInterpreter;

import java.awt.Canvas;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import go.graphics.event.GOEvent;
import go.graphics.event.GOKeyEvent;
import org.junit.Test;
import static org.junit.Assert.*;

public class GOSwingKeyModifiersTest {
	@Test
	public void altDigitIsDeliveredAndRemainsModifiedWhenAltIsReleasedFirst() {
		Canvas source = new Canvas();
		List<GOKeyEvent> events = new ArrayList<>();
		GOSwingEventConverter converter = new GOSwingEventConverter(source, event -> events.add((GOKeyEvent) event));
		converter.keyPressed(key(source, KeyEvent.KEY_PRESSED, KeyEvent.VK_ALT, InputEvent.ALT_DOWN_MASK));
		converter.keyPressed(key(source, KeyEvent.KEY_PRESSED, KeyEvent.VK_1, InputEvent.ALT_DOWN_MASK));
		assertEquals(1, events.size());
		GOKeyEvent digit = events.get(0);
		assertEquals("1", digit.getKeyCode());
		assertTrue(digit.isAltDown());
		converter.keyReleased(key(source, KeyEvent.KEY_RELEASED, KeyEvent.VK_ALT, 0));
		converter.keyReleased(key(source, KeyEvent.KEY_RELEASED, KeyEvent.VK_2, 0));
		assertEquals(GOEvent.PHASE_MODAL, digit.getPhase());
		converter.keyReleased(key(source, KeyEvent.KEY_RELEASED, KeyEvent.VK_1, 0));
		assertEquals(GOEvent.PHASE_FINISHED, digit.getPhase());
		assertTrue(digit.isAltDown());
		converter.keyPressed(key(source, KeyEvent.KEY_PRESSED, KeyEvent.VK_1, 0));
		assertEquals(2, events.size());
		assertFalse(events.get(1).isAltDown());
		converter.keyReleased(key(source, KeyEvent.KEY_RELEASED, KeyEvent.VK_1, 0));
	}

	@Test
	public void controlAndShiftDoNotConsumeTheDigitKeyEvent() {
		Canvas source = new Canvas();
		List<GOKeyEvent> events = new ArrayList<>();
		GOSwingEventConverter converter = new GOSwingEventConverter(source, event -> events.add((GOKeyEvent) event));
		int modifiers = InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;
		converter.keyPressed(key(source, KeyEvent.KEY_PRESSED, KeyEvent.VK_CONTROL, InputEvent.CTRL_DOWN_MASK));
		converter.keyPressed(key(source, KeyEvent.KEY_PRESSED, KeyEvent.VK_SHIFT, modifiers));
		converter.keyPressed(key(source, KeyEvent.KEY_PRESSED, KeyEvent.VK_9, modifiers));
		assertEquals(1, events.size());
		assertTrue(events.get(0).isControlDown());
		assertTrue(events.get(0).isShiftDown());
		assertFalse(events.get(0).isAltDown());
		converter.keyReleased(key(source, KeyEvent.KEY_RELEASED, KeyEvent.VK_9, modifiers));
	}

	private static KeyEvent key(Canvas source, int event, int key, int modifiers) {
		return new KeyEvent(source, event, System.currentTimeMillis(), modifiers, key, KeyEvent.CHAR_UNDEFINED);
	}
}
