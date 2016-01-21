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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.text.Position.Bias;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.browser.gui.find.FindContext;
import org.janelia.it.workstation.gui.browser.gui.find.FindContextRegistration;
import org.janelia.it.workstation.gui.browser.gui.find.FindToolbar;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.workstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelAdapter;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
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
public abstract class IconGridViewerPanel<T,S> extends JPanel implements FindContext {

    private static final Logger log = LoggerFactory.getLogger(IconGridViewerPanel.class);

    // Main components
    private IconGridViewerToolbar toolbar;
    private ImagesPanel<T,S> imagesPanel;
    private FindToolbar findToolbar;
    
    // These members deal with the context and entities within it
    private List<T> objectList;
    private Map<S,T> objectMap;
    private ImageModel<T,S> imageModel;
    private SelectionModel<T,S> selectionModel;
    // TODO: use this reference to implement an export button, like in TableViewerPanel
    private SearchProvider searchProvider;
    
    // UI state
    private int currTableHeight = ImagesPanel.DEFAULT_TABLE_HEIGHT;
    private Integer selectionAnchorIndex;
    private Integer selectionCurrIndex;

    // Listeners
    private final SessionModelListener sessionModelListener;

    public IconGridViewerPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);

        toolbar = createToolbar();
        toolbar.addMouseListener(new MouseForwarder(this, "ViewerToolbar->IconDemoPanel"));

        imagesPanel = new ImagesPanel<T,S>() {
            
            @Override
            protected void moreAnnotationsButtonDoubleClicked(AnnotatedImageButton<T, S> button) {
                IconGridViewerPanel.this.moreAnnotationsButtonDoubleClicked(button.getUserObject());
            }

            @Override
            protected JPopupMenu getPopupMenu(AnnotatedImageButton<T, S> button, Annotation annotation) {
                return IconGridViewerPanel.this.getAnnotationPopupMenu(annotation);
            }
        };

        imagesPanel.setButtonKeyListener(keyListener);
        addKeyListener(keyListener);
        imagesPanel.setButtonMouseListener(mouseListener);
        imagesPanel.addMouseListener(new MouseForwarder(this, "ImagesPanel->IconGridViewerPanel"));
        
        findToolbar = new FindToolbar(this);
        findToolbar.addMouseListener(new MouseForwarder(this, "FindToolbar->IconGridViewerPanel"));

        addHierarchyListener(new FindContextRegistration(this, this));
        addComponentListener(new ComponentAdapter() {
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
                    imagesPanel.setMaxImageWidth(toolbar.getCurrImageSize());
                    imagesPanel.recalculateGrid();
                    imagesPanel.scrollSelectedObjectsToCenter();
                    imagesPanel.loadUnloadImages();
                }
            }
        };
        
        SessionMgr.getSessionMgr().addSessionModelListener(sessionModelListener);
    }

    private IconGridViewerToolbar createToolbar() {

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
                        imagesPanel.scrollSelectedObjectsToCenter();
                    }
                });
            }
        };
    }

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
                    for (T object : objectList) {
                        selectObject(object, clearAll);
                        clearAll = false;
                    }
                    return;
                } 
                else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    // TODO: notify our hud container
//                    updateHud(true);
                    e.consume();
                    return;
                }
                else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    enterKeyPressed();
                    return;
                }
                else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteKeyPressed();
                    e.consume();
                    return;
                }

                if (e.isShiftDown() && (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT)) {
                    
                    if (selectionAnchorIndex==null) {
                        // Begin anchored selection mode
                        T selectionAnchorObject = getLastSelectedObject();
                        beginRangeSelection(objectList.indexOf(selectionAnchorObject));
                    }
                    
                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        if (selectionCurrIndex<1) return;
                        selectionCurrIndex -= 1;
                        if (selectionCurrIndex<selectionAnchorIndex) {
                            selectObject(objectList.get(selectionCurrIndex), false);
                        }
                        else if (selectionCurrIndex+1!=selectionAnchorIndex) {
                            deselectObject(objectList.get(selectionCurrIndex+1));
                        }
                    }
                    else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        if (selectionCurrIndex>=objectList.size()) return;
                        selectionCurrIndex += 1;
                        if (selectionCurrIndex>selectionAnchorIndex) {
                            selectObject(objectList.get(selectionCurrIndex), false);
                        }
                        else if (selectionCurrIndex-1!=selectionAnchorIndex) {
                            deselectObject(objectList.get(selectionCurrIndex-1));
                        }
                    }

                }
                else {
                    endRangeSelection();
                    T object = null;
                    if (e.getKeyCode() == KeyEvent.VK_TAB) {
                        if (e.isShiftDown()) {
                            object = getPreviousObject();
                        }
                        else {
                            object = getNextObject();
                        }
                    }
                    else {
                        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                            object = getPreviousObject();
                        }
                        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                            object = getNextObject();
                        }
                    }

                    if (object != null) {
                        selectObject(object, true);
                        // TODO: the rest of this should happen automatically as a consequence of the selectObject call
                        S id = getImageModel().getImageUniqueId(object);
                        AnnotatedImageButton<T,S> button = imagesPanel.getButtonById(id);
                        if (button != null) {
                            imagesPanel.scrollObjectToCenter(object);
                            button.requestFocus();
//                            updateHud(false);
                        }
                    }
                }
            }

            revalidate();
            repaint();
        }
    };
    
    protected void enterKeyPressed() {}
    
    protected void deleteKeyPressed() {}
    
    // Listener for clicking on buttons
    protected MouseListener mouseListener = new MouseHandler() {

        @Override
        protected void popupTriggered(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            AnnotatedImageButton<T,S> button = getButtonAncestor(e.getComponent());
            if (button!=null) {
                // Make sure the button is selected
                if (!button.isSelected()) {
                    buttonSelection(button, false, false);
                }
                getContextualPopupMenu().show(e.getComponent(), e.getX(), e.getY());
                e.consume();
            }
        }

        @Override
        protected void doubleLeftClicked(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            AnnotatedImageButton<T,S> button = getButtonAncestor(e.getComponent());
            if (button!=null) {
                final DomainObject domainObject = (DomainObject)button.getUserObject();
                buttonDrillDown(domainObject);
                e.consume();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            if (e.isConsumed()) {
                return;
            }
            AnnotatedImageButton<T,S> button = getButtonAncestor(e.getComponent());
            if (button!=null) {
                if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 1) {
                    return;
                }
    //            hud.setKeyListener(keyListener);
                buttonSelection(button, (SystemInfo.isMac && e.isMetaDown()) || e.isControlDown(), e.isShiftDown());
//                e.consume();
            }
        }
    };

    protected abstract void moreAnnotationsButtonDoubleClicked(T userObject);
    
    protected abstract JPopupMenu getAnnotationPopupMenu(Annotation annotation);
    
    protected abstract JPopupMenu getContextualPopupMenu();

    protected abstract void buttonDrillDown(DomainObject domainObject);

    protected void buttonSelection(AnnotatedImageButton<T,S> button, boolean multiSelect, boolean rangeSelect) {

        final T object = (T)button.getUserObject();
        final S uniqueId = getImageModel().getImageUniqueId(object);

        if (multiSelect) {
            // With the meta key we toggle items in the current selection without clearing it
            if (!button.isSelected()) {
                selectObject(object, false);
            }
            else {
                deselectObject(object);
            }
            endRangeSelection();
        }
        else {
            // With shift, we select ranges
            S lastSelectedId = selectionModel.getLastSelectedId();
            log.trace("lastSelectedId="+lastSelectedId);
            if (rangeSelect && lastSelectedId != null) {
                // Walk through the buttons and select everything between the last and current selections
                boolean selecting = false;
                for (T otherObject : objectList) {
                    final S otherUniqueId = getImageModel().getImageUniqueId(otherObject);
                    log.trace("Consider "+otherUniqueId);
                    if (otherUniqueId.equals(lastSelectedId) || otherUniqueId.equals(uniqueId)) {
                        if (otherUniqueId.equals(lastSelectedId)) {
                            log.trace("  Last selected!");
                        }
                        if (otherUniqueId.equals(uniqueId)) {
                            // Always select the button that was clicked
                            selectObject(otherObject, false);
                            // This becomes the selection anchor if the user keeps holding shift
                            beginRangeSelection(objectList.indexOf(otherObject));
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
                        selectObject(otherObject, false);
                        endRangeSelection();
                    }
                }
            }
            else {
                // This is a good old fashioned single button selection
                selectObject(object, true);
                endRangeSelection();
            }
        }

        button.requestFocus();
    }

    private void beginRangeSelection(int anchorIndex) {
        selectionAnchorIndex = selectionCurrIndex = anchorIndex;
    }
    
    private void endRangeSelection() {
        selectionAnchorIndex = selectionCurrIndex = null;
    }
    
    protected void selectObject(T object, boolean clearAll) {
        if (object==null) return;
        S id = getImageModel().getImageUniqueId(object);
        imagesPanel.setSelectionByUniqueId(id, true, clearAll);
        selectionModel.select(object, clearAll);
    }

    protected void deselectObject(T object) {
        if (object==null) return;
        S id = getImageModel().getImageUniqueId(object);
        imagesPanel.setSelectionByUniqueId(id, false, false);
        selectionModel.deselect(object);
    }

    private AnnotatedImageButton<T,S> getButtonAncestor(Component component) {
        Component c = component;
        while (!(c instanceof AnnotatedImageButton)) {
            c = c.getParent();
            if (c==null) return null;
        }
        return (AnnotatedImageButton<T,S>) c;
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
    
    public void showObjects(final List<T> objects, final Callable<Void> success) {
        
        log.debug("showObjects(objects.size={})",objects.size());

        // Cancel previous loads
        imagesPanel.cancelAllLoads();

        // Temporarily disable scroll loading
        imagesPanel.setScrollLoadingEnabled(false);

        // Set state
        setObjects(objects);
        
        // Create the image buttons
        imagesPanel.setImageObjects(objects);

        // Update preferences for each button
        Boolean tagTable = (Boolean) SessionMgr.getSessionMgr().getModelProperty(
                ViewerSettingsPanel.SHOW_ANNOTATION_TABLES_PROPERTY);
        if (tagTable == null) {
            tagTable = false;
        }

        imagesPanel.setTagTable(tagTable);
        imagesPanel.setTagVisbility(toolbar.areTagsVisible());
        imagesPanel.setTitleVisbility(toolbar.areTitlesVisible());

        // Actually display everything
        showAll();

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

    public void refreshObject(T object) {
        S uniqueId = imageModel.getImageUniqueId(object);
        for(AnnotatedImageButton<T,S> button : imagesPanel.getButtonsByUniqueId(uniqueId)) {
            button.refresh(object);
        }
    }

    public void refresh() {
        showObjects(objectList, null);
    }
    
    public void totalRefresh() {
        DomainMgr.getDomainMgr().getModel().invalidateAll();
    }

    public synchronized void clear() {
        this.objectList = null;
        removeAll();
        revalidate();
        repaint();
    }

    public void close() {
        // TODO: this should be called by something 
        SessionMgr.getSessionMgr().removeSessionModelListener(sessionModelListener);
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
    }

    public synchronized void showAll() {
        removeAll();
        add(toolbar, BorderLayout.NORTH);
        add(imagesPanel, BorderLayout.CENTER);
        add(findToolbar, BorderLayout.SOUTH);
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
        return objectMap.get(uniqueId);
    }
    
    private synchronized void setObjects(List<T> objectList) {
        log.debug("Setting {} objects",objectList.size());
        this.objectList = objectList;
        this.objectMap = new HashMap<>();
        for(T object : objectList) {
            objectMap.put(getImageModel().getImageUniqueId(object), object);
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

    
    protected Map<S, T> getObjectMap() {
        return objectMap;
    }
    
    public List<T> getObjectList() {
        return objectList;
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

    public IconGridViewerToolbar getToolbar() {
        return toolbar;
    }
    
    public void scrollSelectedObjectsToCenter() {
        imagesPanel.scrollSelectedObjectsToCenter();
    }

    @Override
    public void showFindUI() {
        findToolbar.open();
    }

    @Override
    public void hideFindUI() {
        findToolbar.close();
    }

    @Override
    public void findPrevMatch(String text, boolean skipStartingNode) {
        IconGridViewerFind<T,S> searcher = new IconGridViewerFind<>(this, text, getLastSelectedObject(), Bias.Backward, skipStartingNode);
        selectObject(searcher.find(), true);
        scrollSelectedObjectsToCenter();
    }

    @Override
    public void findNextMatch(String text, boolean skipStartingNode) {
        IconGridViewerFind<T,S> searcher = new IconGridViewerFind<>(this, text, getLastSelectedObject(), Bias.Forward, skipStartingNode);
        selectObject(searcher.find(), true);
        scrollSelectedObjectsToCenter();
    }

    @Override
    public void openMatch() {
    }
}
