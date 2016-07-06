package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.browser.gui.support.AnnotationTablePanel;
import org.janelia.it.workstation.gui.browser.gui.support.AnnotationTagCloudPanel;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Self-adjusting grid of image buttons which may be resized together.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class ImagesPanel<T,S> extends JScrollPane {

    private static final Logger log = LoggerFactory.getLogger(ImagesPanel.class);

    // Constants
    public static final int MIN_IMAGE_WIDTH = 100;
    public static final int DEFAULT_THUMBNAIL_SIZE = 300;
    public static final int MAX_IMAGE_WIDTH = 1000;
    public static final int MIN_TABLE_HEIGHT = 50;
    public static final int DEFAULT_TABLE_HEIGHT = 200;
    public static final int MAX_TABLE_HEIGHT = 500;
    
    // Listeners
    private KeyListener buttonKeyListener;
    private MouseListener buttonMouseListener;

    // UI Components
    private final HashMap<S, AnnotatedImageButton<T,S>> buttons = new LinkedHashMap<>();
    private final ScrollableGridPanel buttonsPanel;
    
    // Model
    private ImageModel<T,S> imageModel;
    private SelectionModel<T,S> selectionModel;
    private SelectionModel<T,S> editSelectionModel;

    // State
    private boolean tagTable = false;
    private boolean titlesVisible = true;
    private boolean tagsVisible = true;
    private final AtomicBoolean loadUnloadImagesInterrupt = new AtomicBoolean(false);
    private Double lowestAspectRatio;
    private Integer maxImageWidth = DEFAULT_THUMBNAIL_SIZE;
    private Integer currTableHeight = DEFAULT_TABLE_HEIGHT;
    private Rectangle currViewRect;
    private Timer timer;

    // Listen for scroll events
    private final AdjustmentListener scrollListener = new AdjustmentListener() {
        @Override
        public void adjustmentValueChanged(final AdjustmentEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final JViewport viewPort = getViewport();
                    Rectangle viewRect = viewPort.getViewRect();
                    if (viewRect.equals(currViewRect)) {
                        return;
                    }
                    currViewRect = viewRect;
                    if (!e.getValueIsAdjusting()) {
                        loadUnloadImages();
                    }

                    if (timer != null) {
                        timer.cancel();
                    }

                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            loadUnloadImages();
                        }
                    }, 500);
                }
            });
        }
    };

    public ImagesPanel() {
        buttonsPanel = new ScrollableGridPanel();
        setViewportView(buttonsPanel);
        setBorder(BorderFactory.createEmptyBorder());
        if (!SessionMgr.getSessionMgr().isDarkLook()) {
            getViewport().setBackground(Color.white);
        }
    }
    
    public void setImageModel(ImageModel<T,S> imageModel) {
        this.imageModel = imageModel;
    }
    
    public void setSelectionModel(SelectionModel<T, S> selectionModel) {
        this.selectionModel = selectionModel;
    }

    public void setEditSelectionModel(SelectionModel<T, S> editSelectionModel) {
        this.editSelectionModel = editSelectionModel;
    }
    
    /**
     * Returns the button with the given unique id.
     */
    public AnnotatedImageButton<T,S> getButtonById(S uniqueId) {
        return buttons.get(uniqueId);
    }

    public void setButtonKeyListener(KeyListener buttonKeyListener) {
        this.buttonKeyListener = buttonKeyListener;
    }

    public void setButtonMouseListener(MouseListener buttonMouseListener) {
        this.buttonMouseListener = buttonMouseListener;
    }

    public void setScrollLoadingEnabled(boolean enabled) {
        if (enabled) {
            // Reset scroll bar and re-add the listener
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getVerticalScrollBar().setValue(0);
                    getVerticalScrollBar().addAdjustmentListener(scrollListener);
                }
            });
        }
        else {
            // Remove the scroll listener so that we don't get a bunch of bogus events as things are added to the imagesPanel
            getVerticalScrollBar().removeAdjustmentListener(scrollListener);
            getVerticalScrollBar().setValue(0);
        }
    }

    public void cancelAllLoads() {
        for (AnnotatedImageButton<T,S> button : buttons.values()) {
            if (button instanceof DynamicImageButton) {
                ((DynamicImageButton<T,S>) button).cancelLoad();
            }
        }
    }

    /**
     * Create the image buttons, but leave the images unloaded for now.
     */
    public void setImageObjects(List<T> imageObjects) {
        
        buttons.clear();
        for (Component component : buttonsPanel.getComponents()) {
            if (component instanceof AnnotatedImageButton) {
                AnnotatedImageButton<T,S> button = (AnnotatedImageButton<T,S>) component;
                buttonsPanel.remove(button);
            }
        }

        this.lowestAspectRatio = null;

        for (final T imageObject : imageObjects) {
            S imageId = imageModel.getImageUniqueId(imageObject);
            
            if (buttons.containsKey(imageId)) {
                continue;
            }            

            AnnotatedImageButton<T,S> button = AnnotatedImageButton.create(imageObject, imageModel, selectionModel, this);

            button.setTitleVisible(titlesVisible);
            button.setTagsVisible(tagsVisible);

            if (buttonKeyListener != null) {
                button.addKeyListener(buttonKeyListener);
            }
            if (buttonMouseListener != null) {
                button.addMouseListener(buttonMouseListener);
            }

            button.addMouseListener(new MouseForwarder(this, "AnnotatedImageButton->ImagesPanel"));

            // Disable tab traversal, we will do it ourselves
            button.setFocusTraversalKeysEnabled(false);

            buttons.put(imageId, button);
            buttonsPanel.add(button);
        }
    }

    public void removeImageObject(T imageObject) {
        S imageId = imageModel.getImageUniqueId(imageObject);
        AnnotatedImageButton<T,S> button = buttons.get(imageId);
        if (button == null) {
            return; // Button was already removed
        }
        buttonsPanel.remove(button);
        buttons.remove(imageId);
    }

    public List<AnnotatedImageButton<T,S>> getButtonsByUniqueId(S uniqueId) {
        List<AnnotatedImageButton<T,S>> imageButtons = new ArrayList<>();
        for (AnnotatedImageButton<T,S> button : buttons.values()) {
            S imageId = imageModel.getImageUniqueId(button.getUserObject());
            if (imageId.equals(uniqueId)) {
                imageButtons.add(button);
            }
        }
        return imageButtons;
    }

    /**
     * Scale all the images to the given max size.
     */
    public synchronized void setMaxImageWidth(int maxImageWidth) {
        if (maxImageWidth < MIN_IMAGE_WIDTH || maxImageWidth > MAX_IMAGE_WIDTH) {
            return;
        }
        log.trace("setMaxImageWidth: {}", maxImageWidth);
        this.maxImageWidth = maxImageWidth;

        double aspectRatio = lowestAspectRatio == null ? 1.0 : lowestAspectRatio;

        int maxImageHeight = (int) Math.round(maxImageWidth / aspectRatio);

        for (AnnotatedImageButton<T,S> button : buttons.values()) {
            try {
                button.setImageSize(maxImageWidth, maxImageHeight);
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
        }

        recalculateGrid();
    }

    public synchronized void resizeTables(int tableHeight) {
        if (tableHeight < MIN_TABLE_HEIGHT || tableHeight > MAX_TABLE_HEIGHT) {
            return;
        }
        log.trace("resizeTables: {}", tableHeight);
        this.currTableHeight = tableHeight;
        for (AnnotatedImageButton<T,S> button : buttons.values()) {
            try {
                button.resizeTable(tableHeight);
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
        }
    }

    public synchronized void showAllButtons() {
        buttonsPanel.removeAll();
        for (AnnotatedImageButton<T,S> button : buttons.values()) {
            buttonsPanel.add(button);
        }
        revalidate();
        repaint();
    }

    public synchronized void hideButtons(Collection<S> uniqueIds) {
        for (S uniqueId : uniqueIds) {
            AnnotatedImageButton<T,S> button = getButtonById(uniqueId);
            buttonsPanel.remove(button);
        }
        revalidate();
        repaint();
    }

    public int getMaxImageWidth() {
        return maxImageWidth;
    }

    public int getCurrTableHeight() {
        return currTableHeight;
    }

    public void scrollToBottom() {
        getViewport().scrollRectToVisible(new Rectangle(0, buttonsPanel.getHeight(), 1, 1));
    }

    public void scrollObjectToCenter(T imageObject) {
        if (imageObject == null) {
            return;
        }
        S uniqueId = imageModel.getImageUniqueId(imageObject);
        AnnotatedImageButton<T,S> selectedButton = getButtonById(uniqueId);
        scrollButtonToCenter(selectedButton);
    }

    public void scrollButtonToVisible(AnnotatedImageButton<T,S> button) {
        if (button == null) {
            return;
        }
        getViewport().scrollRectToVisible(button.getBounds());
    }

    public void scrollButtonToCenter(AnnotatedImageButton<T,S> button) {

        if (button == null) {
            return;
        }

        // This rectangle is relative to the table where the
        // northwest corner of cell (0,0) is always (0,0).
        Rectangle rect = button.getBounds();
        log.trace("Button rect: {}",rect);
        
        // The location of the view relative to the table
        Rectangle viewRect = viewport.getViewRect();
        log.trace("View rect: {}",viewRect);
        
        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0).
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        // Calculate location of rect if it were at the center of view
        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;

        // Fake the location of the cell so that scrollRectToVisible
        // will move the cell to the center
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);
        
        // Scroll the area into view.
        log.debug("Scroll to visible: {}",rect);
        viewport.scrollRectToVisible(rect);
    }

    public void scrollButtonToTop(AnnotatedImageButton<T,S> button) {

        if (button == null) {
            return;
        }

	    // This rectangle is relative to the table where the
        // northwest corner of cell (0,0) is always (0,0).
        Rectangle rect = button.getBounds();

        // The location of the view relative to the table
        Rectangle viewRect = viewport.getViewRect();

	    // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0). Also make the rect as large as the view, 
        // so that the relevant portion goes to the top.
        rect.setBounds(rect.x - viewRect.x, rect.y - viewRect.y, viewRect.width, viewRect.height);

        // Scroll the area into view.
        viewport.scrollRectToVisible(rect);
    }

    public void scrollSelectedObjectsToCenter() {
        log.debug("Scrolling selected objects to center");
        List<AnnotatedImageButton<T,S>> selected = getSelectedButtons();
        if (selected.isEmpty()) {
            return;
        }
        int i = selected.size() / 2;
        AnnotatedImageButton<T,S> centerOfMass = selected.get(i);
        scrollButtonToCenter(centerOfMass);
    }

    public void scrollSelectedEntitiesToTop() {
        List<AnnotatedImageButton<T,S>> selected = getSelectedButtons();
        if (selected.isEmpty()) {
            return;
        }
        scrollButtonToTop(selected.get(0));
    }

    public synchronized void registerAspectRatio(Double aspectRatio) {
        if (lowestAspectRatio == null || aspectRatio < lowestAspectRatio) {
            this.lowestAspectRatio = aspectRatio;
        }
    }

    public void setTitleVisbility(boolean visible) {
        this.titlesVisible = visible;
        for (AnnotatedImageButton<T,S> button : buttons.values()) {
            button.setTitleVisible(visible);
        }
    }

    public void setTagVisbility(boolean visible) {
        this.tagsVisible = visible;
        for (AnnotatedImageButton<T,S> button : buttons.values()) {
            button.setTagsVisible(visible);
        }
    }

    public void setTagTable(boolean tagTable) {
        this.tagTable = tagTable;
        for (final AnnotatedImageButton<T,S> button : buttons.values()) {
            ensureCorrectAnnotationView(button);
        }
    }

    public void setEditMode(boolean mode) {
        for (final AnnotatedImageButton<T,S> button : buttons.values()) {
            button.toggleEditMode(mode);
        }
    }
    
    void ensureCorrectAnnotationView(final AnnotatedImageButton<T, S> button) {
        if (tagTable) {
            if (button.getAnnotationView() instanceof AnnotationTablePanel) {
                return;
            }
            button.setAnnotationView(new AnnotationTablePanel());
        }
        else {
            if (button.getAnnotationView() instanceof AnnotationTagCloudPanel) {
                return;
            }
            button.setAnnotationView(new AnnotationTagCloudPanel() {
                @Override
                protected void moreDoubleClicked(MouseEvent e) {
                    ImagesPanel.this.moreAnnotationsButtonDoubleClicked(button);
                }
                @Override
                protected JPopupMenu getPopupMenu(Annotation tag) {
                    return ImagesPanel.this.getPopupMenu(button, tag);
                }
            });
        }
    }

    protected abstract void moreAnnotationsButtonDoubleClicked(AnnotatedImageButton<T,S> button);
    
    protected abstract JPopupMenu getPopupMenu(AnnotatedImageButton<T,S> button, Annotation annotation);
    
    private boolean setSelection(final AnnotatedImageButton<T,S> button, boolean selection) {
        if (button.isSelected() != selection) {
            button.setSelected(selection);
            return true;
        }
        return false;
    }

    private boolean setEditSelection(final AnnotatedImageButton<T,S> button, boolean selection) {
        updateEditSelectModel(button.getUserObject(), selection);
        if (button.getEditModeValue() != selection) {
            button.setEditModeValue(selection);
            return true;
        }
        return false;
    }

    public void updateEditSelectModel(T imgObject, boolean select) {
        if (select) {
            editSelectionModel.select(imgObject, false, true);
        } else {
            editSelectionModel.deselect(imgObject, true);
        }
    }

    public void setSelection(T selectedObject, boolean selection, boolean clearAll) {
        if (clearAll) {
            for (AnnotatedImageButton<T,S> button : buttons.values()) {
                T imageObject = button.getUserObject();
                if (imageObject.equals(selectedObject)) {
                    setSelection(button, true);
                }
                else {
                    setSelection(button, false);
                }
            }
        }
        else {
            S selectedId = imageModel.getImageUniqueId(selectedObject);
            AnnotatedImageButton<T,S> button = buttons.get(selectedId);
            if (button != null) {
                setSelection(button, selection);
            }
        }
    }
    
    public void setSelectedObjects(Set<T> selectedObjects) {
        for (AnnotatedImageButton<T,S> button : buttons.values()) {
            T imageObject = button.getUserObject();
            if (selectedObjects.contains(imageObject)) {
                setSelection(button, true);
            }
            else {
                setSelection(button, false);
            }
        }
    }
    
    public void setSelectionByUniqueIds(List<S> selectedIds, boolean selection, boolean clearAll) {
        for (AnnotatedImageButton<T,S> button : buttons.values()) {
            T imageObject = button.getUserObject();
            S imageId = imageModel.getImageUniqueId(imageObject);
            if (selectedIds.contains(imageId)) {
                setSelection(button, selection);
            }
            else if (clearAll) {
                setSelection(button, !selection);
            }
        }
    }

    public void setEditSelection(List<S> selectedIds, boolean editSelection) {
        for (AnnotatedImageButton<T,S> button : buttons.values()) {
            T imageObject = button.getUserObject();
            S imageId = imageModel.getImageUniqueId(imageObject);
            if (selectedIds.contains(imageId)) {
                setEditSelection(button, editSelection);
            }
        }
    }

    public List<AnnotatedImageButton<T,S>> getSelectedButtons() {
        List<AnnotatedImageButton<T,S>> selected = new ArrayList<>();
        for (AnnotatedImageButton<T,S> button : buttons.values()) {
            if (button.isSelected()) {
                selected.add(button);
            }
        }
        return selected;
    }

    public void repaintButtons() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                buttonsPanel.revalidate();
                buttonsPanel.repaint();
            }
        });
    }

    /**
     * Set the number of columns in the grid layout based on the width of the parent component and the width of the
     * buttons.
     *
     * This method must be called in the EDT.
     */
    public synchronized void recalculateGrid() {

        log.trace("Recalculating image grid");

        double maxButtonWidth = 0;
        for (AnnotatedImageButton<T,S> button : buttons.values()) {
            int w = button.getPreferredSize().width;
            if (w > maxButtonWidth) {
                maxButtonWidth = w;
            }
        }

        // Should not be needed, but just in case, lets make sure we never divide by zero
        if (maxButtonWidth == 0) {
            maxButtonWidth = 400;
        }

        int fullWidth = getSize().width - getVerticalScrollBar().getWidth();

        int numCols = (int) Math.max(Math.floor(fullWidth / maxButtonWidth), 1);
        if (buttonsPanel.getColumns() != numCols) {
            buttonsPanel.setColumns(numCols);
            repaintButtons();
        }

        loadUnloadImages();
    }

    private Date lastQueueDate = new Date();

    public void loadUnloadImages() {
        loadUnloadImagesInterrupt.set(true);
        final Date queueDate = new Date();
        lastQueueDate = queueDate;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (queueDate.before(lastQueueDate)) {
                    log.trace("Ignoring duplicate loadUnloadImages request");
                    return;
                }
                loadUnloadImagesInterrupt.set(false);
                final JViewport viewPort = getViewport();
                Rectangle viewRect = viewPort.getViewRect();
                log.trace("Running loadUnloadImages (viewRect: {})",viewRect);
                if (buttonsPanel.getColumns() == 1) {
                    viewRect.setSize(viewRect.width, viewRect.height + 100);
                }
                for (AnnotatedImageButton<T,S> button : buttons.values()) {
                    if (loadUnloadImagesInterrupt.get()) {
                        log.trace("loadUnloadImages interrupted");
                        return;
                    }
                    try {
                        boolean wantViewable = viewRect.intersects(button.getBounds());
                        log.trace("viewRect.intersects(({}) = {}",button.getBounds(),wantViewable);
                        button.setViewable(wantViewable);
                    }
                    catch (Exception e) {
                        SessionMgr.getSessionMgr().handleException(e);
                    }
                }
            }
        });
    }

    private class ScrollableGridPanel extends JPanel implements Scrollable {

        public ScrollableGridPanel() {
            setLayout(new GridLayout(0, 2));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setOpaque(false);
            for (ComponentListener l : getComponentListeners()) {
                removeComponentListener(l);
            }
        }

        @Override
        public synchronized void addComponentListener(ComponentListener l) {
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 30;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 300;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return false;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        public int getColumns() {
            return ((GridLayout) getLayout()).getColumns();
        }

        public void setColumns(int columns) {
            ((GridLayout) getLayout()).setColumns(columns);
        }
    }
}
