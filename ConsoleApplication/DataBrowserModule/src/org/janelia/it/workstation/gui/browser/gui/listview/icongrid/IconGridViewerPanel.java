package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.workstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelAdapter;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.workstation.gui.util.MouseForwarder;
import org.janelia.it.workstation.gui.util.MouseHandler;
import org.janelia.it.workstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This viewer shows images in a grid. It is modeled after OS X Finder. It wraps an ImagesPanel and provides a lot of
 * functionality on top of it, such as:
 * 1) Asynchronous entity loading
 * 2) Entity selection and navigation
 * 3) Toolbar with various features
 * 4) HUD display for currently selected image
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class IconGridViewerPanel<T,S> extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(IconGridViewerPanel.class);

    // Main components
    private ImagesPanel<T,S> imagesPanel;
    private IconGridViewerToolbar iconDemoToolbar;
    
    // These members deal with the context and entities within it
    private List<T> objectList;
    private Map<S,T> imageObjectMap;
    private int currTableHeight = ImagesPanel.DEFAULT_TABLE_HEIGHT;
    private ImageModel<T,S> imageModel;
    private SelectionModel<T,S> selectionModel;
    private SearchProvider searchProvider;

    // Listeners
    private final SessionModelListener sessionModelListener;
    private ModelMgrObserver modelMgrObserver;
    
    // Listen for key strokes and execute the appropriate key bindings
    protected KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {

            if (KeymapUtil.isModifier(e)) {
                return;
            }
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return;
            }

            KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);
            if (!SessionMgr.getKeyBindings().executeBinding(shortcut)) {

                // No keybinds matched, use the default behavior
                // Ctrl-A or Meta-A to select all
                if (e.getKeyCode() == KeyEvent.VK_A && ((SystemInfo.isMac && e.isMetaDown()) || (e.isControlDown()))) {
                    boolean clearAll = true;
                    for (T imageObject : objectList) {
                        selectImageObject(imageObject, clearAll);
                        clearAll = false;
                    }
                    searchProvider.userRequestedSelectAll();
                    return;
                }

                // Space on a single entity triggers a preview 
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    // TODO: notify our hud container
//                    updateHud(true);
                    e.consume();
                    return;
                }

                // Enter with a single entity selected triggers an outline
                // navigation
//                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
//                    List<S> selectedIds = selectionModel.getSelectedIds();
//                    if (selectedIds.size() != 1) {
//                        return;
//                    }
//                    S selectedId = selectedIds.get(0);
//                    T selectedObject = getImageByUniqueId(selectedId);
//                    selectionModel.select(selectedObject, true);
//                    return;
//                }

                // Delete triggers deletion
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    List<AnnotatedImageButton<T,S>> selected = imagesPanel.getSelectedButtons();
                    List<T> toDelete = new ArrayList<>();
                    for(AnnotatedImageButton<T,S> button : selected) {
                        T imageObject = button.getImageObject();
                        toDelete.add(imageObject);
                    }
                    
                    if (selected.isEmpty()) {
                        return;
                    }
                    // TODO: implement DomainObject deletion
//                    final Action action = new RemoveEntityAction(toDelete, true, false);
//                    action.doAction();
                    e.consume();
                    return;
                }

                // Tab and arrow navigation to page through the images
                boolean clearAll = false;
                T imageObj = null;
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    clearAll = true;
                    if (e.isShiftDown()) {
                        imageObj = getPreviousObject();
                    }
                    else {
                        imageObj = getNextObject();
                    }
                }
                else {
                    clearAll = true;
                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        imageObj = getPreviousObject();
                    }
                    else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        imageObj = getNextObject();
                    }
                }

                if (imageObj != null) {
                    S id = getImageModel().getImageUniqueId(imageObj);
                    AnnotatedImageButton<T,S> button = imagesPanel.getButtonById(id);
                    if (button != null) {
                        selectImageObject(imageObj, clearAll);
                        imagesPanel.scrollObjectToCenter(imageObj);
                        button.requestFocus();
//                        updateHud(false);
                    }
                }
            }

            revalidate();
            repaint();
        }
    };

    // Listener for clicking on buttons
    protected MouseListener buttonMouseListener = new MouseHandler() {

        @Override
        protected void popupTriggered(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            AnnotatedImageButton<T,S> button = getButtonAncestor(e.getComponent());
            // Make sure the button is selected
            if (!button.isSelected()) {
                buttonSelection(button, false, false);
            }
//            log.info("popupTriggered: {}",button.getImageObject());
            getButtonPopupMenu().show(e.getComponent(), e.getX(), e.getY());
            e.consume();
        }

        @Override
        protected void doubleLeftClicked(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            AnnotatedImageButton<T,S> button = getButtonAncestor(e.getComponent());
            final DomainObject domainObject = (DomainObject)button.getImageObject();
//            log.info("doubleLeftClicked: {}",button.getImageObject());
            buttonDrillDown(domainObject);
            e.consume();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            if (e.isConsumed()) {
                return;
            }
            AnnotatedImageButton<T,S> button = getButtonAncestor(e.getComponent());
            if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 1) {
                return;
            }
//            log.info("mouseReleased: {}",button.getImageObject());
//            hud.setKeyListener(keyListener);
            buttonSelection(button, (SystemInfo.isMac && e.isMetaDown()) || e.isControlDown(), e.isShiftDown());
            e.consume();
        }
    };

    protected abstract JPopupMenu getButtonPopupMenu();

    /**
     * This is a separate method so that it can be overridden to accommodate other behavior patterns.
     */
    protected abstract void buttonDrillDown(DomainObject domainObject);

    protected void buttonSelection(AnnotatedImageButton<T,S> button, boolean multiSelect, boolean rangeSelect) {
        
        final T imageObject = (T)button.getImageObject();
        final S uniqueId = getImageModel().getImageUniqueId(imageObject);
        
//        selectionButtonContainer.setVisible(false);

        if (multiSelect) {
            // With the meta key we toggle items in the current
            // selection without clearing it
            if (!button.isSelected()) {
                selectImageObject(imageObject, false);
            }
            else {
                deselectImageObject(imageObject);
            }
        }
        else {
            // With shift, we select ranges
            S lastSelectedId = selectionModel.getLastSelectedId();
            log.trace("lastSelectedId="+lastSelectedId);
            if (rangeSelect && lastSelectedId != null) {
                // Walk through the buttons and select everything between the last and current selections
                boolean selecting = false;
                for (T otherImageObject : objectList) {
                    final S otherUniqueId = getImageModel().getImageUniqueId(otherImageObject);
                    log.trace("Consider "+otherUniqueId);
                    if (otherUniqueId.equals(lastSelectedId) || otherUniqueId.equals(uniqueId)) {
                        if (otherUniqueId.equals(lastSelectedId)) {
                            log.trace("  Last selected!");
                        }
                        if (otherUniqueId.equals(uniqueId)) {
                            // Always select the button that was clicked
                            selectImageObject(otherImageObject, false);
                        }
                        if (selecting) {
                            log.trace("  End selecting");
                            return; // We already selected, this is the end
                        }
                        log.trace("  Begin selecting");
                        selecting = true; // Start selecting
                        continue; // Skip selection of the first and last items, which should already be selected
                    }
                    if (selecting) {
                        selectImageObject(otherImageObject, false);
                    }
                }
            }
            else {
                // This is a good old fashioned single button selection
                selectImageObject(imageObject, true);
            }
        }

        button.requestFocus();
    }
    
    private AnnotatedImageButton<T,S> getButtonAncestor(Component component) {
        Component c = component;
        while (!(c instanceof AnnotatedImageButton)) {
            c = c.getParent();
        }
        return (AnnotatedImageButton<T,S>) c;
    }

    public IconGridViewerPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);

        iconDemoToolbar = createToolbar();
        iconDemoToolbar.addMouseListener(new MouseForwarder(this, "JToolBar->IconDemoPanel"));

        imagesPanel = new ImagesPanel<>();
        imagesPanel.setButtonKeyListener(keyListener);
        imagesPanel.setButtonMouseListener(buttonMouseListener);
        imagesPanel.addMouseListener(new MouseForwarder(this, "ImagesPanel->IconDemoPanel"));

        addKeyListener(keyListener);
        
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                imagesPanel.recalculateGrid();
            }
        });

        sessionModelListener = new SessionModelAdapter() {

            @Override
            public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {

                if (key == "console.serverLogin") {
                    IconGridViewerPanel.this.clear();
                }
                else if (ViewerSettingsPanel.SHOW_ANNOTATION_TABLES_PROPERTY.equals(key)) {
                    refresh();
                }
                else if (ViewerSettingsPanel.ANNOTATION_TABLES_HEIGHT_PROPERTY.equals(key)) {
                    int tableHeight = (Integer) newValue;
                    if (currTableHeight == tableHeight) {
                        return;
                    }
                    currTableHeight = tableHeight;
                    imagesPanel.resizeTables(tableHeight);
                    imagesPanel.setMaxImageWidth(iconDemoToolbar.getCurrImageSize());
                    imagesPanel.recalculateGrid();
                    imagesPanel.scrollSelectedEntitiesToCenter();
                    imagesPanel.loadUnloadImages();
                }
            }
        };
        
        SessionMgr.getSessionMgr().addSessionModelListener(sessionModelListener);
    }

    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
    }
    
    protected ImageModel<T, S> getImageModel() {
        return imageModel;
    }

    protected void setImageModel(ImageModel<T, S> imageModel) {
        this.imageModel = imageModel;
        imagesPanel.setImageModel(imageModel);
    }
    
    public void setSelectionModel(SelectionModel<T,S> selectionModel) {
        this.selectionModel = selectionModel;
        imagesPanel.setSelectionModel(selectionModel);
        selectionModel.setSource(this);
    }
    
    public SelectionModel<T,S> getSelectionModel() {
        return selectionModel;
    }
    
    protected IconGridViewerToolbar createToolbar() {

        return new IconGridViewerToolbar() {

            @Override
            protected void refresh() {
                IconGridViewerPanel.this.totalRefresh();
            }

            @Override
            protected void showTitlesButtonPressed() {
                imagesPanel.setTitleVisbility(showTitlesButton.isSelected());
                imagesPanel.recalculateGrid();
            }

            @Override
            protected void showTagsButtonPressed() {
                imagesPanel.setTagVisbility(showTagsButton.isSelected());
                imagesPanel.recalculateGrid();
            }

            @Override
            protected void currImageSizeChanged(int imageSize) {
                imagesPanel.setMaxImageWidth(imageSize);
                imagesPanel.recalculateGrid();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        imagesPanel.scrollSelectedEntitiesToCenter();
                    }
                });
            }
        };
    }

    protected void selectImageObject(T imageObject, boolean clearAll) {
        final S id = getImageModel().getImageUniqueId(imageObject);
        imagesPanel.setSelectionByUniqueId(id, true, clearAll);
        selectionModel.select(imageObject, clearAll);
    }

    protected void deselectImageObject(T imageObject) {
        final S id = getImageModel().getImageUniqueId(imageObject);
        imagesPanel.setSelectionByUniqueId(id, false, false);
        selectionModel.deselect(imageObject);
    }

    /**
     * This should be called by any handler that wishes to show/unshow the HUD.
     */
//    private void updateHud(boolean toggle) {
//        List<String> selectedIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory());
//        if (selectedIds.size() != 1) {
//            hud.hideDialog();
//            return;
//        }
//        Entity entity = null;
//        String selectedId = selectedIds.get(0);
//        for (RootedEntity re : getRootedEntitiesById(selectedId)) {
//            // Get the image from the annotated image button which is also a Dynamic Image Button.
//            final AnnotatedImageButton button = imagesPanel.getButtonById(re.getId());
//            if (button instanceof DynamicImageButton) {
//                entity = re.getEntity();
//                break;   // Only one.
//            }
//        }
//        if (toggle) {
//            hud.setEntityAndToggleDialog(entity);
//        }
//        else {
//            hud.setEntity(entity);
//        }
//    }
    
//    public void showImageObjects(List<T> imageObjects) {
//        showImageObjects(imageObjects, null);
//    }
    
    public void showImageObjects(final List<T> imageObjects, final Callable<Void> success) {
        
        log.debug("showImageObjects(imageObjects.size={})",imageObjects.size());
        
        // Cancel previous loads
        imagesPanel.cancelAllLoads();

        // Temporarily disable scroll loading
        imagesPanel.setScrollLoadingEnabled(false);

        // Set state
        setImageObjects(imageObjects);
        
        // Create the image buttons
        imagesPanel.setImageObjects(imageObjects);

        // Update preferences for each button
        Boolean tagTable = (Boolean) SessionMgr.getSessionMgr().getModelProperty(
                ViewerSettingsPanel.SHOW_ANNOTATION_TABLES_PROPERTY);
        if (tagTable == null) {
            tagTable = false;
        }

        imagesPanel.setTagTable(tagTable);
        imagesPanel.setTagVisbility(iconDemoToolbar.areTagsVisible());
        imagesPanel.setTitleVisbility(iconDemoToolbar.areTitlesVisible());

        // Actually display everything
        showImagePanel();

        // Wait until everything is recomputed
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                imagesPanel.resizeTables(imagesPanel.getCurrTableHeight());
                imagesPanel.setMaxImageWidth(imagesPanel.getMaxImageWidth());
                imagesPanel.setScrollLoadingEnabled(true);
                
                // Finally, we're done, we can call the success callback
                ConcurrentUtils.invokeAndHandleExceptions(success);
            }
        });
    }

    public void refreshImageObject(T imageObject) {
        S uniqueId = imageModel.getImageUniqueId(imageObject);
        for(AnnotatedImageButton<T,S> button : imagesPanel.getButtonsByUniqueId(uniqueId)) {
            button.refresh(imageObject);
        }
    }
    
    public void refresh() {
        refresh(false, null);
    }

    public void totalRefresh() {
        refresh(true, null);
    }

    public void refresh(final Callable<Void> successCallback) {
        refresh(false, successCallback);
    }

    public void totalRefresh(final Callable<Void> successCallback) {
        refresh(true, successCallback);
    }

    private AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    public void refresh(final boolean invalidateCache, final Callable<Void> successCallback) {

//        if (contextImageObject == null) {
//            return;
//        }
//
//        if (refreshInProgress.getAndSet(true)) {
//            log.debug("Skipping refresh, since there is one already in progress");
//            return;
//        }
//
//        log.debug("Starting a refresh");
//
//        final List<String> selectedIds = new ArrayList<String>(ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory()));
//        final Callable<Void> success = new Callable<Void>() {
//            @Override
//            public Void call() throws Exception {
//                // At the very end, reselect our buttons if possible
//                boolean first = true;
//                for (String selectedId : selectedIds) {
//                    ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), selectedId, first);
//                    first = false;
//                }
//                // Now call the user's callback 
//                if (successCallback != null) {
//                    successCallback.call();
//                }
//                return null;
//            }
//        };
//
//        SimpleWorker refreshWorker = new SimpleWorker() {
//
//            T imageObject = contextImageObject;
//
//            protected void doStuff() throws Exception {
//                if (invalidateCache) {
//                    ModelMgr.getModelMgr().invalidateCache(rootedEntity.getEntity(), true);
//                }
//                Entity entity = ModelMgr.getModelMgr().getEntityAndChildren(rootedEntity.getEntity().getId());
//                rootedEntity.setEntity(entity);
//            }
//
//            protected void hadSuccess() {
//                SwingUtilities.invokeLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (rootedEntity.getEntity() == null) {
//                            clear();
//                            if (success != null) {
//                                try {
//                                    success.call();
//                                }
//                                catch (Exception e) {
//                                    hadError(e);
//                                }
//                            }
//                        }
//                        else {
//                            //loadEntity(rootedEntity, success);
//                        }
//                        refreshInProgress.set(false);
//                        log.debug("Refresh complete");
//                    }
//                });
//            }
//
//            protected void hadError(Throwable error) {
//                refreshInProgress.set(false);
//                SessionMgr.getSessionMgr().handleException(error);
//            }
//        };
//
//        refreshWorker.execute();
    }

    public synchronized void clear() {
        this.objectList = null;
        removeAll();
        revalidate();
        repaint();
    }

    public void close() {
        SessionMgr.getSessionMgr().removeSessionModelListener(sessionModelListener);
        ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrObserver);
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
    }

    public synchronized void showImagePanel() {

        removeAll();
        add(iconDemoToolbar, BorderLayout.NORTH);
        add(imagesPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    public T getPreviousObject() {
        if (objectList == null) {
            return null;
        }
        int i = objectList.indexOf(getLastSelectedObject());
        if (i < 1) {
            // Already at the beginning
            return null;
        }
        return objectList.get(i - 1);
    }

    public T getNextObject() {
        if (objectList == null) {
            return null;
        }
        int i = objectList.indexOf(getLastSelectedObject());
        if (i > objectList.size() - 2) {
            // Already at the end
            return null;
        }
        return objectList.get(i + 1);
    }

    public synchronized T getLastSelectedObject() {
        S uniqueId = selectionModel.getLastSelectedId();
        if (uniqueId == null) {
            return null;
        }
        return imageObjectMap.get(uniqueId);
    }
    
    private synchronized void setImageObjects(List<T> objectList) {
        log.debug("Setting {} image objects",objectList.size());
        this.objectList = objectList;
        this.imageObjectMap = new HashMap<>();
        for(T imageObject : objectList) {
            imageObjectMap.put(getImageModel().getImageUniqueId(imageObject), imageObject);
        }
    }

//
//    public List<RootedEntity> getSelectedEntities() {
//        List<RootedEntity> selectedEntities = new ArrayList<RootedEntity>();
//        if (pageRootedEntities == null) {
//            return selectedEntities;
//        }
//        for (RootedEntity rootedEntity : pageRootedEntities) {
//            AnnotatedImageButton button = imagesPanel.getButtonById(rootedEntity.getId());
//            if (button.isSelected()) {
//                selectedEntities.add(rootedEntity);
//            }
//        }
//        return selectedEntities;
//    }

    public IconGridViewerToolbar getToolbar() {
        return iconDemoToolbar;
    }
    
//    public Hud getHud() {
//        return hud;
//    }

//    public EntityViewerState saveViewerState() {
//        // We could get this from the EntitySelectionModel, but sometimes that 
//        // doesn't have the latest select the user is currently making.
//        Set<String> selectedIds = new HashSet<String>();
//        for(AnnotatedImageButton button : imagesPanel.getSelectedButtons()) {
//            selectedIds.add(button.getRootedEntity().getId());
//        }
//        return new EntityViewerState(getClass(), contextRootedEntity, selectedIds);
//    }
//
//    public void restoreViewerState(final EntityViewerState state) {
//        // It's critical to call loadEntity in the ViewerPane not the local one.
//        // The ViewerPane version does extra stuff to get the ancestors button
//        // and breadcrumbs to show correctly.
//        getViewerPane().loadEntity(state.getContextRootedEntity(), new Callable<Void>() {
//            @Override
//            public Void call() throws Exception {
//                // Go to the right page
//                int i = 0;
//                int firstIdIndex = 0;
//                for (RootedEntity rootedEntity : allRootedEntities) {
//                    if (state.getSelectedIds().contains(rootedEntity.getId())) {
//                        firstIdIndex = i;
//                        break;
//                    }
//                    i++;
//                }
//
//                Callable<Void> makeSelections = new Callable<Void>() {
//                    @Override
//                    public Void call() throws Exception {
//                        for (String selectedId : state.getSelectedIds()) {
//                            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), selectedId, false);
//                        }
//                        // Wait for all selections to finish before we scroll
//                        SwingUtilities.invokeLater(new Runnable() {
//                            @Override
//                            public void run() {
//                                imagesPanel.scrollSelectedEntitiesToCenter();
//                            }
//                        });
//                        return null;
//                    }
//                };
//
//                int page = (int) Math.floor((double) firstIdIndex / (double) PAGE_SIZE);
//                if (page != currPage) {
//                    loadImageEntities(page, makeSelections);
//                }
//                else {
//                    makeSelections.call();
//                }
//
//                return null;
//            }
//        });
//    }
}
