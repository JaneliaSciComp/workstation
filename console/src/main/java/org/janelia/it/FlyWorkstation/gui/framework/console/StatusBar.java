package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.shared.util.FreeMemoryWatcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class StatusBar extends JPanel {
    private FreeMemoryViewer freeMemoryViewer;
    private BoxLayout boxLayout = new BoxLayout(this, BoxLayout.X_AXIS);
    private JLabel label = new JLabel();
    private Component glue = Box.createHorizontalGlue();

    public StatusBar() {
        setLayout(boxLayout);
        add(label);
        add(glue);

        this.addComponentListener(new ComponentAdapter() {
            /**
             * Invoked when the component's size changes.
             */
            public void componentResized(ComponentEvent e) {
                resetPreferredSize();
                // This series of calls MUST BE DONE to ensure that
                // this widget is redrawn to reflect the new sizes
                // of its child widgets on screen.  Please do not
                // change this to "validate/repaint" or any other
                // combination, because it just does not work!  Maybe
                // because this method is always called AFTER the
                // container has been repainted.
                invalidate();
                validate();
                repaint();
            }

        });
    }

    public void setDescription(String description) {
        label.setText(description);
        label.setToolTipText(description);
        if (freeMemoryViewer != null) {
            resetPreferredSize();
        }
    }

    public void useFreeMemoryViewer(boolean use) {
        if (use) {
            if (freeMemoryViewer == null) freeMemoryViewer = new FreeMemoryViewer();
            FreeMemoryWatcher.getFreeMemoryWatcher().addObserver(freeMemoryViewer);
            add(freeMemoryViewer);
            validate();
        } else {
            if (freeMemoryViewer != null) {
                remove(freeMemoryViewer);
                FreeMemoryWatcher.getFreeMemoryWatcher().deleteObserver(freeMemoryViewer);
                freeMemoryViewer = null;
                label.setText(label.getText());
                this.repaint();
            }
        }
    }

    private void resetPreferredSize() {
        if (this.getParent() != null) {
            int height = this.getHeight();
            int width = this.getParent().getWidth() - freeMemoryViewer.getWidth();
            label.setPreferredSize(new Dimension(width, height));
        }
    }
}
