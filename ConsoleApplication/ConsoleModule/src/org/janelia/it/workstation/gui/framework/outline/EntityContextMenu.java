package org.janelia.it.workstation.gui.framework.outline;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.ProgressMonitor;

import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.shared.utils.entity.DataReporter;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;
import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrEntityLoader;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.workstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.workstation.gui.dialogs.SetSortCriteriaDialog;
import org.janelia.it.workstation.gui.dialogs.SpecialAnnotationChooserDialog;
import org.janelia.it.workstation.gui.dialogs.TaskDetailsDialog;
import org.janelia.it.workstation.gui.dialogs.choose.EntityChooser;
import org.janelia.it.workstation.gui.framework.actions.Action;
import org.janelia.it.workstation.gui.framework.actions.AnnotateAction;
import org.janelia.it.workstation.gui.framework.actions.EditLVVSamplePathActionListener;
import org.janelia.it.workstation.gui.framework.actions.GoToRelatedEntityAction;
import org.janelia.it.workstation.gui.framework.actions.OpenInFinderAction;
import org.janelia.it.workstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.workstation.gui.framework.actions.RemoveEntityAction;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolMgr;
import org.janelia.it.workstation.gui.framework.viewer.Hud;
import org.janelia.it.workstation.gui.top_component.IconPanelTopComponent;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.model.utils.AnnotationSession;
import org.janelia.it.workstation.nb_action.EntityAcceptor;
import org.janelia.it.workstation.nb_action.ServiceAcceptorHelper;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SampleDownloadWorker;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.shared.workers.TaskMonitoringWorker;
import org.janelia.it.workstation.ws.ExternalClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityContextMenu extends JPopupMenu {

    private static final Logger log = LoggerFactory.getLogger(EntityContextMenu.class);

    protected static final Browser browser = SessionMgr.getBrowser();
    protected static final Component mainFrame = SessionMgr.getMainFrame();

    // Lock to make sure only one file is downloaded at a time
    private static final Lock copyFileLock = new ReentrantLock();

    // Current selection
    protected List<RootedEntity> rootedEntityList;
    protected RootedEntity rootedEntity;
    protected boolean multiple;
    protected boolean virtual;
    
    // Internal state
    protected boolean nextAddRequiresSeparator = false;

    public EntityContextMenu() {
    }
    
    public EntityContextMenu(List<RootedEntity> rootedEntityList) {
        init(rootedEntityList);
    }

    public  EntityContextMenu(RootedEntity rootedEntity) {
        List<RootedEntity> singleEntityList = new ArrayList<>();
        singleEntityList.add(rootedEntity);
        init(singleEntityList);
    }
    
    public final void init(List<RootedEntity> rootedEntityList) {
        this.rootedEntityList = rootedEntityList;
        this.rootedEntity = rootedEntityList.size() == 1 ? rootedEntityList.get(0) : null;
        this.multiple = rootedEntityList.size() > 1;
        for(RootedEntity re : rootedEntityList) {
            if (re.getEntityData()!=null && EntityUtils.isVirtual(re.getEntity())) {
                virtual = true;
                break;
            }
        }
    }
    
    public void addMenuItems() {
        
        if (rootedEntityList.isEmpty()) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }
        
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        add(getPasteAnnotationItem());
        add(getDetailsItem());
        add(getSetSortCriteriaItem());
        add(getGotoRelatedItem());
        
        setNextAddRequiresSeparator(true);
        add(getNewFolderItem());
        add(getAddToRootFolderItem());
        add(getRenameItem());
        add(getErrorFlag());
        add(getDeleteItem());
        add(getDeleteInBackgroundItem());
        add(getMarkForReprocessingItem());
        add(getSampleCompressionTypeItem());
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
        add(getSortBySimilarityItem());
        add(getMergeItem());
        add(getDownloadMenu());
        add(getImportItem());

        setNextAddRequiresSeparator(true);
        add(getHudMenuItem());
        for ( JComponent item: getOpenForContextItems() ) {
            add(item);
        }
        add(getEditLVVSamplePath());
        if (getWrapEntityItem() != null) {
            for (JMenuItem wrapItem: getWrapEntityItem()) {
                add(wrapItem);
            }
        }

        if ((SessionMgr.getSubjectKey().equals("user:simpsonj") || SessionMgr.getSubjectKey()
                .equals("group:simpsonlab")) && !this.multiple) {
            add(getSpecialAnnotationSession());
        }
    }

    private void addBadDataButtons(JMenu errorMenu) {

        Entity errorOntology = ModelMgr.getModelMgr().getErrorOntology();
        
        if (errorOntology!=null && EntityUtils.isInitialized(errorOntology)) {
            for (final EntityData entityData : ModelMgrUtils.getAccessibleEntityDatasWithChildren(errorOntology)) {
            	final OntologyElement element = new OntologyElement(entityData.getParentEntity(), entityData.getChildEntity());
                errorMenu.add(new JMenuItem(element.getName())).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {

                        final AnnotateAction action = new AnnotateAction();
                        action.init(element);
                        final String value = (String)JOptionPane.showInputDialog(mainFrame,
                                "Please provide details:\n", element.getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);
                        if (value==null || value.equals("")) return;
                        
                        SimpleWorker simpleWorker = new SimpleWorker() {
                            @Override
                            protected void doStuff() throws Exception {
                                action.doAnnotation(rootedEntity.getEntity(), element, value);
                                String annotationValue = "";
                                List<Entity> annotationEntities = ModelMgr.getModelMgr().getAnnotationsForEntity(rootedEntity.getEntity().getId());
                                for (Entity annotation : annotationEntities) {
                                    if (annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM).contains(element.getName())) {
                                        annotationValue = annotation.getName();
                                    }
                                }
                                DataReporter reporter = new DataReporter((String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL), ConsoleProperties.getString("console.HelpEmail"));
                                reporter.reportData(rootedEntity.getEntity(), annotationValue);
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
                    }
                });

            }

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
        detailsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.META_MASK));
        detailsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new EntityDetailsDialog().showForRootedEntity(rootedEntity);
            }
        });
        return detailsMenuItem;
    }

    protected JMenuItem getHudMenuItem() {
        JMenuItem toggleHudMI = null;
        if (rootedEntity != null && rootedEntity.getEntity() != null) {
            Entity entity = rootedEntity.getEntity();
            if (!entity.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
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

                // TODO: this should be encapsulated in an Action
                
                SimpleWorker worker = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        int i = 1;
                        
                        final List<RootedEntity> selectedEntities = browser.getViewerManager()
                                .getActiveViewer().getSelectedEntities();
                            
                        for (RootedEntity rootedEntity : selectedEntities) {
                            
                            OntologyAnnotation baseAnnotation = ModelMgr.getModelMgr().getCurrentSelectedOntologyAnnotation();
                            AnnotationSession tmpSession = ModelMgr.getModelMgr().getCurrentAnnotationSession();
                            OntologyAnnotation annotation = new OntologyAnnotation((null == tmpSession) ? null
                                    : tmpSession.getId(), rootedEntity.getEntityId(), baseAnnotation.getKeyEntityId(),
                                    baseAnnotation.getKeyString(), baseAnnotation.getValueEntityId(), baseAnnotation
                                            .getValueString());

                            Entity annotationEntity = ModelMgr.getModelMgr().createOntologyAnnotation(annotation);
                            log.info("Saved annotation as " + annotationEntity.getId());

                            for(EntityActorPermission eap : baseAnnotation.getEntity().getEntityActorPermissions()) {
                                ModelMgr.getModelMgr().grantPermissions(annotationEntity.getId(), eap.getSubjectKey(), eap.getPermissions(), false);
                                log.info("Shared copied annotation with " + eap.getSubjectKey());
                            }
                            
                            setProgress(i++, selectedEntities.size());
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }

                };

                worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Copying annotations", "", 0, 100));
                worker.execute();
            }
        });
        return pasteItem;
    }

    /** Makes the item for showing the entity in its own viewer iff the entity type is correct. */
    public Collection<JComponent> getOpenForContextItems() {
        TreeMap<Integer,JComponent> orderedMap = new TreeMap<>();
        if (rootedEntity != null && rootedEntity.getEntityData() != null) {
            final Entity entity = rootedEntity.getEntity();
            if (entity!=null) {

                final ServiceAcceptorHelper helper = new ServiceAcceptorHelper();
                Collection<EntityAcceptor> entityAcceptors
                        = helper.findHandler(
                                entity, 
                                EntityAcceptor.class,
                                EntityAcceptor.PERSPECTIVE_CHANGE_LOOKUP_PATH
                        );
                boolean lastItemWasSeparator = false;
                int expectedCount = 0;
                List<JComponent> actionItemList = new ArrayList<>();
                for ( EntityAcceptor entityAcceptor: entityAcceptors ) {                    
                    final Integer order = entityAcceptor.getOrder();
                    if (entityAcceptor.isPrecededBySeparator() && (! lastItemWasSeparator)) {
                        orderedMap.put(order - 1, new JSeparator());
                        expectedCount ++;
                    }
                    JMenuItem item = new JMenuItem(entityAcceptor.getActionLabel());
                    item.addActionListener( new EntityAcceptorActionListener( entityAcceptor ) );
                    orderedMap.put(order, item);
                    actionItemList.add( item ); // Bail alternative if ordering fails.
                    expectedCount ++;
                    if (entityAcceptor.isSucceededBySeparator()) {
                        orderedMap.put(order + 1, new JSeparator());
                        expectedCount ++;
                        lastItemWasSeparator = true;
                    }
                    else {
                        lastItemWasSeparator = false;
                    }
                }
                
                // This is the bail strategy for order key clashes.
                if ( orderedMap.size() < expectedCount) {
                    log.warn("With menu items and separators, expected {} but added {} open-for-context items." +
                            "  This indicates an order key clash.  Please check the getOrder methods of all impls." +
                            "  Returning an unordered version of item list.",
                            expectedCount, orderedMap.size());
                    return actionItemList;
                }
            }
        }
        return orderedMap.values();
    }
    
    public List<JMenuItem> getWrapEntityItem() {
        if (multiple) return Collections.EMPTY_LIST;
        return new WrapperCreatorItemFactory().makeEntityWrapperCreatorItem(rootedEntity);
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
        final Entity entity = rootedEntity.getEntity();
        String type = entity.getEntityTypeName();
        
        if (EntityConstants.TYPE_NEURON_FRAGMENT.equals(type)
                || EntityConstants.TYPE_LSM_STACK.equals(type)
                // TODO: this should be more specific, but right now we're only using virtual entities to represent LSMs
                || EntityConstants.IN_MEMORY_TYPE_VIRTUAL_ENTITY.equals(type) 
                || EntityConstants.TYPE_PIPELINE_RUN.equals(type) 
                || EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT.equals(type) 
                || EntityConstants.TYPE_ALIGNMENT_RESULT.equals(type) 
                || EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT.equals(type)
                || EntityConstants.TYPE_CURATED_NEURON.equals(type)
                || EntityConstants.TYPE_CURATED_NEURON_COLLECTION.equals(type)
                || EntityConstants.TYPE_CELL_COUNTING_RESULT.equals(type)) {
            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_SAMPLE, EntityConstants.TYPE_SAMPLE));
        }
        
        if (EntityConstants.TYPE_NEURON_FRAGMENT.equals(type)
                || EntityConstants.TYPE_CURATED_NEURON.equals(type)
                || EntityConstants.TYPE_CURATED_NEURON_COLLECTION.equals(type)) {
            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT,
                                EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT));
        }
        else if (EntityConstants.TYPE_FLY_LINE.equals(type)) {
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
        
        JMenuItem showAllRefsItem = new JMenuItem("Show All References");
        showAllRefsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gotoReference(rootedEntity);
            }
        });
        relatedMenu.add(showAllRefsItem);
        
        JMenuItem showAllPathsItem = new JMenuItem("Show All Paths...");
        showAllPathsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GoToRelatedEntityAction action = new GoToRelatedEntityAction(entity, null);
                action.doAction();
            }
        });
        relatedMenu.add(showAllPathsItem);
        
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

    private void gotoReference(final RootedEntity rootedEntity) {

        log.trace("Showing references of {}",rootedEntity.getName());
        
        Utils.setWaitingCursor(mainFrame);

        SimpleWorker worker = new SimpleWorker() {

            RootedEntity tempSearchRE;

            @Override
            protected void doStuff() throws Exception {
                List<EntityData> parentEds = ModelMgr.getModelMgr().getAllParentEntityDatas(rootedEntity.getEntityId());
                Entity entity = new Entity();
                entity.setId(TimebasedIdentifierGenerator.generateIdList(1).get(0));
                entity.setName("Entities referencing "+rootedEntity.getName());
                entity.setEntityTypeName(EntityConstants.TYPE_FOLDER);

                HashSet<EntityData> eds = new HashSet<>();
                for(EntityData parentEd : parentEds) {
                    EntityData ed = new EntityData();
                    ed.setEntityAttrName(EntityConstants.ATTRIBUTE_ENTITY);
                    ed.setParentEntity(entity);
                    Entity childEntity = ModelMgr.getModelMgr().getEntityById(parentEd.getParentEntity().getId());
                    ed.setChildEntity(childEntity);
                    eds.add(ed);
                }

                log.info("Setting "+eds.size()+" children");
                entity.setEntityData(eds);
                entity.setOwnerKey(SessionMgr.getSubjectKey());
                this.tempSearchRE = new RootedEntity(entity);
            }

            @Override
            protected void hadSuccess() {
                WindowLocator.activateAndGet(IconPanelTopComponent.PREFERRED_ID, "editor");
                browser.getViewerManager().showEntityInActiveViewer(tempSearchRE);
                Utils.queueDefaultCursor(mainFrame);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(mainFrame);
            }
        };

        worker.execute();
    }
    
    private void gotoEntity(final Entity entity, final String ancestorType) {

        log.trace("Navigating to ancestor {} of entity {}",ancestorType,entity);
        
        Utils.setWaitingCursor(mainFrame);

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
                }
                else {
                    // Find the best context to show the entity in
                    List<List<EntityData>> edPaths = ModelMgr.getModelMgr().getPathsToRoots(targetEntity.getId());
                    List<EntityDataPath> paths = new ArrayList<>();
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
                WindowLocator.activateAndGet(IconPanelTopComponent.PREFERRED_ID, "editor");
                log.info("Selecting {}",uniqueId);
                browser.getEntityOutline().selectEntityByUniqueId(uniqueId);
                Utils.queueDefaultCursor(mainFrame);
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setDefaultCursor(mainFrame);
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
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
        if (virtual) return null;
        
        JMenuItem renameItem = new JMenuItem("  Rename");
        renameItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                String newName = (String) JOptionPane.showInputDialog(mainFrame, "Name:\n", "Rename "
                        + rootedEntity.getEntity().getName(), JOptionPane.PLAIN_MESSAGE, null, null, rootedEntity
                        .getEntity().getName());
                if ((newName == null) || (newName.length() <= 0)) {
                    return;
                }

                try {
                    ModelMgr.getModelMgr().renameEntity(rootedEntity.getEntity(), newName);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
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

    protected JMenuItem getProcessingBlockItem() {

        final List<Entity> samples = new ArrayList<>();
        for (RootedEntity re : rootedEntityList) {
            Entity sample = re.getEntity();
            if (sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
                if (!sample.getName().contains("~")) {
                    samples.add(sample);
                }
            }
        }
        
        if (samples.isEmpty()) return null;
        
        final String samplesText = multiple?samples.size()+" Samples":"Sample";
        
        JMenuItem blockItem = new JMenuItem("  Purge And Block "+samplesText+" (Background Task)");
        blockItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                int result = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want to purge " + samples.size() + " sample(s) " +
                                "by deleting all large files associated with them, and block all future processing?",
                        "Purge And Block Processing", JOptionPane.OK_CANCEL_OPTION);

                if (result != 0) return;

                Task task;
                try {
                    StringBuilder sampleIdBuf = new StringBuilder();
                    for (Entity sample : samples) {
                        if (sampleIdBuf.length() > 0) sampleIdBuf.append(",");
                        sampleIdBuf.append(sample.getId());
                    }

                    HashSet<TaskParameter> taskParameters = new HashSet<>();
                    taskParameters.add(new TaskParameter("sample entity id", sampleIdBuf.toString(), null));
                    task = ModelMgr.getModelMgr().submitJob("ConsolePurgeAndBlockSample", "Purge And Block Sample", taskParameters);
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                    return;
                }

                TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                    @Override
                    public String getName() {
                        return "Purging and blocking " + samples.size() + " samples";
                    }

                    @Override
                    protected void doStuff() throws Exception {
                        setStatus("Executing");
                        super.doStuff();
                        for (Entity sample : samples) {
                            ModelMgr.getModelMgr().invalidateCache(sample, true);
                        }
                    }

                    @Override
                    public Callable<Void> getSuccessCallback() {
                        return new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                SessionMgr.getBrowser().getEntityOutline().refresh();
                                return null;
                            }
                        };
                    }
                };

                taskWorker.executeWithEvents();
            }
        });

        for(RootedEntity rootedEntity : rootedEntityList) {
            Entity sample = rootedEntity.getEntity();
            if (!ModelMgrUtils.hasWriteAccess(sample) || EntityUtils.isProtected(sample)) {
                blockItem.setEnabled(false);
                break;
            }
        }
        
        return blockItem;
    }
    
    private List<Entity> getSelectedSamples() {
        final List<Entity> samples = new ArrayList<>();
        for (RootedEntity rootedEntity : rootedEntityList) {
            Entity sample = rootedEntity.getEntity();
            if (sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE) && !sample.getName().contains("~")) {
                samples.add(sample);
            }
        }
        return samples;
    }

    protected JMenuItem getSampleCompressionTypeItem() {
    
        final List<Entity> samples = getSelectedSamples();
        if (samples.isEmpty()) return null;
        
        JMenu submenu = new JMenu("  Change Sample Compression Strategy");
        
        final int count = samples.size();
        final String samplesText = multiple?count+" Samples":"Sample";
        
        JMenuItem vllMenuItem = new JMenuItem("Visually Lossless (h5j)");
        vllMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                
                final String targetCompression = EntityConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS;
                String message = "Are you sure you want to convert "+samplesText+" to Visually Lossless (h5j) format?\n"
                        + "This will immediately delete all Lossless v3dpbd files for this Sample and result in a large decrease in disk space usage.\n"
                        + "Lossless files can be regenerated by reprocessing the Sample later.";
                int result = JOptionPane.showConfirmDialog(mainFrame, message,  "Change Sample Compression", JOptionPane.OK_CANCEL_OPTION);
                
                if (result != 0) return;

                SimpleWorker worker = new SimpleWorker() {
                    
                    StringBuilder sampleIdBuf = new StringBuilder();
                        
                    @Override
                    protected void doStuff() throws Exception {
                        for(final Entity sample : samples) {
                            ModelMgr.getModelMgr().setOrUpdateValue(sample, EntityConstants.ATTRIBUTE_COMRESSION_TYPE, targetCompression);
                            // Target is Visually Lossless, just run the compression service
                            if (sampleIdBuf.length()>0) sampleIdBuf.append(",");
                            sampleIdBuf.append(sample.getId());
                        }
                    }
                    
                    @Override
                    protected void hadSuccess() {  
                        if (sampleIdBuf.length()==0) return;
                        
                        Task task;
                        try {
                            HashSet<TaskParameter> taskParameters = new HashSet<>();
                            taskParameters.add(new TaskParameter("sample entity id", sampleIdBuf.toString(), null));
                            task = ModelMgr.getModelMgr().submitJob("ConsoleSampleCompression", "Console Sample Compression", taskParameters);
                        }
                        catch (Exception e) {
                            SessionMgr.getSessionMgr().handleException(e);
                            return;
                        }
                        
                        TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                            @Override
                            public String getName() {
                                return "Compressing "+samples.size()+" samples";
                            }

                            @Override
                            protected void doStuff() throws Exception {
                                setStatus("Executing");
                                super.doStuff();
                                for(Entity sample : samples) {
                                    ModelMgr.getModelMgr().invalidateCache(sample, true);
                                }
                            }
                            
                            @Override
                            public Callable<Void> getSuccessCallback() {
                                return new Callable<Void>() {
                                    @Override
                                    public Void call() throws Exception {
                                        SessionMgr.getBrowser().getEntityOutline().refresh();
                                        return null;
                                    }
                                };
                            }
                        };

                        taskWorker.executeWithEvents();
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                
                worker.execute();
            }
        });
        
        submenu.add(vllMenuItem);

        JMenuItem llMenuItem = new JMenuItem("Lossless (v3dpbd)");
        llMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                
                final String targetCompression = EntityConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J;
                String message = "Are you sure you want to mark "+samplesText+" for reprocessing into Lossless (v3dpbd) format?";
                int result = JOptionPane.showConfirmDialog(mainFrame, message,  "Change Sample Compression", JOptionPane.OK_CANCEL_OPTION);
                
                if (result != 0) return;

                SimpleWorker worker = new SimpleWorker() {
                                            
                    @Override
                    protected void doStuff() throws Exception {
                        for(final Entity sample : samples) {
                            ModelMgr.getModelMgr().setOrUpdateValue(sample, EntityConstants.ATTRIBUTE_COMRESSION_TYPE, targetCompression);
                            ModelMgr.getModelMgr().setOrUpdateValue(sample, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_MARKED);
                        }
                    }
                    
                    @Override
                    protected void hadSuccess() {  
                         JOptionPane.showMessageDialog(mainFrame, samplesText+" are marked for reprocessing to Lossless (v3dpbd) format, and will be available once the pipeline is run.", 
                                 "Marked Samples", JOptionPane.INFORMATION_MESSAGE);
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                
                worker.execute();
            }
        });
        
        submenu.add(llMenuItem);
        
        for(Entity sample : samples) {
            if (!ModelMgrUtils.hasWriteAccess(sample) || EntityUtils.isProtected(sample)) {
                vllMenuItem.setEnabled(false);
                break;
            }
        }

        return submenu;
    }
    
    protected JMenuItem getMarkForReprocessingItem() {

        final List<Entity> samples = new ArrayList<>();
        for (RootedEntity rootedEntity : rootedEntityList) {
            Entity sample = rootedEntity.getEntity();
            if (sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
                if (!sample.getName().contains("~")) {
                    samples.add(sample);
                }
            }
        }
        
        if (samples.isEmpty()) return null;

        final String samplesText = multiple?samples.size()+" Samples":"Sample";
        
        JMenuItem markItem = new JMenuItem("  Mark "+samplesText+" for Reprocessing");
        markItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                int result = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want these " + samples.size() + " sample(s) to be reprocessed "
                        + "during the next scheduled refresh?", "Mark for Reprocessing", JOptionPane.OK_CANCEL_OPTION);

                if (result != 0) return;

                SimpleWorker worker = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        for (final Entity sample : samples) {
                            ModelMgr.getModelMgr().setOrUpdateValue(sample, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_MARKED);
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                worker.execute();
            }
        });

        for(RootedEntity rootedEntity : rootedEntityList) {
            Entity sample = rootedEntity.getEntity();
            if (!ModelMgrUtils.hasWriteAccess(sample) || EntityUtils.isProtected(sample)) {
                markItem.setEnabled(false);
                break;
            }
        }

        return markItem;
    }
    
    private JMenuItem getVerificationMovieItem() {
        if (multiple) return null;

        if (!OpenWithDefaultAppAction.isSupported())
            return null;
        
        final Entity sample = rootedEntity.getEntity();

        if (!sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
            return null;
        }
        
        JMenuItem movieItem = new JMenuItem("  View Alignment Verification Movie");
        movieItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                SimpleWorker worker = new SimpleWorker() {

                    private Entity movie;

                    @Override
                    protected void doStuff() throws Exception {
                        ModelMgr.getModelMgr().loadLazyEntity(sample, false);
                        Entity alignedSample = null;
                        for (Entity child : ModelMgrUtils.getAccessibleChildren(sample)) {
                            if (child.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)
                                    && child.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE) != null) {
                                alignedSample = child;
                            }
                        }

                        if (alignedSample == null) {
                            alignedSample = sample;
                        }

                        final ModelMgrEntityLoader loader = new ModelMgrEntityLoader();
                        EntityVistationBuilder.create(loader).startAt(alignedSample)
                                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
                                .childrenOfType(EntityConstants.TYPE_ALIGNMENT_RESULT)
                                .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                                .run(new EntityVisitor() {
                                    public void visit(Entity supportingData) throws Exception {
                                        loader.populateChildren(supportingData);
                                        for (Entity child : ModelMgrUtils.getAccessibleChildren(supportingData)) {
                                            if (child.getName().equals("VerifyMovie.mp4")
                                                    || child.getName().equals("AlignVerify.mp4")) {
                                                movie = child;
                                                break;
                                            }
                                        }
                                    }
                                });
                    }

                    @Override
                    protected void hadSuccess() {

                        if (movie == null) {
                            JOptionPane.showMessageDialog(mainFrame, "Could not locate verification movie",
                                    "Not Found", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        String filepath = EntityUtils.getFilePath(movie);
                        if (StringUtils.isEmpty(filepath)) {
                            JOptionPane.showMessageDialog(mainFrame, "Verification movie has no path",
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

        if (EntityConstants.VALUE_BLOCKED.equals(sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS))) {
            movieItem.setEnabled(false);
        }
        
        if (!ModelMgrUtils.hasWriteAccess(sample) || EntityUtils.isProtected(sample)) {
            movieItem.setEnabled(false);
        }

        return movieItem;
    }
    
    private static final int MAX_ADD_TO_ROOT_HISTORY = 5;
    
    private void updateAddToRootFolderHistory(Entity commonRoot) {
        List<Long> addHistory = (List<Long>)SessionMgr.getSessionMgr().getModelProperty(Browser.ADD_TO_ROOT_HISTORY);
        if (addHistory==null) {
            addHistory = new ArrayList<>();
        }
        if (addHistory.contains(commonRoot.getId())) {
            return;
        }
        if (addHistory.size()>=MAX_ADD_TO_ROOT_HISTORY) {
            addHistory.remove(addHistory.size()-1);
        }
        addHistory.add(0, commonRoot.getId());
        SessionMgr.getSessionMgr().setModelProperty(Browser.ADD_TO_ROOT_HISTORY, addHistory);
    }
    
    protected JMenu getAddToRootFolderItem() {

        final EntityOutline entityOutline = SessionMgr.getBrowser().getEntityOutline();
                
        JMenu newFolderMenu = new JMenu("  Add To Folder");

        JMenuItem createNewItem = new JMenuItem("Create New Top-Level Folder...");

        createNewItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                // Add button clicked
                final String folderName = (String) JOptionPane.showInputDialog(mainFrame, "Folder Name:\n",
                        "Create top-level folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
                if ((folderName == null) || (folderName.length() <= 0)) {
                    return;
                }

                SimpleWorker worker = new SimpleWorker() {
                    private Entity commonRoot;

                    @Override
                    protected void doStuff() throws Exception {
                        // Update database
                        commonRoot = ModelMgr.getModelMgr().createCommonRoot(folderName);
                        addToCommonRoot(commonRoot);
                    }

                    @Override
                    protected void hadSuccess() {
                        // No need to update the UI, the event bus will get it done
                        log.debug("Added to common root {}",commonRoot.getName());
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Creating folder...", ""));
                worker.execute();
            }
        });

        newFolderMenu.add(createNewItem);
        
        JMenuItem chooseItem = new JMenuItem("Choose Folder...");
        
        chooseItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                final EntityChooser entityChooser = new EntityChooser("Choose folder to add to", entityOutline);
                int returnVal = entityChooser.showDialog(entityOutline);
                if (returnVal != EntityChooser.CHOOSE_OPTION) return;
                if (entityChooser.getChosenElements().isEmpty()) return;
                final Entity commonRoot = entityChooser.getChosenElements().get(0).getChildEntity();
                
                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        addToCommonRoot(commonRoot);
                    }

                    @Override
                    protected void hadSuccess() {
                        // No need to update the UI, the event bus will get it done
                        log.debug("Added to common root {}",commonRoot.getName());
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });

        newFolderMenu.add(chooseItem);
        newFolderMenu.addSeparator();
        
        List<Long> addHistory = (List<Long>)SessionMgr.getSessionMgr().getModelProperty(Browser.ADD_TO_ROOT_HISTORY);
        if (addHistory!=null && !addHistory.isEmpty()) {
            
            JMenuItem item = new JMenuItem("Recent:");
            item.setEnabled(false);
            newFolderMenu.add(item);
            
            for (Long rootId : addHistory) {

                Set<Entity> entities = entityOutline.getEntitiesById(rootId);

                if (entities.isEmpty()) {
                    continue;
                }
                if (entities.size()>1) {
                    log.warn("More than one entity in the entity outline for id={}",rootId);
                }

                final Entity commonRoot = entities.iterator().next();
                if (!ModelMgrUtils.hasWriteAccess(commonRoot)) continue;

                JMenuItem commonRootItem = new JMenuItem(commonRoot.getName());
                commonRootItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                
                        SimpleWorker worker = new SimpleWorker() {
                            @Override
                            protected void doStuff() throws Exception {
                                addToCommonRoot(commonRoot);
                            }

                            @Override
                            protected void hadSuccess() {
                                // No need to update the UI, the event bus will get it done
                                log.debug("Added to common root {}",commonRoot.getName());
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
        }
        
        return newFolderMenu;
    }

    private void addToCommonRoot(Entity commonRoot) throws Exception {

        // Update database
        List<Long> ids = new ArrayList<>();
        for (RootedEntity re : rootedEntityList) {
            Entity entity = re.getEntity();
            if (!EntityConstants.IN_MEMORY_TYPE_PLACEHOLDER_ENTITY.equals(entity.getEntityTypeName()) && entity.getId()!=null) {
                ids.add(entity.getId());
            }
        }
        
        ModelMgr.getModelMgr().addChildren(commonRoot.getId(), ids, EntityConstants.ATTRIBUTE_ENTITY);
        // Update history
        updateAddToRootFolderHistory(commonRoot);                
    }
    
    protected JMenuItem getDeleteItem() {
        return getDeleteItem("", false);
    }
    
    protected JMenuItem getDeleteInBackgroundItem() {
        return getDeleteItem(" (Background Task)", true);
    }

    private JMenuItem getDeleteItem(String nameSuffix, boolean runInBackground) {

        for (RootedEntity rootedEntity : rootedEntityList) {
            EntityData ed = rootedEntity.getEntityData();
            if (ed.getId() == null && !EntityUtils.isCommonRoot(ed.getChildEntity()) && !EntityUtils.isOntologyRoot(ed.getChildEntity())) {
                // Fake ED, not a root, this must be part of an annotation session.
                // TODO: this check could be done more robustly
                return null;
            }
        }

        final Action action = new RemoveEntityAction(rootedEntityList, true, runInBackground);

        JMenuItem deleteItem = new JMenuItem("  " + action.getName()+nameSuffix);
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
                // Unless they own the parent
                if (parent!=null && parent.getId()!=null && ModelMgrUtils.hasWriteAccess(parent)) {
                    canDelete = true;
                }
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

        HashSet<Long> parentIds = new HashSet<>();
        for (RootedEntity rootedEntity : rootedEntityList) {
            // Add all parent ids to a collection
            if (null != rootedEntity.getEntityData().getParentEntity()
                    && EntityConstants.TYPE_NEURON_FRAGMENT.equals(rootedEntity.getEntity().getEntityTypeName())) {
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

        JMenuItem mergeItem = new JMenuItem("  Merge " + rootedEntityList.size() + " Selected Neurons");

        mergeItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                SimpleWorker mergeTask = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        setProgress(1);
                        Long parentId = null;
                        List<Entity> fragments = new ArrayList<>();
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

                        HashSet<String> fragmentIds = new LinkedHashSet<>();
                        for (Entity fragment : fragments) {
                            fragmentIds.add(fragment.getId().toString());
                        }

                        // This should never happen
                        if (null == parentId) {
                            return;
                        }
                        
                        HashSet<TaskParameter> taskParameters = new HashSet<>();
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

        if (!targetEntity.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)
                && !targetEntity.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
            return null;
        }

        String parentId = Utils.getParentIdFromUniqueId(rootedEntity.getUniqueId());
        final Entity folder = browser.getEntityOutline().getEntityByUniqueId(parentId);

        if (!folder.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
            return null;
        }

        JMenuItem sortItem = new JMenuItem("  Sort Folder By Similarity To This Image");

        sortItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    HashSet<TaskParameter> taskParameters = new HashSet<>();
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

    protected JMenuItem getSetSortCriteriaItem() {

        if (multiple) {
            return null;
        }

        final Entity targetEntity = rootedEntity.getEntity();
        final String targetType = targetEntity.getEntityTypeName();
        if (!targetType.equals(EntityConstants.TYPE_FOLDER) && !targetType.equals(EntityConstants.TYPE_WORKSPACE)) {
            return null;
        }
        
        JMenuItem sortItem = new JMenuItem("  Set Sorting Criteria");

        sortItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    SetSortCriteriaDialog dialog = new SetSortCriteriaDialog();
                    dialog.showForEntity(targetEntity);
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });

        return sortItem;
    }
    
    protected JMenuItem getDownloadMenu() {

        boolean allLsm = true;
        List<Entity> entitiesWithFilepaths = new ArrayList<>();
        for(final RootedEntity re : rootedEntityList) {
            final Entity entity = re.getEntity();
            final String filepath = EntityUtils.getDefault3dImageFilePath(entity);
            if (filepath!=null) {
                entitiesWithFilepaths.add(entity);
                if (!EntityConstants.TYPE_LSM_STACK.equals(entity.getEntityTypeName())) {
                    allLsm = false;
                }
            }
        }
        if (entitiesWithFilepaths.isEmpty()) {
            return null;
        }
        
        String[] DOWNLOAD_EXTENSIONS = {"tif", "v3draw", "v3dpbd", "mp4", "h5j"};
        String itemTitle;
        if (entitiesWithFilepaths.size()>1) {
            itemTitle = "  Download "+entitiesWithFilepaths.size()+" 3D Images As...";
        }
        else {
            itemTitle = "  Download 3D Image As...";
        }
        
        JMenu downloadMenu = new JMenu(itemTitle);
        
        if (allLsm) {
            add(downloadMenu, getDownloadItem(entitiesWithFilepaths, false, Utils.EXTENSION_LSM));
            add(downloadMenu, getDownloadItem(entitiesWithFilepaths, false, Utils.EXTENSION_LSM_BZ2));
        }
        
        for(String extension : DOWNLOAD_EXTENSIONS) {
            add(downloadMenu, getDownloadItem(entitiesWithFilepaths, false, extension));
        }
        for(String extension : DOWNLOAD_EXTENSIONS) {
            JMenuItem downloadItem = getDownloadItem(entitiesWithFilepaths, true, extension);
            add(downloadMenu, downloadItem);
        }
        
        return downloadMenu;
    }

    protected JMenuItem getDownloadItem(final Collection<Entity> entitiesWithFilepaths,
                                        final boolean splitChannels,
                                        final String extension) {

        String itemTitle;
        if (splitChannels) {
            if (multiple) {
                itemTitle = "Split Channel "+extension;
            }
            else {
                itemTitle = "Split Channel "+extension;
            }
        }
        else {
            if (multiple) {
                itemTitle = extension;
            }
            else {
                itemTitle = extension;
            }
        }
        
        JMenuItem downloadItem = new JMenuItem(itemTitle);

        downloadItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    for (Entity entity : entitiesWithFilepaths) {
                        SampleDownloadWorker sampleDownloadWorker =
                                new SampleDownloadWorker(entity, extension, splitChannels, copyFileLock);
                        sampleDownloadWorker.execute();
                    }
                } catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        
        return downloadItem;
    }

    protected JMenuItem getOpenInFirstViewerItem() {
        if (multiple) return null;
        if (virtual) return null;
        if (StringUtils.isEmpty(rootedEntity.getUniqueId())) return null;
        
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
        if (multiple) return null;
        if (virtual) return null;
        if (StringUtils.isEmpty(rootedEntity.getUniqueId())) return null;
        
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
        String path = EntityUtils.getAnyFilePath(rootedEntity.getEntity());
        JMenuItem menuItem = null;
        if (!StringUtils.isEmpty(path)) {
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
        String path = EntityUtils.getAnyFilePath(rootedEntity.getEntity());
        if (!StringUtils.isEmpty(path)) {
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
        String tmpPath = EntityUtils.getDefault3dImageFilePath(rootedEntity.getEntity());

        // Not every image has a default image
        String tmpAnyPath = EntityUtils.getAnyFilePath(rootedEntity.getEntity());
        if (null!=tmpAnyPath&&!(tmpAnyPath.toLowerCase().endsWith("nrrd")||tmpAnyPath.toLowerCase().endsWith("h5j")||
                                tmpAnyPath.toLowerCase().endsWith("v3dpbd")||tmpAnyPath.endsWith("v3draw")||
                                tmpAnyPath.endsWith("tiff")||tmpAnyPath.endsWith("tif"))) {
            tmpAnyPath=null;
        }
        final String path = (!StringUtils.isEmpty(tmpPath))?tmpPath:tmpAnyPath;
        if (!StringUtils.isEmpty(path)) {
            JMenuItem fijiMenuItem = new JMenuItem("  View In Fiji");
            fijiMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        ToolMgr.openFile(ToolMgr.TOOL_FIJI, path, null);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(mainFrame, "Could not launch this tool. "
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
        final String entityType = rootedEntity.getEntity().getEntityTypeName();
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
                                ArrayList<ExternalClient> finalList = new ArrayList<>();
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
                                JOptionPane.showMessageDialog(mainFrame,
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
        if (!StringUtils.isEmpty(path)) {
            JMenuItem vaa3dMenuItem = new JMenuItem("  View In Vaa3D Tri-View");
            vaa3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        ToolMgr.openFile(ToolMgr.TOOL_VAA3D, path, null);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(mainFrame, "Could not launch this tool. "
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
        if (!StringUtils.isEmpty(path)) {
            JMenuItem vaa3dMenuItem = new JMenuItem("  View In Vaa3D 3D View");
            vaa3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        ToolMgr.openFile(ToolMgr.TOOL_VAA3D, path, ToolMgr.MODE_3D);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(mainFrame, "Could not launch this tool. "
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
        
        String entityTypeName = rootedEntity.getEntity().getEntityTypeName();
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
        
        if (EntityConstants.TYPE_FOLDER.equals(rootedEntity.getEntity().getEntityTypeName())) {
            JMenuItem newFolderItem = new JMenuItem("  Create New Folder");
            newFolderItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {

                    // Add button clicked
                    String folderName = (String) JOptionPane.showInputDialog(mainFrame, "Folder Name:\n",
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

    protected JMenuItem getSearchHereItem() {
        if (multiple) return null;
        if (virtual) return null;
        
        JMenuItem searchHereMenuItem = new JMenuItem("  Search Here");
        searchHereMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    SessionMgr.getBrowser().getGeneralSearchDialog()
                            .showDialog(rootedEntity.getEntity());
                } catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        return searchHereMenuItem;
    }

    protected JMenuItem getActionItem(final Action action) {
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
                    JOptionPane.showMessageDialog(mainFrame,
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

    protected JMenuItem getEditLVVSamplePath() {
        if (multiple)
            return null;
        final String entityType = rootedEntity.getEntity().getEntityTypeName();
        if (entityType.equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            JMenuItem menuItem = new JMenuItem("  Edit sample path");
            menuItem.addActionListener(new EditLVVSamplePathActionListener(rootedEntity));
            return menuItem;
        } else {
            return null;
        }
    }

    protected JMenuItem getRemoteSWCLoad() {
        if (multiple) {
            return null;
        }
        final String entityType = rootedEntity.getEntity().getEntityTypeName();
        if (entityType.equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            JMenuItem menuItem = new JMenuItem("  ????");
            menuItem.addActionListener(new EditLVVSamplePathActionListener(rootedEntity));
            return menuItem;
        } else {
            return null;
        }
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
    
    public class EntityAcceptorActionListener implements ActionListener {

        private EntityAcceptor entityAcceptor;

        public EntityAcceptorActionListener(EntityAcceptor entityAcceptor) {
            this.entityAcceptor = entityAcceptor;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Entity entity = rootedEntity.getEntity();
                // Pickup the sought value.
                entityAcceptor.acceptEntity(entity);
            } catch (Exception ex) {
                ModelMgr.getModelMgr().handleException(ex);
            }

        }
    }
}
