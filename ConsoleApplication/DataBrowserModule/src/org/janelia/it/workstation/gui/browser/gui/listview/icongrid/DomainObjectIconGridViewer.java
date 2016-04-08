package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.actions.AnnotationContextMenu;
import org.janelia.it.workstation.gui.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.gui.browser.actions.RemoveItemsFromObjectSetAction;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.it.workstation.gui.browser.gui.hud.Hud;
import org.janelia.it.workstation.gui.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.gui.browser.gui.listview.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.gui.browser.gui.support.ImageTypeSelectionButton;
import org.janelia.it.workstation.gui.browser.gui.support.ResultSelectionButton;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;
import org.janelia.it.workstation.gui.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An IconGridViewer implementation for viewing domain objects. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectIconGridViewer extends IconGridViewerPanel<DomainObject,Reference> implements AnnotatedDomainObjectListViewer {
    
    private static final Logger log = LoggerFactory.getLogger(DomainObjectIconGridViewer.class);

    private ResultSelectionButton resultButton;
    private ImageTypeSelectionButton typeButton;
    
    private AnnotatedDomainObjectList domainObjectList;
    private DomainObjectSelectionModel selectionModel;
    
    private final ImageModel<DomainObject,Reference> imageModel = new ImageModel<DomainObject, Reference>() {
        
        @Override
        public Reference getImageUniqueId(DomainObject domainObject) {
            return Reference.createFor(domainObject);
        }

        @Override
        public String getImageFilepath(DomainObject domainObject) {
            HasFiles result = null;
            if (domainObject instanceof Sample) {
                Sample sample = (Sample)domainObject;
                result = DomainModelViewUtils.getResult(sample, resultButton.getResultDescriptor());
            }
            else if (domainObject instanceof HasFiles) {
                result = (HasFiles)domainObject;
            }
            return result==null? null : DomainUtils.getFilepath(result, typeButton.getImageType());
        }
        
        @Override
        public DomainObject getImageByUniqueId(Reference id) {
            return DomainMgr.getDomainMgr().getModel().getDomainObject(id);
        }
        
        @Override
        public String getImageLabel(DomainObject domainObject) {
            return domainObject.getName();
        }
        
        @Override
        public List<Annotation> getAnnotations(DomainObject domainObject) {
            return domainObjectList.getAnnotations(domainObject.getId());
        }
    };

    public DomainObjectIconGridViewer() {
        setImageModel(imageModel);
        resultButton = new ResultSelectionButton() {
            @Override
            protected void resultChanged(ResultDescriptor resultDescriptor) {
                setPreference(DomainConstants.PREFERENCE_CATEGORY_SAMPLE_RESULT, resultDescriptor.toString());
            }
        };
        typeButton = new ImageTypeSelectionButton() {
            @Override
            protected void imageTypeChanged(String typeName) {
                setPreference(DomainConstants.PREFERENCE_CATEGORY_IMAGE_TYPE, typeName);
            }
        };
        getToolbar().addCustomComponent(resultButton);
        getToolbar().addCustomComponent(typeButton);
    }

    private void setPreference(final String name, final String value) {

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                
                final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
                if (parentObject.getId()!=null) {
                    Preference preference = DomainMgr.getDomainMgr().getPreference(name, parentObject.getId().toString());
                    if (preference==null) {
                        preference = new Preference(AccessManager.getSubjectKey(), name, parentObject.getId().toString(), value);
                    }
                    else {
                        preference.setValue(value);
                    }
                    DomainMgr.getDomainMgr().savePreference(preference);
                }
                // TODO: If the parent object has not been persisted, the preferences will not get saved here. They should be saved when the object is persisted.
            }

            @Override
            protected void hadSuccess() {
                showDomainObjects(domainObjectList, null);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }
    
    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void setSearchProvider(SearchProvider searchProvider) {
        super.setSearchProvider(searchProvider);
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
    public void selectDomainObjects(List<DomainObject> domainObjects, boolean select, boolean clearAll) {
        log.info("selectDomainObjects(domainObjects.size={},select={},clearAll={})",domainObjects.size(),select,clearAll);
        
        if (domainObjects.isEmpty()) {
            return;
        }

        Set<Reference> selectedIds = new HashSet<>();

        if (select) {
            selectObjects(domainObjects, clearAll);
        }
        else {
            deselectObjects(domainObjects);
        }
        
//        boolean currClearAll = clearAll;
//        for(DomainObject domainObject : domainObjects) {
//            Reference id = getImageModel().getImageUniqueId(domainObject);
//            if (select && getObjectMap().get(id)!=null) {
//                selectObject(domainObject, currClearAll);
//                selectedIds.add(id);
//            }
//            currClearAll = false;
//        }
//
//        if (clearAll) {
//            // Clear out everything that was not selected above
//            for(Reference selectedId : new ArrayList<>(selectionModel.getSelectedIds())) {
//                if (!selectedIds.contains(selectedId)) {
//                    deselectObject(imageModel.getImageByUniqueId(selectedId));
//                }
//            }
//        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrollSelectedObjectsToCenter();
            }
        });   
    }

    @Override
    public void showDomainObjects(AnnotatedDomainObjectList objects, final Callable<Void> success) {

        this.domainObjectList = objects;
        log.debug("showDomainObjects(domainObjectList.size={})",domainObjectList.getDomainObjects().size());
        
        final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
        if (parentObject!=null && parentObject.getId()!=null) {
            Preference preference = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_SAMPLE_RESULT, parentObject.getId().toString());
            if (preference!=null) {
                resultButton.setResultDescriptor(new ResultDescriptor(preference.getValue()));
            }
            Preference preference2 = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_IMAGE_TYPE, parentObject.getId().toString());
            if (preference2!=null) {
                typeButton.setImageType(preference2.getValue());
            }
        }
        
        resultButton.populate(objects.getDomainObjects());
        typeButton.setResultDescriptor(resultButton.getResultDescriptor());
        typeButton.populate(objects.getDomainObjects());
                
        showObjects(domainObjectList.getDomainObjects(), success);
    }
    
    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    @Override
    public void refreshDomainObject(DomainObject domainObject) {
        refreshObject(domainObject);
    }

    @Override
    protected DomainObjectContextMenu getContextualPopupMenu() {
        DomainObjectContextMenu popupMenu = new DomainObjectContextMenu((DomainObject)selectionModel.getParentObject(), getSelectedObjects(), resultButton.getResultDescriptor(), typeButton.getImageType());
        popupMenu.addMenuItems();
        return popupMenu;
    }

    @Override
    protected JPopupMenu getAnnotationPopupMenu(Annotation annotation) {
        AnnotationContextMenu menu = new AnnotationContextMenu(annotation, getSelectedObjects(), imageModel);
        menu.addMenuItems();
        return menu;
    }

    @Override
    protected void moreAnnotationsButtonDoubleClicked(DomainObject domainObject) {
        new DomainDetailsDialog().showForDomainObject(domainObject, DomainInspectorPanel.TAB_NAME_ANNOTATIONS);
    }
    
    @Override
    protected void buttonDrillDown(DomainObject domainObject) {
        getContextualPopupMenu().runDefaultAction();
    }
    
    @Override
    protected void deleteKeyPressed() {
        IsParent parent = selectionModel.getParentObject();
        if (parent instanceof ObjectSet) {
            ObjectSet objectSet = (ObjectSet)parent; 
            if (ClientDomainUtils.hasWriteAccess(objectSet)) {                
                List<DomainObject> selectedObjects = DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
                RemoveItemsFromObjectSetAction action = new RemoveItemsFromObjectSetAction(objectSet, selectedObjects);
                action.doAction();
            }
        }
    }
    
    @Override
    protected void updateHud(boolean toggle) {

        Hud hud = Hud.getSingletonInstance();
        hud.setKeyListener(keyListener);
        
        List<DomainObject> selected = getSelectedObjects();
        
        if (selected.size() != 1) {
            hud.hideDialog();
            return;
        }
        
        DomainObject domainObject = selected.get(0);
        if (toggle) {
            hud.setObjectAndToggleDialog(domainObject, resultButton.getResultDescriptor(), typeButton.getImageType());
        }
        else {
            hud.setObject(domainObject, resultButton.getResultDescriptor(), typeButton.getImageType(), false);
        }
    }
    
    private List<DomainObject> getSelectedObjects() {
        return DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
    }
}
