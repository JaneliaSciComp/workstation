package org.janelia.it.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.Sample;

/**
 * Export the unique line names for the picked results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExportPickedLineNames extends AbstractAction {

    protected static final String DEFAULT_EXPORT_DIR = System.getProperty("java.io.tmpdir");
    private static final String NEW_LINE = System.getProperty("line.separator");
    
    private List<Reference> refs;

    public ExportPickedLineNames(List<Reference> refs) {
        this.refs = refs;
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {

        if (refs.isEmpty()) {
            JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                    "Select some items with the checkboxes first.", 
                    "No items picked for export", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        ActivityLogHelper.logUserAction("ExportPickedLineNames.doAction");
        File destFile = Utils.getOutputFile(DEFAULT_EXPORT_DIR, "exported_lines", "txt");

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

        worker.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkImplProvider.getMainFrame(), "Exporting data...", ""));
        worker.execute();
    }
    
    private void export(File destFile) throws Exception {

        Set<String> lines = new LinkedHashSet<>();
        for (DomainObject domainObject : DomainMgr.getDomainMgr().getModel().getDomainObjects(refs)) {

            if (domainObject instanceof Sample) {
                Sample sample = (Sample)domainObject;
                lines.add(sample.getLine());
            }
            else if (domainObject instanceof LSMImage) {
                LSMImage image = (LSMImage)domainObject;
                lines.add(image.getLine());
            }
            else {
                lines.add("Item has no associated line: "+domainObject);
            }
        }
        
        try (FileWriter writer = new FileWriter(destFile)) {
            StringBuffer buf = new StringBuffer();
            for (String line : lines) {
                buf.append(line);
                buf.append(NEW_LINE);
            }
            writer.write(buf.toString());
        }
    }
}
