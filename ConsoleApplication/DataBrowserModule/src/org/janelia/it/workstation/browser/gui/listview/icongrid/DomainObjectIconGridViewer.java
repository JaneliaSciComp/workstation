package org.janelia.it.workstation.browser.gui.listview.icongrid;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.jacs.integration.framework.domain.ServiceAcceptorHelper;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.AnnotationContextMenu;
import org.janelia.it.workstation.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.browser.actions.RemoveItemsFromFolderAction;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.components.DomainObjectProviderHelper;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.it.workstation.browser.gui.dialogs.IconGridViewerConfigDialog;
import org.janelia.it.workstation.browser.gui.hud.Hud;
import org.janelia.it.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.browser.gui.listview.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.ImageTypeSelectionButton;
import org.janelia.it.workstation.browser.gui.support.ResultSelectionButton;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.model.AnnotatedDomainObjectList;
import org.janelia.it.workstation.browser.model.ImageDecorator;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.DescriptorUtils;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.access.domain.DynamicDomainObjectProxy;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Preference;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.interfaces.IsParent;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.workspace.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An IconGridViewer implementation for viewing domain objects. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectIconGridViewer extends IconGridViewerPanel<DomainObject,Reference> implements AnnotatedDomainObjectListViewer {
    
    private static final Logger log = LoggerFactory.getLogger(DomainObjectIconGridViewer.class);

    // UI Components
    private ResultSelectionButton resultButton;
    private ImageTypeSelectionButton typeButton;
    
    // Configuration
    private IconGridViewerConfiguration config;
    
    // These members deal with the context and entities within it
    private AnnotatedDomainObjectList domainObjectList;
    private DomainObjectSelectionModel selectionModel;
    private DomainObjectSelectionModel editSelectionModel;
    private DomainObjectProviderHelper domainObjectProviderHelper = new DomainObjectProviderHelper();
    @SuppressWarnings("unused")
    private SearchProvider searchProvider;

    // UI state
    private boolean editMode;
    
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
                result = DescriptorUtils.getResult(sample, resultButton.getResultDescriptor());
            }
            else if (domainObject instanceof HasFiles) {
                result = (HasFiles)domainObject;
            }
            return result==null? null : DomainUtils.getFilepath(result, typeButton.getImageTypeName());
        }

        @Override
        public BufferedImage getStaticIcon(DomainObject imageObject) {
            String filename = null;
            DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(imageObject);
            if (provider!=null) {
                filename = provider.getLargeIcon(imageObject);
            }
            return filename==null?null:Icons.getImage(filename);
        }

        @Override
        public DomainObject getImageByUniqueId(Reference id) throws Exception {
            return DomainMgr.getDomainMgr().getModel().getDomainObject(id);
        }
        
        @Override
        public String getImageTitle(DomainObject domainObject) {
            String titlePattern = config.getDomainClassTitle(domainObject.getClass().getSimpleName());
            if (StringUtils.isEmpty(titlePattern)) return domainObject.getName();
            DynamicDomainObjectProxy proxy = new DynamicDomainObjectProxy(domainObject);
            return StringUtils.replaceVariablePattern(titlePattern, proxy);
        }

        @Override
        public String getImageSubtitle(DomainObject domainObject) {
            String subtitlePattern = config.getDomainClassSubtitle(domainObject.getClass().getSimpleName());
            if (StringUtils.isEmpty(subtitlePattern)) return null;
            DynamicDomainObjectProxy proxy = new DynamicDomainObjectProxy(domainObject);
            return StringUtils.replaceVariablePattern(subtitlePattern, proxy);
        }

        @Override
        public List<Annotation> getAnnotations(DomainObject domainObject) {
            return domainObjectList.getAnnotations(domainObject.getId());
        }
        
        @Override
        public List<ImageDecorator> getDecorators(DomainObject imageObject) {
            List<ImageDecorator> decorators = new ArrayList<>();
            if (imageObject instanceof Sample) {
                Sample sample = (Sample)imageObject;
                if (sample.isSamplePurged()) {
                    decorators.add(ImageDecorator.PURGED);
                }
                if (!sample.isSampleSageSynced()) {
                    decorators.add(ImageDecorator.DESYNC);
                }   
            }
            else if (imageObject instanceof LSMImage) {
                LSMImage lsm = (LSMImage)imageObject;
                if (!lsm.isLSMSageSynced()) {
                    decorators.add(ImageDecorator.DESYNC);
                }   
            }
            
            return decorators;
        }
    };

    public DomainObjectIconGridViewer() {
        setImageModel(imageModel);
        this.config = IconGridViewerConfiguration.loadConfig();
        resultButton = new ResultSelectionButton(true) {
            @Override
            protected void resultChanged(ArtifactDescriptor resultDescriptor) {
                log.info("Setting result preference: "+resultDescriptor.toString());
                try {
                    setPreferenceAsync(DomainConstants.PREFERENCE_CATEGORY_SAMPLE_RESULT, DescriptorUtils.serialize(resultDescriptor));
                }
                catch (Exception e) {
                    log.error("Error serializing sample result preference: "+resultDescriptor,e);
                }
            }
        };
        typeButton = new ImageTypeSelectionButton(true, true) {
            @Override
            protected void imageTypeChanged(FileType fileType) {
                log.info("Setting image type preference: "+fileType);
                setPreferenceAsync(DomainConstants.PREFERENCE_CATEGORY_IMAGE_TYPE, fileType.name());
            }
        };
                
        getToolbar().addCustomComponent(resultButton);
        getToolbar().addCustomComponent(typeButton);
    }
    
    private void setPreferenceAsync(final String category, final Object value) {

        Utils.setMainFrameCursorWaitStatus(true);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                setPreference(category, value);
            }

            @Override
            protected void hadSuccess() {
                refreshDomainObjects();
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setMainFrameCursorWaitStatus(false);
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }
    
    private String getPreference(String category) {
        try {
            final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
            return FrameworkImplProvider.getRemotePreferenceValue(category, parentObject.getId().toString(), null);
        }
        catch (Exception e) {
            log.error("Error getting preference", e);
            return null;
        }
    }
    
    private void setPreference(final String category, final Object value) throws Exception {
        final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
        if (parentObject.getId()!=null) {
            FrameworkImplProvider.setRemotePreferenceValue(category, parentObject.getId().toString(), value);
        }
    }
    
    @Override
    public JPanel getPanel() {
        return this;
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
    public void selectDomainObjects(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven, boolean notifyModel) {
        log.info("selectDomainObjects(domainObjects={},select={},clearAll={},isUserDriven={},notifyModel={})", DomainUtils.abbr(domainObjects), select, clearAll, isUserDriven, notifyModel);

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
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        updateUI();
    }

    private void refreshDomainObjects() {
        showDomainObjects(domainObjectList, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Utils.setMainFrameCursorWaitStatus(false);
                return null;
            }
        });
    }
    
    @Override
    public void showDomainObjects(AnnotatedDomainObjectList objects, final Callable<Void> success) {

        this.domainObjectList = objects;
        log.debug("showDomainObjects(domainObjectList={})",DomainUtils.abbr(domainObjectList.getDomainObjects()));

        SimpleWorker worker = new SimpleWorker() {
            
            List<DomainObject> domainObjects;
            
            @Override
            protected void doStuff() throws Exception {

                // Update toolbar
                boolean mustHaveImage = isMustHaveImage();
                getToolbar().getMustHaveImageMenuItem().setSelected(mustHaveImage);
                
                final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
                if (parentObject!=null && parentObject.getId()!=null) {
                    
                    String preference = getPreference(DomainConstants.PREFERENCE_CATEGORY_SAMPLE_RESULT);
                    log.info("Got result preference: "+preference);
                    if (preference!=null) {
                        try {
                            ArtifactDescriptor resultDescriptor = DescriptorUtils.deserialize(preference);
                            resultButton.setResultDescriptor(resultDescriptor);
                        }
                        catch (Exception e) {
                            FrameworkImplProvider.handleExceptionQuietly(e);
                            log.error("Error deserializing preference {}. Clearing it.", preference, e);
                            setPreference(DomainConstants.PREFERENCE_CATEGORY_SAMPLE_RESULT, null);
                        }
                    }
                    else {
                        resultButton.reset();
                    }
                    
                    String preference2 = getPreference(DomainConstants.PREFERENCE_CATEGORY_IMAGE_TYPE);
                    log.info("Got image type preference: "+preference2);
                    if (preference2!=null) {
                        typeButton.setImageTypeName(preference2);
                    }
                    else {
                        typeButton.reset();
                    }   
                }

                resultButton.populate(domainObjectList.getDomainObjects());
                typeButton.setResultDescriptor(resultButton.getResultDescriptor());
                typeButton.populate(domainObjectList.getDomainObjects());

                if (mustHaveImage) {
                    domainObjects = domainObjectList.getDomainObjects().stream()
                        .filter(domainObject -> imageModel.getImageFilepath(domainObject)!=null || ServiceAcceptorHelper.findFirstHelper(domainObject)!=null)
                        .collect(Collectors.toList());
                }
                else {
                    domainObjects = domainObjectList.getDomainObjects();
                }
                
            }

            @Override
            protected void hadSuccess() {
                showObjects(domainObjects, success);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }
    
    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    @Override
    public void toggleEditMode(boolean editMode) {
        this.editMode = editMode;
        imagesPanel.setEditMode(editMode);
    }

    @Override
    public void refreshEditMode() {
        imagesPanel.setEditMode(editMode);
        if (editSelectionModel!=null) {
            imagesPanel.setEditSelection(editSelectionModel.getSelectedIds(), true);
        }
    }


    @Override
    public void setEditSelectionModel(DomainObjectSelectionModel editSelectionModel) {
        this.editSelectionModel = editSelectionModel;
        imagesPanel.setEditSelectionModel(editSelectionModel);
    }

    @Override
    public DomainObjectSelectionModel getEditSelectionModel() {
        return editSelectionModel;
    }

    @Override
    public boolean matches(ResultPage resultPage, DomainObject domainObject, String text) {
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

        for(Annotation annotation : resultPage.getAnnotations(domainObject.getId())) {
            if (annotation.getName().toUpperCase().contains(tupper)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void refreshDomainObject(DomainObject domainObject) {
        refreshObject(domainObject);
    }

    @Override
    protected DomainObjectContextMenu getContextualPopupMenu() {
        return getPopupMenu(getSelectedObjects());
    }
    
    private DomainObjectContextMenu getPopupMenu(List<DomainObject> domainObjectList) {
        DomainObjectContextMenu popupMenu = new DomainObjectContextMenu((DomainObject)selectionModel.getParentObject(), domainObjectList, resultButton.getResultDescriptor(), typeButton.getImageTypeName());
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
    protected void objectDoubleClick(DomainObject object) {
        if (domainObjectProviderHelper.isSupported(object)) {
            domainObjectProviderHelper.service(object);
        }
        else {
            getPopupMenu(Arrays.asList(object)).runDefaultAction();            
        }
    }
    
    @Override
    protected void deleteKeyPressed() {
        try {
            IsParent parent = selectionModel.getParentObject();
            if (parent instanceof TreeNode) {
                TreeNode treeNode = (TreeNode)parent;
                if (ClientDomainUtils.hasWriteAccess(treeNode)) {
                    List<DomainObject> selectedObjects = DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
                    RemoveItemsFromFolderAction action = new RemoveItemsFromFolderAction(treeNode, selectedObjects);
                    action.actionPerformed(null);
                }
            }
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }

    @Override
    protected void configButtonPressed() {
        try {
            if (domainObjectList.getDomainObjects().isEmpty()) return;

            DomainObject firstObject;
            List<DomainObject> selectedObjects = DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
            if (selectedObjects.isEmpty()) {
                firstObject = domainObjectList.getDomainObjects().get(0);
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
            ConsoleApp.handleException(e);
        }
    }

    @Override
    protected void setMustHaveImage(boolean mustHaveImage) {
        try {
            FrameworkImplProvider.setRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_MUST_HAVE_IMAGE, DomainConstants.PREFERENCE_CATEGORY_MUST_HAVE_IMAGE, mustHaveImage);
            refreshDomainObjects();
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
            
            if (selected.size() != 1) {
                hud.hideDialog();
                return;
            }
            
            DomainObject domainObject = selected.get(0);
            if (toggle) {
                hud.setObjectAndToggleDialog(domainObject, resultButton.getResultDescriptor(), typeButton.getImageTypeName());
            }
            else {
                hud.setObject(domainObject, resultButton.getResultDescriptor(), typeButton.getImageTypeName(), false);
            }
        } 
        catch (Exception ex) {
            ConsoleApp.handleException(ex);
        }
    }
    
    private List<DomainObject> getSelectedObjects() {
        try {
            return DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
        }  catch (Exception e) {
            ConsoleApp.handleException(e);
            return null;
        }
    }

    @Override
    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
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

    
}
