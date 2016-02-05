package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.actions.AnnotationContextMenu;
import org.janelia.it.workstation.gui.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.gui.browser.actions.RemoveItemsFromObjectSetAction;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;
import org.janelia.it.workstation.gui.browser.model.DomainConstants;
import org.janelia.it.workstation.gui.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

/**
 * An IconGridViewer implementation for viewing domain objects. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectIconGridViewer extends IconGridViewerPanel<DomainObject,Reference> implements AnnotatedDomainObjectListViewer {
    
    private static final Logger log = LoggerFactory.getLogger(DomainObjectIconGridViewer.class);
    
    private AnnotatedDomainObjectList domainObjectList;
    private DomainObjectSelectionModel selectionModel;
    
    private ResultDescriptor currResult;
    private String currImage2dType;
    
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
                result = DomainModelViewUtils.getResult(sample, currResult);
            }
            else if (domainObject instanceof HasFiles) {
                result = (HasFiles)domainObject;
            }
            return result==null? null : DomainUtils.getFilepath(result, currImage2dType);
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
        
        boolean currClearAll = clearAll;
        for(DomainObject domainObject : domainObjects) {
            Reference id = getImageModel().getImageUniqueId(domainObject);
            if (select && getObjectMap().get(id)!=null) {
                selectObject(domainObject, currClearAll);
                selectedIds.add(id);
            }
            currClearAll = false;
        }

        if (clearAll) {
            // Clear out everything that was not selected above
            for(Reference selectedId : new ArrayList<>(selectionModel.getSelectedIds())) {
                if (!selectedIds.contains(selectedId)) {
                    deselectObject(imageModel.getImageByUniqueId(selectedId));
                }
            }
        }
        
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
        this.currResult = null;
        this.currImage2dType = null;
        log.debug("showDomainObjects(domainObjectList.size={})",domainObjectList.getDomainObjects().size());
        
        final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
        if (parentObject!=null && parentObject.getId()!=null) {
            Preference preference = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_DEFAULT_SAMPLE_RESULT, parentObject.getId().toString());
            if (preference!=null) {
                this.currResult = new ResultDescriptor(preference.getValue());
            }
            Preference preference2 = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_DEFAULT_IMAGE_TYPE, parentObject.getId().toString());
            if (preference2!=null) {
                this.currImage2dType = preference2.getValue();
            }
        }
        
        if (currResult == null) {
            this.currResult = new ResultDescriptor(DomainConstants.PREFERENCE_VALUE_LATEST);
        }
        
        Multiset<String> countedTypeNames = LinkedHashMultiset.create();
        Multiset<String> countedResultNames = LinkedHashMultiset.create();
        // Add twice so that it is selected by >1 filter below
        countedResultNames.add(DomainConstants.PREFERENCE_VALUE_LATEST);
        countedResultNames.add(DomainConstants.PREFERENCE_VALUE_LATEST);
            
        for(DomainObject domainObject : domainObjectList.getDomainObjects()) {
            if (domainObject instanceof Sample) {
                Sample sample = (Sample)domainObject;
                for(String objective : sample.getOrderedObjectives()) {
                    ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
                    SamplePipelineRun run = objectiveSample.getLatestRun();
                    if (run==null || run.getResults()==null) continue;
                    for(PipelineResult result : run.getResults()) {
                        if (result instanceof HasFileGroups) {
                            countedTypeNames.addAll(get2dTypeNames((HasFileGroups)result));
                            HasFileGroups hasGroups = (HasFileGroups)result;
                            for(String groupKey : hasGroups.getGroupKeys()) {
                                String name = objective+" "+result.getName()+" ("+groupKey+")";
                                countedResultNames.add(name);
                            }
                        }
                        else {
                            String name = objective+" "+result.getName();
                            countedResultNames.add(name);
                            countedTypeNames.addAll(get2dTypeNames(result));
                        }
                    }
                }
            }
            else if (domainObject instanceof HasFileGroups) {
                countedTypeNames.addAll(get2dTypeNames((HasFileGroups)domainObject));
            }
            else if (domainObject instanceof HasFiles) {
                countedTypeNames.addAll(get2dTypeNames((HasFiles)domainObject));
            }
        }
        
        getToolbar().getDefaultResultButton().setVisible(countedResultNames.size()>2);
        JPopupMenu popupMenu = getToolbar().getDefaultResultButton().getPopupMenu();
        popupMenu.removeAll();
        
        for(final String resultName : countedResultNames.elementSet()) {
            if (countedResultNames.count(resultName)>1 || countedResultNames.size()==1) {
                JMenuItem menuItem = new JRadioButtonMenuItem(resultName, resultName.equals(currResult.getResultKey()));
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        currResult = new ResultDescriptor(resultName);
                        SimpleWorker worker = new SimpleWorker() {

                            @Override
                            protected void doStuff() throws Exception {
                                if (parentObject.getId()!=null) {
                                    Preference preference = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_DEFAULT_SAMPLE_RESULT,parentObject.getId().toString());
                                    if (preference==null) {
                                        preference = new Preference(AccessManager.getSubjectKey(), DomainConstants.PREFERENCE_CATEGORY_DEFAULT_SAMPLE_RESULT, parentObject.getId().toString(), resultName);
                                    }
                                    else {
                                        preference.setValue(resultName);
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
                });
                popupMenu.add(menuItem);
            }
        }        

        getToolbar().getDefaultTypeButton().setVisible(!countedTypeNames.isEmpty());
        JPopupMenu popupMenu2 = getToolbar().getDefaultTypeButton().getPopupMenu();
        popupMenu2.removeAll();

        for(final String typeName : countedTypeNames.elementSet()) {
            if (countedTypeNames.count(typeName)>1 || countedTypeNames.size()==1) {
                if (currImage2dType == null) {
                    this.currImage2dType = typeName;
                }
                FileType fileType = FileType.valueOf(typeName);
                JMenuItem menuItem = new JRadioButtonMenuItem(fileType.getLabel(), typeName.equals(currImage2dType));
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        currImage2dType = typeName;
                        
                        SimpleWorker worker = new SimpleWorker() {

                            @Override
                            protected void doStuff() throws Exception {
                                if (parentObject.getId()!=null) {
                                    Preference preference = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_DEFAULT_IMAGE_TYPE,parentObject.getId().toString());
                                    if (preference==null) {
                                        preference = new Preference(AccessManager.getSubjectKey(), DomainConstants.PREFERENCE_CATEGORY_DEFAULT_IMAGE_TYPE, parentObject.getId().toString(), typeName);
                                    }
                                    else {
                                        preference.setValue(typeName);
                                    }
                                    DomainMgr.getDomainMgr().savePreference(preference);
                                }
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
                });
                popupMenu2.add(menuItem);
            }
        }        
        
        if (currImage2dType == null) {
            this.currImage2dType = FileType.SignalMip.name();
        }
        
        showObjects(domainObjectList.getDomainObjects(), success);
    }
    
    private Multiset<String> get2dTypeNames(HasFileGroups hasGroups) {
        Multiset<String> countedTypeNames = LinkedHashMultiset.create();
        for(String groupKey : hasGroups.getGroupKeys()) {
            HasFiles hasFiles = hasGroups.getGroup(groupKey);
            if (hasFiles.getFiles()!=null) {
                countedTypeNames.addAll(get2dTypeNames(hasFiles));
            }
        }
        return countedTypeNames;
    }
    
    private Multiset<String> get2dTypeNames(HasFiles hasFiles) {
        Multiset<String> countedTypeNames = LinkedHashMultiset.create();
        if (hasFiles.getFiles()!=null) {
            for(FileType fileType : hasFiles.getFiles().keySet()) {
                if (!fileType.is2dImage()) continue;
                countedTypeNames.add(fileType.name());
            }
        }
        return countedTypeNames;
    }

    @Override
    public void refreshDomainObject(DomainObject domainObject) {
        refreshObject(domainObject);
    }

    @Override
    protected DomainObjectContextMenu getContextualPopupMenu() {
        List<Reference> ids = selectionModel.getSelectedIds();
        List<DomainObject> selected = DomainMgr.getDomainMgr().getModel().getDomainObjects(ids);
        DomainObjectContextMenu popupMenu = new DomainObjectContextMenu((DomainObject)selectionModel.getParentObject(), selected, currResult);
        popupMenu.addMenuItems();
        return popupMenu;
    }

    @Override
    protected JPopupMenu getAnnotationPopupMenu(Annotation annotation) {
        List<DomainObject> selectedObjects = DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
        AnnotationContextMenu menu = new AnnotationContextMenu(annotation, selectedObjects, imageModel);
        menu.addMenuItems();
        return menu;
    }

    @Override
    protected void moreAnnotationsButtonDoubleClicked(DomainObject domainObject) {
        // TODO: popup dialog with annotation details
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
}
