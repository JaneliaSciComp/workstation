package org.janelia.it.workstation.gui.browser.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Action to copy a named string to the clipboard. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CopyToClipboardAction implements NamedAction {

    private final String name;
    private final String value;

    public CopyToClipboardAction(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() {
        return "Copy "+name+" To Clipboard";
    }

    @Override
    public void doAction() {
        try {
            Transferable t = new StringSelection(value);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
}
