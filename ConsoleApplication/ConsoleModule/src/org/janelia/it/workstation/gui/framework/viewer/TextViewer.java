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

import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.outline.EntitySelectionHistory;
import org.janelia.it.workstation.gui.framework.outline.EntityViewerState;
import org.janelia.it.workstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.MouseForwarder;
import org.janelia.it.workstation.model.entity.RootedEntity;
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
    protected RootedEntity contextRootedEntity;
    protected SessionModelListener sessionModelListener;

    public TextViewer(ViewerPane viewerPane) {
        super(viewerPane);

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);

        toolbar = createToolbar();
        toolbar.addMouseListener(new MouseForwarder(this, "JToolBar->ErrorViewer"));
        add(toolbar, BorderLayout.NORTH);

        this.textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(false);

        this.scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        textArea.addMouseListener(new MouseForwarder(this, "JTextPane->ErrorViewer"));

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
                    TextViewer.this.clear();
                }
            }
        };
        SessionMgr.getSessionMgr().addSessionModelListener(sessionModelListener);
    }

    protected final ViewerToolbar createToolbar() {

        return new ViewerToolbar() {

            @Override
            protected void goBack() {
                final EntitySelectionHistory history = getViewerPane().getEntitySelectionHistory();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        history.goBack(saveViewerState());
                    }
                });
            }

            @Override
            protected void goForward() {
                final EntitySelectionHistory history = getViewerPane().getEntitySelectionHistory();
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

        };
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        this.updateUI();
    }

    public void showViewer() {
        removeAll();

        textArea.setCaretPosition(0);

        // Update back/forward navigation
        EntitySelectionHistory history = getViewerPane().getEntitySelectionHistory();
        toolbar.getPrevButton().setEnabled(history.isBackEnabled());
        toolbar.getNextButton().setEnabled(history.isNextEnabled());

        add(toolbar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        this.updateUI();
    }

    @Override
    public void loadEntity(RootedEntity rootedEntity) {
        loadEntity(rootedEntity, null);
    }

    @Override
    public synchronized void loadEntity(final RootedEntity rootedEntity, final Callable<Void> success) {

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
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();

    }

    public abstract String getText(RootedEntity rootedEntity) throws Exception;

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

            RootedEntity rootedEntity = contextRootedEntity;

            @Override
            protected void doStuff() throws Exception {
                Entity entity = ModelMgr.getModelMgr().getEntityById(rootedEntity.getEntity().getId());
                if (entity == null) {
                    return;
                }
                rootedEntity.setEntity(ModelMgr.getModelMgr().loadLazyEntity(entity, false));
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
                SessionMgr.getSessionMgr().handleException(error);
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
        SessionMgr.getSessionMgr().removeSessionModelListener(sessionModelListener);
    }

    @Override
    public List<RootedEntity> getRootedEntities() {
        List<RootedEntity> rootedEntities = new ArrayList<RootedEntity>();
        rootedEntities.add(contextRootedEntity);
        return rootedEntities;
    }

    @Override
    public List<RootedEntity> getSelectedEntities() {
        List<RootedEntity> rootedEntities = new ArrayList<RootedEntity>();
        rootedEntities.add(contextRootedEntity);
        return rootedEntities;
    }

    @Override
    public RootedEntity getRootedEntityById(String uniqueId) {
        return contextRootedEntity.getUniqueId().equals(uniqueId) ? contextRootedEntity : null;
    }

    @Override
    public RootedEntity getContextRootedEntity() {
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
