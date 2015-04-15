package org.janelia.it.workstation.gui.browser.components.icongrid;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.components.viewer.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectIconGridViewer extends IconGridViewer<DomainObject> implements AnnotatedDomainObjectListViewer {
    
    private static final Logger log = LoggerFactory.getLogger(DomainObjectIconGridViewer.class);
    
    @Override
    protected void populateImageRoles(List<DomainObject> domainObjects) {
        Set<String> imageRoles = new HashSet<>();
        for(DomainObject domainObject : domainObjects) {
            if (domainObject instanceof HasFiles) {
                HasFiles hasFiles = (HasFiles)domainObject;
                for(FileType fileType : hasFiles.getFiles().keySet()) {
                    if (fileType.isIs2dImage()) {
                        imageRoles.add(fileType.name());
                    }
                }
            }
        }
        allImageRoles.clear();
        allImageRoles.addAll(imageRoles);
        Collections.sort(allImageRoles);
    }

    @Override
    public Object getImageUniqueId(DomainObject domainObject) {
        return domainObject.getId();
    }

    @Override
    public String getImageFilepath(DomainObject domainObject) {
        return getImageFilepath(domainObject, FileType.SignalMip.toString());
    }

    @Override
    public String getImageFilepath(DomainObject domainObject, String role) {
        // TODO: this needs to be generalized and user configurable
        Sample sample = (Sample)domainObject;
        if (sample==null) return null;
        List<String> objectives = sample.getOrderedObjectives();
        if (objectives==null) return null;
        ObjectiveSample objSample = sample.getObjectiveSample(objectives.get(objectives.size()-1));
        if (objSample==null) return null;
        SamplePipelineRun run = objSample.getLatestRun();
        if (run==null) return null;
        HasFiles lastResult = run.getLatestResultWithFiles();
        if (lastResult==null) return null;
        return DomainUtils.get2dImageFilepath(lastResult, role);
    }

    @Override
    public Object getImageLabel(DomainObject domainObject) {
        return domainObject.getName();
    }
    
    @Override
    protected void buttonDrillDown(AnnotatedImageButton button) {
    
        DomainObject domainObject = (DomainObject)button.getImageObject();
        
    }
    
    @Override
    public void showDomainObjects(AnnotatedDomainObjectList domainObjectList) {
        showImageObjects(domainObjectList.getDomainObjects());
        // TODO: set annotations?
    }

    @Override
    public JPanel getViewerPanel() {
        return this;
    }
}
