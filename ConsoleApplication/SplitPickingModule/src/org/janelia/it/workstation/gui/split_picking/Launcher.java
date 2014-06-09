package org.janelia.it.workstation.gui.split_picking;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.janelia.it.workstation.nb_action.EntityAcceptor;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.Group;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 * Launches the Data Viewer from a context-menu.
 */
@ServiceProvider(service = EntityAcceptor.class, path=EntityAcceptor.PERSPECTIVE_CHANGE_LOOKUP_PATH)
public class Launcher implements EntityAcceptor  {
    
    public Launcher() {
    }

    public void launch( long entityId ) {
        
        SplitPickingTopComponent win = (SplitPickingTopComponent)WindowManager.getDefault().findTopComponent("SplitPickingTopComponent");
        if ( win != null ) {
            if ( ! win.isOpened() ) {
                win.open();
            }
            if (win.isOpened()) {
                win.requestActive();
            }
            
            
            
        }
    }

    private void addToSplitFolder(Entity commonRoot, RootedEntity re) throws Exception {

        final List<Long> ads = new ArrayList<Long>();
        final List<Long> dbds = new ArrayList<Long>();

        String splitPart = re.getEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
        if ("AD".equals(splitPart)) {
            ads.add(re.getEntityId());
        } else if ("DBD".equals(splitPart)) {
            dbds.add(re.getEntityId());
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

    // TODO: this used to be part of the EntityContextMenu, where making submenus is easy. With this new Acceptor pattern, it's not clear how to accept 
    //multiple entities, or show submenus, so this feature will be disabled until we figure that out.
            
//    protected JMenu getAddToSplitPickingSessionItem() {
//
//        JMenu newFolderMenu = new JMenu("  Add To Screen Picking Folder");
//
//        List<EntityData> rootEds = ModelMgrUtils.getAccessibleEntityDatasWithChildren(SessionMgr.getBrowser().getEntityOutline().getRootEntity());
//
//        for (final EntityData rootEd : rootEds) {
//            final Entity commonRoot = rootEd.getChildEntity();
//            if (!ModelMgrUtils.hasWriteAccess(commonRoot))
//                continue;
//
//            JMenuItem commonRootItem = new JMenuItem(commonRoot.getName());
//            commonRootItem.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent actionEvent) {
//                    SimpleWorker worker = new SimpleWorker() {
//                        @Override
//                        protected void doStuff() throws Exception {
//                            addToSplitFolder(commonRoot);
//                        }
//
//                        @Override
//                        protected void hadSuccess() {
//                            // No need to update the UI, the event bus will get it done
//                        }
//
//                        @Override
//                        protected void hadError(Throwable error) {
//                            SessionMgr.getSessionMgr().handleException(error);
//                        }
//                    };
//                    worker.execute();
//                }
//            });
//
//            newFolderMenu.add(commonRootItem);
//        }
//
//        newFolderMenu.addSeparator();
//
//        JMenuItem createNewItem = new JMenuItem("Create New...");
//
//        createNewItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//
//                // Add button clicked
//                final String folderName = (String) JOptionPane.showInputDialog(mainFrame, "Folder Name:\n",
//                        "Create Split Picking Folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
//                if ((folderName == null) || (folderName.length() <= 0)) {
//                    return;
//                }
//
//                SimpleWorker worker = new SimpleWorker() {
//                    private Entity newFolder;
//
//                    @Override
//                    protected void doStuff() throws Exception {
//                        // Update database
//                        newFolder = ModelMgr.getModelMgr().createCommonRoot(folderName);
//                        addToSplitFolder(newFolder);
//                    }
//
//                    @Override
//                    protected void hadSuccess() {
//                        // No need to update the UI, the event bus will get it done
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
//        newFolderMenu.add(createNewItem);
//
//        return newFolderMenu;
//    }
    
    @Override
    public void acceptEntity(Entity e) {
        launch(e.getId());
    }

    @Override
    public String getActionLabel() {
        return "  Add To Screen Picking Folder";
    }

    @Override
    public boolean isCompatible(Entity e) {
        // TODO: this is disabled for now
        return false;
    }

    @Override
    public Integer getOrder() {
        return 100;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return false;
    }

    @Override
    public boolean isSucceededBySeparator() {
        return true;
    }
}
