package org.janelia.workstation.common.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;

/**
 * Action to copy a named string to the clipboard. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CopyToClipboardAction extends AbstractAction implements ContextualActionBuilder {

    private final String value;

    public CopyToClipboardAction(String name, String value) {
        super("Copy "+name+" To Clipboard");
        this.value = value;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
    }

//    @Override
//    public String getName() {
//        return null;
//    }

    @Override
    public boolean isCompatible(Object obj) {
        return false;
    }

    @Override
    public Action getAction(Object obj) {
        return null;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return null;
    }

    //    @Override
//    public void acceptObject(Object obj) {
//        try {
//            ActivityLogHelper.logUserAction("CopyToClipboardAction.doAction", value);
//            Transferable t = new StringSelection(value);
//            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
//        }
//        catch (Exception e) {
//            FrameworkAccess.handleException(e);
//        }
//    }
}
