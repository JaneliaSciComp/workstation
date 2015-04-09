package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;

// this action centers the 2d view on the annotation that is "selected"
public class CenterNextParentAction extends AbstractAction 
{

    private QuadViewUi ui;
    
    public CenterNextParentAction(QuadViewUi ui) {
        this.ui = ui;
        putValue(NAME, "Center next parent");
        String acc = "C";
        KeyStroke accelerator = KeyStroke.getKeyStroke(acc);
        putValue(ACCELERATOR_KEY, accelerator);
        putValue(SHORT_DESCRIPTION, 
                "<html>"
                + "Center the selected annotation in the 2D view ["+acc+"]"
                +"</html>");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) 
    {
        ui.centerNextParentMicron();
    }
}
