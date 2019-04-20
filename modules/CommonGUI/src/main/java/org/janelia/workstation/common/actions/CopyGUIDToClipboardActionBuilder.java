package org.janelia.workstation.common.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import org.janelia.model.domain.interfaces.HasIdentifier;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.SimpleActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * Action to copy a GUID to the clipboard. Supports any object which implements the HasIdentifier interface.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=3)
public class CopyGUIDToClipboardActionBuilder extends SimpleActionBuilder {

    @Override
    protected String getName() {
        return "Copy GUID To Clipboard";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof HasIdentifier;
    }

    @Override
    public boolean isSucceededBySeparator() {
        return true;
    }

    @Override
    protected void performAction(Object obj) {
        HasIdentifier domainObject = (HasIdentifier)obj;
        String value = domainObject.getId()+"";
        ActivityLogHelper.logUserAction("CopyGUIDToClipboardActionBuilder.performAction", value);
        Transferable t = new StringSelection(value);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
    }
}
