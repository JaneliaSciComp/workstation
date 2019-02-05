package org.janelia.it.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;

/**
 * Export the unique GUIDs for the picked results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExportPickedGUIDs extends AbstractAction {

    protected static final String DEFAULT_EXPORT_DIR = System.getProperty("java.io.tmpdir");
    private static final String NEW_LINE = System.getProperty("line.separator");
    
    private List<Reference> refs;

    public ExportPickedGUIDs(List<Reference> refs) {
        this.refs = refs;
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {

        if (refs.isEmpty()) {
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), 
                    "Select some items with the checkboxes first.", 
                    "No items picked for export", JOptionPane.ERROR_MESSAGE);
        }
        
        ActivityLogHelper.logUserAction("ExportPickedGUIDs.doAction");

        File destFile = Utils.getOutputFile(DEFAULT_EXPORT_DIR, "exported_guids", "txt");

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                export(destFile);
            }

            @Override
            protected void hadSuccess() {
                OpenWithDefaultAppAction action = new OpenWithDefaultAppAction(destFile.getAbsolutePath());
                action.actionPerformed(null);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Exporting data...", ""));
        worker.execute();
    }
    
    private void export(File destFile) throws Exception {

        Set<Long> guids = new LinkedHashSet<>();
        for (DomainObject domainObject : DomainMgr.getDomainMgr().getModel().getDomainObjects(refs)) {
            guids.add(domainObject.getId());
        }
        
        try (FileWriter writer = new FileWriter(destFile)) {
            StringBuffer buf = new StringBuffer();
            for (Long guid : guids) {
                buf.append(guid);
                buf.append(NEW_LINE);
            }
            writer.write(buf.toString());
        }
    }
}
