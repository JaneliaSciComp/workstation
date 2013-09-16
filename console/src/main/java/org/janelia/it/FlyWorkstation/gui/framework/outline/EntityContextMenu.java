package org.janelia.it.FlyWorkstation.gui.framework.outline;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgrEntityLoader;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.SpecialAnnotationChooserDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.TaskDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.*;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.console.Perspective;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tool_manager.ToolMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Hud;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.JScrollMenu;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapperFactory;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.utils.AnnotationSession;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.SystemInfo;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.workers.TaskMonitoringWorker;
import org.janelia.it.FlyWorkstation.ws.ExternalClient;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.MailHelper;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityContextMenu extends JPopupMenu {

    private static final Logger log = LoggerFactory.getLogger(EntityContextMenu.class);

    protected static final Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();

    // Download directories
    private static final File downloadDir = new File(SystemInfo.getDownloadsDir(), "Workstation Images");
    private static final File splitsDir = new File(SystemInfo.getDownloadsDir(), "Split Channel Images");

    // Lock to make sure only one file is downloaded at a time 
    private static final Lock copyFileLock = new ReentrantLock();
    
    // Current selection
    protected final List<RootedEntity> rootedEntityList;
    protected final RootedEntity rootedEntity;
    protected final boolean multiple;

    // Internal state
    protected boolean nextAddRequiresSeparator = false;

    public EntityContextMenu(List<RootedEntity> rootedEntityList) {
        this.rootedEntityList = rootedEntityList;
        this.rootedEntity = rootedEntityList.size() == 1 ? rootedEntityList.get(0) : null;
        this.multiple = rootedEntityList.size() > 1;
        if (!multiple) {
            checkNotNull(rootedEntity, "Rooted entity cannot be null");
        }
    }

    public EntityContextMenu(RootedEntity rootedEntity) {
        this.rootedEntity = rootedEntity;
        this.rootedEntityList = new ArrayList<RootedEntity>();
        rootedEntityList.add(rootedEntity);
        this.multiple = false;
        checkNotNull(rootedEntity, "Rooted entity cannot be null");
    }

    public void addMenuItems() {
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        add(getPasteAnnotationItem());
        add(getDetailsItem());
        add(getPermissionItem());
        add(getGotoRelatedItem());

        setNextAddRequiresSeparator(true);
        add(getNewFolderItem());
        add(getAddToRootFolderItem());
        add(getAddToSplitPickingSessionItem());
        add(getRenameItem());
        add(getErrorFlag());
        add(getDeleteItem());
        add(getProcessingBlockItem());
        add(getVerificationMovieItem());
        
        setNextAddRequiresSeparator(true);
        add(getOpenInFirstViewerItem());
        add(getOpenInSecondViewerItem());
        add(getOpenInFinderItem());
        add(getOpenWithAppItem());
        add(getNeuronAnnotatorItem());
        add(getVaa3dTriViewItem());
        add(getVaa3d3dViewItem());
        add(getFijiViewerItem());

        setNextAddRequiresSeparator(true);
        add(getSearchHereItem());

        setNextAddRequiresSeparator(true);
        add(getMergeItem());
        add(getSortBySimilarityItem());
        add(getDownloadMenu());
        add(getImportItem());
//        add(getCreateSessionItem());

        setNextAddRequiresSeparator(true);
        add(getHudMenuItem());
        add(getOpenAlignmentBoardItem());
        add(getOpenInNewAlignmentBoardItem());
        add(getOpenSliceViewerItem());

        if ((SessionMgr.getSubjectKey().equals("user:simpsonj") || SessionMgr.getSubjectKey()
                .equals("group:simpsonlab")) && !this.multiple) {
            add(getSpecialAnnotationSession());
        }
    }

    private void addBadDataButtons(JMenu errorMenu) {

        if (null != ModelMgr.getModelMgr().getErrorOntology()) {
            List<OntologyElement> ontologyElements = ModelMgr.getModelMgr().getErrorOntology().getChildren();
            for (final OntologyElement element : ontologyElements) {
                errorMenu.add(new JMenuItem(element.getName())).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {

                        Callable<Void> doSuccess = new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                SimpleWorker simpleWorker = new SimpleWorker() {
                                    @Override
                                    protected void doStuff() throws Exception {
                                        String annotationValue = "";
                                        List<Entity> annotationEntities = ModelMgr.getModelMgr()
                                                .getAnnotationsForEntity(rootedEntity.getEntity().getId());
                                        for (Entity annotation : annotationEntities) {
                                            if (annotation.getValueByAttributeName(
                                                    EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM).contains(
                                                    element.getName())) {
                                                annotationValue = annotation.getName();
                                            }
                                        }

                                        String tempsubject = "Reported Data: " + rootedEntity.getEntity().getName();
                                        StringBuilder sBuf = new StringBuilder();
                                        sBuf.append("Name: ").append(rootedEntity.getEntity().getName()).append("\n");
                                        sBuf.append("Type: ")
                                                .append(rootedEntity.getEntity().getEntityType().getName())
                                                .append("\n");
                                        sBuf.append("ID: ").append(rootedEntity.getEntity().getId().toString())
                                                .append("\n");
                                        sBuf.append("Annotation: ").append(annotationValue).append("\n\n");
                                        MailHelper helper = new MailHelper();
                                        helper.sendEmail(
                                                (String) SessionMgr.getSessionMgr().getModelProperty(
                                                        SessionMgr.USER_EMAIL),
                                                ConsoleProperties.getString("console.HelpEmail"), tempsubject,
                                                sBuf.toString());
                                    }

                                    @Override
                                    protected void hadSuccess() {
                                    }

                                    @Override
                                    protected void hadError(Throwable error) {
                                        SessionMgr.getSessionMgr().handleException(error);
                                    }
                                };
                                simpleWorker.execute();
                                return null;
                            }
                        };

                        AnnotateAction action = new AnnotateAction(doSuccess);
                        action.init(element);
                        action.doAction();
                    }
                });

            }
            // OntologyElementChooser flagType = new
            // OntologyElementChooser("Please choose a bad data flag from the list",
            // ModelMgr.getModelMgr().getOntology(tmpErrorOntology.getId()));
            // flagType.setSize(400,400);
            // flagType.setIconImage(browser.getIconImage());
            // flagType.setCanAnnotate(true);
            // flagType.showDialog(browser);

        }
    }

    protected JMenuItem getTitleItem() {
        String name = multiple ? "(Multiple selected)" : rootedEntity.getEntity().getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }

    protected JMenuItem getDetailsItem() {
        if (multiple) return null;
        
        JMenuItem detailsMenuItem = new JMenuItem("  View Details");
        detailsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, java.awt.Event.META_MASK));
        detailsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new EntityDetailsDialog().showForRootedEntity(rootedEntity);
            }
        });
        return detailsMenuItem;
    }

    protected JMenuItem getPermissionItem() {
        if (multiple) return null;
        
        if (!ModelMgrUtils.isOwner(rootedEntity.getEntity())) return null;
        
        JMenuItem detailsMenuItem = new JMenuItem("  Change Permissions");
        detailsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new EntityDetailsDialog().showForRootedEntity(rootedEntity, EntityDetailsPanel.TAB_NAME_PERMISSIONS);
            }
        });
        return detailsMenuItem;
    }

    protected JMenuItem getHudMenuItem() {
        JMenuItem toggleHudMI = null;
        if (rootedEntity != null && rootedEntity.getEntity() != null) {
            Entity entity = rootedEntity.getEntity();
            if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
                toggleHudMI = new JMenuItem("  Show in Lightbox");
                toggleHudMI.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Entity entity = rootedEntity.getEntity();
                        Hud.getSingletonInstance().setEntityAndToggleDialog(entity);
                    }
                });
            }
        }

        return toggleHudMI;
    }

    private void gotoEntity(final Entity entity, final String ancestorType) {

        Utils.setWaitingCursor(browser);

        SimpleWorker worker = new SimpleWorker() {

            private Entity targetEntity = entity;
            private String uniqueId;

            @Override
            protected void doStuff() throws Exception {
                if (ancestorType != null) {
                    targetEntity = ModelMgr.getModelMgr().getAncestorWithType(entity, ancestorType);
                }

                final String currUniqueId = rootedEntity.getUniqueId();
                final String targetId = targetEntity.getId().toString();
                if (currUniqueId.contains(targetId)) {
                    // A little optimization, if you're already in the right
                    // subtree
                    this.uniqueId = currUniqueId.substring(0, currUniqueId.indexOf(targetId) + targetId.length());
                } else {
                    // Find the best context to show the entity in
                    List<List<EntityData>> edPaths = ModelMgr.getModelMgr().getPathsToRoots(targetEntity.getId());
                    List<EntityDataPath> paths = new ArrayList<EntityDataPath>();
                    for (List<EntityData> path : edPaths) {
                        EntityDataPath edp = new EntityDataPath(path);
                        if (!edp.isHidden()) {
                            paths.add(edp);
                        }
                    }
                    sortPathsByPreference(paths);

                    if (paths.isEmpty()) {
                        throw new Exception("Could not find the related entity");
                    }

                    EntityDataPath chosen = paths.get(0);
                    this.uniqueId = chosen.getUniqueId();
                }
            }

            @Override
            protected void hadSuccess() {
                browser.getEntityOutline().selectEntityByUniqueId(uniqueId);
                Utils.setDefaultCursor(browser);
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setDefaultCursor(browser);
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }

    private void sortPathsByPreference(List<EntityDataPath> paths) {

        Collections.sort(paths, new Comparator<EntityDataPath>() {
            @Override
            public int compare(EntityDataPath p1, EntityDataPath p2) {
                Integer p1Score = 0;
                Integer p2Score = 0;
                p1Score += p1.getRootOwner().startsWith("group:") ? 2 : 0;
                p2Score += p2.getRootOwner().startsWith("group:") ? 2 : 0;
                p1Score += SessionMgr.getSubjectKey().equals(p1.getRootOwner()) ? 1 : 0;
                p2Score += SessionMgr.getSubjectKey().equals(p2.getRootOwner()) ? 1 : 0;
                EntityData e1 = p1.getPath().get(0);
                EntityData e2 = p2.getPath().get(0);
                int c = p2Score.compareTo(p1Score);
                if (c == 0) {
                    return e2.getId().compareTo(e1.getId());
                }
                return c;
            }

        });
    }

    public JMenuItem getPasteAnnotationItem() {
        // If no curent annotation item selected then do nothing
        if (null == ModelMgr.getModelMgr().getCurrentSelectedOntologyAnnotation()) {
            return null;
        }
        JMenuItem pasteItem = new JMenuItem("  Paste Annotation");
        pasteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    final List<RootedEntity> selectedEntities = browser.getViewerManager()
                            .getActiveViewer().getSelectedEntities();
                    OntologyAnnotation baseAnnotation = ModelMgr.getModelMgr().getCurrentSelectedOntologyAnnotation();
                    for (RootedEntity entity : selectedEntities) {
                        AnnotationSession tmpSession = ModelMgr.getModelMgr().getCurrentAnnotationSession();
                        OntologyAnnotation tmpAnnotation = new OntologyAnnotation((null == tmpSession) ? null
                                : tmpSession.getId(), entity.getEntityId(), baseAnnotation.getKeyEntityId(),
                                baseAnnotation.getKeyString(), baseAnnotation.getValueEntityId(), baseAnnotation
                                        .getValueString());
                        ModelMgr.getModelMgr().createOntologyAnnotation(tmpAnnotation);
                    }
                } catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        return pasteItem;
    }

    /** Makes the item for showing the entity in its own viewer iff the entity type is correct. */
    public JMenuItem getOpenAlignmentBoardItem() {
        JMenuItem alignBrdVwItem = null;
        if (rootedEntity != null && rootedEntity.getEntityData() != null) {
            Entity entity = rootedEntity.getEntity();
            if (entity!=null && entity.getEntityType().getName().equals(EntityConstants.TYPE_ALIGNMENT_BOARD)) {
                alignBrdVwItem = new JMenuItem("  Open In Alignment Board Viewer");
                alignBrdVwItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        browser.setPerspective(Perspective.AlignmentBoard);
                        browser.getLayersPanel().openAlignmentBoard(rootedEntity.getEntityId());
                    }
                });
            }
        }

        return alignBrdVwItem;
    }

    public JMenuItem getOpenInNewAlignmentBoardItem() {
        if (multiple) return null;
        
        JMenuItem alignmentBoardItem = null;
        
        if (rootedEntity != null && rootedEntity.getEntityData() != null) {
            Entity entity = rootedEntity.getEntity();
            
            if (entity!=null && (entity.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE))) {
                
                alignmentBoardItem = new JMenuItem("  Open In New Alignment Board Viewer");
                alignmentBoardItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {

                        SimpleWorker worker = new SimpleWorker() {
                            
                            private Sample sample;
                            private List<AlignmentContext> contexts;
                            
                            @Override
                            protected void doStuff() throws Exception {
                                this.sample = (Sample)EntityWrapperFactory.wrap(rootedEntity);
                                this.contexts = sample.getAvailableAlignmentContexts();
                            }
                            
                            @Override
                            protected void hadSuccess() {
                                
                                if (contexts.isEmpty()) {
                                    JOptionPane.showMessageDialog(browser,
                                            "Sample is not aligned to a compatible alignment space", "Error", JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                                
                                // Pick an alignment context for the new board
                                AlignmentContext values[] = new AlignmentContext[contexts.size()];
                                contexts.toArray(values);
                                final AlignmentContext alignmentContext = (AlignmentContext)JOptionPane.showInputDialog(browser, "Choose an alignment space for this sample", 
                                        "Choose alignment space", JOptionPane.QUESTION_MESSAGE, Icons.getIcon("folder_graphite_palette.png"), 
                                        values, values[0]);
                                if (alignmentContext==null) return;
                                
                                // Pick a name for the new board
                                final String boardName = (String) JOptionPane.showInputDialog(browser, "Board Name:\n",
                                        "Open In New Alignment Board", JOptionPane.PLAIN_MESSAGE, null, null, null);
                                if (StringUtils.isEmpty(boardName)) return;
                                
                                SimpleWorker worker = new SimpleWorker() {
                                    
                                    private RootedEntity newBoard;
                                    
                                    @Override
                                    protected void doStuff() throws Exception {
                                        newBoard = ModelMgr.getModelMgr().createAlignmentBoard(boardName, 
                                                alignmentContext.getAlignmentSpaceName(), alignmentContext.getOpticalResolution(), alignmentContext.getPixelResolution());
                                        AlignmentBoardContext alignmentBoardContext = new AlignmentBoardContext(newBoard);
                                        alignmentBoardContext.addNewAlignedEntity(sample);
                                    }
                                    
                                    @Override
                                    protected void hadSuccess() {
                                        // Update Tree UI
                                        browser.getEntityOutline().totalRefresh(true, new Callable<Void>() {
                                            @Override
                                            public Void call() throws Exception {
                                                browser.getEntityOutline().selectEntityByUniqueId(newBoard.getUniqueId());
                                                browser.setPerspective(Perspective.AlignmentBoard);
                                                browser.getLayersPanel().openAlignmentBoard(newBoard.getEntityId());
                                                return null;
                                            }
                                        });
                                    }
                                    
                                    @Override
                                    protected void hadError(Throwable error) {
                                        SessionMgr.getSessionMgr().handleException(error);
                                    }
                                };
                                worker.setProgressMonitor(new IndeterminateProgressMonitor(browser, "Adding aligned entities...", ""));
                                worker.execute();
                            }
                            
                            @Override
                            protected void hadError(Throwable error) {
                                SessionMgr.getSessionMgr().handleException(error);
                            }
                        };
                        worker.setProgressMonitor(new IndeterminateProgressMonitor(browser, "Finding alignments...", ""));
                        worker.execute();
                    }
                });
            }
        }

        return alignmentBoardItem;
    }
       
    public JMenuItem getOpenSliceViewerItem() {
        JMenuItem sliceVwItem = null;
        if (rootedEntity != null && rootedEntity.getEntityData() != null) {
            Entity entity = rootedEntity.getEntity();
            if (entity.getEntityType().getName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE) || 
                entity.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                sliceVwItem = new JMenuItem("  Open Slice Viewer");
                sliceVwItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        browser.setPerspective(Perspective.SliceViewer);
                        browser.getViewerManager().getActiveViewer().loadEntity(rootedEntity);
                    }
                });
            }
        }

        return sliceVwItem;
    }

    private class EntityDataPath {
        private List<EntityData> path;
        private String rootOwner;
        private boolean isHidden = false;

        public EntityDataPath(List<EntityData> path) {
            this.path = path;
            EntityData first = path.get(0);
            this.rootOwner = first.getParentEntity().getOwnerKey();
            for (EntityData ed : path) {
                if (EntityUtils.isHidden(ed))
                    this.isHidden = true;
            }
        }

        public String getUniqueId() {
            return EntityUtils.getUniqueIdFromParentEntityPath(path);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (EntityData pathEd : path) {
                if (sb.length() <= 0) {
                    sb.append(" / ").append(pathEd.getParentEntity().getName());
                }
                sb.append(" / ").append(pathEd.getChildEntity().getName());
            }
            return sb.toString();
        }

        public List<EntityData> getPath() {
            return path;
        }

        public String getRootOwner() {
            return rootOwner;
        }

        public boolean isHidden() {
            return isHidden;
        }
    }

    protected JMenuItem getGotoRelatedItem() {
        if (multiple) return null;
        
        JMenu relatedMenu = new JMenu("  Go To Related");
        Entity entity = rootedEntity.getEntity();
        String type = entity.getEntityType().getName();
        if (type.equals(EntityConstants.TYPE_NEURON_FRAGMENT) || type.equals(EntityConstants.TYPE_LSM_STACK)
                || type.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)
                || type.equals(EntityConstants.TYPE_CURATED_NEURON)
                || type.equals(EntityConstants.TYPE_CURATED_NEURON_COLLECTION)) {
            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT,
                                EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT));
            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_SAMPLE, EntityConstants.TYPE_SAMPLE));
        }
        else if (entity.getEntityType().getName().equals(EntityConstants.TYPE_FLY_LINE)) {
            add(relatedMenu, getChildEntityItem(entity, EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE));
            add(relatedMenu, getChildEntityItem(entity, EntityConstants.ATTRIBUTE_ORIGINAL_FLYLINE));
            add(relatedMenu, getChildEntityItem(entity, EntityConstants.ATTRIBUTE_BALANCED_FLYLINE));
        }
        else if (EntityConstants.TYPE_ALIGNED_BRAIN_STACK.equals(type)) {
            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_SCREEN_SAMPLE, EntityConstants.TYPE_SCREEN_SAMPLE));
            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_FLY_LINE, EntityConstants.TYPE_FLY_LINE));
        }
        else if (EntityConstants.TYPE_SCREEN_SAMPLE.equals(type)) {
            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_FLY_LINE, EntityConstants.TYPE_FLY_LINE));
        }
        else if (EntityConstants.TYPE_CELL_COUNTING_RESULT.equals(type)) {
            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_SAMPLE, EntityConstants.TYPE_SAMPLE));
        }
        return relatedMenu;
    }

    private JMenuItem getChildEntityItem(Entity entity, String attributeName) {
        final EntityData ed = entity.getEntityDataByAttributeName(attributeName);
        if (ed == null) return null;
        return getAncestorEntityItem(ed.getChildEntity(), null, attributeName);
    }

    private JMenuItem getAncestorEntityItem(final Entity entity, final String ancestorType, final String label) {
        if (entity == null) return null;
        
        JMenuItem relatedMenuItem = new JMenuItem(label);
        relatedMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gotoEntity(entity, ancestorType);
            }
        });
        return relatedMenuItem;
    }

    protected JMenuItem getCopyNameToClipboardItem() {
        if (multiple) return null;
        
        JMenuItem copyMenuItem = new JMenuItem("  Copy Name To Clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transferable t = new StringSelection(rootedEntity.getEntity().getName());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getCopyIdToClipboardItem() {
        if (multiple) return null;
        
        JMenuItem copyMenuItem = new JMenuItem("  Copy GUID To Clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transferable t = new StringSelection(rootedEntity.getEntity().getId().toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getRenameItem() {
        if (multiple) return null;
        
        JMenuItem renameItem = new JMenuItem("  Rename");
        renameItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                String newName = (String) JOptionPane.showInputDialog(browser, "Name:\n", "Rename "
                        + rootedEntity.getEntity().getName(), JOptionPane.PLAIN_MESSAGE, null, null, rootedEntity
                        .getEntity().getName());
                if ((newName == null) || (newName.length() <= 0)) {
                    return;
                }

                try {
                    ModelMgr.getModelMgr().renameEntity(rootedEntity.getEntity(), newName);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(SessionMgr.getSessionMgr().getActiveBrowser(),
                            "Error renaming entity", "Error", JOptionPane.ERROR_MESSAGE);
                }

            }
        });

        Entity entity = rootedEntity.getEntity();
        if (!ModelMgrUtils.hasWriteAccess(entity) || EntityUtils.isProtected(entity)) {
            renameItem.setEnabled(false);
        }

        return renameItem;
    }

    protected JMenu getErrorFlag() {
        if (multiple) return null;
        
        JMenu errorMenu = new JMenu("  Report A Problem With This Data");
        addBadDataButtons(errorMenu);
        return errorMenu;
    }

    // private void bugReport_actionPerformed(){
    // String tempsubject = "Flagged Data: " +
    // rootedEntity.getEntity().getName();
    // StringBuilder sBuf = new StringBuilder();
    // sBuf.append("Name: ").append(rootedEntity.getEntity().getName()).append("\n");
    // sBuf.append("Type: ").append(rootedEntity.getEntity().getEntityType().getName()).append("\n");
    // sBuf.append("ID: ").append(rootedEntity.getEntity().getId().toString()).append("\n\n");
    // MailHelper helper = new MailHelper();
    // helper.sendEmail((String)
    // SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL),
    // ConsoleProperties.getString("console.HelpEmail"),
    // tempsubject, sBuf.toString());
    //
    // Entity tmpErrorOntology = null;
    //
    // try {
    // tmpErrorOntology = ModelMgr.getModelMgr().getErrorOntology();
    // }
    //
    // catch (Exception e) {
    // e.printStackTrace();
    // }
    // if (null!=tmpErrorOntology){
    // OntologyElementChooser flagType = new
    // OntologyElementChooser("Please choose a bad data flag from the list",
    // ModelMgr.getModelMgr().getOntology(tmpErrorOntology.getId()));
    // flagType.setSize(400,400);
    // flagType.setIconImage(browser.getIconImage());
    // flagType.setCanAnnotate(true);
    // flagType.showDialog(browser);
    //
    // }
    //
    // }

    protected JMenuItem getProcessingBlockItem() {
        if (multiple) return null;

        final Entity sample = rootedEntity.getEntity();

        if (!sample.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE)) {
            return null;
        }
        
        JMenuItem blockItem = new JMenuItem("  Purge And Block Processing (Background Task)");
        blockItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                int result = JOptionPane.showConfirmDialog(browser, "Are you sure you want to purge " 
                        + rootedEntity.getEntity().getName()+" by deleting all large files associated with it, " +
                		"and block all of its future processing?",  "Purge And Block Processing", JOptionPane.OK_CANCEL_OPTION);
                
                if (result != 0) return;

                Task task = null;
                try {
                    HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
                    taskParameters.add(new TaskParameter("sample entity id", sample.getId().toString(), null));
                    task = ModelMgr.getModelMgr().submitJob("ConsolePurgeAndBlockSample", "Purge And Block Sample", taskParameters);
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                    return;
                }

                TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                    @Override
                    public String getName() {
                        return "Purging and blocking "+sample.getName();
                    }

                    @Override
                    protected void doStuff() throws Exception {
                        setStatus("Executing");
                        super.doStuff(); 
                    }
                    
                    @Override
                    public Callable<Void> getSuccessCallback() {
                        return new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                SessionMgr.getBrowser().getEntityOutline().totalRefresh();
                                return null;
                            }
                        };
                    }
                };

                taskWorker.executeWithEvents();
            }
        });

        if (sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_PROCESSING_BLOCK)!=null) {
            blockItem.setEnabled(false);
        }
        
        if (!ModelMgrUtils.hasWriteAccess(sample) || EntityUtils.isProtected(sample)) {
            blockItem.setEnabled(false);
        }

        return blockItem;
    }


    private JMenuItem getVerificationMovieItem() {
        if (multiple) return null;

        if (!OpenWithDefaultAppAction.isSupported())
            return null;
        
        final Entity sample = rootedEntity.getEntity();

        if (!sample.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE)) {
            return null;
        }
        
        JMenuItem movieItem = new JMenuItem("  View Alignment Verification Movie");
        movieItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                SimpleWorker worker = new SimpleWorker() {
                  
                    private Entity movie;
                    
                    @Override
                    protected void doStuff() throws Exception {
                        EntityVistationBuilder.create(new ModelMgrEntityLoader()).startAt(sample)
                                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
                                .childrenOfType(EntityConstants.TYPE_ALIGNMENT_RESULT)
                                .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                                .childrenOfType(EntityConstants.TYPE_MOVIE)
                                .run(new EntityVisitor() {
                            public void visit(Entity movieEntity) throws Exception {
                                if (movieEntity.getName().startsWith("AlignVerify")) {
                                    movie = movieEntity;
                                }
                            }
                        });
                    }
                    
                    @Override
                    protected void hadSuccess() {

                        if (movie == null) {
                            JOptionPane.showMessageDialog(browser, "Could not locate verification movie",
                                    "Not Found", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        String filepath = EntityUtils.getAnyFilePath(movie);
                        
                        if (StringUtils.isEmpty(filepath)) {
                            JOptionPane.showMessageDialog(browser, "Verification movie has no path",
                                    "Not Found", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        
                        OpenWithDefaultAppAction action = new OpenWithDefaultAppAction(movie);
                        action.doAction();
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                worker.execute();
            }
        });

        if (sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_PROCESSING_BLOCK)!=null) {
            movieItem.setEnabled(false);
        }
        
        if (!ModelMgrUtils.hasWriteAccess(sample) || EntityUtils.isProtected(sample)) {
            movieItem.setEnabled(false);
        }

        return movieItem;
    }
    
    private void addToSplitFolder(Entity commonRoot) throws Exception {

        final List<Long> ads = new ArrayList<Long>();
        final List<Long> dbds = new ArrayList<Long>();

        for (RootedEntity re : rootedEntityList) {
            String splitPart = re.getEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
            if ("AD".equals(splitPart)) {
                ads.add(re.getEntityId());
            } else if ("DBD".equals(splitPart)) {
                dbds.add(re.getEntityId());
            }
        }

        RootedEntity workingFolder = new RootedEntity(commonRoot);
        RootedEntity splitLinesFolder = ModelMgrUtils.getChildFolder(workingFolder,
                SplitPickingPanel.FOLDER_NAME_SEARCH_RESULTS, true);

        if (!ads.isEmpty()) {
            RootedEntity adFolder = ModelMgrUtils.getChildFolder(splitLinesFolder,
                    SplitPickingPanel.FOLDER_NAME_SPLIT_LINES_AD, true);
            ModelMgr.getModelMgr().addChildren(adFolder.getEntityId(), ads, EntityConstants.ATTRIBUTE_ENTITY);
        }

        if (!dbds.isEmpty()) {
            RootedEntity dbdFolder = ModelMgrUtils.getChildFolder(splitLinesFolder,
                    SplitPickingPanel.FOLDER_NAME_SPLIT_LINES_DBD, true);
            ModelMgr.getModelMgr().addChildren(dbdFolder.getEntityId(), dbds, EntityConstants.ATTRIBUTE_ENTITY);
        }
    }

    protected JMenu getAddToSplitPickingSessionItem() {

        if (!browser.getScreenEvaluationDialog().isAccessible()) {
            return null;
        }

        JMenu newFolderMenu = new JMenu("  Add To Screen Picking Folder");

        List<EntityData> rootEds = browser.getEntityOutline().getRootEntity().getOrderedEntityData();

        for (final EntityData rootEd : rootEds) {
            final Entity commonRoot = rootEd.getChildEntity();
            if (!ModelMgrUtils.hasWriteAccess(commonRoot))
                continue;

            JMenuItem commonRootItem = new JMenuItem(commonRoot.getName());
            commonRootItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    SimpleWorker worker = new SimpleWorker() {
                        @Override
                        protected void doStuff() throws Exception {
                            addToSplitFolder(commonRoot);
                        }

                        @Override
                        protected void hadSuccess() {
                            // No need to update the UI, the event bus will get it done
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            SessionMgr.getSessionMgr().handleException(error);
                        }
                    };
                    worker.execute();
                }
            });

            newFolderMenu.add(commonRootItem);
        }

        newFolderMenu.addSeparator();

        JMenuItem createNewItem = new JMenuItem("Create New...");

        createNewItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                // Add button clicked
                final String folderName = (String) JOptionPane.showInputDialog(browser, "Folder Name:\n",
                        "Create Split Picking Folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
                if ((folderName == null) || (folderName.length() <= 0)) {
                    return;
                }

                SimpleWorker worker = new SimpleWorker() {
                    private Entity newFolder;

                    @Override
                    protected void doStuff() throws Exception {
                        // Update database
                        newFolder = ModelMgr.getModelMgr().createCommonRoot(folderName);
                        addToSplitFolder(newFolder);
                    }

                    @Override
                    protected void hadSuccess() {
                        // No need to update the UI, the event bus will get it done
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });

        newFolderMenu.add(createNewItem);

        return newFolderMenu;
    }

    protected JMenu getAddToRootFolderItem() {

        if (!multiple && rootedEntity.getEntity() != null
                && rootedEntity.getEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT) != null) {
            return null;
        }

        JMenu newFolderMenu = new JScrollMenu("  Add To Top-Level Folder");

        JMenuItem createNewItem = new JMenuItem("Create New...");

        createNewItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                // Add button clicked
                final String folderName = (String) JOptionPane.showInputDialog(browser, "Folder Name:\n",
                        "Create top-level folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
                if ((folderName == null) || (folderName.length() <= 0)) {
                    return;
                }

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        // Update database
                        Entity newFolder = ModelMgr.getModelMgr().createCommonRoot(folderName);

                        List<Long> ids = new ArrayList<Long>();
                        for (RootedEntity rootedEntity : rootedEntityList) {
                            ids.add(rootedEntity.getEntity().getId());
                        }
                        ModelMgr.getModelMgr().addChildren(newFolder.getId(), ids, EntityConstants.ATTRIBUTE_ENTITY);
                    }

                    @Override
                    protected void hadSuccess() {
                        // No need to update the UI, the event bus will get it done
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });

        newFolderMenu.add(createNewItem);
        newFolderMenu.addSeparator();
        
        List<EntityData> rootEds = browser.getEntityOutline().getRootEntity().getOrderedEntityData();

        for (EntityData rootEd : rootEds) {
            final Entity commonRoot = rootEd.getChildEntity();
            if (!ModelMgrUtils.hasWriteAccess(commonRoot))
                continue;

            JMenuItem commonRootItem = new JMenuItem(commonRoot.getName());
            commonRootItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    SimpleWorker worker = new SimpleWorker() {
                        @Override
                        protected void doStuff() throws Exception {
                            List<Long> ids = new ArrayList<Long>();
                            for (RootedEntity rootedEntity : rootedEntityList) {
                                ids.add(rootedEntity.getEntity().getId());
                            }
                            ModelMgr.getModelMgr().addChildren(commonRoot.getId(), ids,
                                    EntityConstants.ATTRIBUTE_ENTITY);
                        }

                        @Override
                        protected void hadSuccess() {
                            // No need to update the UI, the event bus will get it done
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            SessionMgr.getSessionMgr().handleException(error);
                        }
                    };
                    worker.execute();
                }
            });

            newFolderMenu.add(commonRootItem);
        }

        return newFolderMenu;
    }

    protected JMenuItem getDeleteItem() {

        for (RootedEntity rootedEntity : rootedEntityList) {
            EntityData ed = rootedEntity.getEntityData();
            if (ed.getId() == null && !EntityUtils.isCommonRoot(ed.getChildEntity())) {
                // Fake ED, not a common root, this must be part of an
                // annotation session.
                // TODO: this check could be done more robustly
                return null;
            }
        }

        final Action action = new RemoveEntityAction(rootedEntityList, true);

        JMenuItem deleteItem = new JMenuItem("  " + action.getName());
        deleteItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                action.doAction();
            }
        });
        
        for (RootedEntity rootedEntity : rootedEntityList) {
            Entity entity = rootedEntity.getEntity();
            Entity parent = rootedEntity.getEntityData().getParentEntity();
            
            boolean canDelete = true;
            // User can't delete if they don't have write access
            if (!ModelMgrUtils.hasWriteAccess(entity)) {
                canDelete = false;
                // Unless they own the parent
                if (parent!=null && parent.getId()!=null && ModelMgrUtils.hasWriteAccess(parent)) {
                    canDelete = true;
                }
            }
            // Can never delete protected entities
            if (EntityUtils.isProtected(entity)) {
                canDelete = false;
            }
            if (!canDelete) deleteItem.setEnabled(false);
        }
        
        return deleteItem;
    }

    protected JMenuItem getMergeItem() {

        // If multiple items are not selected then leave
        if (!multiple) {
            return null;
        }

        HashSet<Long> parentIds = new HashSet<Long>();
        for (RootedEntity rootedEntity : rootedEntityList) {
            // Add all parent ids to a collection
            if (null != rootedEntity.getEntityData().getParentEntity()
                    && EntityConstants.TYPE_NEURON_FRAGMENT.equals(rootedEntity.getEntity().getEntityType().getName())) {
                parentIds.add(rootedEntity.getEntityData().getParentEntity().getId());
            }
            // if one of the selected entities has no parent or isn't owner by
            // the user then leave; cannot merge
            else {
                return null;
            }
        }
        // Anything but one parent id for selected entities should not allow
        // merge
        if (parentIds.size() != 1) {
            return null;
        }

        JMenuItem mergeItem = new JMenuItem("  Merge " + rootedEntityList.size() + " Selected Entities");

        mergeItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                SimpleWorker mergeTask = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        setProgress(1);
                        Long parentId = null;
                        List<Entity> fragments = new ArrayList<Entity>();
                        for (RootedEntity entity : rootedEntityList) {
                            Long resultId = ModelMgr
                                    .getModelMgr()
                                    .getAncestorWithType(entity.getEntity(),
                                            EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT).getId();
                            if (parentId == null) {
                                parentId = resultId;
                            } else if (resultId == null || !parentId.equals(resultId)) {
                                throw new IllegalStateException(
                                        "The selected neuron fragments are not part of the same neuron separation result: parentId="
                                                + parentId + " resultId=" + resultId);
                            }
                            fragments.add(entity.getEntityData().getChildEntity());
                        }

                        Collections.sort(fragments, new Comparator<Entity>() {
                            @Override
                            public int compare(Entity o1, Entity o2) {
                                Integer o1n = Integer.parseInt(o1
                                        .getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER));
                                Integer o2n = Integer.parseInt(o2
                                        .getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER));
                                return o1n.compareTo(o2n);
                            }
                        });

                        HashSet<String> fragmentIds = new LinkedHashSet<String>();
                        for (Entity fragment : fragments) {
                            fragmentIds.add(fragment.getId().toString());
                        }

                        // This should never happen
                        if (null == parentId) {
                            return;
                        }
                        
                        HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
                        taskParameters.add(new TaskParameter(NeuronMergeTask.PARAM_separationEntityId, parentId.toString(), null));
                        taskParameters.add(new TaskParameter(NeuronMergeTask.PARAM_commaSeparatedNeuronFragmentList, Task.csvStringFromCollection(fragmentIds), null));
                        ModelMgr.getModelMgr().submitJob("NeuronMerge", "Neuron Merge Task", taskParameters);
                    }

                    @Override
                    protected void hadSuccess() {
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }

                };

                mergeTask.execute();
            }
        });

        mergeItem.setEnabled(multiple);
        return mergeItem;
    }

    protected JMenuItem getSortBySimilarityItem() {

        // If multiple items are selected then leave
        if (multiple) {
            return null;
        }

        final Entity targetEntity = rootedEntity.getEntity();

        if (!targetEntity.getEntityType().getName().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)
                && !targetEntity.getEntityType().getName().equals(EntityConstants.TYPE_IMAGE_3D)) {
            return null;
        }

        String parentId = Utils.getParentIdFromUniqueId(rootedEntity.getUniqueId());
        final Entity folder = browser.getEntityOutline().getEntityByUniqueId(parentId);

        if (!folder.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
            return null;
        }

        JMenuItem sortItem = new JMenuItem("  Sort Folder By Similarity To This Image");

        sortItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
                    taskParameters.add(new TaskParameter("folder id", folder.getId().toString(), null));
                    taskParameters.add(new TaskParameter("target stack id", targetEntity.getId().toString(), null));
                    Task task = ModelMgr.getModelMgr().submitJob("SortBySimilarity", "Sort By Similarity", taskParameters);

                    final TaskDetailsDialog dialog = new TaskDetailsDialog(true);
                    dialog.showForTask(task);
                    browser.getViewerManager().getActiveViewer().refresh();
                } 
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });

        sortItem.setEnabled(ModelMgrUtils.hasWriteAccess(folder));
        return sortItem;
    }

    protected JMenuItem getDownloadMenu() {

        List<Entity> entitiesWithFilepaths = new ArrayList<Entity>();
        for(final RootedEntity rootedEntity : rootedEntityList) {
            final Entity targetEntity = rootedEntity.getEntity();
            final String filepath = EntityUtils.getDefault3dImageFilePath(targetEntity);
            if (filepath!=null) {
                entitiesWithFilepaths.add(targetEntity);
            }
        }
        if (entitiesWithFilepaths.isEmpty()) return null;
        
        String[] DOWNLOAD_EXTENSIONS = {"tif", "v3draw", "v3dpbd", "mp4"};
        String itemTitle = null;
        if (entitiesWithFilepaths.size()>1) {
            itemTitle = "  Download "+entitiesWithFilepaths.size()+" 3D Images As...";
        }
        else {
            itemTitle = "  Download 3D Image As...";
        }
        
        JMenu downloadMenu = new JMenu(itemTitle);
        for(String extension : DOWNLOAD_EXTENSIONS) {
            add(downloadMenu, getDownloadItem(entitiesWithFilepaths, false, extension));    
        }
        for(String extension : DOWNLOAD_EXTENSIONS) {
            add(downloadMenu, getDownloadItem(entitiesWithFilepaths, true, extension));    
        }
        return downloadMenu;
    }
    
    protected JMenuItem getDownloadItem(final List<Entity> entitiesWithFilepaths, final boolean splitChannels, final String extension) {
        
        String itemTitle = null;
        if (splitChannels) {
            if (multiple) {
                itemTitle = "Split Channel "+extension+" Files (Background Task)";
            }
            else {
                itemTitle = "Split Channel "+extension+" File (Background Task)";
            }
        }
        else {
            if (multiple) {
                itemTitle = extension+" Files (Background Task)";
            }
            else {
                itemTitle = extension+" File (Background Task)";
            }
        }
        
        JMenuItem downloadItem = new JMenuItem(itemTitle);

        downloadItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    for(final Entity entity : entitiesWithFilepaths) {

                        final String filepath = EntityUtils.getDefault3dImageFilePath(entity);
                        
                        SimpleWorker worker = new SimpleWorker() {

                            protected Entity default3dImage;
                            protected Entity sample;
                            protected File targetDir;
                            
                            @Override
                            protected void doStuff() throws Exception {
                                Entity targetLoaded = ModelMgr.getModelMgr().loadLazyEntity(entity, false);
                                this.default3dImage = targetLoaded.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                                if (entity.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE)) {
                                    this.sample = entity;
                                }
                                else {
                                    this.sample = ModelMgr.getModelMgr().getAncestorWithType(entity, EntityConstants.TYPE_SAMPLE);
                                }
                                this.targetDir = new File(splitChannels ? splitsDir : downloadDir, sample.getName());
                            }
                            
                            @Override
                            protected void hadSuccess() {
                                try {
                                    final String localFilePrefix = sample.getName()+"_ID"+default3dImage.getId()+"_";
                                    
                                    log.debug("Checking {} for files that start with {} and end with "+extension,targetDir,localFilePrefix);
                                    
                                    File[] files = targetDir.listFiles(new FilenameFilter() {
                                        @Override
                                        public boolean accept(File dir, String name) {
                                            return name.startsWith(localFilePrefix) && name.endsWith(extension);
                                        }
                                    });

                                    if (files!=null && files.length>0) {
                                        Object[] options = { "Open folder", "Run anyway" };
                                        int n = JOptionPane.showOptionDialog(browser, 
                                                "Files already exist. Open existing folder, or run the download anyway?", "Files already exist", JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                                        if (n==0) {
                                            OpenInFinderAction.revealFile(targetDir);
                                            return;
                                        }
                                    }
                                    
                                    Task task = null;
                                    if (splitChannels) {
                                        HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
                                        taskParameters.add(new TaskParameter("filepath", filepath, null));
                                        taskParameters.add(new TaskParameter("output extension", extension, null));
                                        ModelMgr.getModelMgr().submitJob("ConsoleSplitChannels", "Split Channels", taskParameters);
                                    }
                                    else {    
                                        HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
                                        taskParameters.add(new TaskParameter("filepath", filepath, null));
                                        taskParameters.add(new TaskParameter("output extension", extension, null));
                                        ModelMgr.getModelMgr().submitJob("ConsoleConvertFile", "Convert File", taskParameters);
                                    }

                                    TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                                        @Override
                                        public String getName() {
                                            return "Downloading "+sample.getName();
                                        }

                                        @Override
                                        protected void doStuff() throws Exception {

                                            setStatus("Grid execution");
                                            
                                            // Wait until task is finished
                                            super.doStuff(); 
                                            
                                            if (isCancelled()) throw new CancellationException();
                                            setStatus("Parse result");
                                            
                                            // Since there is no way to log task output vars, we use a convention where the last message 
                                            // will contain the output directory path.
                                            String resultFiles = null;
                                            List<TaskMessage> messages = new ArrayList<TaskMessage>(getTask().getMessages());
                                            if (!messages.isEmpty()) {
                                                Collections.sort(messages, new Comparator<TaskMessage>() {
                                                    @Override
                                                    public int compare(TaskMessage o1, TaskMessage o2) {
                                                        return o2.getMessageId().compareTo(o1.getMessageId());
                                                    }
                                                });
                                                resultFiles = messages.get(0).getMessage();
                                            }
                                            
                                            if (isCancelled()) throw new CancellationException();
                                            
                                            if (resultFiles==null) {
                                                throw new Exception("No result files generated");
                                            }
                                            
                                            // Copy the files to the local drive
                                            String[] pathAndFiles = resultFiles.split(":");
                                            String path = pathAndFiles[0];
                                            String[] files = pathAndFiles[1].split(",");
                                            for(String filepath : files) {
                                                copyChannelFile(path+"/"+filepath);    
                                            }
                                            
                                            if (isCancelled()) throw new CancellationException();
                                            setStatus("Done");
                                            
                                            setProgress(100);
                                        }

                                        private void copyChannelFile(String standardFilepath) throws Exception {
                                            File remoteFile = new File(standardFilepath);
                                            File localFile = new File(targetDir, localFilePrefix+remoteFile.getName());

                                            setStatus("Waiting to download...");
                                            copyFileLock.lock();
                                            try {
                                                setStatus("Downloading "+remoteFile.getName());
                                                Utils.copyURLToFile(standardFilepath, localFile, this);
                                            } finally {
                                                copyFileLock.unlock();
                                            }    
                                        }
                                        
                                        @Override
                                        public Callable<Void> getSuccessCallback() {
                                            return new Callable<Void>() {
                                                @Override
                                                public Void call() throws Exception {
                                                    OpenInFinderAction.revealFile(targetDir);
                                                    return null;
                                                }
                                            };
                                        }
                                    };

                                    taskWorker.executeWithEvents();
                                    
                                }
                                catch (Exception e) {
                                    hadError(e);
                                    return;
                                }   
                            }
                            
                            @Override
                            protected void hadError(Throwable error) {
                                SessionMgr.getSessionMgr().handleException(error);
                            }
                        };
                        
                        worker.execute();
                    }
                } 
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        
        return downloadItem;
    }
    
    protected JMenuItem getOpenInFirstViewerItem() {
        if (multiple)
            return null;
        if (StringUtils.isEmpty(rootedEntity.getUniqueId()))
            return null;
        JMenuItem copyMenuItem = new JMenuItem("  Open (Left Pane)");

        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        if (EntityUtils.isInitialized(rootedEntity.getEntity())) {
                            rootedEntity.setEntity(ModelMgr.getModelMgr().loadLazyEntity(rootedEntity.getEntity(),
                                    false));
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        browser.getViewerManager().showEntityInMainViewer(rootedEntity);
                        ModelMgr.getModelMgr().getEntitySelectionModel()
                                .selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, rootedEntity.getUniqueId(), true);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getOpenInSecondViewerItem() {
        if (multiple)
            return null;
        if (StringUtils.isEmpty(rootedEntity.getUniqueId()))
            return null;
        JMenuItem copyMenuItem = new JMenuItem("  Open (Right Pane)");

        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SimpleWorker worker = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        if (EntityUtils.isInitialized(rootedEntity.getEntity())) {
                            rootedEntity.setEntity(ModelMgr.getModelMgr().loadLazyEntity(rootedEntity.getEntity(),
                                    false));
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        browser.getViewerManager().showEntityInSecViewer(rootedEntity);
                        ModelMgr.getModelMgr().getEntitySelectionModel()
                                .selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, rootedEntity.getUniqueId(), true);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getOpenInFinderItem() {
        if (multiple)
            return null;
        if (!OpenInFinderAction.isSupported())
            return null;
        String filepath = EntityUtils.getAnyFilePath(rootedEntity.getEntity());
        JMenuItem menuItem = null;
        if (!StringUtils.isEmpty(filepath)) {
            menuItem = getActionItem(new OpenInFinderAction(rootedEntity.getEntity()) {
                @Override
                public String getName() {
                    String name = super.getName();
                    if (name == null)
                        return null;
                    return "  " + name;
                }
            });
        }
        return menuItem;
    }

    protected JMenuItem getOpenWithAppItem() {
        if (multiple)
            return null;
        if (!OpenWithDefaultAppAction.isSupported())
            return null;
        String filepath = EntityUtils.getAnyFilePath(rootedEntity.getEntity());
        if (!StringUtils.isEmpty(filepath)) {
            OpenWithDefaultAppAction action = new OpenWithDefaultAppAction(rootedEntity.getEntity()) {
                @Override
                public String getName() {
                    return "  Open With OS";
                }
            };
            return getActionItem(action);
        }
        return null;
    }

    protected JMenuItem getFijiViewerItem() {
        if (multiple)
            return null;
        final String path = EntityUtils.getDefault3dImageFilePath(rootedEntity.getEntity());
        if (path != null) {
            JMenuItem fijiMenuItem = new JMenuItem("  View In Fiji");
            fijiMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        ToolMgr.openFile(ToolMgr.TOOL_FIJI, path, null);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(browser, "Could not launch this tool. "
                                + "Please choose the appropriate file path from the Tools->Configure Tools area",
                                "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            return fijiMenuItem;
        }
        return null;
    }

    protected JMenuItem getNeuronAnnotatorItem() {
        if (multiple)
            return null;
        final String entityType = rootedEntity.getEntity().getEntityType().getName();
        if (entityType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)
                || entityType.equals(EntityConstants.TYPE_NEURON_FRAGMENT)) {
            JMenuItem vaa3dMenuItem = new JMenuItem("  View In Neuron Annotator");
            vaa3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        Entity result = rootedEntity.getEntity();
                        if (!entityType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                            result = ModelMgr.getModelMgr().getAncestorWithType(result,
                                    EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
                        }

                        if (result != null) {
                            // Check that there is a valid NA instance running
                            List<ExternalClient> clients = SessionMgr.getSessionMgr().getExternalClientsByName(ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME);
                            // If no NA client then try to start one
                            if (clients.isEmpty()) {
                                startNA();
                            }
                            // If NA clients "exist", make sure they are up
                            else {
                                ArrayList<ExternalClient> finalList = new ArrayList<ExternalClient>();
                                for (ExternalClient client : clients) {
                                    boolean connected = client.isConnected();
                                    if (!connected) {
                                        log.debug("Removing client "+client.getName()+" as the heartbeat came back negative.");
                                        SessionMgr.getSessionMgr().removeExternalClientByPort(client.getClientPort());
                                    }
                                    else {
                                        finalList.add(client);
                                    }
                                }
                                // If none are up then start one
                                if (finalList.size()==0) {
                                    startNA();
                                }
                            }

                            if (SessionMgr.getSessionMgr()
                                    .getExternalClientsByName(ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME).isEmpty()) {
                                JOptionPane.showMessageDialog(browser,
                                        "Could not get Neuron Annotator to launch and connect. "
                                                + "Please contact support.", "Launch ERROR", JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            log.debug("Requesting entity view in Neuron Annotator: " + result.getId());
                            ModelMgr.getModelMgr().notifyEntityViewRequestedInNeuronAnnotator(result.getId());
                        }
                    } catch (Exception e) {
                        SessionMgr.getSessionMgr().handleException(e);
                    }
                }
            });
            return vaa3dMenuItem;
        }
        return null;
    }

    private void startNA() throws Exception {
        log.debug("Client {} is not running. Starting a new instance.",
                ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME);
        ToolMgr.runTool(ToolMgr.TOOL_NA);
        boolean notRunning = true;
        int killCount = 0;
        while (notRunning && killCount < 2) {
            if (SessionMgr.getSessionMgr()
                    .getExternalClientsByName(ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME).isEmpty()) {
                log.debug("Waiting for {} to start.", ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME);
                Thread.sleep(3000);
                killCount++;
            }
            else {
                notRunning = false;
            }
        }
    }

    protected JMenuItem getVaa3dTriViewItem() {
        if (multiple)
            return null;
        final String path = EntityUtils.getDefault3dImageFilePath(rootedEntity.getEntity());
        if (path != null) {
            JMenuItem vaa3dMenuItem = new JMenuItem("  View In Vaa3D Tri-View");
            vaa3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        ToolMgr.openFile(ToolMgr.TOOL_VAA3D, path, null);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(browser, "Could not launch this tool. "
                                + "Please choose the appropriate file path from the Tools->Configure Tools area",
                                "ToolInfo Launch ERROR", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            return vaa3dMenuItem;
        }
        return null;
    }

    protected JMenuItem getVaa3d3dViewItem() {
        if (multiple)
            return null;
        final String path = EntityUtils.getDefault3dImageFilePath(rootedEntity.getEntity());
        if (path != null) {
            JMenuItem vaa3dMenuItem = new JMenuItem("  View In Vaa3D 3D View");
            vaa3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        ToolMgr.openFile(ToolMgr.TOOL_VAA3D, path, ToolMgr.MODE_3D);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(browser, "Could not launch this tool. "
                                + "Please choose the appropriate file path from the Tools->Configure Tools area",
                                "ToolInfo Launch ERROR", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            return vaa3dMenuItem;
        }
        return null;
    }

    protected JMenuItem getImportItem() {
        if (multiple) return null;
        
        String entityTypeName = rootedEntity.getEntity().getEntityType().getName();
        if (EntityConstants.TYPE_FOLDER.equals(entityTypeName) || EntityConstants.TYPE_SAMPLE.equals(entityTypeName)) {
            JMenuItem newAttachmentItem = new JMenuItem("  Import File(s) Here");
            newAttachmentItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        browser.getImportDialog().showDialog(rootedEntity);
                    } catch (Exception ex) {
                        SessionMgr.getSessionMgr().handleException(ex);
                    }
                }
            });

            return newAttachmentItem;
        }
        return null;
    }

    protected JMenuItem getNewFolderItem() {
        if (multiple) return null;
        
        if (EntityConstants.TYPE_FOLDER.equals(rootedEntity.getEntity().getEntityType().getName())) {
            JMenuItem newFolderItem = new JMenuItem("  Create New Folder");
            newFolderItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {

                    // Add button clicked
                    String folderName = (String) JOptionPane.showInputDialog(browser, "Folder Name:\n",
                            "Create folder under " + rootedEntity.getEntity().getName(), JOptionPane.PLAIN_MESSAGE,
                            null, null, null);
                    if ((folderName == null) || (folderName.length() <= 0)) {
                        return;
                    }

                    try {
                        // Update database
                        Entity parentFolder = rootedEntity.getEntity();
                        Entity newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
                        ModelMgr.getModelMgr().addEntityToParent(parentFolder, newFolder,
                                parentFolder.getMaxOrderIndex() + 1, EntityConstants.ATTRIBUTE_ENTITY);

                    } catch (Exception ex) {
                        SessionMgr.getSessionMgr().handleException(ex);
                    }
                }
            });

            return newFolderItem;
        }
        return null;
    }

    protected JMenuItem getCreateSessionItem() {
        if (multiple) return null;

        JMenuItem newFragSessionItem = new JMenuItem("  Create Annotation Session...");
        newFragSessionItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                final Entity entity = rootedEntity.getEntity();
                final String uniqueId = rootedEntity.getUniqueId();
                if (uniqueId == null)
                    return;

                SimpleWorker loadingWorker = new SimpleWorker() {
                    private Entity fullEntity;
                    private List<Entity> entities;

                    @Override
                    protected void doStuff() throws Exception {
                        fullEntity = ModelMgr.getModelMgr().loadLazyEntity(entity, true);
                        entities = EntityUtils.getDescendantsOfType(fullEntity, EntityConstants.TYPE_NEURON_FRAGMENT,
                                true);
                    }

                    @Override
                    protected void hadSuccess() {
                        browser.getAnnotationSessionPropertyDialog().showForNewSession(fullEntity.getName(), entities);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                loadingWorker.execute();

            }
        });

        return newFragSessionItem;
    }

    protected JMenuItem getSearchHereItem() {
        if (multiple) return null;
        
        JMenuItem searchHereMenuItem = new JMenuItem("  Search Here");
        searchHereMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    SessionMgr.getSessionMgr().getActiveBrowser().getGeneralSearchDialog()
                            .showDialog(rootedEntity.getEntity());
                } catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        return searchHereMenuItem;
    }

    private JMenuItem getActionItem(final Action action) {
        JMenuItem actionMenuItem = new JMenuItem(action.getName());
        actionMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.doAction();
            }
        });
        return actionMenuItem;
    }

    private JMenuItem getSpecialAnnotationSession() {
        JMenuItem specialAnnotationSession = new JMenuItem("  Special Annotation");
        specialAnnotationSession.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (null == ModelMgr.getModelMgr().getCurrentOntology()) {
                    JOptionPane.showMessageDialog(browser,
                            "Please select an ontology in the ontology window.", "Null Ontology Warning",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    if (!SpecialAnnotationChooserDialog.getDialog().isVisible()) {
                        SpecialAnnotationChooserDialog.getDialog().setVisible(true);
                    } else {
                        SpecialAnnotationChooserDialog.getDialog().transferFocus();
                    }
                }
            }
        });

        return specialAnnotationSession;
    }

    @Override
    public JMenuItem add(JMenuItem menuItem) {

        if (menuItem == null)
            return null;

        if ((menuItem instanceof JMenu)) {
            JMenu menu = (JMenu) menuItem;
            if (menu.getItemCount() == 0)
                return null;
        }

        if (nextAddRequiresSeparator) {
            addSeparator();
            nextAddRequiresSeparator = false;
        }

        return super.add(menuItem);
    }

    public JMenuItem add(JMenu menu, JMenuItem menuItem) {
        if (menu == null || menuItem == null)
            return null;
        return menu.add(menuItem);
    }

    public void setNextAddRequiresSeparator(boolean nextAddRequiresSeparator) {
        this.nextAddRequiresSeparator = nextAddRequiresSeparator;
    }
}
