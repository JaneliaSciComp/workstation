package org.janelia.horta;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.MouseInputListener;

/**
 * High-level mouse event dispatcher, so drag-vertex from TracingInteractor
 * can definitely override drag-world in OrbitPanZoomInteractor
 * @author brunsc
 */
public class HortaMouseEventDispatcher 
implements MouseInputListener
{
    private final List<MouseInputListener> listeners = new ArrayList<>();
    
    public HortaMouseEventDispatcher(
            MouseInputListener TracingInteractor,
            MouseInputListener WorldInteractor,
            MouseInputListener HortaInteractor)
    {
        // Use explicit ordering of event dispatch
        listeners.add(TracingInteractor);
        listeners.add(WorldInteractor);
        listeners.add(HortaInteractor);
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mouseClicked(event);
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mousePressed(event);
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mouseReleased(event);
        }
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mouseEntered(event);
        }
    }

    @Override
    public void mouseExited(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mouseExited(event);
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mouseDragged(event);
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        for (MouseInputListener listener : listeners) {
            if (event.isConsumed())
                return;
            listener.mouseMoved(event);
        }
    }
}
