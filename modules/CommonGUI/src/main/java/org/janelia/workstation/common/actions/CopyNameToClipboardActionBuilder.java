package org.janelia.workstation.common.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import org.janelia.model.domain.interfaces.HasName;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.SimpleActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * Builds action to copy a domain object name to the clipboard.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=2)
public class CopyNameToClipboardActionBuilder extends SimpleActionBuilder {

    @Override
    protected String getName() {
        return "Copy Name To Clipboard";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof HasName;
    }

    @Override
    protected void performAction(Object obj) {
        HasName domainObject = (HasName)obj;
        String value = domainObject.getName();
        ActivityLogHelper.logUserAction("CopyNameToClipboardActionBuilder.performAction", value);
        Transferable t = new StringSelection(value);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
    }
}
