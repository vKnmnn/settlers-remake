package jsettlers.algorithms.simplebehaviortree;

import java.io.*;
import org.junit.Test;
import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;
import static org.junit.Assert.*;

public class TickMigrationTest {
    @Test public void migrationConsumesObsoleteNodeIdsAndRestartsWithoutTouchingFollowingData() throws Exception {
        Root<Object> oldRoot = new Root<>(sequence(alwaysSucceed(), alwaysSucceed(), alwaysRunning()));
        Tick<Object> oldTick = new Tick<>(new Object(), oldRoot);
        assertEquals(NodeStatus.RUNNING, oldTick.tick());
        oldTick.setProperty(42, "legacy job");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            oldTick.serialize(out);
            out.writeUTF("next object");
        }
        Root<Object> changedRoot = new Root<>(alwaysSucceed());
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            Tick<Object> migrated = Tick.deserialize(in, new Object(), changedRoot, true);
            assertNull(migrated.getProperty(42));
            assertEquals(NodeStatus.SUCCESS, migrated.tick());
            assertEquals("next object", in.readUTF());
        }
    }
}
