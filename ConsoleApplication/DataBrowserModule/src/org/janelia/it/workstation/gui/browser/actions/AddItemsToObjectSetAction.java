package org.janelia.it.workstation.gui.browser.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.gui.support.NodeChooser;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.janelia.it.workstation.gui.browser.nodes.UserViewConfiguration;
import org.janelia.it.workstation.gui.browser.nodes.UserViewObjectSetNode;
import org.janelia.it.workstation.gui.browser.nodes.UserViewRootNode;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Menu system for adding the selected items to an object set.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AddItemsToObjectSetAction implements NamedAction {

    private final static Logger log = LoggerFactory.getLogger(AddItemsToObjectSetAction.class);

    private static final int MAX_ADD_TO_ROOT_HISTORY = 5;
    private final Component mainFrame = SessionMgr.getMainFrame();
    private final Collection<DomainObject> domainObjects;

    public AddItemsToObjectSetAction(Collection<DomainObject> domainObjects) {
        this.domainObjects = domainObjects;
    }

    @Override
    public String getName() {
        return domainObjects.size() > 1 ? "Add \"" + domainObjects.size() + "\" Items To Set" : "Add Item To Set";
    }

    @Override
    public void doAction() {
    }
    
    public JMenuItem getPopupPresenter() {
        
        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        JMenu newSetMenu = new JMenu(getName());
        JMenuItem createNewItem = new JMenuItem("Create New Set...");
        
        createNewItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                // Add button clicked
                final String setName = (String) JOptionPane.showInputDialog(mainFrame, "Set Name:\n",
                        "Create new set in workspace", JOptionPane.PLAIN_MESSAGE, null, null, null);
                if ((setName == null) || (setName.length() <= 0)) {
                    return;
                }

                SimpleWorker worker = new SimpleWorker() {
                    
                    private ObjectSet objectSet;
                    private Long[] idPath;

                    @Override
                    protected void doStuff() throws Exception {
                        objectSet = new ObjectSet();
                        objectSet.setName(setName);
                        objectSet = model.create(objectSet);
                        Workspace workspace = model.getDefaultWorkspace();
                        idPath = NodeUtils.createIdPath(workspace, objectSet);
                        model.addChild(workspace, objectSet);
                        addObjectsToSet(objectSet, idPath);
                    }

                    @Override
                    protected void hadSuccess() {
                        log.debug("Added to set {}",objectSet.getId());
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                explorer.expand(idPath);
                                explorer.select(idPath);
                            }
                        });
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                
                worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Creating set...", ""));
                worker.execute();
            }
        });

        newSetMenu.add(createNewItem);

        JMenuItem chooseItem = new JMenuItem("Choose Set...");

        chooseItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                // TODO: we should only display object sets of a particular type (the type of the selected domain objects)
                NodeChooser nodeChooser = new NodeChooser(new UserViewRootNode(UserViewConfiguration.create(TreeNode.class, ObjectSet.class)), "Choose set to add to");
                nodeChooser.setRootVisible(false);
                
                int returnVal = nodeChooser.showDialog(explorer);
                if (returnVal != NodeChooser.CHOOSE_OPTION) return;
                if (nodeChooser.getChosenElements().isEmpty()) return;
                final UserViewObjectSetNode selectedNode = (UserViewObjectSetNode)nodeChooser.getChosenElements().get(0);
                final ObjectSet objectSet = selectedNode.getObjectSet();
                SimpleWorker worker = new SimpleWorker() {

                    private Long[] idPath;
                
                    @Override
                    protected void doStuff() throws Exception {
                        idPath = NodeUtils.createIdPath(selectedNode);
                        addObjectsToSet(objectSet, idPath);
                    }

                    @Override
                    protected void hadSuccess() {
                        log.info("Added to set {}",objectSet.getId());
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                explorer.expand(idPath);
                                explorer.select(idPath);
                            }
                        });
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });

        newSetMenu.add(chooseItem);
        newSetMenu.addSeparator();

        List<String> addHistory = (List<String>)SessionMgr.getSessionMgr().getModelProperty(Browser.ADD_TO_SET_HISTORY);
        if (addHistory!=null && !addHistory.isEmpty()) {

            // TODO: we should only display object sets of a particular type (the type of the selected domain objects)
            
            JMenuItem item = new JMenuItem("Recent:");
            item.setEnabled(false);
            newSetMenu.add(item);

            for (String path : addHistory) {

                final Long[] idPath = NodeUtils.createIdPath(path);
                final Long setId = idPath[idPath.length-1];
                
                ObjectSet objectSet;
                try {
                    objectSet = model.getDomainObject(ObjectSet.class, setId);
                    if (objectSet == null) continue;
                }
                catch (Exception e) {
                    log.error("Error getting recent set with id "+setId,e);
                    continue;
                }

                final ObjectSet finalSet = objectSet;

                JMenuItem commonRootItem = new JMenuItem(finalSet.getName());
                commonRootItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {

                        SimpleWorker worker = new SimpleWorker() {
                            @Override
                            protected void doStuff() throws Exception {
                                addObjectsToSet(finalSet, idPath);
                            }

                            @Override
                            protected void hadSuccess() {
                                log.info("Added to set {}",finalSet.getId());
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        explorer.expand(idPath);
                                        explorer.select(idPath);
                                    }
                                });
                            }

                            @Override
                            protected void hadError(Throwable error) {
                                SessionMgr.getSessionMgr().handleException(error);
                            }
                        };
                        worker.execute();
                    }
                });

                commonRootItem.setEnabled(ClientDomainUtils.hasWriteAccess(finalSet));
                newSetMenu.add(commonRootItem);
            }
        }

        return newSetMenu;
    }
    
    private void addObjectsToSet(ObjectSet objectSet, Long[] idPath) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        
        // Add them to the given set
        model.addMembers(objectSet, domainObjects);
        
        // Update history
        updateAddToSetHistory(idPath);                
    }
    
    private void updateAddToSetHistory(Long[] idPath) {
        String pathString = NodeUtils.createPathString(idPath);
        List<String> addHistory = (List<String>)SessionMgr.getSessionMgr().getModelProperty(Browser.ADD_TO_SET_HISTORY);
        if (addHistory==null) {
            addHistory = new ArrayList<>();
        }
        if (addHistory.contains(pathString)) {
            return;
        }
        if (addHistory.size()>=MAX_ADD_TO_ROOT_HISTORY) {
            addHistory.remove(addHistory.size()-1);
        }
        addHistory.add(0, pathString);
        SessionMgr.getSessionMgr().setModelProperty(Browser.ADD_TO_SET_HISTORY, addHistory);
    }
}
