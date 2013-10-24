package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmAnchoredPathEndpoints;

import java.util.Set;
import java.util.HashSet;

import javax.swing.*;


/**
 * this class displays the current status of the automatic tracing in text in the UI; it's
 * a stopgap; eventually we'll show the status via the style (color, etc) of the drawn line
 *
 * User: olbrisd
 * Date: 10/24/13
 * Time: 10:19 AM
 */
public class PathTracingStatusPanel extends JPanel {
    private Set<TmAnchoredPathEndpoints> inProgressPaths = new HashSet<TmAnchoredPathEndpoints>();
    private JLabel statusLabel;


    PathTracingStatusPanel() {

        // set layout, pack label
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        statusLabel = new JLabel("", JLabel.LEADING);
        add(statusLabel);

        updateLabel();

    }

    public void startTracing(TmAnchoredPathEndpoints endpoints) {
        if (!inProgressPaths.contains(endpoints)) {
            inProgressPaths.add(endpoints);
            updateLabel();
        }
    }

    public void stopTracing(TmAnchoredPathEndpoints endpoints) {
        if (inProgressPaths.contains(endpoints)) {
            inProgressPaths.remove(endpoints);
            updateLabel();
        }
    }

    private void updateLabel() {
        int nPaths = inProgressPaths.size();
        if (nPaths == 0) {
            statusLabel.setText("Path tracing: idle");
        } else if (nPaths == 1) {
            statusLabel.setText(String.format("Path tracing: 1 path"));
        } else {
            statusLabel.setText(String.format("Path tracing: %d paths", nPaths));
        }
    }
}
