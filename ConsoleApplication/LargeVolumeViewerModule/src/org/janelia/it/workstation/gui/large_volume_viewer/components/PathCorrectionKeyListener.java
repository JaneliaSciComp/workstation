package org.janelia.it.workstation.gui.large_volume_viewer.components;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JTextField;

/**
 * Pre-adjusts backslashes to forward slashes.  Helpful for correcting paths
 * as usrs type them.
 * 
 * @author fosterl
 */
public class PathCorrectionKeyListener implements KeyListener {

    private JTextField pathTextField;

    public PathCorrectionKeyListener(JTextField pathTextField) {
        this.pathTextField = pathTextField;
    }

    private PathCorrectionKeyListener() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        char keyChar = e.getKeyChar();
        if (keyChar == '\\') {
                // Trim the backslash off the end, and add back
            // a front-slash.
            pathTextField.setText(
                    pathTextField.getText().substring(0, pathTextField.getText().length() - 1) + '/'
            );
        }
    }

}
