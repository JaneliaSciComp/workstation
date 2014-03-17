/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.FlyWorkstation.gui.framework.console.nb_action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

@ActionID(
        category = "Help",
        id = "org.janelia.it.FlyWorkstation.gui.framework.console.nb_action.CallDeveloperMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_CallDeveloperMenuAction"
)
@ActionReference(path = "Menu/Help", position = 100)
@Messages("CTL_CallDeveloperMenuAction=Show Developer List")
public final class CallDeveloperMenuAction extends AbstractAction implements Presenter.Menu {

    private JMenu subMenu = new JMenu("Show Developer List");
    
    public CallDeveloperMenuAction() {
        subMenu.add(new JMenuItem("Call Christopher - x4662"));
        subMenu.add(new JMenuItem("Call Don    - x4656"));
        subMenu.add(new JMenuItem("Call Eric   - x4655"));
        subMenu.add(new JMenuItem("Call Konrad - x4242"));
        subMenu.add(new JMenuItem("Call Les    - x4680"));
        subMenu.add(new JMenuItem("Call Sean   - x4324"));
        subMenu.add(new JMenuItem("Call Todd   - x4696"));
        subMenu.add(new JMenuItem("Call Yang   - x4626"));
        
        for ( Component item: subMenu.getMenuComponents() ) {
            if ( item instanceof JMenuItem ) {
                JMenuItem menuItem = (JMenuItem)item;
                menuItem.addActionListener( this );
            }
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        new CallDeveloperDelegate().actOnCallDeveloper();
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return subMenu;
    }
}
