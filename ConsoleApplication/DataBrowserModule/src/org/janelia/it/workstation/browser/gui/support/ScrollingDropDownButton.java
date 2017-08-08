package org.janelia.it.workstation.browser.gui.support;

import javax.swing.Icon;

import org.janelia.it.workstation.browser.gui.support.buttons.DropDownButton;

/**
 * Wraps the default drop down button with a scrolling menu.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScrollingDropDownButton extends DropDownButton {

    public ScrollingDropDownButton() {
        this(null);
    }
    
    public ScrollingDropDownButton(String label) {
        this(null, label);
    }

    public ScrollingDropDownButton(Icon icon, String label) {
        super(icon, new JScrollPopupMenu());
        setText(label);
        getScrollPopupMenu().setMaximumVisibleRows(40);
    }
    
    public JScrollPopupMenu getScrollPopupMenu() {
        return (JScrollPopupMenu)getPopupMenu();
    }

    public void setVisibleElements(int num) {
        getScrollPopupMenu().setMaximumVisibleRows(num);
    }
}
