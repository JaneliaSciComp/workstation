/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.FlyWorkstation.gui.framework.console.nb_action;

import javax.swing.JFrame;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.openide.windows.WindowManager;

/**
 *
 * @author fosterl
 */
public class EditingActionDelegate {
    public void establishPrefController(String prefLevel) {
        JFrame parent = (JFrame)WindowManager.getDefault().getMainWindow();
        parent.repaint();  // Derived from original, proprietary impl.
        PrefController.getPrefController().getPrefInterface(prefLevel, parent);
    }

}
