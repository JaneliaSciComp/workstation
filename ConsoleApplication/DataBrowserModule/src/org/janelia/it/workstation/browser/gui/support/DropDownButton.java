package org.janelia.it.workstation.browser.gui.support;

import java.awt.Insets;

import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

/**
 * A button with a popup menu. This wraps the Synthetica add-on button to add some sensible defaults.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DropDownButton extends JToggleButton {
	
	protected static final Insets BUTTON_INSETS = new Insets(0,2,0,2);
	
    private JPopupMenu popupMenu;

    public DropDownButton() {
        this(null);
    }
    
    public DropDownButton(String label) {
        super(label);
        setFocusable(false);
        setMargin(BUTTON_INSETS);
        popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);
        //popupMenu.setVisibleElements(20);
        //setPopupMenu(popupMenu);
    }

    public void setVisibleElements(int num) {
        //popupMenu.setVisibleElements(num);
    }

    public void setPopupMenu(JPopupMenu popupMenu2) {

    }
    
    public JPopupMenu getPopupMenu() {
        return new JPopupMenu();
    }
}
