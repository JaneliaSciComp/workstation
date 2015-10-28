package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.gui.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;
import org.janelia.it.workstation.gui.browser.model.DomainObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An IconGridViewer implementation for viewing domain objects. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectIconGridViewer extends IconGridViewerPanel<DomainObject,DomainObjectId> implements AnnotatedDomainObjectListViewer {
    
    private static final Logger log = LoggerFactory.getLogger(DomainObjectIconGridViewer.class);
    
    private final Map<DomainObjectId,DomainObject> domainObjectByUniqueId = new HashMap<>();
    
    private AnnotatedDomainObjectList domainObjectList;
    private DomainObjectSelectionModel selectionModel;
    
    private final ImageModel<DomainObject,DomainObjectId> imageModel = new ImageModel<DomainObject, DomainObjectId>() {
        
        @Override
        public DomainObjectId getImageUniqueId(DomainObject domainObject) {
            return DomainObjectId.createFor(domainObject);
        }

        @Override
        public String getImageFilepath(DomainObject domainObject) {
            return getImageFilepath(domainObject, FileType.SignalMip);
        }

        @Override
        public String getImageFilepath(DomainObject domainObject, FileType fileType) {
            // TODO: this needs to be generalized and user configurable
            if (domainObject instanceof Sample) {
                Sample sample = (Sample)domainObject;
                List<String> objectives = sample.getOrderedObjectives();
                if (objectives==null) return null;
                ObjectiveSample objSample = sample.getObjectiveSample(objectives.get(objectives.size()-1));
                if (objSample==null) return null;
                SamplePipelineRun run = objSample.getLatestRun();
                if (run==null) return null;
                HasFiles lastResult = run.getLatestResult();
                if (lastResult==null) return null;
                return DomainUtils.getFilepath(lastResult, fileType);
            }
            else if (domainObject instanceof HasFiles) {
                HasFiles hasFiles = (HasFiles)domainObject;
                return DomainUtils.getFilepath(hasFiles, fileType);
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
        public List<Annotation> getAnnotations(DomainObject domainObject) {
            return domainObjectList.getAnnotations(domainObject.getId());
        }
    };

    public DomainObjectIconGridViewer() {
        setImageModel(imageModel);
    }

    @Override
    public void setSelectionModel(DomainObjectSelectionModel selectionModel) {
        super.setSelectionModel(selectionModel);
        this.selectionModel = selectionModel;
    }
    
    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }
    
    @Override
    protected JPopupMenu getButtonPopupMenu() {
        List<DomainObjectId> selectionIds = selectionModel.getSelectedIds();
        List<DomainObject> domainObjects = new ArrayList<>();
        for (DomainObjectId id : selectionIds) {
            DomainObject imageObject = getImageModel().getImageByUniqueId(id);
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
    public void refreshDomainObject(DomainObject domainObject) {
        refreshImageObject(domainObject);
    }
    
    @Override
    protected void buttonDrillDown(DomainObject domainObject) {
    }
    
    @Override
    public void showDomainObjects(AnnotatedDomainObjectList domainObjectList) {
        
        this.domainObjectList = domainObjectList;
        showImageObjects(domainObjectList.getDomainObjects());
        
        domainObjectByUniqueId.clear();
        for(DomainObject domainObject : domainObjectList.getDomainObjects()) {
            domainObjectByUniqueId.put(DomainObjectId.createFor(domainObject), domainObject);
        }
    }

    @Override
    public void selectDomainObjects(List<DomainObject> domainObjects, boolean select, boolean clearAll) {
        if (domainObjects.isEmpty()) {
            return;
        }
        DomainObject first = domainObjects.get(0);
        selectDomainObject(first, select, clearAll);
        for(int i=1; i<domainObjects.size(); i++) {
            DomainObject domainObject = domainObjects.get(i);
            selectDomainObject(domainObject, select, false);
        }
    }
    
    public void selectDomainObject(DomainObject domainObject, boolean selected, boolean clearAll) {
        if (selected) {
            selectImageObject(domainObject, clearAll);
        }
        else {
            deselectImageObject(domainObject);
        }
    }
    
    @Override
    public JPanel getPanel() {
        return this;
    }
}
