package org.janelia.it.workstation.browser.gui.listview.icongrid;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.domain.ServiceAcceptorHelper;
import org.janelia.it.workstation.browser.actions.AnnotationContextMenu;
import org.janelia.it.workstation.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.browser.actions.RemoveItemsFromFolderAction;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.events.selection.ChildSelectionModel;
import org.janelia.it.workstation.browser.events.selection.DomainObjectEditSelectionEvent;
import org.janelia.it.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.it.workstation.browser.gui.dialogs.IconGridViewerConfigDialog;
import org.janelia.it.workstation.browser.gui.hud.Hud;
import org.janelia.it.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.browser.gui.listview.ListViewer;
import org.janelia.it.workstation.browser.gui.listview.ListViewerActionListener;
import org.janelia.it.workstation.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.ImageTypeSelectionButton;
import org.janelia.it.workstation.browser.gui.support.PreferenceSupport;
import org.janelia.it.workstation.browser.gui.support.ResultSelectionButton;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.model.AnnotatedObjectList;
import org.janelia.it.workstation.browser.model.DomainObjectImageModel;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.DescriptorUtils;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.util.ConcurrentUtils;
import org.janelia.it.workstation.browser.util.HelpTextUtils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.workspace.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * An IconGridViewer implementation for viewing domain objects. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectIconGridViewer 
        extends IconGridViewerPanel<DomainObject,Reference> 
        implements ListViewer<DomainObject, Reference> {
    
    private static final Logger log = LoggerFactory.getLogger(DomainObjectIconGridViewer.class);

    // UI Components
    private ResultSelectionButton resultButton;
    private ImageTypeSelectionButton typeButton;
    private final JPanel helpPanel;
    
    // Configuration
    private IconGridViewerConfiguration config;
    @SuppressWarnings("unused")
    private SearchProvider searchProvider;
    
    // State
    private PreferenceSupport preferenceSupport;
    private AnnotatedObjectList<DomainObject,Reference> domainObjectList;
    private ChildSelectionModel<DomainObject,Reference> selectionModel;
    private ChildSelectionModel<DomainObject,Reference> editSelectionModel;
    private ListViewerActionListener listener;
    private boolean editMode;
    
    private final DomainObjectImageModel imageModel = new DomainObjectImageModel() {

        @Override
        protected ArtifactDescriptor getArtifactDescriptor() {
            return resultButton.getResultDescriptor();
        }

        @Override
        protected String getImageTypeName() {
            return typeButton.getImageTypeName();
        }

        @Override
        protected String getTitlePattern(Class<? extends DomainObject> clazz) {
            if (config==null) throw new IllegalStateException("Config is null");
            return config.getDomainClassTitle(clazz.getSimpleName());
        }
        
        @Override
        protected String getSubtitlePattern(Class<? extends DomainObject> clazz) {
            if (config==null) throw new IllegalStateException("Config is null");
            return config.getDomainClassSubtitle(clazz.getSimpleName());
        }
        
        @Override
        public List<Annotation> getAnnotations(DomainObject domainObject) {
            return domainObjectList.getAnnotations(Reference.createFor(domainObject));
        }
    };

    public DomainObjectIconGridViewer() {
        setImageModel(imageModel);
        
        this.resultButton = new ResultSelectionButton(true) {
            @Override
            protected void resultChanged(ArtifactDescriptor resultDescriptor) {
                log.info("Setting result preference: "+resultDescriptor.toString());
                try {
                    preferenceSupport.setPreferenceAsync(DomainConstants.PREFERENCE_CATEGORY_SAMPLE_RESULT, 
                            DescriptorUtils.serialize(resultDescriptor))
                            .addListener(() -> refreshView(null));
                }
                catch (Exception e) {
                    log.error("Error serializing sample result preference: "+resultDescriptor,e);
                }
            }
        };
        this.typeButton = new ImageTypeSelectionButton(true, true) {
            @Override
            protected void imageTypeChanged(FileType fileType) {
                log.info("Setting image type preference: "+fileType);
                preferenceSupport.setPreferenceAsync(DomainConstants.PREFERENCE_CATEGORY_IMAGE_TYPE, 
                        fileType.name())
                        .addListener(() -> refreshView(null));
            }
        };
                
        getToolbar().addCustomComponent(resultButton);
        getToolbar().addCustomComponent(typeButton);


        helpPanel = new JPanel();
        helpPanel.setLayout(new GridBagLayout());
        JPanel panel = new JPanel();
        panel.add(new JLabel("<html>All items are hidden by your chosen settings.<br><br>"
                + "Check your selected result and display settings, such as the '"
                + HelpTextUtils.getBoldedLabel("Show only items with selected imagery")+"' setting.<br>"
                + "</html>"));
        helpPanel.add(panel, new GridBagConstraints());
    }
    
    @Override
    public JPanel getPanel() {
        return this;
    }
        
    @Override
    public void setActionListener(ListViewerActionListener listener) {
        this.listener = listener;
    }

    @Override
    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
    }

    @Override
    public void setSelectionModel(ChildSelectionModel<DomainObject,Reference> selectionModel) {
        super.setSelectionModel(selectionModel);
        this.selectionModel = selectionModel;
    }
    
    @Override
    public ChildSelectionModel<DomainObject,Reference> getSelectionModel() {
        return selectionModel;
    }

    @Override
    public void setPreferenceSupport(PreferenceSupport preferenceSupport) {
        this.preferenceSupport = preferenceSupport;
    }
    
    @Override
    public PreferenceSupport getPreferenceSupport() {
        return preferenceSupport;
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
    
    @Override
    public int getNumItemsHidden() {
        if (domainObjectList==null || getObjects()==null) return 0;
        int totalItems = this.domainObjectList.getObjects().size();
        int totalVisibleItems = getObjects().size();
        if (totalVisibleItems > totalItems) {
            log.warn("Visible item count greater than total item count");
            return 0;
        }
        return totalItems-totalVisibleItems;
    }
    
    @Override
    public void select(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven, boolean notifyModel) {
        log.info("selectDomainObjects(domainObjects={},select={},clearAll={},isUserDriven={},notifyModel={})", 
                DomainUtils.abbr(domainObjects), select, clearAll, isUserDriven, notifyModel);

        if (domainObjects.isEmpty()) {
            return;
        }

        if (select) {
            selectObjects(domainObjects, clearAll, isUserDriven, notifyModel);
        }
        else {
            deselectObjects(domainObjects, isUserDriven, notifyModel);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrollSelectedObjectsToCenter();
            }
        });
    }

    @Override
    public void selectEditObjects(List<DomainObject> domainObjects, boolean select) {
        log.info("selectEditObjects(domainObjects={},select={})", DomainUtils.abbr(domainObjects), select);
        if (domainObjects.isEmpty()) {
            return;
        }
        if (select) {
            editSelectionModel.select(domainObjects, true, true);
        }
    }

    @Override
    public void toggleEditMode(boolean editMode) {
        this.editMode = editMode;
        imagesPanel.setEditMode(editMode);
        if (editSelectionModel!=null) {
            editSelectionModel.reset();
        }
    }
    
    @Override
    public void refreshEditMode() {
        imagesPanel.setEditMode(editMode);
        if (editSelectionModel != null) {
            imagesPanel.setEditSelection(editSelectionModel.getSelectedIds());
        }
    }

    @Override
    public void setEditSelectionModel(ChildSelectionModel<DomainObject, Reference> editSelectionModel) {
        this.editSelectionModel = editSelectionModel;
        imagesPanel.setEditSelectionModel(editSelectionModel);
    }

    @Override
    public ChildSelectionModel<DomainObject, Reference> getEditSelectionModel() {
        return editSelectionModel;
    }

    @Subscribe
    public void handleEditSelection(DomainObjectEditSelectionEvent event) {
        // Refresh the edit checkboxes any time the edit selection model changes
        refreshEditMode();
    }
    
    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        updateUI();
    }

    public synchronized void showHelp() {
        removeAll();
        add(getToolbar(), BorderLayout.NORTH);
        add(helpPanel, BorderLayout.CENTER);
        updateUI();
    }
    
    @Override
    public void show(AnnotatedObjectList<DomainObject,Reference> objects, final Callable<Void> success) {

        this.domainObjectList = objects;
        log.debug("show(objects={})",DomainUtils.abbr(domainObjectList.getObjects()));

        SimpleWorker worker = new SimpleWorker() {
            
            List<DomainObject> domainObjects;
            
            @Override
            protected void doStuff() throws Exception {

                // Update toolbar
                boolean mustHaveImage = isMustHaveImage();
                getToolbar().getMustHaveImageMenuItem().setSelected(mustHaveImage);

                // Reload the config
                config = IconGridViewerConfiguration.loadConfig();
                
                final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
                if (parentObject!=null && parentObject.getId()!=null) {
                    
                    String preference = preferenceSupport.getPreference(DomainConstants.PREFERENCE_CATEGORY_SAMPLE_RESULT);
                    log.info("Got result preference: "+preference);
                    if (preference!=null) {
                        try {
                            ArtifactDescriptor resultDescriptor = DescriptorUtils.deserialize(preference);
                            resultButton.setResultDescriptor(resultDescriptor);
                        }
                        catch (Exception e) {
                            log.error("Error deserializing preference {}. Clearing it.", preference, e);
                            preferenceSupport.setPreference(DomainConstants.PREFERENCE_CATEGORY_SAMPLE_RESULT, null);
                        }
                    }
                    else {
                        resultButton.reset();
                    }
                    
                    String preference2 = preferenceSupport.getPreference(DomainConstants.PREFERENCE_CATEGORY_IMAGE_TYPE);
                    log.info("Got image type preference: "+preference2);
                    if (preference2!=null) {
                        typeButton.setImageTypeName(preference2);
                    }
                    else {
                        typeButton.reset();
                    }   
                }

                resultButton.populate(domainObjectList.getObjects());
                typeButton.setResultDescriptor(resultButton.getResultDescriptor());
                typeButton.populate(domainObjectList.getObjects());

                if (mustHaveImage && (resultButton.isVisible() || typeButton.isVisible())) {
                    domainObjects = domainObjectList.getObjects().stream()
                        .filter(domainObject -> imageModel.getImageFilepath(domainObject)!=null || ServiceAcceptorHelper.findFirstHelper(domainObject)!=null)
                        .collect(Collectors.toList());
                }
                else {
                    domainObjects = domainObjectList.getObjects();
                }
            }

            @Override
            protected void hadSuccess() {
                if (!domainObjectList.getObjects().isEmpty() && domainObjects.isEmpty()) {
                    // There are results. but they're all hidden. In this case, show some guidance for the user.
                    showHelp();
                    // Update the state so that methods like getNumItemsHidden() return the correct amount
                    setObjects(domainObjects);
                    // Don't forget to invoke the success callback
                    SwingUtilities.invokeLater(() -> {
                        if (listener!=null) listener.visibleObjectsChanged();
                        ConcurrentUtils.invokeAndHandleExceptions(success);
                    });
                }
                else {
                    showObjects(domainObjects, () -> {
                        if (listener!=null) listener.visibleObjectsChanged();
                        ConcurrentUtils.invokeAndHandleExceptions(success);
                        return null;
                    });
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkImplProvider.handleException(error);
            }
        };

        worker.execute();
    }

    @Override
    public boolean matches(ResultPage<DomainObject, Reference> resultPage, DomainObject domainObject, String text) {
        log.trace("Searching {} for {}", domainObject.getName(), text);

        String tupper = text.toUpperCase();

        // Exact matches on id or name always work
        if (domainObject.getId().toString().equals(text) || domainObject.getName().toUpperCase().equals(tupper)) {
            return true;
        }

        String name = getImageModel().getImageTitle(domainObject);
        if (name!=null && name.toUpperCase().contains(tupper)) {
            return true;
        }

        for(Annotation annotation : resultPage.getAnnotations(Reference.createFor(domainObject))) {
            if (annotation.getName().toUpperCase().contains(tupper)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void refresh(DomainObject domainObject) {
        refreshObject(domainObject);
    }

    public void refreshView(Callable<Void> success) {
        selectionModel.reset();
        show(domainObjectList, success);
    }
    
    @Override
    protected DomainObjectContextMenu getContextualPopupMenu() {
        return getPopupMenu(getSelectedObjects());
    }
    
    private DomainObjectContextMenu getPopupMenu(List<DomainObject> domainObjectList) {
        DomainObjectContextMenu popupMenu = new DomainObjectContextMenu(
                (DomainObject)selectionModel.getParentObject(), 
                domainObjectList, 
                resultButton.getResultDescriptor(),
                typeButton.getImageTypeName(),
                editMode ? editSelectionModel : null);
        popupMenu.addMenuItems();
        return popupMenu;
    }

    @Override
    protected JPopupMenu getAnnotationPopupMenu(DomainObject domainObject, Annotation annotation) {
        AnnotationContextMenu menu = new AnnotationContextMenu(annotation, getSelectedObjects(), imageModel);
        menu.addMenuItems();
        return menu;
    }

    @Override
    protected void moreAnnotationsButtonDoubleClicked(DomainObject domainObject) {
        new DomainDetailsDialog().showForDomainObject(domainObject, DomainInspectorPanel.TAB_NAME_ANNOTATIONS);
    }
    
    @Override
    protected void objectDoubleClick(DomainObject object) {
        getPopupMenu(Arrays.asList(object)).runDefaultAction();
    }
    
    @Override
    protected void deleteKeyPressed() {
        try {
            // TODO: we should move this somewhere else, this logic doesn't belong here
            Object parent = selectionModel.getParentObject();
            if (parent instanceof Node) {
                Node node = (Node)parent;
                if (ClientDomainUtils.hasWriteAccess(node)) {
                    List<DomainObject> selectedObjects = DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
                    RemoveItemsFromFolderAction action = new RemoveItemsFromFolderAction(node, selectedObjects);
                    action.actionPerformed(null);
                }
            }
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
    }

    @Override
    protected void customizeTitlesPressed() {
        try {
            if (domainObjectList.getObjects().isEmpty()) return;

            DomainObject firstObject;
            List<DomainObject> selectedObjects = DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
            if (selectedObjects.isEmpty()) {
                firstObject = domainObjectList.getObjects().get(0);
            }
            else {
                firstObject = selectedObjects.get(0);
            }

            IconGridViewerConfigDialog configDialog = new IconGridViewerConfigDialog(firstObject.getClass());
            if (configDialog.showDialog(this)==1) {
                this.config = IconGridViewerConfiguration.loadConfig();
                refresh();
            }
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
    }

    @Override
    protected void setMustHaveImage(boolean mustHaveImage) {
        try {
            FrameworkImplProvider.setRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_MUST_HAVE_IMAGE, DomainConstants.PREFERENCE_CATEGORY_MUST_HAVE_IMAGE, mustHaveImage);
            refreshView(null);
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
    }

    @Override
    protected boolean isMustHaveImage() {
        boolean defaultValue = false;
        Boolean preference;
        try {
            preference = FrameworkImplProvider.getRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_MUST_HAVE_IMAGE, DomainConstants.PREFERENCE_CATEGORY_MUST_HAVE_IMAGE, defaultValue);
        }
        catch (Exception e) {
            log.error("Error getting preference", e);
            return defaultValue;
        }
        log.info("Got must have image preference: "+preference);
        return preference;
    }
    
    @Override
    protected void updateHud(boolean toggle) {

        if (!toggle && !Hud.isInitialized()) return;
        
        Hud hud = Hud.getSingletonInstance();
        hud.setKeyListener(keyListener);

        try {
            List<DomainObject> selected = getSelectedObjects();
            
            if (selected==null || selected.size() != 1) {
                hud.hideDialog();
                return;
            }
            
            DomainObject domainObject = selected.get(0);
            hud.setObjectAndToggleDialog(domainObject, resultButton.getResultDescriptor(), typeButton.getImageTypeName(), toggle, true);
        } 
        catch (Exception ex) {
            FrameworkImplProvider.handleException(ex);
        }
    }
    
    private List<DomainObject> getSelectedObjects() {
        try {
            return DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
            return null;
        }
    }

    @Override
    public ListViewerState saveState() {
        int maxImageWidth = imagesPanel.getMaxImageWidth();
        log.debug("Saving maxImageWidth={}",maxImageWidth);
        IconGridViewerState state = new IconGridViewerState(maxImageWidth);
        return state;
    }

    @Override
    public void restoreState(ListViewerState viewerState) {
        if (viewerState instanceof IconGridViewerState) {
            final IconGridViewerState tableViewerState = (IconGridViewerState) viewerState;
            SwingUtilities.invokeLater(new Runnable() {
                   public void run() {
                       int maxImageWidth = tableViewerState.getMaxImageWidth();
                       log.debug("Restoring maxImageWidth={}",maxImageWidth);
                       getToolbar().getImageSizeSlider().setValue(maxImageWidth);
                       // Wait until slider resizes images, then fix scroll:
                       SwingUtilities.invokeLater(new Runnable() {
                           @Override
                           public void run() {
                               scrollSelectedObjectsToCenter();
                           }
                       });
                   }
               }
            );
        }
        else {
            log.warn("Cannot restore viewer state of type {}", viewerState.getClass());
        }   
    }
}
