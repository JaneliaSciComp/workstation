package org.janelia.it.workstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.janelia.it.workstation.gui.framework.outline.EntityViewerState;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * This viewer displays text. Override the getText method to define where the text comes from.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class TextViewer extends Viewer {

    protected ViewerToolbar toolbar;
    protected JTextArea textArea;
    protected JScrollPane scrollPane;
    protected org.janelia.it.workstation.model.entity.RootedEntity contextRootedEntity;
    protected SessionModelListener sessionModelListener;

    public TextViewer(ViewerPane viewerPane) {
        super(viewerPane);

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);

        toolbar = createToolbar();
        toolbar.addMouseListener(new org.janelia.it.workstation.gui.util.MouseForwarder(this, "JToolBar->ErrorViewer"));
        add(toolbar, BorderLayout.NORTH);

        this.textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(false);

        this.scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        textArea.addMouseListener(new org.janelia.it.workstation.gui.util.MouseForwarder(this, "JTextPane->ErrorViewer"));

        sessionModelListener = new SessionModelListener() {
            @Override
            public void browserAdded(org.janelia.it.workstation.gui.framework.session_mgr.BrowserModel browserModel) {
            }

            @Override
            public void browserRemoved(org.janelia.it.workstation.gui.framework.session_mgr.BrowserModel browserModel) {
            }

            @Override
            public void sessionWillExit() {
            }

            @Override
            public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
                if (key == "console.serverLogin") {
                    TextViewer.this.clear();
                }
            }
        };
        org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().addSessionModelListener(sessionModelListener);
    }

    protected final ViewerToolbar createToolbar() {

        return new ViewerToolbar() {

            @Override
            protected void goBack() {
                final org.janelia.it.workstation.gui.framework.outline.EntitySelectionHistory history = getViewerPane().getEntitySelectionHistory();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        history.goBack(saveViewerState());
                    }
                });
            }

            @Override
            protected void goForward() {
                final org.janelia.it.workstation.gui.framework.outline.EntitySelectionHistory history = getViewerPane().getEntitySelectionHistory();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        history.goForward();
                    }
                });
            }

            @Override
            protected void refresh() {
                TextViewer.this.totalRefresh();
            }

            @Override
            protected JPopupMenu getPopupPathMenu() {
                List<org.janelia.it.workstation.model.entity.RootedEntity> rootedAncestors = getViewerPane().getRootedAncestors();
                if (rootedAncestors == null) {
                    return null;
                }
                final JPopupMenu pathMenu = new JPopupMenu();
                for (final org.janelia.it.workstation.model.entity.RootedEntity ancestor : rootedAncestors) {
                    JMenuItem pathMenuItem = new JMenuItem(ancestor.getEntity().getName(), org.janelia.it.workstation.gui.util.Icons.getIcon(ancestor.getEntity()));
                    pathMenuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel.CATEGORY_OUTLINE, ancestor.getUniqueId(), true);
                        }
                    });
                    pathMenuItem.setEnabled(pathMenu.getComponentCount() > 0);
                    pathMenu.add(pathMenuItem);
                }
                return pathMenu;
            }

        };
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(org.janelia.it.workstation.gui.util.Icons.getLoadingIcon()));
        this.updateUI();
    }

    public void showViewer() {
        removeAll();

        textArea.setCaretPosition(0);

        // Update back/forward navigation
        org.janelia.it.workstation.gui.framework.outline.EntitySelectionHistory history = getViewerPane().getEntitySelectionHistory();
        toolbar.getPrevButton().setEnabled(history.isBackEnabled());
        toolbar.getNextButton().setEnabled(history.isNextEnabled());

        add(toolbar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        this.updateUI();
    }

    @Override
    public void loadEntity(org.janelia.it.workstation.model.entity.RootedEntity rootedEntity) {
        loadEntity(rootedEntity, null);
    }

    @Override
    public synchronized void loadEntity(final org.janelia.it.workstation.model.entity.RootedEntity rootedEntity, final Callable<Void> success) {

        this.contextRootedEntity = rootedEntity;
        if (contextRootedEntity == null) {
            return;
        }

        showLoadingIndicator();

        SimpleWorker worker = new SimpleWorker() {

            String text;

            @Override
            protected void doStuff() throws Exception {
                this.text = getText(rootedEntity);
            }

            @Override
            protected void hadSuccess() {
                textArea.setText(text);
                showViewer();
                ConcurrentUtils.invokeAndHandleExceptions(success);
            }

            @Override
            protected void hadError(Throwable error) {
                org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();

    }

    public abstract String getText(org.janelia.it.workstation.model.entity.RootedEntity rootedEntity) throws Exception;

    @Override
    public void refresh() {
        refresh(null);
    }

    @Override
    public void totalRefresh() {
        // TODO: implement this with invalidate
        refresh();
    }

    public void refresh(final Callable<Void> successCallback) {

        if (contextRootedEntity == null) {
            return;
        }

        showLoadingIndicator();

        SimpleWorker refreshWorker = new SimpleWorker() {

            org.janelia.it.workstation.model.entity.RootedEntity rootedEntity = contextRootedEntity;

            @Override
            protected void doStuff() throws Exception {
                Entity entity = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getEntityById(rootedEntity.getEntity().getId());
                if (entity == null) {
                    return;
                }
                rootedEntity.setEntity(org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(entity, false));
            }

            @Override
            protected void hadSuccess() {
                if (rootedEntity.getEntity() == null) {
                    clear();
                }
                else {
                    loadEntity(rootedEntity);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(error);
            }
        };

        refreshWorker.execute();
    }

    @Override
    public synchronized void clear() {
        this.contextRootedEntity = null;

        getViewerPane().setTitle(" ");
        removeAll();

        revalidate();
        repaint();
    }

    @Override
    public void close() {
        org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().removeSessionModelListener(sessionModelListener);
    }

    @Override
    public List<org.janelia.it.workstation.model.entity.RootedEntity> getRootedEntities() {
        List<org.janelia.it.workstation.model.entity.RootedEntity> rootedEntities = new ArrayList<org.janelia.it.workstation.model.entity.RootedEntity>();
        rootedEntities.add(contextRootedEntity);
        return rootedEntities;
    }

    @Override
    public List<org.janelia.it.workstation.model.entity.RootedEntity> getSelectedEntities() {
        List<org.janelia.it.workstation.model.entity.RootedEntity> rootedEntities = new ArrayList<org.janelia.it.workstation.model.entity.RootedEntity>();
        rootedEntities.add(contextRootedEntity);
        return rootedEntities;
    }

    @Override
    public org.janelia.it.workstation.model.entity.RootedEntity getRootedEntityById(String uniqueId) {
        return contextRootedEntity.getUniqueId().equals(uniqueId) ? contextRootedEntity : null;
    }

    @Override
    public org.janelia.it.workstation.model.entity.RootedEntity getContextRootedEntity() {
        return contextRootedEntity;
    }
    
    @Override
    public EntityViewerState saveViewerState() {
        Set<String> selectedIds = new HashSet<String>();
        return new EntityViewerState(getClass(), contextRootedEntity, selectedIds);
    }
    
    @Override
    public void restoreViewerState(final EntityViewerState state) {
        loadEntity(state.getContextRootedEntity());
        
    }
}
