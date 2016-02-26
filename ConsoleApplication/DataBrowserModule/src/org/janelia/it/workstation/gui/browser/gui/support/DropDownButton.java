package org.janelia.it.workstation.gui.browser.gui.support;

import de.javasoft.swing.JYPopupMenu;
import de.javasoft.swing.SimpleDropDownButton;

/**
 * A button with a popup menu. This wraps the Synthetica add-on button to add some sensible defaults.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DropDownButton extends SimpleDropDownButton {

    private JYPopupMenu popupMenu;

    public DropDownButton() {
        this(null);
    }
    
    public DropDownButton(String label) {
        super(label);
        popupMenu = new JYPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);
        popupMenu.setVisibleElements(20);
        setPopupMenu(popupMenu);
    }

    public void setVisibleElements(int num) {
        popupMenu.setVisibleElements(num);
    }
}
