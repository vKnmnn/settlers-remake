package jsettlers.graphics.messages;

import jsettlers.common.menu.messages.IMessage;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.statistics.IGameTimeProvider;
import org.junit.Test;
import static org.junit.Assert.*;

public class MessengerTest {
    @Test public void repeatedStatusReplacesOldMessageAndRestartsLifetime() {
        Messenger messenger = new Messenger(IGameTimeProvider.DUMMY_IMPLEMENTATION);
        IMessage first = SimpleMessage.status("save_success");
        assertTrue(messenger.addMessage(first));
        first.ageBy((int) IMessage.MESSAGE_TTL - 1);
        IMessage repeated = SimpleMessage.status("save_success");
        assertTrue(messenger.addMessage(repeated));
        assertArrayEquals(new IMessage[] {repeated}, messenger.getMessages());
        assertEquals(0, repeated.getAge());
        assertNull(messenger.getPosition());
        assertTrue(first.duplicates(repeated));
        assertFalse(first.duplicates(SimpleMessage.status("save_failed")));
    }

    @Test public void regularDuplicateSuppressionStillWorks() {
        Messenger messenger = new Messenger(IGameTimeProvider.DUMMY_IMPLEMENTATION);
        assertTrue(messenger.addMessage(SimpleMessage.attacked((byte) 1, new ShortPoint2D(4, 4))));
        assertFalse(messenger.addMessage(SimpleMessage.attacked((byte) 1, new ShortPoint2D(4, 4))));
        assertEquals(1, messenger.getMessages().length);
    }

    @Test public void expirationRemovesSeveralMessagesWithoutModifyingActiveIterator() {
        Messenger messenger = new Messenger(IGameTimeProvider.DUMMY_IMPLEMENTATION);
        IMessage old = SimpleMessage.status("old");
        IMessage recent = SimpleMessage.status("recent");
        messenger.addMessage(old);
        messenger.addMessage(recent);
        old.ageBy((int) IMessage.MESSAGE_TTL + 1);
        recent.ageBy((int) IMessage.MESSAGE_TTL + 1);
        messenger.doTick();
        assertEquals(0, messenger.getMessages().length);
        assertNull(messenger.getPosition());
    }

    @Test public void messageArrayIsAnIndependentSnapshot() {
        Messenger messenger = new Messenger(IGameTimeProvider.DUMMY_IMPLEMENTATION);
        IMessage message = SimpleMessage.status("save_success");
        messenger.addMessage(message);
        IMessage[] snapshot = messenger.getMessages();
        snapshot[0] = null;
        assertSame(message, messenger.getMessages()[0]);
    }
}
