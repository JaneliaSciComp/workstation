package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.janelia.model.access.domain.SampleUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Export the unique line names for the picked results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExportPickedToSplitGenWebsite extends AbstractAction {

    private final static Logger log = LoggerFactory.getLogger(ExportPickedToSplitGenWebsite.class);

    private List<Reference> refs;

    public ExportPickedToSplitGenWebsite(List<Reference> refs) {
        this.refs = refs;
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {

        if (refs.isEmpty()) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "Select some items with the checkboxes first.", 
                    "No items picked for export", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String defaultExportDir = System.getProperty("java.io.tmpdir");
        String splitgenUrl = ConsoleProperties.getInstance().getProperty("splitgen.url");

        if (splitgenUrl==null) {
            JOptionPane.showMessageDialog(
                    FrameworkAccess.getMainFrame(),
                    "No split generation website has been configured.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    null
            );
            return;
        }

        ActivityLogHelper.logUserAction("ExportPickedToSplitGenWebsite.doAction");
        File destFile = Utils.getOutputFile(defaultExportDir, "exported_lines", "txt");

        SimpleWorker worker = new SimpleWorker() {

            private String idStr;
            
            @Override
            protected void doStuff() throws Exception {
                this.idStr = export(destFile);
            }

            @Override
            protected void hadSuccess() {
                if (StringUtils.isBlank(idStr)) {
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                            "No split line identifiers were found", 
                            "Identifiers missing", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    // Create the URL to open 
                    URI uri = UriBuilder.fromPath(splitgenUrl)
                            .path("uname").path(AccessManager.getSubjectName())
                            .path("lnames").path(idStr)
                            .build();
                    
                    // Send the user to the website
                    log.info("Open splitgen website: {}", uri);
                    Utils.openUrlInBrowser(uri.toString());
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(), "Exporting data...", ""));
        worker.execute();
    }
    
    private String export(File destFile) throws Exception {
      
        Set<String> ids = new LinkedHashSet<>();
        for (DomainObject domainObject : DomainMgr.getDomainMgr().getModel().getDomainObjects(refs)) {
            if (domainObject instanceof Sample) {
                Sample sample = (Sample)domainObject;
                String id = getSplitIdentifier(sample);
                if (id != null) {
                    ids.add(id);
                }
            }
            else if (domainObject instanceof LSMImage) {
                LSMImage image = (LSMImage)domainObject;
                String id = getSplitIdentifier(image);
                if (id != null) {
                    ids.add(id);
                }
            }
            else {
                throw new IllegalArgumentException("Cannot export "+domainObject.getType()+" to splitgen website");
            }
        }
        
        // Collect the user-selected split line ids to send to the website
        StringBuilder idStr = new StringBuilder();
        for(String id : ids) {
            if (idStr.length()>0) idStr.append(' ');
            idStr.append(id);
        }
        
        return idStr.toString();
          
    }

    /**
     * The split generation website doesn't accept regular line names. For VT lines, we
     * need to strip off the "VT" portion. For regular line names, we just need the plate and well.
     * @param sample 
     * @return split identifier suitable for usage with the split gen website
     */
    private String getSplitIdentifier(Sample sample) {
        String vtLine = sample.getVtLine();
        if (vtLine!=null) {
            return vtLine.replaceFirst("VT", "");
        }
        else {
            if (sample.getLine()==null) return null;
            return SampleUtils.getPlateWellFromLineName(sample.getLine());
        }
    }

    private String getSplitIdentifier(LSMImage image) {
        String vtLine = image.getVtLine();
        if (vtLine!=null) {
            return vtLine.replaceFirst("VT", "");
        }
        else {
            if (image.getLine()==null) return null;
            return SampleUtils.getPlateWellFromLineName(image.getLine());
        }
    }
}
