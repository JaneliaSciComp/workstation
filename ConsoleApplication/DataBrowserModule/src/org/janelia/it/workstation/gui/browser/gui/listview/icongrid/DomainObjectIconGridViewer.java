package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.gui.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;
import org.janelia.it.workstation.gui.browser.model.DomainConstants;
import org.janelia.it.workstation.gui.browser.model.DomainObjectId;
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
public class DomainObjectIconGridViewer extends IconGridViewerPanel<DomainObject,DomainObjectId> implements AnnotatedDomainObjectListViewer {
    
    private static final Logger log = LoggerFactory.getLogger(DomainObjectIconGridViewer.class);
    
    private AnnotatedDomainObjectList domainObjectList;
    private DomainObjectSelectionModel selectionModel;
    private SearchProvider searchProvider; // Implement UI for sorting using the search provider
    
    private String defaultSampleResult = DomainConstants.PREFERENCE_VALUE_LATEST;
    private String defaultImageType = FileType.SignalMip.name();
    
    private final ImageModel<DomainObject,DomainObjectId> imageModel = new ImageModel<DomainObject, DomainObjectId>() {
        
        @Override
        public DomainObjectId getImageUniqueId(DomainObject domainObject) {
            return DomainObjectId.createFor(domainObject);
        }

        @Override
        public String getImageFilepath(DomainObject domainObject) {
            if (domainObject instanceof Sample) {
                Sample sample = (Sample)domainObject;
                List<String> objectives = sample.getOrderedObjectives();
                if (objectives==null) return null;
                ObjectiveSample objSample = sample.getObjectiveSample(objectives.get(objectives.size()-1));
                if (objSample==null) return null;
                SamplePipelineRun run = objSample.getLatestRun();
                if (run==null) return null;
                HasFiles chosenResult = null;
                if (DomainConstants.PREFERENCE_VALUE_LATEST.equals(defaultSampleResult)) {
                    chosenResult = run.getLatestResult();
                }
                else {
                    Pattern p = Pattern.compile("(.*?) \\((.*?)\\)");
                    Matcher m = p.matcher(defaultSampleResult);
                    String wantedResultName = m.matches()?m.group(1):null;
                    String wantedResultGroup = m.matches()?m.group(2):null;
                    if (run.getResults()!=null) {
                        for(PipelineResult result : run.getResults()) {
                            if (result instanceof HasFileGroups) {
                                HasFileGroups hasGroups = (HasFileGroups)result;
                                for(String groupKey : hasGroups.getGroupKeys()) {
                                    if (result.getName().equals(wantedResultName) && groupKey.equals(wantedResultGroup)) {
                                        chosenResult = hasGroups.getGroup(groupKey);
                                        break;
                                    }
                                }
                            }
                            else {
                                if (defaultSampleResult.equals(result.getName())) {
                                    chosenResult = result;
                                    break;
                                }
                            }   
                        }
                    }
                }
                
                if (chosenResult==null) return null;
                return DomainUtils.getFilepath(chosenResult, defaultImageType);
            }
            else if (domainObject instanceof HasFiles) {
                HasFiles hasFiles = (HasFiles)domainObject;
                return DomainUtils.getFilepath(hasFiles, defaultImageType);
            }
            return null;
        }
        
        @Override
        public DomainObject getImageByUniqueId(DomainObjectId id) {
            return DomainMgr.getDomainMgr().getModel().getDomainObject(id);
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
    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
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
    public void showDomainObjects(AnnotatedDomainObjectList objects) {

        this.domainObjectList = objects;
        
        final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
        if (parentObject!=null && parentObject.getId()!=null) {
            Preference preference = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_DEFAULT_SAMPLE_RESULT, parentObject.getId().toString());
            if (preference!=null) {
                this.defaultSampleResult = preference.getValue();
            }
            Preference preference2 = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_DEFAULT_IMAGE_TYPE, parentObject.getId().toString());
            if (preference2!=null) {
                this.defaultImageType = preference2.getValue();
            }
        }
        
        Multiset<String> countedTypeNames = LinkedHashMultiset.create();
        Multiset<String> countedResultNames = LinkedHashMultiset.create();
        // Add twice so that it is selected by >1 filter below
        countedResultNames.add(DomainConstants.PREFERENCE_VALUE_LATEST);
        countedResultNames.add(DomainConstants.PREFERENCE_VALUE_LATEST);
        
        boolean allSamples = true;
        for(DomainObject domainObject : domainObjectList.getDomainObjects()) {
            if (domainObject instanceof Sample) {
                Sample sample = (Sample)domainObject;
                for(String objective : sample.getOrderedObjectives()) {
                    ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
                    SamplePipelineRun run = objectiveSample.getLatestRun();
                    if (run==null || run.getResults()==null) continue;
                    for(PipelineResult result : run.getResults()) {
                        if (result instanceof HasFileGroups) {
                            HasFileGroups hasGroups = (HasFileGroups)result;
                            for(String groupKey : hasGroups.getGroupKeys()) {
                                String name = result.getName()+" ("+groupKey+")";
                                countedResultNames.add(name);
                                HasFiles hasFiles = hasGroups.getGroup(groupKey);
                                if (hasFiles.getFiles()!=null) {
                                    for(FileType fileType : hasFiles.getFiles().keySet()) {
                                        if (!fileType.is2dImage()) continue;
                                        countedTypeNames.add(fileType.name());
                                    }
                                }
                            }
                        }
                        else {
                            String name = result.getName();
                            countedResultNames.add(name);
                            if (result.getFiles()!=null) {
                                for(FileType fileType : result.getFiles().keySet()) {
                                    if (!fileType.is2dImage()) continue;
                                    countedTypeNames.add(fileType.name());
                                }
                            }
                        }
                    }
                }
            }
            else {
                allSamples = false;
                break;
            }
        }
        
        getToolbar().getDefaultResultButton().setVisible(allSamples);

        JPopupMenu popupMenu = getToolbar().getDefaultResultButton().getPopupMenu();
        popupMenu.removeAll();
        
        for(final String resultName : countedResultNames.elementSet()) {
            if (countedResultNames.count(resultName)>1) {
                JMenuItem menuItem = new JRadioButtonMenuItem(resultName, resultName.equals(defaultSampleResult));
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        defaultSampleResult = resultName;
                        
                        SimpleWorker worker = new SimpleWorker() {

                            @Override
                            protected void doStuff() throws Exception {
                                Preference preference = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_DEFAULT_SAMPLE_RESULT,parentObject.getId().toString());
                                if (preference==null) {
                                    preference = new Preference(SessionMgr.getSubjectKey(), DomainConstants.PREFERENCE_CATEGORY_DEFAULT_SAMPLE_RESULT, parentObject.getId().toString(), resultName);
                                }
                                else {
                                    preference.setValue(resultName);
                                }
                                DomainMgr.getDomainMgr().savePreference(preference);
                            }

                            @Override
                            protected void hadSuccess() {
                                showDomainObjects(domainObjectList);
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

        JPopupMenu popupMenu2 = getToolbar().getDefaultTypeButton().getPopupMenu();
        popupMenu2.removeAll();

        for(final String typeName : countedTypeNames.elementSet()) {
            if (countedTypeNames.count(typeName)>1) {
                FileType fileType = FileType.valueOf(typeName);
                JMenuItem menuItem = new JRadioButtonMenuItem(fileType.getLabel(), typeName.equals(defaultImageType));
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        defaultImageType = typeName;
                        
                        SimpleWorker worker = new SimpleWorker() {

                            @Override
                            protected void doStuff() throws Exception {
                                Preference preference = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_DEFAULT_IMAGE_TYPE,parentObject.getId().toString());
                                if (preference==null) {
                                    preference = new Preference(SessionMgr.getSubjectKey(), DomainConstants.PREFERENCE_CATEGORY_DEFAULT_IMAGE_TYPE, parentObject.getId().toString(), typeName);
                                }
                                else {
                                    preference.setValue(typeName);
                                }
                                DomainMgr.getDomainMgr().savePreference(preference);
                            }

                            @Override
                            protected void hadSuccess() {
                                showDomainObjects(domainObjectList);
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
        
        
        
        
        
        showImageObjects(domainObjectList.getDomainObjects());
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
    public void preferenceChanged(Preference preference) {
    }
    
    @Override
    public JPanel getPanel() {
        return this;
    }
}
