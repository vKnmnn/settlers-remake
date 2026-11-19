package go.graphics.swing.event.swingInterpreter;

import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class GOSwingEventConverterTest {
    @Test public void mouseEventsBeforeAttachmentAndAfterRemovalDoNotRequireGraphicsConfiguration() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel component = new JPanel();
            component.setSize(200, 100);
            assertNull(component.getGraphicsConfiguration());
            GOSwingEventConverter converter = new GOSwingEventConverter(component, event -> { });
            converter.mouseExited(event(component, MouseEvent.MOUSE_EXITED));
            converter.mouseEntered(event(component, MouseEvent.MOUSE_ENTERED));
            converter.mouseMoved(event(component, MouseEvent.MOUSE_MOVED));
            converter.mouseExited(event(component, MouseEvent.MOUSE_EXITED));
        });
    }
    private static MouseEvent event(JPanel component, int id) {
        return new MouseEvent(component, id, 0, 0, 12, 25, 0, false);
    }
}
