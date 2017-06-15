package org.janelia.it.workstation.browser.gui.support;

import java.awt.Insets;

import javax.swing.Icon;

import org.janelia.it.workstation.browser.gui.support.buttons.DropDownButton;

/**
 * A button with a popup menu. This wraps the Synthetica add-on button to add some sensible defaults.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WrappingDropDownButton extends DropDownButton {
	
	protected static final Insets BUTTON_INSETS = new Insets(0,2,0,2);

    public WrappingDropDownButton() {
        this(null);
    }
    
    public WrappingDropDownButton(String label) {
        this(null, label);
    }

    public WrappingDropDownButton(Icon icon, String label) {
        super(icon, new JScrollPopupMenu());
        setText(label);
        setFocusable(false);
        setMargin(BUTTON_INSETS);
        getScrollPopupMenu().setMaximumVisibleRows(20);
    }
    
    public JScrollPopupMenu getScrollPopupMenu() {
        return (JScrollPopupMenu)getPopupMenu();
    }

    public void setVisibleElements(int num) {
        getScrollPopupMenu().setMaximumVisibleRows(num);
    }
}
