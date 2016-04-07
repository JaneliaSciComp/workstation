package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.Insets;

import de.javasoft.swing.JYPopupMenu;
import de.javasoft.swing.SimpleDropDownButton;

/**
 * A button with a popup menu. This wraps the Synthetica add-on button to add some sensible defaults.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DropDownButton extends SimpleDropDownButton {
	
	private static final Insets BUTTON_INSETS = new Insets(0,2,0,2);
	
    private JYPopupMenu popupMenu;

    public DropDownButton() {
        this(null);
    }
    
    public DropDownButton(String label) {
        super(label);
        setFocusable(false);
        setMargin(BUTTON_INSETS);
        popupMenu = new JYPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);
        popupMenu.setVisibleElements(20);
        setPopupMenu(popupMenu);
    }

    public void setVisibleElements(int num) {
        popupMenu.setVisibleElements(num);
    }
}
