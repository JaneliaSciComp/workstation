package org.janelia.workstation.gui.large_volume_viewer.launch;

import javax.swing.JOptionPane;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.SimpleActionBuilder;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;

/**
 * Right-click context menu that allows user to edit a TmSample file path.
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=1520)
public class EditSamplePath extends SimpleActionBuilder {

    @Override
    protected String getName() {
        return "Edit Sample Path";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof TmSample;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return obj != null && ClientDomainUtils.hasWriteAccess((DomainObject)obj);
    }

    @Override
    protected void performAction(Object obj) {

        final TmSample sample = (TmSample)obj;

        final String editedPath = (String) JOptionPane.showInputDialog(
                FrameworkAccess.getMainFrame(),
                "New Linux path to sample:",
                "Edit sample path",
                JOptionPane.PLAIN_MESSAGE,
                null, // icon
                null, // choice list
                sample.getFilepath()
        );
        if (editedPath == null || editedPath.length() == 0) {
            // canceled
            return;
        }
        else {
            SimpleWorker saver = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    sample.setFilepath(editedPath);
                    TiledMicroscopeDomainMgr.getDomainMgr().save(sample);
                }
                @Override
                protected void hadSuccess() {
                    // Handled by event system
                }
                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };
            saver.execute();
        }
    }
}
