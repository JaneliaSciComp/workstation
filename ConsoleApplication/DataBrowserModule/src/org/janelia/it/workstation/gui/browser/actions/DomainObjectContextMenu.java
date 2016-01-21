package org.janelia.it.workstation.gui.browser.actions;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.StateMgr;
import org.janelia.it.workstation.gui.browser.components.DomainViewerManager;
import org.janelia.it.workstation.gui.browser.components.DomainViewerTopComponent;
import org.janelia.it.workstation.gui.browser.components.SampleResultViewerManager;
import org.janelia.it.workstation.gui.browser.components.SampleResultViewerTopComponent;
import org.janelia.it.workstation.gui.browser.components.ViewerUtils;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.selection.SampleResultSelectionEvent;
import org.janelia.it.workstation.gui.browser.gui.support.PopupContextMenu;
import org.janelia.it.workstation.gui.browser.model.SampleResult;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectContextMenu extends PopupContextMenu {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectContextMenu.class);

    // Current selection
    protected Component source;
    protected DomainObject contextObject;
    protected List<DomainObject> domainObjectList;
    protected DomainObject domainObject;
    protected boolean multiple;

    public DomainObjectContextMenu(Component source, DomainObject contextObject, List<DomainObject> domainObjectList) {
        this.source = source;
        this.contextObject = contextObject;
        this.domainObjectList = domainObjectList;
        this.domainObject = domainObjectList.size() == 1 ? domainObjectList.get(0) : null;
        this.multiple = domainObjectList.size() > 1;
    }
    
    public void addMenuItems() {
        
        if (domainObjectList.isEmpty()) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }
        
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        
        setNextAddRequiresSeparator(true);
        add(getOpenInNewEditorItem());
        add(getOpenSampleInNewEditorItem());
        add(getOpenSeparationInNewEditorItem());
        
        setNextAddRequiresSeparator(true);
        add(getPasteAnnotationItem());
//        add(getDetailsItem());
//        add(getPermissionItem());
//        add(getGotoRelatedItem());
        
        add(getAddToSetItem());
        add(getRemoveFromSetItem());
        
//      setNextAddRequiresSeparator(true);
//        add(getErrorFlag());
//        add(getMarkForReprocessingItem());
//        add(getProcessingBlockItem());
//        add(getVerificationMovieItem());
//        
//        setNextAddRequiresSeparator(true);
//        add(getSortBySimilarityItem());
//        add(getMergeItem());
//        add(getImportItem());
//
//        setNextAddRequiresSeparator(true);
//        add(getHudMenuItem());
//        for ( JComponent item: getOpenForContextItems() ) {
//            add(item);
//        }
//        add(getWrapEntityItem());
//
//        if ((AccessManager.getSubjectKey().equals("user:simpsonj") || AccessManager.getSubjectKey()
//                .equals("group:simpsonlab")) && !this.multiple) {
//            add(getSpecialAnnotationSession());
//        }
    }

    protected JMenuItem getTitleItem() {
        String name = multiple ? "(Multiple selected)" : domainObject.getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }

    protected JMenuItem getCopyNameToClipboardItem() {
        if (multiple) return null;
        
        JMenuItem copyMenuItem = new JMenuItem("  Copy Name To Clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transferable t = new StringSelection(domainObject.getName());
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
                Transferable t = new StringSelection(domainObject.getId().toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        });
        return copyMenuItem;
    }
    
    protected JMenuItem getOpenInNewEditorItem() {
        if (multiple) return null;
        if (!DomainViewerTopComponent.isSupported(domainObject)) return null;
        
        JMenuItem copyMenuItem = new JMenuItem("  Open In New Viewer");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DomainViewerTopComponent viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
                viewer.requestActive();
                viewer.loadDomainObject(domainObject);
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getOpenSampleInNewEditorItem() {
        if (multiple) return null;
        if (!(domainObject instanceof NeuronFragment)) return null;
        
        JMenuItem copyMenuItem = new JMenuItem("  Open Sample In New Viewer");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final DomainViewerTopComponent viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
                final NeuronFragment neuronFragment = (NeuronFragment)domainObject;
                
                SimpleWorker worker = new SimpleWorker() {
                    Sample sample;
                    
                    @Override
                    protected void doStuff() throws Exception {
                        sample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(neuronFragment.getSample());
                    }

                    @Override
                    protected void hadSuccess() {
                        viewer.requestActive();
                        viewer.loadDomainObject(sample);
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
    
    protected JMenuItem getOpenSeparationInNewEditorItem() {
        if (multiple) return null;
        if (!(domainObject instanceof NeuronFragment)) return null;
        
        JMenuItem copyMenuItem = new JMenuItem("  Open Neuron Separation In New Viewer");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SampleResultViewerTopComponent viewer = ViewerUtils.createNewViewer(SampleResultViewerManager.getInstance(), "editor3");
                final NeuronFragment neuronFragment = (NeuronFragment)domainObject;
                

                SimpleWorker worker = new SimpleWorker() {
                    SampleResult sampleResult;
                    
                    @Override
                    protected void doStuff() throws Exception {
                        Sample sample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(neuronFragment.getSample());
                        sampleResult = getNeuronSeparation(sample, neuronFragment);
                    }

                    @Override
                    protected void hadSuccess() {
                        viewer.requestActive();
                        viewer.loadSampleResult(sampleResult, true, null);
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
    
    public static SampleResult getNeuronSeparation(Sample sample, NeuronFragment neuronFragment) {
        if (neuronFragment==null) return null;
        for(String objective : sample.getOrderedObjectives()) {
            ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
            for(SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
                if (run!=null && run.getResults()!=null) {
                    for(PipelineResult result : run.getResults()) {
                        if (result!=null && result.getResults()!=null) {
                            for(PipelineResult secondaryResult : result.getResults()) {
                                if (secondaryResult!=null && secondaryResult instanceof NeuronSeparation) {
                                    NeuronSeparation separation = (NeuronSeparation)secondaryResult;
                                    if (separation.getFragmentsReference().getReferenceId().equals(neuronFragment.getSeparationId())) {
                                        return new SampleResult(sample, separation);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
//    private void addBadDataButtons(JMenu errorMenu) {
//
//        Entity errorOntology = ModelMgr.getModelMgr().getErrorOntology();
//        
//        if (errorOntology!=null && EntityUtils.isInitialized(errorOntology)) {
//            for (final EntityData entityData : ModelMgrUtils.getAccessibleEntityDatasWithChildren(errorOntology)) {
//            	final OntologyElement element = new OntologyElement(entityData.getParentEntity(), entityData.getChildEntity());
//                errorMenu.add(new JMenuItem(element.getName())).addActionListener(new ActionListener() {
//                    @Override
//                    public void actionPerformed(ActionEvent e) {
//
//                        final AnnotateAction action = new AnnotateAction();
//                        action.init(element);
//                        final String value = (String)JOptionPane.showInputDialog(mainFrame,
//                                "Please provide details:\n", element.getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);
//                        if (value==null || value.equals("")) return;
//                        
//                        SimpleWorker simpleWorker = new SimpleWorker() {
//                            @Override
//                            protected void doStuff() throws Exception {
//                                action.doAnnotation(rootedEntity.getEntity(), element, value);
//                                String annotationValue = "";
//                                List<Entity> annotationEntities = ModelMgr.getModelMgr().getAnnotationsForEntity(rootedEntity.getEntity().getId());
//                                for (Entity annotation : annotationEntities) {
//                                    if (annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM).contains(element.getName())) {
//                                        annotationValue = annotation.getName();
//                                    }
//                                }
//                                DataReporter reporter = new DataReporter((String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL), ConsoleProperties.getString("console.HelpEmail"));
//                                reporter.reportData(rootedEntity.getEntity(), annotationValue);
//                            }
//
//                            @Override
//                            protected void hadSuccess() {
//                            }
//
//                            @Override
//                            protected void hadError(Throwable error) {
//                                SessionMgr.getSessionMgr().handleException(error);
//                            }
//                        };
//                        
//                        simpleWorker.execute();
//                    }
//                });
//
//            }
//
//        }
//    }

    
//    protected JMenuItem getDetailsItem() {
//        if (multiple) return null;
//        
//        JMenuItem detailsMenuItem = new JMenuItem("  View Details");
//        detailsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.META_MASK));
//        detailsMenuItem.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                new EntityDetailsDialog().showForRootedEntity(rootedEntity);
//            }
//        });
//        return detailsMenuItem;
//    }
//
//    protected JMenuItem getPermissionItem() {
//        if (multiple) return null;
//        if (virtual) return null;
//        
//        if (!ModelMgrUtils.isOwner(rootedEntity.getEntity())) return null;
//        
//        JMenuItem detailsMenuItem = new JMenuItem("  Change Permissions");
//        detailsMenuItem.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                new EntityDetailsDialog().showForRootedEntity(rootedEntity, EntityDetailsPanel.TAB_NAME_PERMISSIONS);
//            }
//        });
//        return detailsMenuItem;
//    }
//
//    protected JMenuItem getHudMenuItem() {
//        JMenuItem toggleHudMI = null;
//        if (rootedEntity != null && rootedEntity.getEntity() != null) {
//            Entity entity = rootedEntity.getEntity();
//            if (!entity.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
//                toggleHudMI = new JMenuItem("  Show in Lightbox");
//                toggleHudMI.addActionListener(new ActionListener() {
//                    @Override
//                    public void actionPerformed(ActionEvent e) {
//                        Entity entity = rootedEntity.getEntity();
//                        Hud.getSingletonInstance().setEntityAndToggleDialog(entity);
//                    }
//                });
//            }
//        }
//
//        return toggleHudMI;
//    }
//
//    private void sortPathsByPreference(List<EntityDataPath> paths) {
//
//        Collections.sort(paths, new Comparator<EntityDataPath>() {
//            @Override
//            public int compare(EntityDataPath p1, EntityDataPath p2) {
//                Integer p1Score = 0;
//                Integer p2Score = 0;
//                p1Score += p1.getRootOwner().startsWith("group:") ? 2 : 0;
//                p2Score += p2.getRootOwner().startsWith("group:") ? 2 : 0;
//                p1Score += AccessManager.getSubjectKey().equals(p1.getRootOwner()) ? 1 : 0;
//                p2Score += AccessManager.getSubjectKey().equals(p2.getRootOwner()) ? 1 : 0;
//                EntityData e1 = p1.getPath().get(0);
//                EntityData e2 = p2.getPath().get(0);
//                int c = p2Score.compareTo(p1Score);
//                if (c == 0) {
//                    return e2.getId().compareTo(e1.getId());
//                }
//                return c;
//            }
//
//        });
//    }
//
    public JMenuItem getPasteAnnotationItem() {
        if (null == StateMgr.getStateMgr().getCurrentSelectedOntologyAnnotation()) {
            return null;
        }
        NamedAction action = new PasteAnnotationTermAction(domainObjectList);
        JMenuItem pasteItem = getNamedActionItem(action);
        return pasteItem;
    }

//    /** Makes the item for showing the entity in its own viewer iff the entity type is correct. */
//    public Collection<JComponent> getOpenForContextItems() {
//        TreeMap<Integer,JComponent> orderedMap = new TreeMap<>();
//        if (rootedEntity != null && rootedEntity.getEntityData() != null) {
//            final Entity entity = rootedEntity.getEntity();
//            if (entity!=null) {
//
//                final ServiceAcceptorHelper helper = new ServiceAcceptorHelper();
//                Collection<EntityAcceptor> entityAcceptors
//                        = helper.findHandler(
//                                entity, 
//                                EntityAcceptor.class,
//                                EntityAcceptor.PERSPECTIVE_CHANGE_LOOKUP_PATH
//                        );
//                boolean lastItemWasSeparator = false;
//                int expectedCount = 0;
//                List<JComponent> actionItemList = new ArrayList<>();
//                for ( EntityAcceptor entityAcceptor: entityAcceptors ) {                    
//                    final Integer order = entityAcceptor.getOrder();
//                    if (entityAcceptor.isPrecededBySeparator() && (! lastItemWasSeparator)) {
//                        orderedMap.put(order - 1, new JSeparator());
//                        expectedCount ++;
//                    }
//                    JMenuItem item = new JMenuItem(entityAcceptor.getActionLabel());
//                    item.addActionListener( new EntityAcceptorActionListener( entityAcceptor ) );
//                    orderedMap.put(order, item);
//                    actionItemList.add( item ); // Bail alternative if ordering fails.
//                    expectedCount ++;
//                    if (entityAcceptor.isSucceededBySeparator()) {
//                        orderedMap.put(order + 1, new JSeparator());
//                        expectedCount ++;
//                        lastItemWasSeparator = true;
//                    }
//                    else {
//                        lastItemWasSeparator = false;
//                    }
//                }
//                
//                // This is the bail strategy for order key clashes.
//                if ( orderedMap.size() < expectedCount) {
//                    log.warn("With menu items and separators, expected {} but added {} open-for-context items." +
//                            "  This indicates an order key clash.  Please check the getOrder methods of all impls." +
//                            "  Returning an unordered version of item list.",
//                            expectedCount, orderedMap.size());
//                    return actionItemList;
//                }
//            }
//        }
//        return orderedMap.values();
//    }
//    
//    public JMenuItem getWrapEntityItem() {
//        if (multiple) return null;
//        return new WrapperCreatorItemFactory().makeEntityWrapperCreatorItem( rootedEntity );
//    }
//       
//    private class EntityDataPath {
//        private List<EntityData> path;
//        private String rootOwner;
//        private boolean isHidden = false;
//
//        public EntityDataPath(List<EntityData> path) {
//            this.path = path;
//            EntityData first = path.get(0);
//            this.rootOwner = first.getParentEntity().getOwnerKey();
//            for (EntityData ed : path) {
//                if (EntityUtils.isHidden(ed))
//                    this.isHidden = true;
//            }
//        }
//
//        public String getUniqueId() {
//            return EntityUtils.getUniqueIdFromParentEntityPath(path);
//        }
//
//        @Override
//        public String toString() {
//            StringBuilder sb = new StringBuilder();
//            for (EntityData pathEd : path) {
//                if (sb.length() <= 0) {
//                    sb.append(" / ").append(pathEd.getParentEntity().getName());
//                }
//                sb.append(" / ").append(pathEd.getChildEntity().getName());
//            }
//            return sb.toString();
//        }
//
//        public List<EntityData> getPath() {
//            return path;
//        }
//
//        public String getRootOwner() {
//            return rootOwner;
//        }
//
//        public boolean isHidden() {
//            return isHidden;
//        }
//    }
//
//    protected JMenuItem getGotoRelatedItem() {
//        if (multiple) return null;
//        
//        JMenu relatedMenu = new JMenu("  Go To Related");
//        final Entity entity = rootedEntity.getEntity();
//        String type = entity.getEntityTypeName();
//        
//        if (EntityConstants.TYPE_NEURON_FRAGMENT.equals(type)
//                || EntityConstants.TYPE_LSM_STACK.equals(type)
//                // TODO: this should be more specific, but right now we're only using virtual entities to represent LSMs
//                || EntityConstants.IN_MEMORY_TYPE_VIRTUAL_ENTITY.equals(type) 
//                || EntityConstants.TYPE_PIPELINE_RUN.equals(type) 
//                || EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT.equals(type) 
//                || EntityConstants.TYPE_ALIGNMENT_RESULT.equals(type) 
//                || EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT.equals(type)
//                || EntityConstants.TYPE_CURATED_NEURON.equals(type)
//                || EntityConstants.TYPE_CURATED_NEURON_COLLECTION.equals(type)
//                || EntityConstants.TYPE_CELL_COUNTING_RESULT.equals(type)) {
//            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_SAMPLE, EntityConstants.TYPE_SAMPLE));
//        }
//        
//        if (EntityConstants.TYPE_NEURON_FRAGMENT.equals(type)
//                || EntityConstants.TYPE_CURATED_NEURON.equals(type)
//                || EntityConstants.TYPE_CURATED_NEURON_COLLECTION.equals(type)) {
//            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT,
//                                EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT));
//        }
//        else if (EntityConstants.TYPE_FLY_LINE.equals(type)) {
//            add(relatedMenu, getChildEntityItem(entity, EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE));
//            add(relatedMenu, getChildEntityItem(entity, EntityConstants.ATTRIBUTE_ORIGINAL_FLYLINE));
//            add(relatedMenu, getChildEntityItem(entity, EntityConstants.ATTRIBUTE_BALANCED_FLYLINE));
//        }
//        else if (EntityConstants.TYPE_ALIGNED_BRAIN_STACK.equals(type)) {
//            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_SCREEN_SAMPLE, EntityConstants.TYPE_SCREEN_SAMPLE));
//            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_FLY_LINE, EntityConstants.TYPE_FLY_LINE));
//        }
//        else if (EntityConstants.TYPE_SCREEN_SAMPLE.equals(type)) {
//            add(relatedMenu, getAncestorEntityItem(entity, EntityConstants.TYPE_FLY_LINE, EntityConstants.TYPE_FLY_LINE));
//        }
//        
//        JMenuItem showAllRefsItem = new JMenuItem("Show All References");
//        showAllRefsItem.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                gotoReference(rootedEntity);
//            }
//        });
//        relatedMenu.add(showAllRefsItem);
//        
//        JMenuItem showAllPathsItem = new JMenuItem("Show All Paths...");
//        showAllPathsItem.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                GoToRelatedEntityAction action = new GoToRelatedEntityAction(entity, null);
//                action.doAction();
//            }
//        });
//        relatedMenu.add(showAllPathsItem);
//        
//        return relatedMenu;
//    }
//    
//    private JMenuItem getChildEntityItem(Entity entity, String attributeName) {
//        final EntityData ed = entity.getEntityDataByAttributeName(attributeName);
//        if (ed == null) return null;
//        return getAncestorEntityItem(ed.getChildEntity(), null, attributeName);
//    }
//
//    private JMenuItem getAncestorEntityItem(final Entity entity, final String ancestorType, final String label) {
//        if (entity == null) return null;
//        
//        JMenuItem relatedMenuItem = new JMenuItem(label);
//        relatedMenuItem.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                gotoEntity(entity, ancestorType);
//            }
//        });
//        return relatedMenuItem;
//    }
//
//    private void gotoReference(final RootedEntity rootedEntity) {
//
//        log.trace("Showing references of {}",rootedEntity.getName());
//        
//        Utils.setWaitingCursor(mainFrame);
//
//        SimpleWorker worker = new SimpleWorker() {
//
//            RootedEntity tempSearchRE;
//
//            @Override
//            protected void doStuff() throws Exception {
//                List<EntityData> parentEds = ModelMgr.getModelMgr().getAllParentEntityDatas(rootedEntity.getEntityId());
//                Entity entity = new Entity();
//                entity.setId(TimebasedIdentifierGenerator.generateIdList(1).get(0));
//                entity.setName("Entities referencing "+rootedEntity.getName());
//                entity.setEntityTypeName(EntityConstants.TYPE_FOLDER);
//
//                HashSet<EntityData> eds = new HashSet<>();
//                for(EntityData parentEd : parentEds) {
//                    EntityData ed = new EntityData();
//                    ed.setEntityAttrName(EntityConstants.ATTRIBUTE_ENTITY);
//                    ed.setParentEntity(entity);
//                    Entity childEntity = ModelMgr.getModelMgr().getEntityById(parentEd.getParentEntity().getId());
//                    ed.setChildEntity(childEntity);
//                    eds.add(ed);
//                }
//
//                log.info("Setting "+eds.size()+" children");
//                entity.setEntityData(eds);
//                entity.setOwnerKey(AccessManager.getSubjectKey());
//                this.tempSearchRE = new RootedEntity(entity);
//            }
//
//            @Override
//            protected void hadSuccess() {
//                WindowLocator.activateAndGet(IconPanelTopComponent.PREFERRED_ID, "editor");
//                browser.getViewerManager().showEntityInActiveViewer(tempSearchRE);
//                Utils.queueDefaultCursor(mainFrame);
//            }
//
//            @Override
//            protected void hadError(Throwable error) {
//                SessionMgr.getSessionMgr().handleException(error);
//                Utils.setDefaultCursor(mainFrame);
//            }
//        };
//
//        worker.execute();
//    }
//    
//    private void gotoEntity(final Entity entity, final String ancestorType) {
//
//        log.trace("Navigating to ancestor {} of entity {}",ancestorType,entity);
//        
//        Utils.setWaitingCursor(mainFrame);
//
//        SimpleWorker worker = new SimpleWorker() {
//
//            private Entity targetEntity = entity;
//            private String uniqueId;
//
//            @Override
//            protected void doStuff() throws Exception {
//                if (ancestorType != null) {
//                    targetEntity = ModelMgr.getModelMgr().getAncestorWithType(entity, ancestorType);
//                }
//
//                final String currUniqueId = rootedEntity.getUniqueId();
//                final String targetId = targetEntity.getId().toString();
//                if (currUniqueId.contains(targetId)) {
//                    // A little optimization, if you're already in the right
//                    // subtree
//                    this.uniqueId = currUniqueId.substring(0, currUniqueId.indexOf(targetId) + targetId.length());
//                }
//                else {
//                    // Find the best context to show the entity in
//                    List<List<EntityData>> edPaths = ModelMgr.getModelMgr().getPathsToRoots(targetEntity.getId());
//                    List<EntityDataPath> paths = new ArrayList<>();
//                    for (List<EntityData> path : edPaths) {
//                        EntityDataPath edp = new EntityDataPath(path);
//                        if (!edp.isHidden()) {
//                            paths.add(edp);
//                        }
//                    }
//                    sortPathsByPreference(paths);
//
//                    if (paths.isEmpty()) {
//                        throw new Exception("Could not find the related entity");
//                    }
//
//                    EntityDataPath chosen = paths.get(0);
//                    this.uniqueId = chosen.getUniqueId();
//                }
//            }
//
//            @Override
//            protected void hadSuccess() {
//                WindowLocator.activateAndGet(IconPanelTopComponent.PREFERRED_ID, "editor");
//                log.info("Selecting {}",uniqueId);
//                browser.getEntityOutline().selectEntityByUniqueId(uniqueId);
//                Utils.queueDefaultCursor(mainFrame);
//            }
//
//            @Override
//            protected void hadError(Throwable error) {
//                Utils.setDefaultCursor(mainFrame);
//                SessionMgr.getSessionMgr().handleException(error);
//            }
//        };
//
//        worker.execute();
//    }
//
//    protected JMenu getErrorFlag() {
//        if (multiple) return null;
//        
//        JMenu errorMenu = new JMenu("  Report A Problem With This Data");
//        addBadDataButtons(errorMenu);
//        return errorMenu;
//    }
//
//    protected JMenuItem getProcessingBlockItem() {
//
//        final List<Entity> samples = new ArrayList<>();
//        for (RootedEntity re : rootedEntityList) {
//            Entity sample = re.getEntity();
//            if (sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
//                if (!sample.getName().contains("~")) {
//                    samples.add(sample);
//                }
//            }
//        }
//        
//        if (samples.isEmpty()) return null;
//        
//        final String samplesText = multiple?samples.size()+" Samples":"Sample";
//        
//        JMenuItem blockItem = new JMenuItem("  Purge And Block "+samplesText+" (Background Task)");
//        blockItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//
//                int result = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want to purge "+samples.size()+" sample(s) "+
//                        "by deleting all large files associated with them, and block all future processing?",  
//                		"Purge And Block Processing", JOptionPane.OK_CANCEL_OPTION);
//                
//                if (result != 0) return;
//
//                Task task;
//                try {
//                    StringBuilder sampleIdBuf = new StringBuilder();
//                    for(Entity sample : samples) {
//                        if (sampleIdBuf.length()>0) sampleIdBuf.append(",");
//                        sampleIdBuf.append(sample.getId());
//                    }
//                    
//                    HashSet<TaskParameter> taskParameters = new HashSet<>();
//                    taskParameters.add(new TaskParameter("sample entity id", sampleIdBuf.toString(), null));
//                    task = ModelMgr.getModelMgr().submitJob("ConsolePurgeAndBlockSample", "Purge And Block Sample", taskParameters);
//                }
//                catch (Exception e) {
//                    SessionMgr.getSessionMgr().handleException(e);
//                    return;
//                }
//
//                TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {
//
//                    @Override
//                    public String getName() {
//                        return "Purging and blocking "+samples.size()+" samples";
//                    }
//
//                    @Override
//                    protected void doStuff() throws Exception {
//                        setStatus("Executing");
//                        super.doStuff();
//                        for(Entity sample : samples) {
//                            ModelMgr.getModelMgr().invalidateCache(sample, true);
//                        }
//                    }
//                };
//
//                taskWorker.executeWithEvents();
//            }
//        });
//
//        for(RootedEntity rootedEntity : rootedEntityList) {
//            Entity sample = rootedEntity.getEntity();
//            if (!ModelMgrUtils.hasWriteAccess(sample) || EntityUtils.isProtected(sample)) {
//                blockItem.setEnabled(false);
//                break;
//            }
//        }
//        
//        return blockItem;
//    }
//
//    protected JMenuItem getMarkForReprocessingItem() {
//
//        final List<Entity> samples = new ArrayList<>();
//        for (RootedEntity rootedEntity : rootedEntityList) {
//            Entity sample = rootedEntity.getEntity();
//            if (sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
//                if (!sample.getName().contains("~")) {
//                    samples.add(sample);
//                }
//            }
//        }
//        
//        if (samples.isEmpty()) return null;
//
//        final String samplesText = multiple?samples.size()+" Samples":"Sample";
//        
//        JMenuItem markItem = new JMenuItem("  Mark "+samplesText+" for Reprocessing");
//        markItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//
//                int result = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want these "+samples.size()+" sample(s) to be reprocessed "
//                        + "during the next scheduled refresh?",  "Mark for Reprocessing", JOptionPane.OK_CANCEL_OPTION);
//                
//                if (result != 0) return;
//
//                SimpleWorker worker = new SimpleWorker() {
//                    
//                    @Override
//                    protected void doStuff() throws Exception {
//                        for(final Entity sample : samples) {
//                            ModelMgr.getModelMgr().setOrUpdateValue(sample, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_MARKED);
//                        }
//                    }
//                    
//                    @Override
//                    protected void hadSuccess() {   
//                    }
//                    
//                    @Override
//                    protected void hadError(Throwable error) {
//                        SessionMgr.getSessionMgr().handleException(error);
//                    }
//                };
//                
//                worker.execute();
//            }
//        });
//
//        for(RootedEntity rootedEntity : rootedEntityList) {
//            Entity sample = rootedEntity.getEntity();
//            if (!ModelMgrUtils.hasWriteAccess(sample) || EntityUtils.isProtected(sample)) {
//                markItem.setEnabled(false);
//                break;
//            }
//        }
//
//        return markItem;
//    }
//    
//    private JMenuItem getVerificationMovieItem() {
//        if (multiple) return null;
//
//        if (!OpenWithDefaultAppAction.isSupported())
//            return null;
//        
//        final Entity sample = rootedEntity.getEntity();
//
//        if (!sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
//            return null;
//        }
//        
//        JMenuItem movieItem = new JMenuItem("  View Alignment Verification Movie");
//        movieItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//
//                SimpleWorker worker = new SimpleWorker() {
//                  
//                    private Entity movie;
//                    
//                    @Override
//                    protected void doStuff() throws Exception {
//                        ModelMgr.getModelMgr().loadLazyEntity(sample, false);
//                        Entity alignedSample = null;
//                        for(Entity child : ModelMgrUtils.getAccessibleChildren(sample)) {
//                            if (child.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE) 
//                                    && child.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE)!=null) {
//                                alignedSample = child;
//                            }
//                        }
//                        
//                        if (alignedSample==null) {
//                            alignedSample = sample;
//                        }
//                        
//                        final ModelMgrEntityLoader loader = new ModelMgrEntityLoader();
//                        EntityVistationBuilder.create(loader).startAt(alignedSample)
//                                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
//                                .childrenOfType(EntityConstants.TYPE_ALIGNMENT_RESULT)
//                                .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA)
//                                .run(new EntityVisitor() {
//                            public void visit(Entity supportingData) throws Exception {
//                                loader.populateChildren(supportingData);
//                                for(Entity child : ModelMgrUtils.getAccessibleChildren(supportingData)) {
//                                    if (child.getName().equals("VerifyMovie.mp4") 
//                                            || child.getName().equals("AlignVerify.mp4")) {
//                                        movie = child;
//                                        break;   
//                                    }
//                                }
//                            }
//                        });
//                    }
//                    
//                    @Override
//                    protected void hadSuccess() {
//
//                        if (movie == null) {
//                            JOptionPane.showMessageDialog(mainFrame, "Could not locate verification movie",
//                                    "Not Found", JOptionPane.ERROR_MESSAGE);
//                            return;
//                        }
//
//                        String filepath = EntityUtils.getAnyFilePath(movie);
//                        
//                        if (StringUtils.isEmpty(filepath)) {
//                            JOptionPane.showMessageDialog(mainFrame, "Verification movie has no path",
//                                    "Not Found", JOptionPane.ERROR_MESSAGE);
//                            return;
//                        }
//                        
//                        OpenWithDefaultAppAction action = new OpenWithDefaultAppAction(movie);
//                        action.doAction();
//                    }
//                    
//                    @Override
//                    protected void hadError(Throwable error) {
//                        SessionMgr.getSessionMgr().handleException(error);
//                    }
//                };
//
//                worker.execute();
//            }
//        });
//
//        if (EntityConstants.VALUE_BLOCKED.equals(sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS))) {
//            movieItem.setEnabled(false);
//        }
//        
//        if (!ModelMgrUtils.hasWriteAccess(sample) || EntityUtils.isProtected(sample)) {
//            movieItem.setEnabled(false);
//        }
//
//        return movieItem;
//    }
//    
//    private static final int MAX_ADD_TO_ROOT_HISTORY = 5;
//    
//    private void updateAddToRootFolderHistory(Entity commonRoot) {
//        List<Long> addHistory = (List<Long>)SessionMgr.getSessionMgr().getModelProperty(Browser.ADD_TO_ROOT_HISTORY);
//        if (addHistory==null) {
//            addHistory = new ArrayList<>();
//        }
//        if (addHistory.contains(commonRoot.getId())) {
//            return;
//        }
//        if (addHistory.size()>=MAX_ADD_TO_ROOT_HISTORY) {
//            addHistory.remove(addHistory.size()-1);
//        }
//        addHistory.add(0, commonRoot.getId());
//        SessionMgr.getSessionMgr().setModelProperty(Browser.ADD_TO_ROOT_HISTORY, addHistory);
//    }
//    
    protected JMenuItem getAddToSetItem() {
        AddItemsToObjectSetAction action = new AddItemsToObjectSetAction(domainObjectList);
        JMenuItem item = action.getPopupPresenter();
        item.setText("  "+item.getText());
        return item;
    }
    
//        final EntityOutline entityOutline = SessionMgr.getBrowser().getEntityOutline();
//                
//        JMenu newFolderMenu = new JMenu("  Add To Folder");
//
//        JMenuItem createNewItem = new JMenuItem("Create New Top-Level Folder...");
//
//        createNewItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//
//                // Add button clicked
//                final String folderName = (String) JOptionPane.showInputDialog(mainFrame, "Folder Name:\n",
//                        "Create top-level folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
//                if ((folderName == null) || (folderName.length() <= 0)) {
//                    return;
//                }
//
//                SimpleWorker worker = new SimpleWorker() {
//                    private Entity commonRoot;
//
//                    @Override
//                    protected void doStuff() throws Exception {
//                        // Update database
//                        commonRoot = ModelMgr.getModelMgr().createCommonRoot(folderName);
//                        addToCommonRoot(commonRoot);
//                    }
//
//                    @Override
//                    protected void hadSuccess() {
//                        // No need to update the UI, the event bus will get it done
//                        log.debug("Added to common root {}",commonRoot.getName());
//                    }
//
//                    @Override
//                    protected void hadError(Throwable error) {
//                        SessionMgr.getSessionMgr().handleException(error);
//                    }
//                };
//                worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Creating folder...", ""));
//                worker.execute();
//            }
//        });
//
//        newFolderMenu.add(createNewItem);
//        
//        JMenuItem chooseItem = new JMenuItem("Choose Folder...");
//        
//        chooseItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//
//                final EntityChooser entityChooser = new EntityChooser("Choose folder to add to", entityOutline);
//                int returnVal = entityChooser.showDialog(entityOutline);
//                if (returnVal != EntityChooser.CHOOSE_OPTION) return;
//                if (entityChooser.getChosenElements().isEmpty()) return;
//                final Entity commonRoot = entityChooser.getChosenElements().get(0).getChildEntity();
//                
//                SimpleWorker worker = new SimpleWorker() {
//                    @Override
//                    protected void doStuff() throws Exception {
//                        addToCommonRoot(commonRoot);
//                    }
//
//                    @Override
//                    protected void hadSuccess() {
//                        // No need to update the UI, the event bus will get it done
//                        log.debug("Added to common root {}",commonRoot.getName());
//                    }
//
//                    @Override
//                    protected void hadError(Throwable error) {
//                        SessionMgr.getSessionMgr().handleException(error);
//                    }
//                };
//                worker.execute();
//            }
//        });
//
//        newFolderMenu.add(chooseItem);
//        newFolderMenu.addSeparator();
//        
//        List<Long> addHistory = (List<Long>)SessionMgr.getSessionMgr().getModelProperty(Browser.ADD_TO_ROOT_HISTORY);
//        if (addHistory!=null && !addHistory.isEmpty()) {
//            
//            JMenuItem item = new JMenuItem("Recent:");
//            item.setEnabled(false);
//            newFolderMenu.add(item);
//            
//            for (Long rootId : addHistory) {
//
//                Set<Entity> entities = entityOutline.getEntitiesById(rootId);
//
//                if (entities.isEmpty()) {
//                    continue;
//                }
//                if (entities.size()>1) {
//                    log.warn("More than one entity in the entity outline for id={}",rootId);
//                }
//
//                final Entity commonRoot = entities.iterator().next();
//                if (!ModelMgrUtils.hasWriteAccess(commonRoot)) continue;
//
//                JMenuItem commonRootItem = new JMenuItem(commonRoot.getName());
//                commonRootItem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent actionEvent) {
//                
//                        SimpleWorker worker = new SimpleWorker() {
//                            @Override
//                            protected void doStuff() throws Exception {
//                                addToCommonRoot(commonRoot);
//                            }
//
//                            @Override
//                            protected void hadSuccess() {
//                                // No need to update the UI, the event bus will get it done
//                                log.debug("Added to common root {}",commonRoot.getName());
//                            }
//
//                            @Override
//                            protected void hadError(Throwable error) {
//                                SessionMgr.getSessionMgr().handleException(error);
//                            }
//                        };
//                        worker.execute();
//                    }
//                });
//
//                newFolderMenu.add(commonRootItem);
//            }
//        }
//        
//        return newFolderMenu;
//    }
//
//    private void addToCommonRoot(Entity commonRoot) throws Exception {
//
//        // Update database
//        List<Long> ids = new ArrayList<>();
//        for (RootedEntity re : rootedEntityList) {
//            Entity entity = re.getEntity();
//            if (!EntityConstants.IN_MEMORY_TYPE_PLACEHOLDER_ENTITY.equals(entity.getEntityTypeName()) && entity.getId()!=null) {
//                ids.add(entity.getId());
//            }
//        }
//        
//        ModelMgr.getModelMgr().addChildren(commonRoot.getId(), ids, EntityConstants.ATTRIBUTE_ENTITY);
//        // Update history
//        updateAddToRootFolderHistory(commonRoot);                
//    }
//    
    protected JMenuItem getRemoveFromSetItem() {
        
        NamedAction action = null;
        if (contextObject instanceof ObjectSet) {
            action = new RemoveItemsFromObjectSetAction((ObjectSet)contextObject, domainObjectList);
        }
        else {
            return null;
        }
        
        JMenuItem deleteItem = getNamedActionItem(action);

        // User can't delete if they don't have write access
        if (!ClientDomainUtils.hasWriteAccess(contextObject)) {
            deleteItem.setEnabled(false);
        }
        
        return deleteItem;
    }
//    
//    
//    protected JMenuItem getMergeItem() {
//
//        // If multiple items are not selected then leave
//        if (!multiple) {
//            return null;
//        }
//
//        HashSet<Long> parentIds = new HashSet<>();
//        for (RootedEntity rootedEntity : rootedEntityList) {
//            // Add all parent ids to a collection
//            if (null != rootedEntity.getEntityData().getParentEntity()
//                    && EntityConstants.TYPE_NEURON_FRAGMENT.equals(rootedEntity.getEntity().getEntityTypeName())) {
//                parentIds.add(rootedEntity.getEntityData().getParentEntity().getId());
//            }
//            // if one of the selected entities has no parent or isn't owner by
//            // the user then leave; cannot merge
//            else {
//                return null;
//            }
//        }
//        // Anything but one parent id for selected entities should not allow
//        // merge
//        if (parentIds.size() != 1) {
//            return null;
//        }
//
//        JMenuItem mergeItem = new JMenuItem("  Merge " + rootedEntityList.size() + " Selected Neurons");
//
//        mergeItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//                SimpleWorker mergeTask = new SimpleWorker() {
//                    @Override
//                    protected void doStuff() throws Exception {
//                        setProgress(1);
//                        Long parentId = null;
//                        List<Entity> fragments = new ArrayList<>();
//                        for (RootedEntity entity : rootedEntityList) {
//                            Long resultId = ModelMgr
//                                    .getModelMgr()
//                                    .getAncestorWithType(entity.getEntity(),
//                                            EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT).getId();
//                            if (parentId == null) {
//                                parentId = resultId;
//                            } else if (resultId == null || !parentId.equals(resultId)) {
//                                throw new IllegalStateException(
//                                        "The selected neuron fragments are not part of the same neuron separation result: parentId="
//                                                + parentId + " resultId=" + resultId);
//                            }
//                            fragments.add(entity.getEntityData().getChildEntity());
//                        }
//
//                        Collections.sort(fragments, new Comparator<Entity>() {
//                            @Override
//                            public int compare(Entity o1, Entity o2) {
//                                Integer o1n = Integer.parseInt(o1
//                                        .getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER));
//                                Integer o2n = Integer.parseInt(o2
//                                        .getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER));
//                                return o1n.compareTo(o2n);
//                            }
//                        });
//
//                        HashSet<String> fragmentIds = new LinkedHashSet<>();
//                        for (Entity fragment : fragments) {
//                            fragmentIds.add(fragment.getId().toString());
//                        }
//
//                        // This should never happen
//                        if (null == parentId) {
//                            return;
//                        }
//                        
//                        HashSet<TaskParameter> taskParameters = new HashSet<>();
//                        taskParameters.add(new TaskParameter(NeuronMergeTask.PARAM_separationEntityId, parentId.toString(), null));
//                        taskParameters.add(new TaskParameter(NeuronMergeTask.PARAM_commaSeparatedNeuronFragmentList, Task.csvStringFromCollection(fragmentIds), null));
//                        ModelMgr.getModelMgr().submitJob("NeuronMerge", "Neuron Merge Task", taskParameters);
//                    }
//
//                    @Override
//                    protected void hadSuccess() {
//                    }
//
//                    @Override
//                    protected void hadError(Throwable error) {
//                        SessionMgr.getSessionMgr().handleException(error);
//                    }
//
//                };
//
//                mergeTask.execute();
//            }
//        });
//
//        mergeItem.setEnabled(multiple);
//        return mergeItem;
//    }
//
//    protected JMenuItem getSortBySimilarityItem() {
//
//        // If multiple items are selected then leave
//        if (multiple) {
//            return null;
//        }
//
//        final Entity targetEntity = rootedEntity.getEntity();
//
//        if (!targetEntity.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)
//                && !targetEntity.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
//            return null;
//        }
//
//        String parentId = Utils.getParentIdFromUniqueId(rootedEntity.getUniqueId());
//        final Entity folder = browser.getEntityOutline().getEntityByUniqueId(parentId);
//
//        if (!folder.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
//            return null;
//        }
//
//        JMenuItem sortItem = new JMenuItem("  Sort Folder By Similarity To This Image");
//
//        sortItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//                try {
//                    HashSet<TaskParameter> taskParameters = new HashSet<>();
//                    taskParameters.add(new TaskParameter("folder id", folder.getId().toString(), null));
//                    taskParameters.add(new TaskParameter("target stack id", targetEntity.getId().toString(), null));
//                    Task task = ModelMgr.getModelMgr().submitJob("SortBySimilarity", "Sort By Similarity", taskParameters);
//
//                    final TaskDetailsDialog dialog = new TaskDetailsDialog(true);
//                    dialog.showForTask(task);
//                    browser.getViewerManager().getActiveViewer().refresh();
//                } 
//                catch (Exception e) {
//                    SessionMgr.getSessionMgr().handleException(e);
//                }
//            }
//        });
//
//        sortItem.setEnabled(ModelMgrUtils.hasWriteAccess(folder));
//        return sortItem;
//    }
//    
//    protected JMenuItem getImportItem() {
//        if (multiple) return null;
//        
//        String entityTypeName = rootedEntity.getEntity().getEntityTypeName();
//        if (EntityConstants.TYPE_FOLDER.equals(entityTypeName) || EntityConstants.TYPE_SAMPLE.equals(entityTypeName)) {
//            JMenuItem newAttachmentItem = new JMenuItem("  Import File(s) Here");
//            newAttachmentItem.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent actionEvent) {
//                    try {
//                        browser.getImportDialog().showDialog(rootedEntity);
//                    } catch (Exception ex) {
//                        SessionMgr.getSessionMgr().handleException(ex);
//                    }
//                }
//            });
//
//            return newAttachmentItem;
//        }
//        return null;
//    }
//
//    protected JMenuItem getActionItem(final Action action) {
//        JMenuItem actionMenuItem = new JMenuItem(action.getName());
//        actionMenuItem.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                action.doAction();
//            }
//        });
//        return actionMenuItem;
//    }
//
//    private JMenuItem getSpecialAnnotationSession() {
//        JMenuItem specialAnnotationSession = new JMenuItem("  Special Annotation");
//        specialAnnotationSession.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if (null == ModelMgr.getModelMgr().getCurrentOntology()) {
//                    JOptionPane.showMessageDialog(mainFrame,
//                            "Please select an ontology in the ontology window.", "Null Ontology Warning",
//                            JOptionPane.WARNING_MESSAGE);
//                } else {
//                    if (!SpecialAnnotationChooserDialog.getDialog().isVisible()) {
//                        SpecialAnnotationChooserDialog.getDialog().setVisible(true);
//                    } else {
//                        SpecialAnnotationChooserDialog.getDialog().transferFocus();
//                    }
//                }
//            }
//        });
//
//        return specialAnnotationSession;
//    }
//    
//    public class EntityAcceptorActionListener implements ActionListener {
//
//        private EntityAcceptor entityAcceptor;
//
//        public EntityAcceptorActionListener(EntityAcceptor entityAcceptor) {
//            this.entityAcceptor = entityAcceptor;
//        }
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            try {
//                Entity entity = rootedEntity.getEntity();
//                // Pickup the sought value.
//                entityAcceptor.acceptEntity(entity);
//            } catch (Exception ex) {
//                ModelMgr.getModelMgr().handleException(ex);
//            }
//
//        }
//    }

}
