package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.workstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.entity_model.management.UserColorMapping;
import org.janelia.it.workstation.gui.framework.outline.Annotations;
import org.janelia.it.workstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.MouseForwarder;
import org.janelia.it.workstation.gui.util.MouseHandler;
import org.janelia.it.workstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.workstation.gui.framework.keybind.KeymapUtil;

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
public abstract class IconGridViewerPanel<T,S> extends IconPanel<T,S> {

    private static final Logger log = LoggerFactory.getLogger(IconGridViewerPanel.class);

    // Main components
    private ImagesPanel<T,S> imagesPanel;
    private IconDemoToolbar iconDemoToolbar;

    private String currImageRole = EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE;
    
    // These members deal with the context and entities within it
    protected List<T> imageObjects;
    protected final Annotations annotations = new Annotations();
    protected final List<String> allUsers = new ArrayList<>();
    protected final Set<String> hiddenUsers = new HashSet<>();
    protected int currTableHeight = ImagesPanel.DEFAULT_TABLE_HEIGHT;
    protected final List<String> allImageRoles = new ArrayList<>();

    // Listeners
    protected SessionModelListener sessionModelListener;
    protected ModelMgrObserver modelMgrObserver;
    
    protected SelectionModel<T,S> selectionModel;
    

    // Listen for key strokes and execute the appropriate key bindings
    // TODO: we should replace this with an action map in the future
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
                    for (T imageObject : imageObjects) {
                        selectImageObject(imageObject, clearAll);
                        clearAll = false;
                    }
                    // TODO: notify our pagination container
//                    if (imageObjects.size() < allRootedEntities.size()) {
//                        selectionButtonContainer.setVisible(true);
//                    }
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
                    List<AnnotatedImageButton<T>> selected = imagesPanel.getSelectedButtons();
                    List<T> toDelete = new ArrayList<>();
                    for(AnnotatedImageButton<T> button : selected) {
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
                    S id = getImageUniqueId(imageObj);
                    AnnotatedImageButton button = imagesPanel.getButtonById(id);
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
            AnnotatedImageButton<T> button = getButtonAncestor(e.getComponent());
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
            AnnotatedImageButton button = getButtonAncestor(e.getComponent());
//            log.info("doubleLeftClicked: {}",button.getImageObject());
            buttonDrillDown(button);
            e.consume();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            if (e.isConsumed()) {
                return;
            }
            AnnotatedImageButton button = getButtonAncestor(e.getComponent());
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
    protected abstract void buttonDrillDown(AnnotatedImageButton button);

    protected void buttonSelection(AnnotatedImageButton button, boolean multiSelect, boolean rangeSelect) {
        
        final T imageObject = (T)button.getImageObject();
        final S uniqueId = getImageUniqueId(imageObject);
        
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
                for (T otherImageObject : imageObjects) {
                    final S otherUniqueId = getImageUniqueId(otherImageObject);
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
    
    private AnnotatedImageButton getButtonAncestor(Component component) {
        Component c = component;
        while (!(c instanceof AnnotatedImageButton)) {
            c = c.getParent();
        }
        return (AnnotatedImageButton) c;
    }

    public IconGridViewerPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);

        sessionModelListener = new SessionModelListener() {
            @Override
            public void browserAdded(BrowserModel browserModel) {
            }

            @Override
            public void browserRemoved(BrowserModel browserModel) {
            }

            @Override
            public void sessionWillExit() {
            }

            @Override
            public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
                if (key == "console.serverLogin") {
                    IconGridViewerPanel.this.clear();
                }
            }
        };
        SessionMgr.getSessionMgr().addSessionModelListener(sessionModelListener);


        iconDemoToolbar = createToolbar();
        iconDemoToolbar.addMouseListener(new MouseForwarder(this, "JToolBar->IconDemoPanel"));

        imagesPanel = new ImagesPanel<>(this);
        imagesPanel.setButtonKeyListener(keyListener);
        imagesPanel.setButtonMouseListener(buttonMouseListener);
        imagesPanel.addMouseListener(new MouseForwarder(this, "ImagesPanel->IconDemoPanel"));

        addKeyListener(keyListener);
        
//        imagesPanel.addMouseListener(new MouseHandler() {
//            @Override
//            protected void popupTriggered(MouseEvent e) {
//                JPopupMenu popupMenu = new JPopupMenu();
//                JMenuItem titleItem = new JMenuItem("" + contextRootedEntity.getEntity().getName());
//                titleItem.setEnabled(false);
//                popupMenu.add(titleItem);
//
//                JMenuItem newFolderItem = new JMenuItem("  Create New Folder");
//                newFolderItem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent actionEvent) {
//
//                        // Add button clicked
//                        String folderName = (String) JOptionPane.showInputDialog(IconDemoPanel.this, "Folder Name:\n",
//                                "Create folder under " + contextRootedEntity.getEntity().getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);
//                        if ((folderName == null) || (folderName.length() <= 0)) {
//                            return;
//                        }
//
//                        try {
//                            // Update database
//                            Entity parentFolder = contextRootedEntity.getEntity();
//                            Entity newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
//                            ModelMgr.getModelMgr().addEntityToParent(parentFolder, newFolder);
//                        }
//                        catch (Exception ex) {
//                            SessionMgr.getSessionMgr().handleException(ex);
//                        }
//                    }
//                });
//
//                if (!contextRootedEntity.getEntity().getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)
//                        || !contextRootedEntity.getEntity().getOwnerKey().equals(SessionMgr.getSubjectKey())) {
//                    newFolderItem.setEnabled(false);
//                }
//
//                popupMenu.add(newFolderItem);
//                popupMenu.show(imagesPanel, e.getX(), e.getY());
//            }
//        });
        
        modelMgrObserver = new ModelMgrAdapter() {

//            @Override
//            public void annotationsChanged(final long entityId) {
//                if (pageRootedEntities != null) {
//                    SwingUtilities.invokeLater(new Runnable() {
//                        @Override
//                        public void run() {
//                            reloadAnnotations(entityId);
//                            filterEntities();
//                        }
//                    });
//                }
//            }
//
//            @Override
//            public void entitySelected(String category, String entityId, boolean clearAll) {
//                IconGridViewerPanel.this.entitySelected(entityId, clearAll);
//            }
//
//            @Override
//            public void entityDeselected(String category, String entityId) {
//                if (category.equals(getSelectionCategory())) {
//                    IconGridViewerPanel.this.entityDeselected(entityId);
//                }
//            }
        };
//        ModelMgr.getModelMgr().addModelMgrObserver(modelMgrObserver);
//        ModelMgr.getModelMgr().registerOnEventBus(this);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                imagesPanel.recalculateGrid();
            }
        });

//        annotations.setFilter(new AnnotationFilter() {
//            @Override
//            public boolean accept(OntologyAnnotation annotation) {
//
//                // Hidden by user?
//                if (hiddenUsers.contains(annotation.getOwner())) {
//                    return false;
//                }
//                AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();
//
//                // Hidden by session?
//                Boolean onlySession = (Boolean) SessionMgr.getSessionMgr().getModelProperty(
//                        ViewerSettingsPanel.ONLY_SESSION_ANNOTATIONS_PROPERTY);
//                if ((onlySession != null && !onlySession) || session == null) {
//                    return true;
//                }
//
//                // At this point we know there is a current session, and we have to match it
//                return (annotation.getSessionId() != null && annotation.getSessionId().equals(session.getId()));
//            }
//        });

        SessionMgr.getSessionMgr().addSessionModelListener(new SessionModelListener() {

            @Override
            public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {

                if (ViewerSettingsPanel.ONLY_SESSION_ANNOTATIONS_PROPERTY.equals(key)) {
                    refreshAnnotations(null);
                }
                else if (ViewerSettingsPanel.HIDE_ANNOTATED_PROPERTY.equals(key)) {
//                    filterEntities();
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

            @Override
            public void sessionWillExit() {
            }

            @Override
            public void browserRemoved(BrowserModel browserModel) {
            }

            @Override
            public void browserAdded(BrowserModel browserModel) {
            }
        });
    }
    
    @Override
    public void registerAspectRatio(double aspectRatio) {
        imagesPanel.registerAspectRatio(aspectRatio);
    }

    @Override
    public int getMaxImageWidth() {
        return imagesPanel.getMaxImageWidth();
    }
    
    @Override
    public String getCurrImageRole() {
        return currImageRole;
    }

    @Override
    public void setCurrImageRole(String currImageRole) {
        this.currImageRole = currImageRole;
    }

    public void setSelectionModel(SelectionModel<T,S> selectionModel) {
        selectionModel.setSource(this);
        this.selectionModel = selectionModel;
    }
    
    public SelectionModel<T,S> getSelectionModel() {
        return selectionModel;
    }
    
//    @Subscribe
//    public void entityChanged(EntityChangeEvent event) {
//        Entity entity = event.getEntity();
//        if (contextRootedEntity == null) {
//            return;
//        }
//        if (contextRootedEntity.getEntity().getId().equals(entity.getId())) {
//            log.debug("({}) Reloading because context entity was changed: '{}'", getSelectionCategory(), entity.getName());
//            //loadEntity(contextRootedEntity, null);
//        }
//        else {
//            for (AnnotatedImageButton button : imagesPanel.getButtonsByEntityId(entity.getId())) {
//                log.debug("({}) Refreshing button because entity was changed: '{}'", getSelectionCategory(), entity.getName());
//
//                RootedEntity rootedEntity = button.getRootedEntity();
//                if (rootedEntity != null) {
//                    Entity buttonEntity = rootedEntity.getEntity();
//                    if (entity != buttonEntity) {
//                        log.warn("({}) entityChanged: Instance mismatch: " + entity.getName()
//                                + " (cached=" + System.identityHashCode(entity) + ") vs (this=" + System.identityHashCode(buttonEntity) + ")", getSelectionCategory());
//                        rootedEntity.setEntity(entity);
//                    }
//
//                    button.refresh(rootedEntity);
//                    imagesPanel.setMaxImageWidth(imagesPanel.getMaxImageWidth());
//                }
//            }
//        }
//    }
//
//    @Subscribe
//    public void entityRemoved(EntityRemoveEvent event) {
//        Entity entity = event.getEntity();
//        if (contextRootedEntity == null) {
//            return;
//        }
//        if (contextRootedEntity.getEntity() != null && contextRootedEntity.getEntityId().equals(entity.getId())) {
//            goParent();
//        }
//        else {
//            for (RootedEntity rootedEntity : new ArrayList<RootedEntity>(pageRootedEntities)) {
//                if (rootedEntity.getEntityId().equals(entity.getId())) {
//                    removeRootedEntity(rootedEntity);
//                    return;
//                }
//            }
//        }
//    }
//
//    @Subscribe
//    public void entityInvalidated(EntityInvalidationEvent event) {
//
//        if (contextRootedEntity == null) {
//            return;
//        }
//        boolean affected = false;
//
//        if (event.isTotalInvalidation()) {
//            affected = true;
//        }
//        else {
//            for (Entity entity : event.getInvalidatedEntities()) {
//                if (contextRootedEntity.getEntity() != null && contextRootedEntity.getEntityId().equals(entity.getId())) {
//                    affected = true;
//                    break;
//                }
//                else {
//                    for (final RootedEntity rootedEntity : new ArrayList<RootedEntity>(pageRootedEntities)) {
//                        if (rootedEntity.getEntityId().equals(entity.getId())) {
//                            affected = true;
//                            break;
//                        }
//                    }
//                    if (affected) {
//                        break;
//                    }
//                }
//            }
//        }
//
//        if (affected) {
//            log.debug("({}) Some entities were invalidated so we're refreshing the viewer", getSelectionCategory());
//            refresh(false, null);
//        }
//    }

    protected IconDemoToolbar createToolbar() {

        return new IconDemoToolbar() {

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

            @Override
            protected JPopupMenu getPopupUserMenu() {
                final JPopupMenu userListMenu = new JPopupMenu();
                UserColorMapping userColors = ModelMgr.getModelMgr().getUserColorMapping();

                // Save the list of users so that when the function actually runs, the
                // users it affects are the same users that were displayed
                final List<String> savedUsers = new ArrayList<>(allUsers);

                JMenuItem allUsersMenuItem = new JCheckBoxMenuItem("All Users", hiddenUsers.isEmpty());
                allUsersMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (hiddenUsers.isEmpty()) {
                            for (String username : savedUsers) {
                                hiddenUsers.add(username);
                            }
                        }
                        else {
                            hiddenUsers.clear();
                        }
                        refreshAnnotations(null);
                    }
                });
                userListMenu.add(allUsersMenuItem);

                userListMenu.addSeparator();

                for (final String username : savedUsers) {
                    JMenuItem userMenuItem = new JCheckBoxMenuItem(username, !hiddenUsers.contains(username));
                    userMenuItem.setBackground(userColors.getColor(username));
                    userMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (hiddenUsers.contains(username)) {
                                hiddenUsers.remove(username);
                            }
                            else {
                                hiddenUsers.add(username);
                            }
                            refreshAnnotations(null);
                        }
                    });
                    userMenuItem.setIcon(Icons.getIcon("user.png"));
                    userListMenu.add(userMenuItem);
                }

                return userListMenu;
            }

            @Override
            protected JPopupMenu getPopupImageRoleMenu() {

                final JPopupMenu imageRoleListMenu = new JPopupMenu();
                final List<String> imageRoles = new ArrayList<>(allImageRoles);

                for (final String imageRole : imageRoles) {
                    JMenuItem roleMenuItem = new JCheckBoxMenuItem(imageRole, imageRole.equals(getCurrImageRole()));
                    roleMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            setCurrImageRole(imageRole);
                            imageObjectsLoadDone(null);
                        }
                    });
                    imageRoleListMenu.add(roleMenuItem);
                }
                return imageRoleListMenu;
            }
        };
    }

    protected void selectImageObject(T imageObject, boolean clearAll) {
        final S id = getImageUniqueId(imageObject);
        imagesPanel.setSelectionByUniqueId(id, true, clearAll);
        selectionModel.select(imageObject, clearAll);
    }

    protected void deselectImageObject(T imageObject) {
        final S id = getImageUniqueId(imageObject);
        imagesPanel.setSelectionByUniqueId(id, false, false);
        selectionModel.deselect(imageObject);
    }

//    private void updateStatusBar() {
//        if (imageObjects == null) {
//            return;
//        }
//        EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
//        int s = esm.getSelectedEntitiesIds(getSelectionCategory()).size();
//        statusLabel.setText(s + " of " + allImageObjects.size() + " selected");
//        statusLabel.setText("Selection changed");
//    }

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

//    public synchronized void goParent() {
//        final String selectedUniqueId = contextRootedEntity.getUniqueId();
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                String parentId = Utils.getParentIdFromUniqueId(selectedUniqueId);
//                if (StringUtils.isEmpty(parentId)) {
//                    clear();
//                }
//                else {
//                    ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, parentId, true);
//                }
//            }
//        });
//    }
    
    public void showImageObjects(List<T> imageObjects) {
        showImageObjects(imageObjects, null);
    }
    
    public void showImageObjects(List<T> imageObjects, final Callable<Void> success) {

        // Cancel previous loads
        imagesPanel.cancelAllLoads();

        // Temporarily disable scroll loading
        imagesPanel.setScrollLoadingEnabled(false);

        setImageObjects(imageObjects);
        imageObjectsLoadDone(success);
    }
    
//    public synchronized void loadEntity(RootedEntity rootedEntity, final Callable<Void> success) {
//
//        this.contextRootedEntity = rootedEntity;
//        if (contextRootedEntity == null) {
//            return;
//        }
//
//        Entity entity = contextRootedEntity.getEntity();
//
//        log.debug("loadEntity {} (@{})", entity.getName(), System.identityHashCode(entity));
//
//        List<EntityData> eds = ModelUtils.getSortedEntityDatas(entity);
//        List<EntityData> children = new ArrayList<EntityData>();
//        for (EntityData ed : eds) {
//            Entity child = ed.getChildEntity();
//            if (!EntityUtils.isHidden(ed) && child != null && !(child instanceof ForbiddenEntity)) {
//                children.add(ed);
//            }
//        }
//
//        List<RootedEntity> lazyRootedEntities = new ArrayList<RootedEntity>();
//        for (EntityData ed : children) {
//            String childId = ModelUtils.getChildUniqueId(rootedEntity.getUniqueId(), ed);
//            lazyRootedEntities.add(new RootedEntity(childId, ed));
//        }
//
//        if (lazyRootedEntities.isEmpty()) {
//            lazyRootedEntities.add(rootedEntity);
//        }
//
//        // Update back/forward navigation
////        EntitySelectionHistory history = getViewerPane().getEntitySelectionHistory();
////        iconDemoToolbar.getPrevButton().setEnabled(history.isBackEnabled());
////        iconDemoToolbar.getNextButton().setEnabled(history.isNextEnabled());
//
//        loadImageEntities(lazyRootedEntities, success);
//    }
//
//    public void loadImageEntities(final List<RootedEntity> lazyRootedEntities) {
//        // TODO: revisit this, since it doesn't set contextRootedEntity
//        loadImageEntities(lazyRootedEntities, null);
//    }

//    private synchronized void loadImageEntities(final List<RootedEntity> lazyRootedEntities, final Callable<Void> success) {
//
//        this.allRootedEntitiesByEntityId.clear();
//        this.allRootedEntitiesByPathId.clear();
//        this.allRootedEntities = lazyRootedEntities;
//        for (RootedEntity re : allRootedEntities) {
//            allRootedEntitiesByEntityId.put(re.getEntityId(), re);
//            allRootedEntitiesByPathId.put(re.getId(), re);
//        }
//
//        this.numPages = (int) Math.ceil((double) allRootedEntities.size() / (double) PAGE_SIZE);
//        loadImageEntities(0, success);
//    }

    private synchronized void imageObjectsLoadDone(final Callable<Void> success) {
        
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

        // Since the images are not loaded yet, this will just resize the empty
        // buttons so that we can calculate the grid correctly
        imagesPanel.resizeTables(imagesPanel.getCurrTableHeight());
        imagesPanel.setMaxImageWidth(imagesPanel.getMaxImageWidth());

        // Update selection
//        EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
//        esm.deselectAll(getSelectionCategory());

        // Actually display everything
        showImagePanel();

        // Wait until everything is recomputed
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                imagesPanel.recalculateGrid();
                imagesPanel.setScrollLoadingEnabled(true);

                // Finally, we're done, we can call the success callback
                ConcurrentUtils.invokeAndHandleExceptions(success);
            }
        });
    }

//    protected void removeRootedEntity(final RootedEntity rootedEntity) {
//        int index = getRootedEntities().indexOf(rootedEntity);
//        if (index < 0) {
//            return;
//        }
//
//        pageRootedEntities.remove(rootedEntity);
//        allRootedEntities.remove(rootedEntity);
//        allRootedEntitiesByEntityId.removeAll(rootedEntity.getEntityId());
//        allRootedEntitiesByPathId.removeAll(rootedEntity.getId());
//
//        this.numPages = (int) Math.ceil((double) allRootedEntities.size() / (double) PAGE_SIZE);
//        updatePagingStatus();
//
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                RootedEntity next = getNextEntity();
//                if (next != null) {
//                    ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), next.getId(), true);
//                }
//                imagesPanel.removeRootedEntity(rootedEntity);
//                imagesPanel.recalculateGrid();
//            }
//        });
//    }

//    private void filterEntities() {
//
//        AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();
//        if (session == null) {
//            return;
//        }
//        session.clearCompletedIds();
//        Set<Long> completed = session.getCompletedEntityIds();
//
//        imagesPanel.showAllButtons();
//        Boolean hideAnnotated = (Boolean) SessionMgr.getSessionMgr().getModelProperty(
//                ViewerSettingsPanel.HIDE_ANNOTATED_PROPERTY);
//        if (hideAnnotated != null && hideAnnotated) {
//            imagesPanel.hideButtons(completed);
//        }
//    }

    /**
     * Reload the annotations from the database and then refresh the UI.
     */
//    public synchronized void reloadAnnotations(final Long entityId) {
//
//        if (annotations == null || pageRootedEntities == null) {
//            return;
//        }
//        
//        annotationLoadingWorker = new SimpleWorker() {
//
//            protected void doStuff() throws Exception {
//                annotations.reload(entityId);
//            }
//
//            protected void hadSuccess() {
//                refreshAnnotations(entityId);
//            }
//
//            protected void hadError(Throwable error) {
//                SessionMgr.getSessionMgr().handleException(error);
//            }
//        };
//
//        annotationLoadingWorker.execute();
//    }

    /**
     * Refresh the annotation display in the UI, but do not reload anything from
     * the database.
     */
    private synchronized void refreshAnnotations(Long entityId) {
        // Refresh all user list
        allUsers.clear();
        for (OntologyAnnotation annotation : annotations.getAnnotations()) {
            String name = EntityUtils.getNameFromSubjectKey(annotation.getOwner());
            if (!allUsers.contains(name)) {
                allUsers.add(name);
            }
        }
        Collections.sort(allUsers);
        imagesPanel.setAnnotations(annotations);

        if (entityId == null) {
            imagesPanel.showAllAnnotations();
        }
        else {
            imagesPanel.showAnnotationsForEntity(entityId);
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
        this.imageObjects = null;
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
        if (imageObjects == null) {
            return null;
        }
        int i = imageObjects.indexOf(getLastSelectedObject());
        if (i < 1) {
            // Already at the beginning
            return null;
        }
        return imageObjects.get(i - 1);
    }

    public T getNextObject() {
        if (imageObjects == null) {
            return null;
        }
        int i = imageObjects.indexOf(getLastSelectedObject());
        if (i > imageObjects.size() - 2) {
            // Already at the end
            return null;
        }
        return imageObjects.get(i + 1);
    }
    
    protected abstract void populateImageRoles(List<T> imageObjects);

    private synchronized void setImageObjects(List<T> imageObjects) {
        log.debug("Setting {} image objects",imageObjects.size());
        this.imageObjects = imageObjects;
        populateImageRoles(imageObjects);
        iconDemoToolbar.getImageRoleButton().setEnabled(!allImageRoles.isEmpty());
        if (!allImageRoles.contains(getCurrImageRole())) {
            setCurrImageRole(FileType.SignalMip.toString());
        }
    }

    public synchronized T getLastSelectedObject() {
        S uniqueId = selectionModel.getLastSelectedId();
        if (uniqueId == null) {
            return null;
        }
        AnnotatedImageButton<T> button = imagesPanel.getButtonById(uniqueId);
        if (button == null) {
            return null;
        }
        return button.getImageObject();
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

    public IconDemoToolbar getToolbar() {
        return iconDemoToolbar;
    }

//    public Hud getHud() {
//        return hud;
//    }

    public Annotations getAnnotations() {
        return annotations;
    }

//    public RootedEntity getContextRootedEntity() {
//        return contextRootedEntity;
//    }
//
//    private List<RootedEntity> getRootedEntitiesById(String id) {
//        List<RootedEntity> res = new ArrayList<RootedEntity>();
//        // Assume these are path ids
//        res.addAll(allRootedEntitiesByPathId.get(id));
//        if (res.isEmpty()) {
//            // Maybe we were given entity GUIDs
//            try {
//                Long entityId = Long.parseLong(id);
//                res.addAll(allRootedEntitiesByEntityId.get(entityId));
//            }
//            catch (NumberFormatException e) {
//                // Expected
//            }
//        }
//        return res;
//    }
//
//    public RootedEntity getRootedEntityById(String id) {
//        List<RootedEntity> res = getRootedEntitiesById(id);
//        if (res == null || res.isEmpty()) {
//            return null;
//        }
//        return res.get(0);
//    }

    @Override
    public boolean areTitlesVisible() {
        return getToolbar().areTitlesVisible();
    }

    @Override
    public boolean areTagsVisible() {
        return getToolbar().areTagsVisible();
    }

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
