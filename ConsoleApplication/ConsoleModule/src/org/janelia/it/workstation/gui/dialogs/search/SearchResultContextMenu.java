package org.janelia.it.workstation.gui.dialogs.search;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.concurrent.Future;

import javax.swing.JMenuItem;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.workstation.gui.dialogs.choose.EntityChooser;
import org.janelia.it.workstation.gui.dialogs.choose.MultiTreeEntityChooser;
import org.janelia.it.workstation.gui.framework.context_menu.AbstractContextMenu;
import org.janelia.it.workstation.gui.framework.outline.EntityTree;
import org.janelia.it.workstation.gui.framework.outline.EntityTreeCellRenderer;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tree.ExpansionState;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.shared.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context menu for general search results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchResultContextMenu extends AbstractContextMenu<Entity> {

    private static final Logger log = LoggerFactory.getLogger(SearchResultContextMenu.class);

    private SearchResultsPanel searchResultsPanel;

    public SearchResultContextMenu(SearchResultsPanel searchResultsPanel, List<Entity> selectedEntities, String label) {
        super(selectedEntities, label);
        this.searchResultsPanel = searchResultsPanel;
    }

    @Override
    protected void addSingleSelectionItems() {
        Entity entity = getSelectedElement();
        add(getTitleItem("Entity '" + entity.getName() + "'"));
        add(getDetailsItem());
        setNextAddRequiresSeparator(true);
        add(getRelativesItem());
    }

    @Override
    protected void addMultipleSelectionItems() {
        // None
    }

    protected JMenuItem getDetailsItem() {
        final Entity entity = getSelectedElement();
        JMenuItem detailsMenuItem = new JMenuItem("  View Details");
        detailsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new EntityDetailsDialog().showForEntity(entity);
            }
        });
        return detailsMenuItem;
    }

    protected JMenuItem getRelativesItem() {
        final Entity entity = getSelectedElement();
        JMenuItem detailsMenuItem = new JMenuItem("  Map All Results To Related Entities...");
        detailsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Utils.setWaitingCursor(searchResultsPanel);

                SimpleWorker worker = new SimpleWorker() {

                    List<List<Object>> paths = new ArrayList<List<Object>>();
                    MultiTreeEntityChooser entityChooser;

                    @Override
                    protected void doStuff() throws Exception {
                        List<List<Object>> allPaths = getRootPaths(entity, new HashSet<Long>());
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

                        final List<EntityTree> trees = new ArrayList<EntityTree>();
                        final Map<EntityTree, String> startingPaths = new HashMap<EntityTree, String>();
                        final Set<Future<Boolean>> futures = new HashSet<Future<Boolean>>();

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
                            log.debug("Wil select " + selected);
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
                                Utils.setDefaultCursor(searchResultsPanel);

                                entityChooser = new MultiTreeEntityChooser("Select relative", trees);

                                int returnVal = entityChooser.showDialog(null);

                                // Dialog has closed, so we need to clean up subscriptions
                                for (EntityTree entityTree : trees) {
                                    entityTree.deactivate();
                                }

                                if (returnVal != EntityChooser.CHOOSE_OPTION) {
                                    return;
                                }

                                if (entityChooser.getUniqueIds().isEmpty()) {
                                    hadError(new Exception("No selection was made."));
                                    return;
                                }

                                String uniqueId = entityChooser.getUniqueIds().get(0);
                                EntityTree selectedTree = entityChooser.getSelectedTree();
                                String startingPath = startingPaths.get(selectedTree);
                                if (startingPath.equals(uniqueId)) {
                                    hadError(new Exception("Cannot map entity to itself."));
                                }
                                else {
                                    ResultTreeMapping projection = new ResultTreeMapping(selectedTree, startingPath, uniqueId);
                                    searchResultsPanel.projectResults(projection);
                                }
                            }

                            @Override
                            protected void hadError(Throwable error) {
                                Utils.setDefaultCursor(searchResultsPanel);
                                SessionMgr.getSessionMgr().handleException(error);
                            }
                        };

                        worker2.execute();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        Utils.setDefaultCursor(searchResultsPanel);
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                worker.execute();
            }
        });
        return detailsMenuItem;
    }

    private List<List<Object>> getRootPaths(Entity entity, Set<Long> visited) throws Exception {

        List<List<Object>> rootPaths = new ArrayList<List<Object>>();
        if (!EntityConstants.TYPE_WORKSPACE.equals(entity.getEntityTypeName()) && visited.contains(entity.getId())) {
            return rootPaths;
        }
        visited.add(entity.getId());

        List<EntityData> parents = ModelMgr.getModelMgr().getParentEntityDatas(entity.getId());

        if (parents.isEmpty()) {
            List<Object> path = new ArrayList<Object>();
            path.add(entity);
            rootPaths.add(path);
            return rootPaths;
        }

        for (EntityData parentEd : parents) {
            Entity parent = parentEd.getParentEntity();

            if (EntityUtils.isHidden(parentEd)) {
                // Skip this path, because it should be hidden from the user
                log.debug("Skipping path becuase it's hidden: " + parent.getName() + "-(" + parentEd.getEntityAttrName() + ")->" + entity.getName());
                continue;
            }

            parent = ModelMgr.getModelMgr().getEntityById(parent.getId());
            List<List<Object>> parentPaths = getRootPaths(parent, visited);
            for (List<Object> parentPath : parentPaths) {
                parentPath.add(parentEd);
                parentPath.add(entity);
                rootPaths.add(parentPath);
            }
        }

        return rootPaths;
    }
}
