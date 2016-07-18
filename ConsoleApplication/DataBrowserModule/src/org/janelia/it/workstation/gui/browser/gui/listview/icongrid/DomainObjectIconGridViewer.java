package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.DynamicDomainObjectProxy;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.jacs.model.domain.support.SampleUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.actions.AnnotationContextMenu;
import org.janelia.it.workstation.gui.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.gui.browser.actions.RemoveItemsFromFolderAction;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.components.DomainObjectProviderHelper;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.it.workstation.gui.browser.gui.dialogs.IconGridViewerConfigDialog;
import org.janelia.it.workstation.gui.browser.gui.hud.Hud;
import org.janelia.it.workstation.gui.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.gui.browser.gui.listview.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.gui.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.gui.browser.gui.listview.ListViewerType;
import org.janelia.it.workstation.gui.browser.gui.support.ImageTypeSelectionButton;
import org.janelia.it.workstation.gui.browser.gui.support.ResultSelectionButton;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;
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

    private IconGridViewerConfiguration config;
    private AnnotatedDomainObjectList domainObjectList;
    private DomainObjectSelectionModel selectionModel;
    private DomainObjectSelectionModel editSelectionModel;
    private DomainObjectProviderHelper domainObjectProviderHelper = new DomainObjectProviderHelper();
    private SearchProvider searchProvider;

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
                result = SampleUtils.getResult(sample, resultButton.getResultDescriptor());
            }
            else if (domainObject instanceof HasFiles) {
                result = (HasFiles)domainObject;
            }
            return result==null? null : DomainUtils.getFilepath(result, typeButton.getImageTypeName());
        }

        @Override
        public BufferedImage getStaticIcon(DomainObject imageObject) {
            String filename = "question_block_large.png";
            if (imageObject instanceof Filter) {
                filename = "search_large.png";
            }
            else if (imageObject instanceof TreeNode) {
                filename = "folder_large.png";
            }
            ImageIcon icon = Icons.getIcon(filename);
            if (icon==null) return null;
            return Utils.toBufferedImage(icon.getImage());
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
    };

    public DomainObjectIconGridViewer() {
        setImageModel(imageModel);
        this.config = IconGridViewerConfiguration.loadConfig();
        resultButton = new ResultSelectionButton() {
            @Override
            protected void resultChanged(ResultDescriptor resultDescriptor) {
                log.info("Setting result preference: "+resultDescriptor.toString());
                try {
                    setPreference(DomainConstants.PREFERENCE_CATEGORY_SAMPLE_RESULT, ResultDescriptor.serialize(resultDescriptor));
                }
                catch (Exception e) {
                    log.error("Error serializing sample result preference: "+resultDescriptor,e);
                }
            }
        };
        typeButton = new ImageTypeSelectionButton() {
            @Override
            protected void imageTypeChanged(FileType fileType) {
                log.info("Setting image type preference: "+fileType);
                setPreference(DomainConstants.PREFERENCE_CATEGORY_IMAGE_TYPE, fileType.name());
            }
        };
        getToolbar().addCustomComponent(resultButton);
        getToolbar().addCustomComponent(typeButton);
    }

    private void setPreference(final String name, final String value) {

        Utils.setMainFrameCursorWaitStatus(true);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
                if (parentObject.getId()!=null) {
                    DomainMgr.getDomainMgr().setPreference(name, parentObject.getId().toString(), value);
                }
            }

            @Override
            protected void hadSuccess() {
                showDomainObjects(domainObjectList, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Utils.setMainFrameCursorWaitStatus(false);
                        return null;
                    }
                });
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setMainFrameCursorWaitStatus(false);
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
        log.info("selectEditObjects(domainObjects.size={},select={})", domainObjects.size(), select);

        if (domainObjects.isEmpty()) {
            return;
        }
        if (select) {
            editSelectionModel.select(domainObjects, true, true);
        }
    }


    @Override
    public void selectDomainObjects(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        log.info("selectDomainObjects(domainObjects.size={},select={},clearAll={},isUserDriven={})", domainObjects.size(), select, clearAll, isUserDriven);

        if (domainObjects.isEmpty()) {
            return;
        }

        if (select) {
            selectObjects(domainObjects, clearAll, isUserDriven);
        }
        else {
            deselectObjects(domainObjects, isUserDriven);
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

    @Override
    public void showDomainObjects(AnnotatedDomainObjectList objects, final Callable<Void> success) {

        this.domainObjectList = objects;
        log.debug("showDomainObjects(domainObjectList.size={})",domainObjectList.getDomainObjects().size());

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                StopWatch stopWatch = new LoggingStopWatch();

                final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
                if (parentObject!=null && parentObject.getId()!=null) {
                    Preference preference = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_SAMPLE_RESULT, parentObject.getId().toString());
                    log.debug("Got result preference: "+preference);
                    if (preference!=null) {
                        try {
                            ResultDescriptor resultDescriptor = ResultDescriptor.deserialize((String) preference.getValue());
                            resultButton.setResultDescriptor(resultDescriptor);
                        }
                        catch (Exception e) {
                            log.error("Error deserializing preference "+preference.getId(),e);
                        }
                    }
                    Preference preference2 = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_IMAGE_TYPE, parentObject.getId().toString());
                    log.info("Got image type preference: "+preference2);
                    if (preference2!=null) {
                        typeButton.setImageTypeName((String)preference2.getValue());
                    }
                }

                resultButton.populate(domainObjectList.getDomainObjects());
                typeButton.setResultDescriptor(resultButton.getResultDescriptor());
                typeButton.populate(domainObjectList.getDomainObjects());

                stopWatch.stop("showDomainObjects");
            }

            @Override
            protected void hadSuccess() {
                showObjects(domainObjectList.getDomainObjects(), success);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
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
        DomainObjectContextMenu popupMenu = new DomainObjectContextMenu((DomainObject)selectionModel.getParentObject(), getSelectedObjects(), resultButton.getResultDescriptor(), typeButton.getImageTypeName());
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
            getContextualPopupMenu().runDefaultAction();            
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
                    action.doAction();
                }
            }
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

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
            SessionMgr.getSessionMgr().handleException(e);
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
            hud.setObjectAndToggleDialog(domainObject, resultButton.getResultDescriptor(), typeButton.getImageTypeName());
        }
        else {
            hud.setObject(domainObject, resultButton.getResultDescriptor(), typeButton.getImageTypeName(), false);
        }
    }
    
    private List<DomainObject> getSelectedObjects() {
        try {
            return DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
        }  catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
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
                   log.debug("Restoring maxImageWidth={}", maxImageWidth);
                   // TODO: this needs to update the toolbar and trigger a repaint
                   imagesPanel.setMaxImageWidth(maxImageWidth);
               }
           }
        );
    }

    private class IconGridViewerState extends ListViewerState {

        private int maxImageWidth;

        public IconGridViewerState(int maxImageWidth) {
            super(ListViewerType.IconViewer);
            this.maxImageWidth = maxImageWidth;
        }

        public int getMaxImageWidth() {
            return maxImageWidth;
        }
    }
}
