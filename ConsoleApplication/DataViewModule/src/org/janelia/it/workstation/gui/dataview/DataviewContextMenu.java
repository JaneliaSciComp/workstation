package org.janelia.it.workstation.gui.dataview;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.workstation.gui.framework.actions.OpenInFinderAction;
import org.janelia.it.workstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.workstation.gui.framework.context_menu.AbstractContextMenu;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Context pop up menu for entities in the data viewer.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataviewContextMenu extends AbstractContextMenu<Entity> {

    private static final Logger log = LoggerFactory.getLogger(DataviewContextMenu.class);

    protected static final JFrame mainFrame = SessionMgr.getMainFrame();

    public DataviewContextMenu(List<Entity> selectedEntities, String label) {
        super(selectedEntities, label);
    }

    @Override
    protected void addSingleSelectionItems() {
        Entity entity = getSelectedElement();
        add(getTitleItem("Entity '" + entity.getName() + "'"));
        add(getDetailsItem());
        add(getRenameItem());
        add(getDeleteTreeItem());
        add(getUnlinkAndDeleteTreeItem());
        setNextAddRequiresSeparator(true);
        add(getOpenInFinderItem());
        add(getOpenWithAppItem());
    }

    @Override
    protected void addMultipleSelectionItems() {
        add(getDeleteTreeItem());
        add(getUnlinkAndDeleteTreeItem());
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

    protected JMenuItem getRenameItem() {
        final Entity entity = getSelectedElement();
        JMenuItem renameItem = new JMenuItem("  Rename");
        renameItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                String newName = (String) JOptionPane.showInputDialog(mainFrame, "Name:\n", "Rename " + entity.getName(), JOptionPane.PLAIN_MESSAGE, null, null, entity.getName());
                if ((newName == null) || (newName.length() <= 0)) {
                    return;
                }

                try {
                    ModelMgr.getModelMgr().renameEntity(entity, newName);
                }
                catch (Exception error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }

            }
        });
        return renameItem;
    }

    private JMenuItem getDeleteTreeItem() {

        String name = isMultipleSelection() ? "Delete entity trees" : "Delete entity tree";

        JMenuItem deleteTreeMenuItem = new JMenuItem("  " + name);
        deleteTreeMenuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int deleteConfirmation = confirm("Are you sure you want to delete " + getSelectedElements().size() + " entities and all their descendants?");
                if (deleteConfirmation != 0) {
                    return;
                }

                deleteSelectedTrees(false);
            }
        });

        return deleteTreeMenuItem;
    }

    private JMenuItem getUnlinkAndDeleteTreeItem() {

        String name = isMultipleSelection() ? "Unlink and delete entity trees" : "Unlink and delete entity tree";

        JMenuItem deleteTreeMenuItem = new JMenuItem("  " + name);
        deleteTreeMenuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int deleteConfirmation = confirm("Are you sure you want to unlink and delete " + getSelectedElements().size() + " entities and all their descendants?");
                if (deleteConfirmation != 0) {
                    return;
                }

                deleteSelectedTrees(true);
            }
        });

        return deleteTreeMenuItem;
    }

    private void deleteSelectedTrees(final boolean unlink) {

        final List<Entity> toDelete = new ArrayList<Entity>();
        final List<Boolean> needSuArray = new ArrayList<Boolean>();

        for (Entity entity : getSelectedElements()) {
            if (SessionMgr.getSubjectKey() == null || !SessionMgr.getSubjectKey().equals(entity.getOwnerKey())) {
                int overrideConfirmation = confirm("Override owner " + entity.getOwnerKey() + " to delete " + entity.getName() + "?");
                if (overrideConfirmation == 0) {
                    needSuArray.add(true);
                    toDelete.add(entity);
                }
            }
            else {
                needSuArray.add(false);
                toDelete.add(entity);
            }
        }

        Utils.setWaitingCursor(SessionMgr.getBrowser().getMainComponent());

        SimpleWorker deleteTask = new SimpleWorker() {

            private List<Long> numAnnotated = new ArrayList<Long>();
            private List<Entity> toDeleteForReal = new ArrayList<Entity>();
            final List<Boolean> needSuForReal = new ArrayList<Boolean>();

            @Override
            protected void doStuff() throws Exception {
                // Get number of annotations for each deletion candidate
                for (Entity entity : toDelete) {
                    numAnnotated.add(ModelMgr.getModelMgr().getNumDescendantsAnnotated(entity.getId()));
                }
            }

            @Override
            protected void hadSuccess() {

                for (int i = 0; i < toDelete.size(); i++) {
                    Entity entity = toDelete.get(i);
                    Long annots = numAnnotated.get(i);
                    Boolean needSu = needSuArray.get(i);
                    log.info("Entity {} has {} descendant annotations", entity, annots);
                    if (annots != null && annots > 0) {
                        int overrideConfirmation = confirm(entity.getName() + " has annotated descandants. Delete anyway?");
                        if (overrideConfirmation == 0) {
                            toDeleteForReal.add(entity);
                            needSuForReal.add(needSu);
                        }
                    }
                    else {
                        toDeleteForReal.add(entity);
                        needSuForReal.add(needSu);
                    }
                }

                SimpleWorker realDeleteTask = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        // Update database
                        Subject realSubject = SessionMgr.getSessionMgr().getSubject();
                        for (int i = 0; i < toDeleteForReal.size(); i++) {
                            Entity entity = toDeleteForReal.get(i);
                            Boolean needSu = needSuForReal.get(i);
                            if (needSu) {
                                SessionMgr.getSessionMgr().setSubject(ModelMgr.getModelMgr().getSubject(entity.getOwnerKey()));
                            }
                            ModelMgr.getModelMgr().deleteEntityTree(entity.getId(), unlink);
                            if (needSu) {
                                SessionMgr.getSessionMgr().setSubject(realSubject);
                            }
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        Utils.setDefaultCursor(SessionMgr.getBrowser().getMainComponent());
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                realDeleteTask.execute();
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        deleteTask.execute();
    }

    protected JMenuItem getOpenInFinderItem() {
        if (!OpenInFinderAction.isSupported()) {
            return null;
        }
        final Entity entity = getSelectedElement();
        String filepath = EntityUtils.getAnyFilePath(entity);
        JMenuItem menuItem = null;
        if (!StringUtils.isEmpty(filepath)) {
            menuItem = getActionItem(new OpenInFinderAction(entity));
        }
        return menuItem;
    }

    protected JMenuItem getOpenWithAppItem() {
        if (!OpenWithDefaultAppAction.isSupported()) {
            return null;
        }
        final Entity entity = getSelectedElement();
        String filepath = EntityUtils.getAnyFilePath(entity);
        if (!StringUtils.isEmpty(filepath)) {
            return getActionItem(new OpenWithDefaultAppAction(entity));
        }
        return null;
    }

    private int confirm(String message) {
        return JOptionPane.showConfirmDialog(mainFrame, message, "Are you sure?", JOptionPane.YES_NO_OPTION);
    }

}
