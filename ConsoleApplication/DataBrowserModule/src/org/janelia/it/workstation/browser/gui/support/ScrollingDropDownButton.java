package org.janelia.it.workstation.browser.gui.support;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import org.janelia.it.workstation.browser.gui.support.buttons.DropDownButton;

/**
 * Wraps the default drop down button with a scrolling menu.
 * 
 * TODO: We intent to use a scrolling menu here, but it's not working after the move to Darcula. Need to investigate this and fix this class.
 * It looks like the scroll bar works when the menu is created each time the button is pressed, as in OntologyExplorerTopComponent,
 * but it doesn't work here where the menu is persisted between clicks. 
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
        super(icon, new JPopupMenu());
        setText(label);
        
//        super(icon, new JScrollPopupMenu());
//        setText(label);
//        setVisibleElements(40);
    }
    
//    public JScrollPopupMenu getScrollPopupMenu() {
//        return (JScrollPopupMenu)getPopupMenu();
//    }
//
//    public void setVisibleElements(int num) {
//        getScrollPopupMenu().setMaximumVisibleRows(num);
//    }
}
