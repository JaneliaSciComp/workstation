package org.janelia.horta.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import org.janelia.horta.NeuronTracerTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Horta",
        id = "org.janelia.horta.actions.AddUnique1NoteAction"
)
@ActionRegistration(
        displayName = "#CTL_AddUnique1Note",
        lazy = true
)
@Messages("CTL_AddUnique1Note=Add Unique 1 Note")
// Based on example at http://wiki.netbeans.org/DevFaqActionNodePopupSubmenu
public final class AddUnique1NoteAction
extends AbstractAction
implements ActionListener
{
    private final NeuronTracerTopComponent context;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    // Netbeans magically enables this menu Action when the current Lookup
    // contains a NeuronTracerTopComponent
    public AddUnique1NoteAction(NeuronTracerTopComponent horta) {
        context = horta;
        putValue(NAME, Bundle.CTL_AddUnique1Note());
        // Repeat key shortcut, so it could appear on the Horta context menu item
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.CTRL_DOWN_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        context.addUnique1Note();
    }
}
