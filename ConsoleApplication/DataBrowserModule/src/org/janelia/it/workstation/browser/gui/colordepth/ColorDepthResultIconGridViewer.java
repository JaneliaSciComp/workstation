package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.browser.ConsoleApp;
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
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.Reference;
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

    // Configuration
    private IconGridViewerConfiguration config;
    @SuppressWarnings("unused")
    private SearchProvider searchProvider;
    
    // State
    private AnnotatedObjectList<ColorDepthMatch, String> matchList;
    private Map<Reference, Sample> sampleMap = new HashMap<>();
    private Map<String, ColorDepthMatch> matchMap = new HashMap<>();
    private ChildSelectionModel<ColorDepthMatch, String> selectionModel;
    
    private final ImageModel<ColorDepthMatch, String> imageModel = new ImageModel<ColorDepthMatch, String>() {

        @Override
        public String getImageUniqueId(ColorDepthMatch match) {
            return match.getFilepath();
        }
        
        @Override
        public String getImageFilepath(ColorDepthMatch match) {
            if (!hasAccess(match)) return null;
            return match.getFilepath();
        }

        @Override
        public BufferedImage getStaticIcon(ColorDepthMatch match) {
            // Assume anything without an image is locked
            return Icons.getImage("file_lock.png");
        }

        @Override
        public ColorDepthMatch getImageByUniqueId(String filepath) throws Exception {
            return matchMap.get(filepath);
        }
        
        @Override
        public String getImageTitle(ColorDepthMatch match) {
            if (!hasAccess(match)) return "Access denied";
            if (match.getSample()==null) {
                return match.getFile().getName();
            }
            else {
                return sampleMap.get(match.getSample()).getName();
            }
        }

        @Override
        public String getImageSubtitle(ColorDepthMatch match) {
            return String.format("Score: %d (%2.0f%%)", match.getScore(), match.getScorePercent()*100);
        }
        
        @Override
        public List<ImageDecorator> getDecorators(ColorDepthMatch match) {
            return Collections.emptyList();
        }

        @Override
        public List<Annotation> getAnnotations(ColorDepthMatch imageObject) {
            return Collections.emptyList();
        }
        
        private boolean hasAccess(ColorDepthMatch match) {
            Sample sample = sampleMap.get(match.getSample());
            if (match.getSample()!=null && sample==null) {
                // The result maps to a sample, but the user has no access to see it
                // TODO: check access to data set?
                return false;
            }
            return true;
        }
        
    };

    public ColorDepthResultIconGridViewer() {
        setImageModel(imageModel);
        this.config = IconGridViewerConfiguration.loadConfig();
        // Hide config button since the options there don't apply to this viewer
        getToolbar().getConfigButton().setVisible(false);
        
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
    
    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void setActionListener(ListViewerActionListener listener) {
    }

    @Override
    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
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

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
    
    @Override
    public int getNumItemsHidden() {
        int totalItems = this.matchList.getObjects().size();
        int totalVisibleItems = getObjects().size();
        return totalItems-totalVisibleItems;
    }

    @Override
    public void select(List<ColorDepthMatch> objects, boolean select, boolean clearAll, boolean isUserDriven, boolean notifyModel) {
        log.info("selectDomainObjects(objects={},select={},clearAll={},isUserDriven={},notifyModel={})", 
                DomainUtils.abbr(objects), select, clearAll, isUserDriven, notifyModel);

        if (objects.isEmpty()) {
            return;
        }

        if (select) {
            selectObjects(objects, clearAll, isUserDriven, notifyModel);
        }
        else {
            deselectObjects(objects, isUserDriven, notifyModel);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrollSelectedObjectsToCenter();
            }
        });
    }
    
    @Override
    public void selectEditObjects(List<ColorDepthMatch> domainObjects, boolean select) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        updateUI();
    }

    @Override
    public void show(AnnotatedObjectList<ColorDepthMatch, String> matchList, Callable<Void> success) {
        
        this.matchList = matchList;
        sampleMap.clear();
        matchMap.clear();
        
        log.info("show(objects={})",DomainUtils.abbr(matchList.getObjects()));

        SimpleWorker worker = new SimpleWorker() {

            DomainModel model = DomainMgr.getDomainMgr().getModel();
            List<ColorDepthMatch> matchObjects;
            
            @Override
            protected void doStuff() throws Exception {
                matchObjects = matchList.getObjects();

                // Populate maps
                Set<Reference> sampleRefs = new HashSet<>();
                for (ColorDepthMatch match : matchObjects) {
                    if (!sampleMap.containsKey(match.getSample())) {
                        log.trace("Will load {}", match.getSample());
                        sampleRefs.add(match.getSample());
                    }
                    matchMap.put(match.getFilepath(), match);
                }
                
                // This does the same thing as ColorDepthResultPanel, but it should pull samples out of the cache.
                // This is not really the best thing to depend on, we should have a more direct way of communicating these
                // objects from ColorDepthResultPanel to this child class. 
                sampleMap.putAll(DomainUtils.getMapByReference(model.getDomainObjectsAs(Sample.class, new ArrayList<>(sampleRefs))));
            }

            @Override
            protected void hadSuccess() {
                showObjects(matchObjects, success);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();        
    }

    @Override
    public void toggleEditMode(boolean editMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshEditMode() {}

    @Override
    public void setEditSelectionModel(ChildSelectionModel<ColorDepthMatch, String> editSelectionModel) {
        throw new UnsupportedOperationException();   
    }

    @Override
    public ChildSelectionModel<ColorDepthMatch, String> getEditSelectionModel() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean matches(ResultPage<ColorDepthMatch, String> resultPage, ColorDepthMatch object, String text) {
        log.trace("Searching {} for {}", object.getFilepath(), text);

        String tupper = text.toUpperCase();

        String name = getImageModel().getImageTitle(object);
        if (name!=null && name.toUpperCase().contains(tupper)) {
            return true;
        }

        return false;
    }

    @Override
    public void refresh(ColorDepthMatch match) {
        refreshObject(match);
    }

    private void refreshView() {
        show(matchList, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Utils.setMainFrameCursorWaitStatus(false);
                return null;
            }
        });
    }

    @Override
    protected ColorDepthMatchContextMenu getContextualPopupMenu() {
        return getPopupMenu(getSelectedObjects());
    }
    
    private ColorDepthMatchContextMenu getPopupMenu(List<ColorDepthMatch> matches) {
        log.info("Selected objects: "+matches);
        ColorDepthMatchContextMenu popupMenu = new ColorDepthMatchContextMenu(
                (ColorDepthResult)selectionModel.getParentObject(), matches, sampleMap);
        popupMenu.addMenuItems();
        return popupMenu;
    }
    
    @Override
    protected JPopupMenu getAnnotationPopupMenu(Annotation annotation) {
        return null;
    }

    @Override
    protected void moreAnnotationsButtonDoubleClicked(ColorDepthMatch match) {}

    @Override
    protected void objectDoubleClick(ColorDepthMatch match) {
//        if (domainObjectProviderHelper.isSupported(object)) {
//            domainObjectProviderHelper.service(object);
//        }
//        else {
            getPopupMenu(Arrays.asList(match)).runDefaultAction();            
//        }
    }
    
    @Override
    protected void deleteKeyPressed() {}

    @Override
    protected void configButtonPressed() {}

    @Override
    protected void setMustHaveImage(boolean mustHaveImage) {}

    @Override
    protected boolean isMustHaveImage() {
        return false;
    }

    @Override
    protected void updateHud(boolean toggle) {

        if (!toggle && !Hud.isInitialized()) return;
        
        Hud hud = Hud.getSingletonInstance();
        hud.setKeyListener(keyListener);

        try {
            List<ColorDepthMatch> selected = getSelectedObjects();
            
            if (selected.size() != 1) {
                hud.hideDialog();
                return;
            }
            
            ColorDepthMatch match = selected.get(0);
            
            String filepath = imageModel.getImageFilepath(match);
            hud.setFilepathAndToggleDialog(filepath, toggle, false);
        } 
        catch (Exception ex) {
            ConsoleApp.handleException(ex);
        }
    }

    @Override
    public ListViewerState saveState() {
        return null;
    }

    @Override
    public void restoreState(ListViewerState viewerState) {
    }

    private List<ColorDepthMatch> getSelectedObjects() {
        try {
            List<ColorDepthMatch> selected = new ArrayList<>();
            for(String filepath : selectionModel.getSelectedIds()) {
                ColorDepthMatch match = imageModel.getImageByUniqueId(filepath);
                if (match==null) {
                    throw new IllegalStateException("Image model has no object for unique id: "+filepath);
                }
                else {
                    selected.add(match);
                }
            }
            return selected;
        }  
        catch (Exception e) {
            ConsoleApp.handleException(e);
            return null;
        }
    }
    
}
