package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.workstation.gui.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.gui.listview.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectId;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectIconGridViewer extends IconGridViewerPanel<DomainObject,DomainObjectId> implements AnnotatedDomainObjectListViewer {
    
    private static final Logger log = LoggerFactory.getLogger(DomainObjectIconGridViewer.class);
    
    private final Map<DomainObjectId,DomainObject> domainObjectByUniqueId = new HashMap<>();
    
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
    public void setSelectionModel(DomainObjectSelectionModel selectionModel) {
        super.setSelectionModel(selectionModel);
    }
    
    @Override
    public DomainObjectId getImageUniqueId(DomainObject domainObject) {
        return DomainObjectId.createFor(domainObject);
    }

    @Override
    public String getImageFilepath(DomainObject domainObject) {
        String imageRole = getCurrImageRole();
        return getImageFilepath(domainObject, imageRole);
    }

    @Override
    public String getImageFilepath(DomainObject domainObject, String role) {
        // TODO: this needs to be generalized and user configurable
        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
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
        else if (domainObject instanceof HasFiles) {
            HasFiles hasFiles = (HasFiles)domainObject;
            return DomainUtils.get2dImageFilepath(hasFiles, role);
        }
        return null;
    }
    
    @Override
    public DomainObject getImageByUniqueId(DomainObjectId id) {
        return domainObjectByUniqueId.get(id);
    }
    
    @Override
    public Object getImageLabel(DomainObject domainObject) {
        return domainObject.getName();
    }
    
    @Override
    protected JPopupMenu getButtonPopupMenu() {
        List<DomainObjectId> selectionIds = selectionModel.getSelectedIds();
        List<DomainObject> domainObjects = new ArrayList<>();
        for (DomainObjectId id : selectionIds) {
            DomainObject imageObject = getImageByUniqueId(id);
            if (imageObject == null) {
                log.warn("Could not locate selected entity with id {}", id);
            }
            else {
                domainObjects.add(imageObject);
            }
        }
        JPopupMenu popupMenu = new DomainObjectContextMenu(domainObjects);
        ((DomainObjectContextMenu) popupMenu).addMenuItems();
        return popupMenu;
    }
    
    @Override
    protected void buttonDrillDown(AnnotatedImageButton button) {
        DomainObject domainObject = (DomainObject)button.getImageObject();
        // TODO: to we even drill down anymore?
    }
    
    @Override
    public void showDomainObjects(AnnotatedDomainObjectList domainObjectList) {
        showImageObjects(domainObjectList.getDomainObjects());
        // TODO: set annotations?
        
        domainObjectByUniqueId.clear();
        for(DomainObject domainObject : domainObjectList.getDomainObjects()) {
            domainObjectByUniqueId.put(DomainObjectId.createFor(domainObject), domainObject);
        }
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}
