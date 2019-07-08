package org.janelia.workstation.browser.gui.listview.icongrid;

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

import org.janelia.workstation.browser.gui.options.BrowserOptions;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.events.lifecycle.SessionEvent;
import org.janelia.workstation.core.events.prefs.LocalPreferenceChanged;
import org.janelia.workstation.core.events.selection.SelectionModel;
import org.janelia.workstation.core.keybind.KeyBindings;
import org.janelia.workstation.core.keybind.KeyboardShortcut;
import org.janelia.workstation.core.keybind.KeymapUtil;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.janelia.workstation.common.gui.support.MouseHandler;
import org.janelia.workstation.core.util.ConcurrentUtils;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.model.domain.ontology.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

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
                return IconGridViewerPanel.this.getAnnotationPopupMenu(button.getUserObject(), annotation);
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
            protected void customizeTitlesPressed() {
                IconGridViewerPanel.this.customizeTitlesPressed();
            }

            @Override
            protected void currImageSizeChanged(int imageSize) {
                imagesPanel.setMaxImageWidth(imageSize);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        imagesPanel.scrollSelectedObjectsToCenter();
                    }
                });
            }

            @Override
            protected boolean isMustHaveImage() {
                return IconGridViewerPanel.this.isMustHaveImage();
            }

            @Override
            protected void setMustHaveImage(boolean mustHaveImage) {
                IconGridViewerPanel.this.setMustHaveImage(mustHaveImage);
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
                // Important: we have to consume the event BEFORE queueing on the EDT. The AWTEventMulticaster
                // may issue the same event again in the meantime, and if we process it twice, we'll get a
                // flickering popup menu.
                e.consume();
                SwingUtilities.invokeLater(() -> {
                    JPopupMenu popupMenu = getContextualPopupMenu();
                    if (popupMenu != null) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                });
            }
        }

        @Override
        protected void doubleLeftClicked(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            AnnotatedImageButton<T,S> button = getButtonAncestor(e.getComponent());
            if (button!=null) {
                objectDoubleClick(button.getUserObject());
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
    
    protected abstract JPopupMenu getAnnotationPopupMenu(T userObject, Annotation annotation);

    protected abstract void customizeTitlesPressed();

    protected abstract void setMustHaveImage(boolean mustHaveImage);

    protected abstract boolean isMustHaveImage();
    
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
            imagesPanel.scrollObjectToCenterIfOutsideViewport(object);
            button.requestFocus();
            updateHud(false);
        }
    }

    protected void userDeselectObject(T object) {
        deselectObjects(Arrays.asList(object), true);
    }
    
    protected void selectObjects(List<T> objects, boolean clearAll, boolean isUserDriven) {
        selectObjects(objects, clearAll, isUserDriven, true);
    }
    
    protected void selectObjects(List<T> objects, boolean clearAll, boolean isUserDriven, boolean informModel) {
        if (objects==null) return;
        List<S> ids = new ArrayList<>();
        for(T object : objects) {
            ids.add(getImageModel().getImageUniqueId(object));
        }
        imagesPanel.setSelectionByUniqueIds(ids, true, clearAll);
        if (informModel) {
            selectionModel.select(objects, clearAll, isUserDriven);    
        }
        
        // Need to repaint the entire panel. Sometimes individual buttons do not repaint correctly for some reason.
        imagesPanel.repaint();
    }

    protected void selectEditObjects(List<T> objects) {
        if (objects==null) return;
        List<S> ids = new ArrayList<>();
        for(T object : objects) {
            ids.add(getImageModel().getImageUniqueId(object));
        }
        imagesPanel.setEditSelection(ids);
    }

    protected void deselectObjects(List<T> objects, boolean isUserDriven) {
        deselectObjects(objects, isUserDriven, true);
    }
    
    protected void deselectObjects(List<T> objects, boolean isUserDriven, boolean informModel) {
        if (objects==null) return;
        List<S> ids = new ArrayList<>();
        for(T object : objects) {
            ids.add(getImageModel().getImageUniqueId(object));
        }
        imagesPanel.setSelectionByUniqueIds(ids, false, false);
        if (informModel) {
            selectionModel.deselect(objects, isUserDriven);
        }
        
        imagesPanel.repaint();
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

    @SuppressWarnings("unchecked")
    private AnnotatedImageButton<T,S> getButtonAncestor(Component component) {
        Component c = component;
        while (!(c instanceof AnnotatedImageButton)) {
            c = c.getParent();
            if (c==null) return null;
        }
        return (AnnotatedImageButton<T,S>) c;
    }

    public void showObjects(final List<T> objects, final Callable<Void> success) {
        
        if (objects==null) {
            log.debug("showObjects given null list of objects");
            return;
        }
        
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
        boolean tagTable = BrowserOptions.getInstance().isShowAnnotationTables();

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
                updateUI(); // Make sure buttons are displayed before calling load/unload to load their contents

                SwingUtilities.invokeLater(new Runnable() {
                   @Override
                   public void run() {
                       imagesPanel.loadUnloadImages();
                   }
                });

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
        scrollSelectedObjectsToCenter();
    }

    protected void setObjects(List<T> objectList) {
        this.objectList = objectList;
        this.objectMap = new HashMap<>();
        for(T object : objectList) {
            objectMap.put(getImageModel().getImageUniqueId(object), object);
        }
    }
    
    protected List<T> getObjects() {
        return objectList;
    }

    public synchronized void clear() {
        this.objectList = null;
        removeAll();
        revalidate();
        repaint();
    }

    public synchronized void showAll() {
        removeAll();
        add(toolbar, BorderLayout.NORTH);
        add(imagesPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
    
    public ImageModel<T, S> getImageModel() {
        return imageModel;
    }

    public void setImageModel(ImageModel<T, S> imageModel) {
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
    
    @Subscribe
    public void prefChange(LocalPreferenceChanged event) {

        Object key = event.getKey();
        if (BrowserOptions.SHOW_ANNOTATION_TABLES_PROPERTY.equals(key)) {
            refresh();
        }
        else if (BrowserOptions.ANNOTATION_TABLES_HEIGHT_PROPERTY.equals(key)) {
            int tableHeight = (Integer) event.getNewValue();
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
    
    @Subscribe
    public void sessionChanged(SessionEvent event) {
        IconGridViewerPanel.this.clear();
    }
}
