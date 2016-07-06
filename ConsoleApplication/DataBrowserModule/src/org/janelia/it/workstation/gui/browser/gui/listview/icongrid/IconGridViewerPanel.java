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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.KeyBindings;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.browser.gui.keybind.KeyboardShortcut;
import org.janelia.it.workstation.gui.browser.gui.keybind.KeymapUtil;
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
 * 2) Item selection and navigation
 * 3) Toolbar with various features
 * 4) HUD display for currently selected image
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class IconGridViewerPanel<T,S> extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(IconGridViewerPanel.class);

    // Main components
    private IconGridViewerToolbar toolbar;
    protected ImagesPanel<T,S> imagesPanel;

    // These members deal with the context and entities within it
    private List<T> objectList;
    private Map<S,T> objectMap;
    private ImageModel<T,S> imageModel;
    private SelectionModel<T,S> selectionModel;
    
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
        imagesPanel.setButtonMouseListener(mouseListener);
        imagesPanel.addMouseListener(new MouseForwarder(this, "ImagesPanel->IconGridViewerPanel"));

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
            protected void configButtonPressed() {
                IconGridViewerPanel.this.configButtonPressed();
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
            if (!KeyBindings.getKeyBindings().executeBinding(shortcut)) {
                
                // No keybinds matched, use the default behavior
                // Ctrl-A or Meta-A to select all
                if (e.getKeyCode() == KeyEvent.VK_A && ((SystemInfo.isMac && e.isMetaDown()) || (e.isControlDown()))) {
                    selectObjects(objectList, true, true);
                    return;
                } 
                else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    updateHud(true);
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
                            userSelectObject(objectList.get(selectionCurrIndex), false);
                        }
                        else if (selectionCurrIndex+1!=selectionAnchorIndex) {
                            userDeselectObject(objectList.get(selectionCurrIndex+1));
                        }
                    }
                    else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        if (selectionCurrIndex>=objectList.size()-1) return;
                        selectionCurrIndex += 1;
                        if (selectionCurrIndex>selectionAnchorIndex) {
                            userSelectObject(objectList.get(selectionCurrIndex), false);
                        }
                        else if (selectionCurrIndex-1!=selectionAnchorIndex) {
                            userDeselectObject(objectList.get(selectionCurrIndex-1));
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
                        userSelectObject(object, true);
                    }
                }
            }

            revalidate();
            repaint();
        }
    };

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
                JPopupMenu popupMenu = getContextualPopupMenu();
                if (popupMenu!=null) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    e.consume();
                }
            }
        }

        @Override
        protected void doubleLeftClicked(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            AnnotatedImageButton<T,S> button = getButtonAncestor(e.getComponent());
            if (button!=null) {
                objectDoubleClick((T)button.getUserObject());
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
                buttonSelection(button, (SystemInfo.isMac && e.isMetaDown()) || e.isControlDown(), e.isShiftDown());
                e.consume();
            }
        }
    };

    protected void enterKeyPressed() {
        T selectedObject = getLastSelectedObject();
        objectDoubleClick(selectedObject);
    }

    protected void deleteKeyPressed() {}

    protected abstract void objectDoubleClick(T object);

    protected abstract JPopupMenu getContextualPopupMenu();

    protected abstract void moreAnnotationsButtonDoubleClicked(T userObject);
    
    protected abstract JPopupMenu getAnnotationPopupMenu(Annotation annotation);

    protected abstract void configButtonPressed();

    protected void updateHud(boolean toggle) {}

    protected void buttonSelection(AnnotatedImageButton<T,S> button, boolean multiSelect, boolean rangeSelect) {

        final T object = button.getUserObject();
        final S uniqueId = getImageModel().getImageUniqueId(object);

        log.trace("buttonSelection(uniqueId={},multiSelect={},rangeSelect={})",uniqueId,multiSelect,rangeSelect);

        if (multiSelect) {
            // With the meta key we toggle items in the current selection without clearing it
            if (!button.isSelected()) {
                userSelectObject(object, false);
            }
            else {
                userDeselectObject(object);
            }
            endRangeSelection();
        }
        else {
            // With shift, we select ranges
            S lastSelectedId = selectionModel.getLastSelectedId();
            if (rangeSelect && lastSelectedId != null) {
                log.trace("Selecting range starting with lastSelectedId={}",lastSelectedId);
                // Walk through the buttons and select everything between the last and current selections
                boolean selecting = false;
                for (T otherObject : objectList) {
                    final S otherUniqueId = getImageModel().getImageUniqueId(otherObject);
                    log.trace("Considering: "+otherUniqueId);
                    if (otherUniqueId.equals(lastSelectedId) || otherUniqueId.equals(uniqueId)) {
                        // Start or end, either way we need to begin selecting
                        if (otherUniqueId.equals(lastSelectedId)) {
                            log.trace("  This was the last selected button");
                        }
                        if (otherUniqueId.equals(uniqueId)) {
                            // Always select the button that was clicked
                            userSelectObject(otherObject, false);
                            // This becomes the selection anchor if the user keeps holding shift
                            int index = objectList.indexOf(otherObject);
                            log.trace("  Begin range selection mode (index={})",index);
                            beginRangeSelection(index);
                        }
                        if (selecting) {
                            log.trace("  End selecting");
                            button.requestFocus();
                            return; // We already selected, this is the end
                        }
                        log.trace("  Begin selecting");
                        selecting = true; // Start selecting
                        continue; // Skip selection of the first and last items, which should already be selected
                    }
                    if (selecting) {
                        userSelectObject(otherObject, false);
                        log.trace("  End range selection mode");
                        endRangeSelection();
                    }
                }
            }
            else {
                // This is a good old fashioned single button selection
                userSelectObject(object, true);
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

    protected void userSelectObject(T object, boolean clearAll) {
        selectObjects(Arrays.asList(object), clearAll, true);

        S id = getImageModel().getImageUniqueId(object);
        AnnotatedImageButton<T,S> button = imagesPanel.getButtonById(id);
        if (button != null) {
            imagesPanel.scrollObjectToCenter(object);
            button.requestFocus();
            updateHud(false);
        }
    }

    protected void userDeselectObject(T object) {
        deselectObjects(Arrays.asList(object), true);
    }
    
    protected void selectObjects(List<T> objects, boolean clearAll, boolean isUserDriven) {
        if (objects==null) return;
        List<S> ids = new ArrayList<>();
        for(T object : objects) {
            ids.add(getImageModel().getImageUniqueId(object));
        }
        imagesPanel.setSelectionByUniqueIds(ids, true, clearAll);
        selectionModel.select(objects, clearAll, isUserDriven);
    }

    protected void selectEditObjects(List<T> objects) {
        if (objects==null) return;
        List<S> ids = new ArrayList<>();
        for(T object : objects) {
            ids.add(getImageModel().getImageUniqueId(object));
        }
        imagesPanel.setEditSelection(ids, true);
    }
    
    protected void deselectObjects(List<T> objects, boolean isUserDriven) {
        if (objects==null) return;
        List<S> ids = new ArrayList<>();
        for(T object : objects) {
            ids.add(getImageModel().getImageUniqueId(object));
        }
        imagesPanel.setSelectionByUniqueIds(ids, false, false);
        selectionModel.deselect(objects, isUserDriven);
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

    private AnnotatedImageButton<T,S> getButtonAncestor(Component component) {
        Component c = component;
        while (!(c instanceof AnnotatedImageButton)) {
            c = c.getParent();
            if (c==null) return null;
        }
        return (AnnotatedImageButton<T,S>) c;
    }

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
                imagesPanel.loadUnloadImages();

                // Finally, we're done, we can call the success callback
                ConcurrentUtils.invokeAndHandleExceptions(success);
            }
        });
    }

    public void refresh() {
        showObjects(objectList, null);
    }
    
    public void totalRefresh() {
        DomainMgr.getDomainMgr().getModel().invalidateAll();
    }

    public void refreshObject(T object) {
        S uniqueId = imageModel.getImageUniqueId(object);
        for(AnnotatedImageButton<T,S> button : imagesPanel.getButtonsByUniqueId(uniqueId)) {
            button.refresh(object);
        }
    }

    private void setObjects(List<T> objectList) {
        log.debug("Setting {} objects",objectList.size());
        this.objectList = objectList;
        this.objectMap = new HashMap<>();
        for(T object : objectList) {
            objectMap.put(getImageModel().getImageUniqueId(object), object);
        }
    }

    public synchronized void clear() {
        this.objectList = null;
        removeAll();
        revalidate();
        repaint();
    }

    public void close() {
        // TODO: this should be invoked somehow if the panel is closed
        SessionMgr.getSessionMgr().removeSessionModelListener(sessionModelListener);
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
    }

    public synchronized void showAll() {
        removeAll();
        add(toolbar, BorderLayout.NORTH);
        add(imagesPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
    
    protected ImageModel<T, S> getImageModel() {
        return imageModel;
    }

    protected void setImageModel(ImageModel<T, S> imageModel) {
        this.imageModel = imageModel;
        imagesPanel.setImageModel(imageModel);
    }
    
    public void setSelectionModel(SelectionModel<T,S> selectionModel) {
        selectionModel.setSource(this);
        this.selectionModel = selectionModel;
        imagesPanel.setSelectionModel(selectionModel);
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
}
