package org.janelia.it.workstation.gui.framework.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.dialogs.choose.EntityChooser;
import org.janelia.it.workstation.gui.dialogs.choose.MultiTreeEntityChooser;
import org.janelia.it.workstation.gui.dialogs.search.SearchResultContextMenu;
import org.janelia.it.workstation.gui.framework.outline.EntityTree;
import org.janelia.it.workstation.gui.framework.outline.EntityTreeCellRenderer;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tree.ExpansionState;
import org.janelia.it.workstation.gui.top_component.IconPanelTopComponent;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Present the user with a dialog to select a related entity, and then navigate to it.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GoToRelatedEntityAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(SearchResultContextMenu.class);
    
    private final Entity entity;
    private final Callable<Void> doSuccess;
    
    public GoToRelatedEntityAction(Entity entity, Callable<Void> doSuccess) {
        this.entity = entity;
        this.doSuccess = doSuccess;
    }
    
    @Override
    public String getName() {
        return "Go to Related";
    }

    @Override
    public void doAction() {

        Utils.setWaitingCursor(SessionMgr.getMainFrame());
                
        SimpleWorker worker = new SimpleWorker() {

            List<List<Object>> paths = new ArrayList<>();
            MultiTreeEntityChooser entityChooser;

            @Override
            protected void doStuff() throws Exception {
                List<List<Object>> allPaths = ModelMgr.getModelMgr().getRootPaths(entity, new HashSet<Long>());
                for (List<Object> path : allPaths) {
                    if (!path.isEmpty()) {
                        paths.add(path);
                    }
                }
                if (paths.isEmpty()) {
                    throw new Exception("No access to a root for this entity");
                }
                Collections.sort(paths, new Comparator<List<Object>>() {
                    @Override
                    public int compare(List<Object> o1, List<Object> o2) {
                        Entity root1 = (Entity) o1.get(0);
                        Entity root2 = (Entity) o2.get(0);
                        // Order by id to get oldest entities first
                        return root1.getId().compareTo(root2.getId());
                    }
                });
            }

            @Override
            protected void hadSuccess() {

                final List<EntityTree> trees = new ArrayList<>();
                final Map<EntityTree, String> startingPaths = new HashMap<>();
                final Set<Future<Boolean>> futures = new HashSet<>();

                PATH_LOOP:
                for (List<Object> path : paths) {
                    ExpansionState expansion = new ExpansionState();
                    expansion.addExpandedUniqueId("/");

                    StringBuilder sb = new StringBuilder();
                    for (Object p : path) {
                        sb.append("/");
                        if (p instanceof Entity) {
                            sb.append("e_");
                            sb.append(((Entity) p).getId());
                        }
                        else {
                            EntityData ed = (EntityData) p;
                            sb.append("ed_");
                            sb.append(ed.getId());
                        }

                        expansion.addExpandedUniqueId(sb.toString());
                    }

                    String selected = sb.toString();
                    log.debug("Will select " + selected);
                    expansion.setSelectedUniqueId(selected);

                    EntityTree tree = new EntityTree();
                    tree.initializeTree((Entity) path.get(0));
                    tree.getDynamicTree().setCellRenderer(new EntityTreeCellRenderer() {
                        @Override
                        protected boolean isHighlighted(Entity entity2) {
                            return entity2.getId().equals(entity.getId());
                        }

                    });
                    futures.add(expansion.restoreExpansionState(tree.getDynamicTree(), true));
                    tree.activate();
                    trees.add(tree);
                    startingPaths.put(tree, selected);
                }

                if (trees.isEmpty()) {
                    hadError(new Exception("Could not find any rooted paths"));
                }

                log.debug("Waiting for trees to expand");

                SimpleWorker worker2 = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        for (Future<Boolean> future : futures) {
                            try {
                                future.get();
                            }
                            catch (Exception e) {
                                log.error("Exception encountered while waiting for tree to expand", e);
                            }
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        log.debug("All trees expanded");
                        Utils.setDefaultCursor(SessionMgr.getMainFrame());

                        entityChooser = new MultiTreeEntityChooser("Select Tree", trees);

                        int returnVal = entityChooser.showDialog(null);

                        // Dialog has closed, so we need to clean up subscriptions
                        for (EntityTree entityTree : trees) {
                            entityTree.deactivate();
                        }

                        if (returnVal != EntityChooser.CHOOSE_OPTION) {
                            return;
                        }

                        List<String> selectedUniqueIds = entityChooser.getUniqueIds();
                        String selectedUniqueId = null;
                        if (selectedUniqueIds.isEmpty()) {
                            EntityTree selectedTree = entityChooser.getSelectedTree();
                            selectedUniqueId = startingPaths.get(selectedTree);
                        }
                        else {
                            if (selectedUniqueIds.size()>1) {
                                log.warn("More than one path was selected: "+selectedUniqueIds);
                            }
                            selectedUniqueId = selectedUniqueIds.get(0);
                        }
                        
                        WindowLocator.activateAndGet(IconPanelTopComponent.PREFERRED_ID, "editor");
                        SessionMgr.getBrowser().getEntityOutline().selectEntityByUniqueId(selectedUniqueId);
                        ConcurrentUtils.invokeAndHandleExceptions(doSuccess);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        Utils.setDefaultCursor(SessionMgr.getMainFrame());
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                worker2.execute();
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }

}
