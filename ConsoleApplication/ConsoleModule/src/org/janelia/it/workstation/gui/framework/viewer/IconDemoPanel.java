package org.janelia.it.workstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.ForbiddenEntity;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.workstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.workstation.api.entity_model.events.EntityChangeEvent;
import org.janelia.it.workstation.api.entity_model.events.EntityInvalidationEvent;
import org.janelia.it.workstation.api.entity_model.events.EntityRemoveEvent;
import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.workstation.api.entity_model.management.UserColorMapping;
import org.janelia.it.workstation.gui.framework.actions.Action;
import org.janelia.it.workstation.gui.framework.actions.RemoveEntityAction;
import org.janelia.it.workstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.workstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.workstation.gui.framework.outline.AnnotationFilter;
import org.janelia.it.workstation.gui.framework.outline.Annotations;
import org.janelia.it.workstation.gui.framework.outline.EntityContextMenu;
import org.janelia.it.workstation.gui.framework.outline.EntitySelectionHistory;
import org.janelia.it.workstation.gui.framework.outline.EntityViewerState;
import org.janelia.it.workstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.MouseForwarder;
import org.janelia.it.workstation.gui.util.MouseHandler;
import org.janelia.it.workstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.model.utils.AnnotationSession;
import org.janelia.it.workstation.model.utils.ModelUtils;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;

/**
 * This viewer shows images in a grid. It is modeled after OS X Finder. It wraps an ImagesPanel and provides a lot of
 * functionality on top of it, such as:
 * 1) Asynchronous entity loading
 * 2) Entity selection and navigation
 * 3) Toolbar with various features
 * 4) HUD display for currently selected image
 * 5) Pagination
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconDemoPanel extends IconPanel {

    private static final Logger log = LoggerFactory.getLogger(IconDemoPanel.class);

    protected final static int PAGE_SIZE = 500;

    // Main components
    protected JLabel splashPanel;
    protected IconDemoToolbar iconDemoToolbar;

    // Status bar
    protected JPanel statusBar;
    protected JLabel statusLabel;
    protected JPanel selectionButtonContainer;
    protected JButton prevPageButton;
    protected JButton nextPageButton;
    protected JButton endPageButton;
    protected JButton startPageButton;
    protected JButton selectAllButton;
    protected JLabel pagingStatusLabel;

    // Hud dialog
    protected Hud hud;

    // These members deal with the context and entities within it
    protected RootedEntity contextRootedEntity;
    protected List<RootedEntity> allRootedEntities;
    protected Multimap<String, RootedEntity> allRootedEntitiesByPathId = HashMultimap.<String, RootedEntity>create();
    protected Multimap<Long, RootedEntity> allRootedEntitiesByEntityId = HashMultimap.<Long, RootedEntity>create();
    protected int numPages;

    // These members deal with entities on the current page only
    protected int currPage;
    protected List<RootedEntity> pageRootedEntities;
    protected final Annotations annotations = new Annotations();
    protected final List<String> allUsers = new ArrayList<String>();
    protected final Set<String> hiddenUsers = new HashSet<String>();
    protected int currTableHeight = ImagesPanel.DEFAULT_TABLE_HEIGHT;
    protected final List<String> allImageRoles = new ArrayList<String>();

    // Tracking of loading operations
    protected AtomicBoolean entityLoadInProgress = new AtomicBoolean(false);
    protected AtomicBoolean annotationLoadInProgress = new AtomicBoolean(false);
    protected SimpleWorker entityLoadingWorker;
    protected SimpleWorker annotationLoadingWorker;
    protected SimpleWorker annotationsInitWorker;

    // Listeners
    protected SessionModelListener sessionModelListener;
    protected ModelMgrObserver modelMgrObserver;

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
                    for (RootedEntity rootedEntity : pageRootedEntities) {
                        ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), rootedEntity.getId(), false);
                    }
                    if (pageRootedEntities.size() < allRootedEntities.size()) {
                        selectionButtonContainer.setVisible(true);
                    }
                    return;
                }

                // Space on a single entity triggers a preview 
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    updateHud(true);
                    e.consume();
                    return;
                }

                // Enter with a single entity selected triggers an outline
                // navigation
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    List<String> selectedIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory());
                    if (selectedIds.size() != 1) {
                        return;
                    }
                    String selectedId = selectedIds.get(0);
                    ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, selectedId, true);
                    return;
                }

                // Delete triggers deletion
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    List<RootedEntity> selected = getSelectedEntities();
                    if (selected.isEmpty()) {
                        return;
                    }
                    final Action action = new RemoveEntityAction(selected, true, false);
                    action.doAction();
                    e.consume();
                    return;
                }

                // Tab and arrow navigation to page through the images
                boolean clearAll = false;
                RootedEntity rootedEntity = null;
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    clearAll = true;
                    if (e.isShiftDown()) {
                        rootedEntity = getPreviousEntity();
                    }
                    else {
                        rootedEntity = getNextEntity();
                    }
                }
                else {
                    clearAll = true;
                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        rootedEntity = getPreviousEntity();
                    }
                    else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        rootedEntity = getNextEntity();
                    }
                }

                if (rootedEntity != null) {
                    AnnotatedImageButton button = imagesPanel.getButtonById(rootedEntity.getId());
                    if (button != null) {
                        ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), rootedEntity.getId(), clearAll);
                        imagesPanel.scrollEntityToCenter(rootedEntity);
                        button.requestFocus();
                        updateHud(false);
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
            AnnotatedImageButton button = getButtonAncestor(e.getComponent());
            // Select the button first
            RootedEntity rootedEntity = button.getRootedEntity();
            if (!button.isSelected()) {
                ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), rootedEntity.getId(), true);
            }
            getButtonPopupMenu().show(e.getComponent(), e.getX(), e.getY());
            e.consume();
        }

        @Override
        protected void doubleLeftClicked(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            AnnotatedImageButton button = getButtonAncestor(e.getComponent());
            buttonDrillDown(button);
            // Double-clicking an image in gallery view triggers an outline selection
            e.consume();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            if (e.isConsumed()) {
                return;
            }
            AnnotatedImageButton button = getButtonAncestor(e.getComponent());
            if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 0) {
                return;
            }
            buttonSelection(button, (SystemInfo.isMac && e.isMetaDown()) || e.isControlDown(), e.isShiftDown());
        }
    };

    protected JPopupMenu getButtonPopupMenu() {
        List<String> selectionIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory());
        List<RootedEntity> rootedEntityList = new ArrayList<RootedEntity>();
        for (String entityId : selectionIds) {
            RootedEntity re = getRootedEntityById(entityId);
            if (re == null) {
                log.warn("Could not locate selected entity with id {}", entityId);
            }
            else {
                rootedEntityList.add(re);
            }
        }
        JPopupMenu popupMenu = new EntityContextMenu(rootedEntityList);
        ((EntityContextMenu) popupMenu).addMenuItems();

        return popupMenu;
    }

    /**
     * This is a separate method so that it can be overridden to accommodate other behavior patterns.
     */
    protected void buttonDrillDown(AnnotatedImageButton button) {
        RootedEntity rootedEntity = button.getRootedEntity();
        RootedEntity currRootedEntity = getContextRootedEntity();
        if (currRootedEntity == null || currRootedEntity == rootedEntity) {
            return;
        }
        if (StringUtils.isEmpty(rootedEntity.getUniqueId())) {
            return;
        }
        ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, rootedEntity.getUniqueId(), true);
    }

    protected void buttonSelection(AnnotatedImageButton button, boolean multiSelect, boolean rangeSelect) {
        final String category = getSelectionCategory();
        final RootedEntity rootedEntity = button.getRootedEntity();
        final String rootedEntityId = rootedEntity.getId();

        selectionButtonContainer.setVisible(false);

        if (multiSelect) {
            // With the meta key we toggle items in the current
            // selection without clearing it
            if (!button.isSelected()) {
                ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, rootedEntityId, false);
            }
            else {
                ModelMgr.getModelMgr().getEntitySelectionModel().deselectEntity(category, rootedEntityId);
            }
        }
        else {
            // With shift, we select ranges
            String lastSelected = ModelMgr.getModelMgr().getEntitySelectionModel().getLastSelectedEntityIdByCategory(getSelectionCategory());
            if (rangeSelect && lastSelected != null) {
                // Walk through the buttons and select everything between the last and current selections
                boolean selecting = false;
                List<RootedEntity> rootedEntities = getRootedEntities();
                for (RootedEntity otherRootedEntity : rootedEntities) {
                    if (otherRootedEntity.getId().equals(lastSelected) || otherRootedEntity.getId().equals(rootedEntityId)) {
                        if (otherRootedEntity.getId().equals(rootedEntityId)) {
                            // Always select the button that was clicked
                            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, otherRootedEntity.getId(), false);
                        }
                        if (selecting) {
                            return; // We already selected, this is the end
                        }
                        selecting = true; // Start selecting
                        continue; // Skip selection of the first and last items, which should already be selected
                    }
                    if (selecting) {
                        ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, otherRootedEntity.getId(), false);
                    }
                }
            }
            else {
                // This is a good old fashioned single button selection
                ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, rootedEntityId, true);
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

    public IconDemoPanel(ViewerPane viewerPane) {

        super(viewerPane);

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
                    IconDemoPanel.this.clear();
                }
            }
        };
        SessionMgr.getSessionMgr().addSessionModelListener(sessionModelListener);

        hud = Hud.getSingletonInstance();
        hud.addKeyListener(keyListener);

        splashPanel = new JLabel(Icons.getIcon("workstation_logo_white.png"));
        add(splashPanel);

        iconDemoToolbar = createToolbar();
        iconDemoToolbar.addMouseListener(new MouseForwarder(this, "JToolBar->IconDemoPanel"));

        imagesPanel = new ImagesPanel(this);
        imagesPanel.setButtonKeyListener(keyListener);
        imagesPanel.setButtonMouseListener(buttonMouseListener);
        imagesPanel.addMouseListener(new MouseForwarder(this, "ImagesPanel->IconDemoPanel"));

        prevPageButton = new JButton(Icons.getIcon("arrow_back.gif"));
        prevPageButton.setToolTipText("Back A Page");
        prevPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goPrevPage();
            }
        });

        nextPageButton = new JButton(Icons.getIcon("arrow_forward.gif"));
        nextPageButton.setToolTipText("Forward A Page");
        nextPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goNextPage();
            }
        });

        startPageButton = new JButton(Icons.getIcon("arrow_double_left.png"));
        startPageButton.setToolTipText("Jump To Start");
        startPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goStartPage();
            }
        });

        endPageButton = new JButton(Icons.getIcon("arrow_double_right.png"));
        endPageButton.setToolTipText("Jump To End");
        endPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goEndPage();
            }
        });

        selectAllButton = new JButton("Select All");
        selectAllButton.setToolTipText("Select all items on all pages");
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectAll();
            }
        });

        statusLabel = new JLabel("");
        pagingStatusLabel = new JLabel("");

        selectionButtonContainer = new JPanel();
        selectionButtonContainer.setLayout(new BoxLayout(selectionButtonContainer, BoxLayout.LINE_AXIS));

        selectionButtonContainer.add(selectAllButton);
        selectionButtonContainer.add(Box.createRigidArea(new Dimension(10, 20)));
        selectionButtonContainer.setVisible(false);

        statusBar = new JPanel();
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
        statusBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, (Color) UIManager.get("windowBorder")), BorderFactory.createEmptyBorder(0, 5, 2, 5)));

        statusBar.add(Box.createRigidArea(new Dimension(10, 20)));
        statusBar.add(statusLabel);
        statusBar.add(Box.createRigidArea(new Dimension(10, 20)));
        statusBar.add(selectionButtonContainer);
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(pagingStatusLabel);
        statusBar.add(Box.createRigidArea(new Dimension(10, 20)));
        statusBar.add(startPageButton);
        statusBar.add(prevPageButton);
        statusBar.add(nextPageButton);
        statusBar.add(endPageButton);

        addKeyListener(keyListener);

        imagesPanel.addMouseListener(new MouseHandler() {
            @Override
            protected void popupTriggered(MouseEvent e) {
                if (contextRootedEntity == null) {
                    return;
                }
                JPopupMenu popupMenu = new JPopupMenu();
                JMenuItem titleItem = new JMenuItem("" + contextRootedEntity.getEntity().getName());
                titleItem.setEnabled(false);
                popupMenu.add(titleItem);

                JMenuItem newFolderItem = new JMenuItem("  Create New Folder");
                newFolderItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {

                        // Add button clicked
                        String folderName = (String) JOptionPane.showInputDialog(IconDemoPanel.this, "Folder Name:\n",
                                "Create folder under " + contextRootedEntity.getEntity().getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);
                        if ((folderName == null) || (folderName.length() <= 0)) {
                            return;
                        }

                        try {
                            // Update database
                            Entity parentFolder = contextRootedEntity.getEntity();
                            Entity newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
                            ModelMgr.getModelMgr().addEntityToParent(parentFolder, newFolder);
                        }
                        catch (Exception ex) {
                            SessionMgr.getSessionMgr().handleException(ex);
                        }
                    }
                });

                if (!contextRootedEntity.getEntity().getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)
                        || !contextRootedEntity.getEntity().getOwnerKey().equals(SessionMgr.getSubjectKey())) {
                    newFolderItem.setEnabled(false);
                }

                popupMenu.add(newFolderItem);
                popupMenu.show(imagesPanel, e.getX(), e.getY());
            }
        });

        modelMgrObserver = new ModelMgrAdapter() {

            @Override
            public void annotationsChanged(final long entityId) {
                if (pageRootedEntities != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            reloadAnnotations(entityId);
                            filterEntities();
                        }
                    });
                }
            }

            @Override
            public void entitySelected(String category, String entityId, boolean clearAll) {
                if (category.equals(getSelectionCategory())) {
                    IconDemoPanel.this.entitySelected(entityId, clearAll);
                }
            }

            @Override
            public void entityDeselected(String category, String entityId) {
                if (category.equals(getSelectionCategory())) {
                    IconDemoPanel.this.entityDeselected(entityId);
                }
            }
        };
        ModelMgr.getModelMgr().addModelMgrObserver(modelMgrObserver);
        ModelMgr.getModelMgr().registerOnEventBus(this);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                imagesPanel.recalculateGrid();
            }
        });

        annotations.setFilter(new AnnotationFilter() {
            @Override
            public boolean accept(OntologyAnnotation annotation) {

                // Hidden by user?
                if (hiddenUsers.contains(annotation.getOwner())) {
                    return false;
                }
                AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();

                // Hidden by session?
                Boolean onlySession = (Boolean) SessionMgr.getSessionMgr().getModelProperty(
                        ViewerSettingsPanel.ONLY_SESSION_ANNOTATIONS_PROPERTY);
                if ((onlySession != null && !onlySession) || session == null) {
                    return true;
                }

                // At this point we know there is a current session, and we have to match it
                return (annotation.getSessionId() != null && annotation.getSessionId().equals(session.getId()));
            }
        });

        SessionMgr.getSessionMgr().addSessionModelListener(new SessionModelListener() {

            @Override
            public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {

                if (ViewerSettingsPanel.INVERT_IMAGE_COLORS_PROPERTY.equals(key)) {
                    Utils.setWaitingCursor(IconDemoPanel.this);
                    try {
                        imagesPanel.setInvertedColors((Boolean) newValue);
                        imagesPanel.repaint();
                    }
                    finally {
                        Utils.setDefaultCursor(IconDemoPanel.this);
                    }
                }
                else if (ViewerSettingsPanel.ONLY_SESSION_ANNOTATIONS_PROPERTY.equals(key)) {
                    refreshAnnotations(null);
                }
                else if (ViewerSettingsPanel.HIDE_ANNOTATED_PROPERTY.equals(key)) {
                    filterEntities();
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

    @Subscribe
    public void entityChanged(EntityChangeEvent event) {
        Entity entity = event.getEntity();
        if (contextRootedEntity == null) {
            return;
        }
        if (contextRootedEntity.getEntity().getId().equals(entity.getId())) {
            log.debug("({}) Reloading because context entity was changed: '{}'", getSelectionCategory(), entity.getName());
            loadEntity(contextRootedEntity, null);
        }
        else {
            for (AnnotatedImageButton button : imagesPanel.getButtonsByEntityId(entity.getId())) {
                log.debug("({}) Refreshing button because entity was changed: '{}'", getSelectionCategory(), entity.getName());

                RootedEntity rootedEntity = button.getRootedEntity();
                if (rootedEntity != null) {
                    Entity buttonEntity = rootedEntity.getEntity();
                    if (entity != buttonEntity) {
                        log.warn("({}) entityChanged: Instance mismatch: " + entity.getName()
                                + " (cached=" + System.identityHashCode(entity) + ") vs (this=" + System.identityHashCode(buttonEntity) + ")", getSelectionCategory());
                        rootedEntity.setEntity(entity);
                    }

                    button.refresh(rootedEntity);
                    imagesPanel.setMaxImageWidth(imagesPanel.getMaxImageWidth());
                }
            }
        }
    }

    @Subscribe
    public void entityRemoved(EntityRemoveEvent event) {
        Entity entity = event.getEntity();
        if (contextRootedEntity == null) {
            return;
        }
        if (contextRootedEntity.getEntity() != null && contextRootedEntity.getEntityId().equals(entity.getId())) {
            goParent();
        }
        else {
            for (RootedEntity rootedEntity : new ArrayList<RootedEntity>(pageRootedEntities)) {
                if (rootedEntity.getEntityId().equals(entity.getId())) {
                    removeRootedEntity(rootedEntity);
                    return;
                }
            }
        }
    }

    @Subscribe
    public void entityInvalidated(EntityInvalidationEvent event) {

        if (contextRootedEntity == null) {
            return;
        }
        boolean affected = false;

        if (event.isTotalInvalidation()) {
            affected = true;
        }
        else {
            for (Entity entity : event.getInvalidatedEntities()) {
                if (contextRootedEntity.getEntity() != null && contextRootedEntity.getEntityId().equals(entity.getId())) {
                    affected = true;
                    break;
                }
                else {
                    for (final RootedEntity rootedEntity : new ArrayList<RootedEntity>(pageRootedEntities)) {
                        if (rootedEntity.getEntityId().equals(entity.getId())) {
                            affected = true;
                            break;
                        }
                    }
                    if (affected) {
                        break;
                    }
                }
            }
        }

        if (affected) {
            log.debug("({}) Some entities were invalidated so we're refreshing the viewer", getSelectionCategory());
            refresh(false, null);
        }
    }

    protected IconDemoToolbar createToolbar() {

        return new IconDemoToolbar() {

            @Override
            protected void goBack() {
                getViewerPane().getEntitySelectionHistory().goBack(saveViewerState());
            }

            @Override
            protected void goForward() {
                getViewerPane().getEntitySelectionHistory().goForward();
            }

            @Override
            protected void refresh() {
                IconDemoPanel.this.totalRefresh();
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
            protected JPopupMenu getPopupPathMenu() {
                List<RootedEntity> rootedAncestors = getViewerPane().getRootedAncestors();
                if (rootedAncestors == null) {
                    return null;
                }
                final JPopupMenu pathMenu = new JPopupMenu();
                for (final RootedEntity ancestor : rootedAncestors) {
                    JMenuItem pathMenuItem = new JMenuItem(ancestor.getEntity().getName(), Icons.getIcon(ancestor.getEntity()));
                    pathMenuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, ancestor.getUniqueId(), true);
                        }
                    });
                    pathMenuItem.setEnabled(pathMenu.getComponentCount() > 0);
                    pathMenu.add(pathMenuItem);
                }
                return pathMenu;
            }

            @Override
            protected JPopupMenu getPopupUserMenu() {
                final JPopupMenu userListMenu = new JPopupMenu();
                UserColorMapping userColors = ModelMgr.getModelMgr().getUserColorMapping();

                // Save the list of users so that when the function actually runs, the
                // users it affects are the same users that were displayed
                final List<String> savedUsers = new ArrayList<String>(allUsers);

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
                final List<String> imageRoles = new ArrayList<String>(allImageRoles);

                for (final String imageRole : imageRoles) {
                    JMenuItem roleMenuItem = new JCheckBoxMenuItem(imageRole, imageRole.equals(getCurrImageRole()));
                    roleMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            setCurrImageRole(imageRole);
                            entityLoadDone(null);
                        }
                    });
                    imageRoleListMenu.add(roleMenuItem);
                }
                return imageRoleListMenu;
            }
        };
    }

    protected void entitySelected(String entityId, boolean clearAll) {
        log.debug("selecting {} in {} viewer", entityId, getSelectionCategory());
        imagesPanel.setSelection(entityId, true, clearAll);
        updateStatusBar();
    }

    public void entityDeselected(String entityId) {
        imagesPanel.setSelection(entityId, false, false);
        updateStatusBar();
    }

    private void updateStatusBar() {
        if (pageRootedEntities == null) {
            return;
        }
        EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
        int s = esm.getSelectedEntitiesIds(getSelectionCategory()).size();
        statusLabel.setText(s + " of " + allRootedEntities.size() + " selected");
    }

    /**
     * This should be called by any handler that wishes to show/unshow the HUD.
     */
    private void updateHud(boolean toggle) {
        List<String> selectedIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory());
        if (selectedIds.size() != 1) {
            hud.hideDialog();
            return;
        }
        Entity entity = null;
        String selectedId = selectedIds.get(0);
        for (RootedEntity re : getRootedEntitiesById(selectedId)) {
            // Get the image from the annotated image button which is also a Dynamic Image Button.
            final AnnotatedImageButton button = imagesPanel.getButtonById(re.getId());
            if (button instanceof DynamicImageButton) {
                entity = re.getEntity();
                break;   // Only one.
            }
        }
        if (toggle) {
            hud.setEntityAndToggleDialog(entity);
        }
        else {
            hud.setEntity(entity);
        }
    }

    public synchronized void goParent() {
        final String selectedUniqueId = contextRootedEntity.getUniqueId();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String parentId = Utils.getParentIdFromUniqueId(selectedUniqueId);
                if (StringUtils.isEmpty(parentId)) {
                    clear();
                }
                else {
                    ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, parentId, true);
                }
            }
        });
    }

    private void updatePagingStatus() {
        startPageButton.setEnabled(currPage != 0);
        prevPageButton.setEnabled(currPage > 0);
        nextPageButton.setEnabled(currPage < numPages - 1);
        endPageButton.setEnabled(currPage != numPages - 1);
    }

    private synchronized void goPrevPage() {
        int page = currPage - 1;
        if (page < 0) {
            return;
        }
        loadImageEntities(page, null);
    }

    private synchronized void goNextPage() {
        int page = currPage + 1;
        if (page >= numPages) {
            return;
        }
        loadImageEntities(page, null);
    }

    private synchronized void goStartPage() {
        loadImageEntities(0, null);
    }

    private synchronized void goEndPage() {
        loadImageEntities(numPages - 1, null);
    }

    private synchronized void selectAll() {
        for (RootedEntity rootedEntity : allRootedEntities) {
            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), rootedEntity.getId(), false);
        }
        selectionButtonContainer.setVisible(false);
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        this.updateUI();
    }

    @Override
    public void loadEntity(RootedEntity rootedEntity) {
        loadEntity(rootedEntity, null);
    }

    @Override
    public synchronized void loadEntity(RootedEntity rootedEntity, final Callable<Void> success) {

        this.contextRootedEntity = rootedEntity;
        if (contextRootedEntity == null) {
            return;
        }

        Entity entity = contextRootedEntity.getEntity();

        log.debug("loadEntity {} (@{})", entity.getName(), System.identityHashCode(entity));

        List<EntityData> eds = ModelUtils.getSortedEntityDatas(entity);
        List<EntityData> children = new ArrayList<EntityData>();
        for (EntityData ed : eds) {
            Entity child = ed.getChildEntity();
            if (!EntityUtils.isHidden(ed) && child != null && !(child instanceof ForbiddenEntity)) {
                children.add(ed);
            }
        }

        List<RootedEntity> lazyRootedEntities = new ArrayList<RootedEntity>();
        for (EntityData ed : children) {
            String childId = ModelUtils.getChildUniqueId(rootedEntity.getUniqueId(), ed);
            lazyRootedEntities.add(new RootedEntity(childId, ed));
        }

        if (lazyRootedEntities.isEmpty()) {
            lazyRootedEntities.add(rootedEntity);
        }

        // Update back/forward navigation
        EntitySelectionHistory history = getViewerPane().getEntitySelectionHistory();
        iconDemoToolbar.getPrevButton().setEnabled(history.isBackEnabled());
        iconDemoToolbar.getNextButton().setEnabled(history.isNextEnabled());

        loadImageEntities(lazyRootedEntities, success);
    }

    public void loadImageEntities(final List<RootedEntity> lazyRootedEntities) {
        // TODO: revisit this, since it doesn't set contextRootedEntity
        loadImageEntities(lazyRootedEntities, null);
    }

    private synchronized void loadImageEntities(final List<RootedEntity> lazyRootedEntities, final Callable<Void> success) {

        this.allRootedEntitiesByEntityId.clear();
        this.allRootedEntitiesByPathId.clear();
        this.allRootedEntities = lazyRootedEntities;
        for (RootedEntity re : allRootedEntities) {
            allRootedEntitiesByEntityId.put(re.getEntityId(), re);
            allRootedEntitiesByPathId.put(re.getId(), re);
        }

        this.numPages = (int) Math.ceil((double) allRootedEntities.size() / (double) PAGE_SIZE);
        loadImageEntities(0, success);
    }

    private synchronized void loadImageEntities(final int pageNum, final Callable<Void> success) {

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("IconDemoPanel.entityLoadDone called outside of EDT");
        }

        this.currPage = pageNum;

        int i = pageNum * PAGE_SIZE;
        int j = i + PAGE_SIZE;
        if (j > allRootedEntities.size()) {
            j = allRootedEntities.size();
        }

        final List<RootedEntity> pageEntities = allRootedEntities.subList(i, j);

        entityLoadInProgress.set(true);
        annotationLoadInProgress.set(true);

        // Indicate a load
        showLoadingIndicator();

        pagingStatusLabel.setText("Page " + (pageNum + 1) + " of " + numPages);

        // Cancel previous loads
        imagesPanel.cancelAllLoads();
        if (entityLoadingWorker != null && !entityLoadingWorker.isDone()) {
            entityLoadingWorker.disregard();
        }
        if (annotationsInitWorker != null && !annotationsInitWorker.isDone()) {
            annotationsInitWorker.disregard();
        }

        // Temporarily disable scroll loading
        imagesPanel.setScrollLoadingEnabled(false);

        entityLoadingWorker = new SimpleWorker() {

            private List<RootedEntity> loadedRootedEntities = new ArrayList<RootedEntity>();

            @Override
            protected void doStuff() throws Exception {
                for (RootedEntity rootedEntity : pageEntities) {
                    if (!EntityUtils.isInitialized(rootedEntity.getEntity())) {
                        log.warn("Had to load entity " + rootedEntity.getEntity().getId());
                        rootedEntity.getEntityData().setChildEntity(ModelMgr.getModelMgr().getEntityById(rootedEntity.getEntity().getId()));
                    }
                    EntityData defaultImageEd = rootedEntity.getEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
                    if (defaultImageEd != null && defaultImageEd.getValue() == null && defaultImageEd.getChildEntity() != null) {
                        log.warn("Had to load default image " + rootedEntity.getEntity().getName());
                        defaultImageEd.setChildEntity(ModelMgr.getModelMgr().getEntityById(defaultImageEd.getChildEntity().getId()));
                    }
                    loadedRootedEntities.add(rootedEntity);
                }
            }

            @Override
            protected void hadSuccess() {
                setRootedEntities(loadedRootedEntities);
                entityLoadDone(success);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        entityLoadingWorker.execute();

        annotationsInitWorker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                List<Long> entityIds = new ArrayList<Long>();
                for (RootedEntity rootedEntity : pageEntities) {
                    entityIds.add(rootedEntity.getEntityId());
                }
                annotations.init(entityIds);
            }

            @Override
            protected void hadSuccess() {

                if (!entityLoadInProgress.get()) {
                    // Entity load finished before we did, so its safe to update the annotations
                    refreshAnnotations(null);
                    filterEntities();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            imagesPanel.recalculateGrid();
                        }
                    });
                }

                annotationLoadInProgress.set(false);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        annotationsInitWorker.execute();
    }

    private synchronized void entityLoadDone(final Callable<Void> success) {

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("IconDemoPanel.entityLoadDone called outside of EDT");
        }

        if (getRootedEntities() == null) {
            log.warn("Rooted entity list is null upon calling entityLoadDone");
            return;
        }

        // Create the image buttons
        imagesPanel.setRootedEntities(getRootedEntities());

        // Update preferences for each button
        Boolean invertImages = (Boolean) SessionMgr.getSessionMgr().getModelProperty(
                ViewerSettingsPanel.INVERT_IMAGE_COLORS_PROPERTY);
        Boolean tagTable = (Boolean) SessionMgr.getSessionMgr().getModelProperty(
                ViewerSettingsPanel.SHOW_ANNOTATION_TABLES_PROPERTY);
        if (invertImages == null) {
            invertImages = false;
        }
        if (tagTable == null) {
            tagTable = false;
        }

        imagesPanel.setTagTable(tagTable);
        imagesPanel.setTagVisbility(iconDemoToolbar.areTagsVisible());
        imagesPanel.setTitleVisbility(iconDemoToolbar.areTitlesVisible());
        imagesPanel.setInvertedColors(invertImages);

        // Since the images are not loaded yet, this will just resize the empty
        // buttons so that we can calculate the grid correctly
        imagesPanel.resizeTables(imagesPanel.getCurrTableHeight());
        imagesPanel.setMaxImageWidth(imagesPanel.getMaxImageWidth());

        // Update selection
        EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
        esm.deselectAll(getSelectionCategory());

        // Actually display everything
        showImagePanel();
        updateStatusBar();
        updatePagingStatus();

        // Wait until everything is recomputed
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                if (!annotationLoadInProgress.get()) {
                    // Annotation load finished before we did, so we have to update the annotations too
                    refreshAnnotations(null);
                    filterEntities();
                }

                imagesPanel.recalculateGrid();
                imagesPanel.setScrollLoadingEnabled(true);
                entityLoadInProgress.set(false);

                // Finally, we're done, we can call the success callback
                ConcurrentUtils.invokeAndHandleExceptions(success);
            }
        });
    }

    protected void removeRootedEntity(final RootedEntity rootedEntity) {
        int index = getRootedEntities().indexOf(rootedEntity);
        if (index < 0) {
            return;
        }

        pageRootedEntities.remove(rootedEntity);
        allRootedEntities.remove(rootedEntity);
        allRootedEntitiesByEntityId.removeAll(rootedEntity.getEntityId());
        allRootedEntitiesByPathId.removeAll(rootedEntity.getId());

        this.numPages = (int) Math.ceil((double) allRootedEntities.size() / (double) PAGE_SIZE);
        updatePagingStatus();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                RootedEntity next = getNextEntity();
                if (next != null) {
                    ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), next.getId(), true);
                }
                imagesPanel.removeRootedEntity(rootedEntity);
                imagesPanel.recalculateGrid();
            }
        });
    }

    private void filterEntities() {

        AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();
        if (session == null) {
            return;
        }
        session.clearCompletedIds();
        Set<Long> completed = session.getCompletedEntityIds();

        imagesPanel.showAllButtons();
        Boolean hideAnnotated = (Boolean) SessionMgr.getSessionMgr().getModelProperty(
                ViewerSettingsPanel.HIDE_ANNOTATED_PROPERTY);
        if (hideAnnotated != null && hideAnnotated) {
            imagesPanel.hideButtons(completed);
        }
    }

    /**
     * Reload the annotations from the database and then refresh the UI.
     */
    public synchronized void reloadAnnotations(final Long entityId) {

        if (annotations == null || pageRootedEntities == null) {
            return;
        }

        annotationLoadingWorker = new SimpleWorker() {

            protected void doStuff() throws Exception {
                annotations.reload(entityId);
            }

            protected void hadSuccess() {
                refreshAnnotations(entityId);
            }

            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        annotationLoadingWorker.execute();
    }

    /**
     * Refresh the annotation display in the UI, but do not reload anything from
     * the database.
     */
    private synchronized void refreshAnnotations(Long entityId) {
        // Refresh all user list
        allUsers.clear();
        for (OntologyAnnotation annotation : annotations.getAnnotations()) {
            String name = ModelMgrUtils.getNameFromSubjectKey(annotation.getOwner());
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

    @Override
    public void refresh() {
        refresh(false, null);
    }

    @Override
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

        if (contextRootedEntity == null) {
            return;
        }

        if (refreshInProgress.getAndSet(true)) {
            log.debug("Skipping refresh, since there is one already in progress");
            return;
        }

        log.debug("Starting a refresh");

        final List<String> selectedIds = new ArrayList<String>(ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory()));
        final Callable<Void> success = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // At the very end, reselect our buttons if possible
                boolean first = true;
                for (String selectedId : selectedIds) {
                    ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), selectedId, first);
                    first = false;
                }
                // Now call the user's callback 
                if (successCallback != null) {
                    successCallback.call();
                }
                return null;
            }
        };

        SimpleWorker refreshWorker = new SimpleWorker() {

            RootedEntity rootedEntity = contextRootedEntity;

            protected void doStuff() throws Exception {
                if (invalidateCache) {
                    ModelMgr.getModelMgr().invalidateCache(rootedEntity.getEntity(), true);
                }
                Entity entity = ModelMgr.getModelMgr().getEntityAndChildren(rootedEntity.getEntity().getId());
                rootedEntity.setEntity(entity);
            }

            protected void hadSuccess() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (rootedEntity.getEntity() == null) {
                            clear();
                            if (success != null) {
                                try {
                                    success.call();
                                }
                                catch (Exception e) {
                                    hadError(e);
                                }
                            }
                        }
                        else {
                            loadEntity(rootedEntity, success);
                        }
                        refreshInProgress.set(false);
                        log.debug("Refresh complete");
                    }
                });
            }

            protected void hadError(Throwable error) {
                refreshInProgress.set(false);
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        refreshWorker.execute();
    }

    @Override
    public synchronized void clear() {

        log.debug("Clearing {}", getSelectionCategory());

        // TODO: move this to the ViewerPane
        this.contextRootedEntity = null;
        this.pageRootedEntities = null;

        getViewerPane().setTitle(" ");
        removeAll();
        add(splashPanel, BorderLayout.CENTER);

        getViewerPane().revalidate();
        getViewerPane().repaint();
    }

    @Override
    public void close() {
        log.debug("Closing {}", getSelectionCategory());
        SessionMgr.getSessionMgr().removeSessionModelListener(sessionModelListener);
        ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrObserver);
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
    }

    public synchronized void showImagePanel() {

        removeAll();
        add(iconDemoToolbar, BorderLayout.NORTH);
        add(imagesPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    public RootedEntity getPreviousEntity() {
        if (pageRootedEntities == null) {
            return null;
        }
        int i = pageRootedEntities.indexOf(getLastSelectedEntity());
        if (i < 1) {
            // Already at the beginning
            return null;
        }
        return pageRootedEntities.get(i - 1);
    }

    public RootedEntity getNextEntity() {
        if (pageRootedEntities == null) {
            return null;
        }
        int i = pageRootedEntities.indexOf(getLastSelectedEntity());
        if (i > pageRootedEntities.size() - 2) {
            // Already at the end
            return null;
        }
        return pageRootedEntities.get(i + 1);
    }

    @Override
    public synchronized List<RootedEntity> getRootedEntities() {
        return pageRootedEntities;
    }

    private synchronized void setRootedEntities(List<RootedEntity> rootedEntities) {
        this.pageRootedEntities = rootedEntities;

        Set<String> imageRoles = new HashSet<String>();
        for (RootedEntity rootedEntity : rootedEntities) {
            for (EntityData ed : rootedEntity.getEntity().getEntityData()) {
                if (EntityUtils.hasImageRole(ed)) {
                    imageRoles.add(ed.getEntityAttrName());
                }
            }
        }

        allImageRoles.clear();
        allImageRoles.addAll(imageRoles);
        Collections.sort(allImageRoles);

        iconDemoToolbar.getImageRoleButton().setEnabled(!allImageRoles.isEmpty());
        if (!allImageRoles.contains(getCurrImageRole())) {
            setCurrImageRole(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        }
    }

    public synchronized RootedEntity getLastSelectedEntity() {
        String entityId = ModelMgr.getModelMgr().getEntitySelectionModel().getLastSelectedEntityIdByCategory(getSelectionCategory());
        if (entityId == null) {
            return null;
        }
        AnnotatedImageButton button = imagesPanel.getButtonById(entityId);
        if (button == null) {
            return null;
        }
        return button.getRootedEntity();
    }

    @Override
    public List<RootedEntity> getSelectedEntities() {
        List<RootedEntity> selectedEntities = new ArrayList<RootedEntity>();
        if (pageRootedEntities == null) {
            return selectedEntities;
        }
        for (RootedEntity rootedEntity : pageRootedEntities) {
            AnnotatedImageButton button = imagesPanel.getButtonById(rootedEntity.getId());
            if (button.isSelected()) {
                selectedEntities.add(rootedEntity);
            }
        }
        return selectedEntities;
    }

    public IconDemoToolbar getToolbar() {
        return iconDemoToolbar;
    }

    public Hud getHud() {
        return hud;
    }

    public Annotations getAnnotations() {
        return annotations;
    }

    @Override
    public RootedEntity getContextRootedEntity() {
        return contextRootedEntity;
    }

    private List<RootedEntity> getRootedEntitiesById(String id) {
        List<RootedEntity> res = new ArrayList<RootedEntity>();
        // Assume these are path ids
        res.addAll(allRootedEntitiesByPathId.get(id));
        if (res.isEmpty()) {
            // Maybe we were given entity GUIDs
            try {
                Long entityId = Long.parseLong(id);
                res.addAll(allRootedEntitiesByEntityId.get(entityId));
            }
            catch (NumberFormatException e) {
                // Expected
            }
        }
        return res;
    }

    @Override
    public RootedEntity getRootedEntityById(String id) {
        List<RootedEntity> res = getRootedEntitiesById(id);
        if (res == null || res.isEmpty()) {
            return null;
        }
        return res.get(0);
    }

    @Override
    public boolean areTitlesVisible() {
        return getToolbar().areTitlesVisible();
    }

    @Override
    public boolean areTagsVisible() {
        return getToolbar().areTagsVisible();
    }

    @Override
    public EntityViewerState saveViewerState() {
        // We could get this from the EntitySelectionModel, but sometimes that 
        // doesn't have the latest select the user is currently making.
        Set<String> selectedIds = new HashSet<String>();
        for(AnnotatedImageButton button : imagesPanel.getSelectedButtons()) {
            selectedIds.add(button.getRootedEntity().getId());
        }
        return new EntityViewerState(getClass(), contextRootedEntity, selectedIds);
    }

    @Override
    public void restoreViewerState(final EntityViewerState state) {
        // It's critical to call loadEntity in the ViewerPane not the local one.
        // The ViewerPane version does extra stuff to get the ancestors button
        // and breadcrumbs to show correctly.
        getViewerPane().loadEntity(state.getContextRootedEntity(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Go to the right page
                int i = 0;
                int firstIdIndex = 0;
                for (RootedEntity rootedEntity : allRootedEntities) {
                    if (state.getSelectedIds().contains(rootedEntity.getId())) {
                        firstIdIndex = i;
                        break;
                    }
                    i++;
                }

                Callable<Void> makeSelections = new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        for (String selectedId : state.getSelectedIds()) {
                            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), selectedId, false);
                        }
                        // Wait for all selections to finish before we scroll
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                imagesPanel.scrollSelectedEntitiesToCenter();
                            }
                        });
                        return null;
                    }
                };

                int page = (int) Math.floor((double) firstIdIndex / (double) PAGE_SIZE);
                if (page != currPage) {
                    loadImageEntities(page, makeSelections);
                }
                else {
                    makeSelections.call();
                }

                return null;
            }
        });
    }
}
