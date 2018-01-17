package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.events.selection.ChildSelectionModel;
import org.janelia.it.workstation.browser.gui.hud.Hud;
import org.janelia.it.workstation.browser.gui.listview.ListViewer;
import org.janelia.it.workstation.browser.gui.listview.ListViewerActionListener;
import org.janelia.it.workstation.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.browser.gui.listview.icongrid.IconGridViewerConfiguration;
import org.janelia.it.workstation.browser.gui.listview.icongrid.IconGridViewerPanel;
import org.janelia.it.workstation.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.model.AnnotatedObjectList;
import org.janelia.it.workstation.browser.model.ImageDecorator;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.model.domain.gui.colordepth.ColorDepthResult;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An IconGridViewer implementation for viewing color depth search results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthResultIconGridViewer 
        extends IconGridViewerPanel<ColorDepthMatch, String> 
        implements ListViewer<ColorDepthMatch, String> {
    
    private static final Logger log = LoggerFactory.getLogger(ColorDepthResultIconGridViewer.class);

    // UI Components
    private JPanel historyPanel;
    
    // Configuration
    private IconGridViewerConfiguration config;
    
    // These members deal with the context and entities within it
    private List<ColorDepthResult> results;
    private ColorDepthMask mask; 
    private Map<Reference, Sample> sampleMap = new HashMap<>();
    private ColorDepthResult selectedResult;
    private Map<String, ColorDepthMatch> matchMap = new HashMap<>();
    
    private ChildSelectionModel<ColorDepthMatch, String> selectionModel;
    
    private final ImageModel<ColorDepthMatch, String> imageModel = new ImageModel<ColorDepthMatch, String>() {

        @Override
        public String getImageUniqueId(ColorDepthMatch imageObject) {
            return imageObject.getFilepath();
        }
        
        @Override
        public String getImageFilepath(ColorDepthMatch match) {
            return match.getFilepath();
        }

        @Override
        public BufferedImage getStaticIcon(ColorDepthMatch match) {
            return null;
        }

        @Override
        public ColorDepthMatch getImageByUniqueId(String filepath) throws Exception {
            return matchMap.get(filepath);
        }
        
        @Override
        public String getImageTitle(ColorDepthMatch match) {
            Sample sample = sampleMap.get(match.getSample());
            if (sample==null) throw new IllegalStateException("Sample not loaded: "+match.getSample());
            return sample.getName();
        }

        @Override
        public String getImageSubtitle(ColorDepthMatch match) {
            return String.format("Score: %d (%2.2f%%)", match.getScore(), match.getScorePercent());
        }
        
        @Override
        public List<ImageDecorator> getDecorators(ColorDepthMatch match) {
            return Collections.emptyList();
        }

        @Override
        public List<Annotation> getAnnotations(ColorDepthMatch imageObject) {
            return Collections.emptyList();
        }
        
    };

    public ColorDepthResultIconGridViewer() {
        setImageModel(imageModel);
        this.config = IconGridViewerConfiguration.loadConfig();
        
        //getToolbar().addCustomComponent(typeButton);
    }
    
//    private void setPreferenceAsync(final String category, final Object value) {
//
//        Utils.setMainFrameCursorWaitStatus(true);
//
//        SimpleWorker worker = new SimpleWorker() {
//
//            @Override
//            protected void doStuff() throws Exception {
//                setPreference(category, value);
//            }
//
//            @Override
//            protected void hadSuccess() {
//                refreshDomainObjects();
//            }
//
//            @Override
//            protected void hadError(Throwable error) {
//                Utils.setMainFrameCursorWaitStatus(false);
//                ConsoleApp.handleException(error);
//            }
//        };
//
//        worker.execute();
//    }
    
//    private String getPreference(String category) {
//        try {
//            final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
//            return FrameworkImplProvider.getRemotePreferenceValue(category, parentObject.getId().toString(), null);
//        }
//        catch (Exception e) {
//            log.error("Error getting preference", e);
//            return null;
//        }
//    }
//    
//    private void setPreference(final String category, final Object value) throws Exception {
//        final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
//        if (parentObject.getId()!=null) {
//            FrameworkImplProvider.setRemotePreferenceValue(category, parentObject.getId().toString(), value);
//        }
//    }
    
    //@Override
    public JPanel getPanel() {
        return this;
    }
            
    @Override
    public void setSelectionModel(ChildSelectionModel<ColorDepthMatch, String> selectionModel) {
        super.setSelectionModel(selectionModel);
        this.selectionModel = selectionModel;
    }
    
    @Override
    public ChildSelectionModel<ColorDepthMatch, String> getSelectionModel() {
        return selectionModel;
    }

//    public void selectMatches(List<ColorDepthMatch> matchList, boolean select, boolean clearAll, boolean isUserDriven, boolean notifyModel) {
//        log.info("selectDomainObjects(domainObjects={},select={},clearAll={},isUserDriven={},notifyModel={})", DomainUtils.abbr(domainObjects), select, clearAll, isUserDriven, notifyModel);
//
//        if (domainObjects.isEmpty()) {
//            return;
//        }
//
//        if (select) {
//            selectObjects(domainObjects, clearAll, isUserDriven, notifyModel);
//        }
//        else {
//            deselectObjects(domainObjects, isUserDriven, notifyModel);
//        }
//
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                scrollSelectedObjectsToCenter();
//            }
//        });
//    }

//    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        updateUI();
    }
//
//    private void refreshDomainObjects() {
//        showDomainObjects(domainObjectList, new Callable<Void>() {
//            @Override
//            public Void call() throws Exception {
//                Utils.setMainFrameCursorWaitStatus(false);
//                return null;
//            }
//        });
//    }
    
    public void showResults(List<ColorDepthResult> results, ColorDepthMask mask, final Callable<Void> success) {

        this.results = new ArrayList<>();
        this.mask = mask;
        log.debug("showResults(search={}, mask={})", DomainUtils.abbr(results), mask);
        
        sampleMap.clear();
        matchMap.clear();

        for(ColorDepthResult result : results) {
            List<ColorDepthMatch> matches = result.getMaskMatches(mask);
            if (matches!=null && !matches.isEmpty()) {
                this.results.add(result);
            }
        }

        // Automatically select the latest result
        selectedResult = results.get(results.size()-1);
        showSelectedResult(success);
    }

    public void showSelectedResult(final Callable<Void> success) {

        SimpleWorker worker = new SimpleWorker() {
            
            DomainModel model = DomainMgr.getDomainMgr().getModel();
            
            @Override
            protected void doStuff() throws Exception {
                // Look up any samples we don't have yet
                Set<Reference> sampleRefs = new HashSet<>();
                for(ColorDepthResult result : results) {
                    for (ColorDepthMatch match : result.getMaskMatches(mask)) {
                        if (!sampleMap.containsKey(match.getSample())) {
                            sampleRefs.add(match.getSample());
                        }
                    }
                }
                sampleMap.putAll(DomainUtils.getMapByReference(model.getDomainObjectsAs(Sample.class, new ArrayList<>(sampleRefs))));
            }

            @Override
            protected void hadSuccess() {
                selectedResult = results.get(results.size()-1);
                showObjects(selectedResult.getMaskMatches(mask), success);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }
    
    @Override
    protected DomainObjectContextMenu getContextualPopupMenu() {
        return null;
//        return getPopupMenu(getSelectedObjects());
    }
    
    private DomainObjectContextMenu getPopupMenu(List<DomainObject> domainObjectList) {
        return null;
//        DomainObjectContextMenu popupMenu = new DomainObjectContextMenu((DomainObject)selectionModel.getParentObject(), domainObjectList, resultButton.getResultDescriptor(), typeButton.getImageTypeName());
//        popupMenu.addMenuItems();
//        return popupMenu;
    }

    @Override
    protected JPopupMenu getAnnotationPopupMenu(Annotation annotation) {
        return null;
    }

    @Override
    protected void moreAnnotationsButtonDoubleClicked(ColorDepthMatch match) {
    }
    
    @Override
    protected void objectDoubleClick(ColorDepthMatch match) {
        
    }
    
    @Override
    protected void deleteKeyPressed() {
    }

    @Override
    protected void configButtonPressed() {
    }

    @Override
    protected void setMustHaveImage(boolean mustHaveImage) {
    }

    @Override
    protected boolean isMustHaveImage() {
        return false;
    }
    
    @Override
    protected void updateHud(boolean toggle) {

        if (!toggle && !Hud.isInitialized()) return;
        
        Hud hud = Hud.getSingletonInstance();
        hud.setKeyListener(keyListener);
//
//        try {
//            List<DomainObject> selected = getSelectedObjects();
//            
//            if (selected.size() != 1) {
//                hud.hideDialog();
//                return;
//            }
//            
//            DomainObject domainObject = selected.get(0);
//            if (toggle) {
////                hud.setObjectAndToggleDialog(domainObject, resultButton.getResultDescriptor(), typeButton.getImageTypeName());
//            }
//            else {
////                hud.setObject(domainObject, resultButton.getResultDescriptor(), typeButton.getImageTypeName(), false);
//            }
//        } 
//        catch (Exception ex) {
//            ConsoleApp.handleException(ex);
//        }
    }

    @Override
    public void setActionListener(ListViewerActionListener listener) {
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    @Override
    public ListViewerState saveState() {
        return null;
    }

    @Override
    public void restoreState(ListViewerState viewerState) {
    }

    @Override
    public void setSearchProvider(SearchProvider searchProvider) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void show(AnnotatedObjectList<ColorDepthMatch, String> domainObjectList, Callable<Void> success) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public int getNumItemsHidden() {
        return 0;
    }

    @Override
    public void refresh(ColorDepthMatch object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void select(List<ColorDepthMatch> objects, boolean select, boolean clearAll, boolean isUserDriven, boolean notifyModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean matches(ResultPage<ColorDepthMatch, String> resultPage, ColorDepthMatch object, String text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEditSelectionModel(ChildSelectionModel<ColorDepthMatch, String> editSelectionModel) {
        throw new UnsupportedOperationException();   
    }

    @Override
    public ChildSelectionModel<ColorDepthMatch, String> getEditSelectionModel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void toggleEditMode(boolean editMode) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void refreshEditMode() {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void selectEditObjects(List<ColorDepthMatch> domainObjects, boolean select) {
        throw new UnsupportedOperationException();
        
    }
    
//    private List<ColorDepthMatch> getSelectedObjects() {
//        try {
//            return DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
//        }  catch (Exception e) {
//            ConsoleApp.handleException(e);
//            return null;
//        }
//    }
    
}
