package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntitySelectionHistory;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.util.ConcurrentUtils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around a Viewer that provides a title bar, a close button, and entity navigation history.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerPane extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(ViewerPane.class);

    private static final Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, 12);

    private final ViewerContainer viewerContainer;
    private final EntitySelectionHistory entitySelectionHistory;
    private final String selectionCategory;
    private final JLabel titleLabel;
    private final JPanel mainTitlePane;
    
    private Viewer viewer;

    protected RootedEntity contextRootedEntity;
    protected List<RootedEntity> rootedAncestors;
    protected SimpleWorker ancestorLoadingWorker;

    public ViewerPane(ViewerContainer viewerContainer, String selectionCategory, boolean showHideButton) {

        setLayout(new BorderLayout());

        this.viewerContainer = viewerContainer;
        this.selectionCategory = selectionCategory;
        this.entitySelectionHistory = new EntitySelectionHistory();

        titleLabel = new JLabel(" ");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 3, 0));
        titleLabel.setFont(titleLabelFont);
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        mainTitlePane = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 1;

        mainTitlePane.add(titleLabel, c);

        if (showHideButton) {
            JButton hideButton = new JButton(Icons.getIcon("close_red.png"));
            hideButton.setPreferredSize(new Dimension(16, 16));
            hideButton.setBorderPainted(false);
            hideButton.setToolTipText("Close this viewer");
            hideButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    closeButtonPressed();
                }
            });

            c.gridx = 1;
            c.gridy = 0;
            c.insets = new Insets(0, 0, 0, 0);
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.LINE_END;
            c.weightx = 0;
            hideButton.setBorder(BorderFactory.createLineBorder(Color.red));
            mainTitlePane.add(hideButton, c);
        }

        add(mainTitlePane, BorderLayout.NORTH);
    }

    protected void closeButtonPressed() {
        throw new UnsupportedOperationException("This method has not been implemented for this ViewerPane instance");
    }

    public void clearViewer() {
        if (this.viewer != null) {
            log.debug("Clearing viewer {}", selectionCategory);
            this.viewer.clear();
            revalidate();
            repaint();
        }
        this.contextRootedEntity = null;
    }

    public void closeViewer() {
        if (this.viewer != null) {
            log.debug("Closing viewer {}", this.viewer);
            this.viewer.close();
            remove(this.viewer);
            revalidate();
            repaint();
        }
        this.viewer = null;
        this.contextRootedEntity = null;
    }

    public void setViewer(Viewer viewer) {
        log.debug("Setting viewer for viewer pane {}", selectionCategory);
        closeViewer();
        this.viewer = viewer;
        if (viewer != null) {
            log.debug("Adding viewer {}", this.viewer);
            add(viewer, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    public Viewer getViewer() {
        return viewer;
    }

    /**
     * Exposes main title pane for adding things to that real-estate.
     *
     * @return pane setup in c'tor.
     */
    public JPanel getMainTitlePane() {
        return mainTitlePane;
    }

    /**
     * Returns the selection category of this viewer in the EntitySelectionModel.
     *
     * @return EntitySelectionModel.CATEGORY_*
     */
    public String getSelectionCategory() {
        return selectionCategory;
    }

    public EntitySelectionHistory getEntitySelectionHistory() {
        return entitySelectionHistory;
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setAsActive() {
        if (viewerContainer != null) {
            viewerContainer.setActiveViewerPane(this);
        }
    }

    public boolean isActive() {
        return this.equals(viewerContainer.getActiveViewerPane());
    }

    public ViewerContainer getViewerContainer() {
        return viewerContainer;
    }

    public void loadEntity(RootedEntity rootedEntity) {
        loadEntity(rootedEntity, null);
    }

    public synchronized void loadEntity(RootedEntity rootedEntity, final Callable<Void> success) {

        if (rootedEntity == null) {
            return;
        }
        log.debug("loadEntity: " + rootedEntity.getId());

        if (contextRootedEntity != null && rootedEntity.getId().equals(contextRootedEntity.getId())) {
            log.debug("Entity is already loaded: " + contextRootedEntity.getId());
            ConcurrentUtils.invokeAndHandleExceptions(success);
            return;
        }

        this.contextRootedEntity = rootedEntity;

        Entity entity = contextRootedEntity.getEntity();
        setTitle(entity.getName());

        if (ancestorLoadingWorker != null && !ancestorLoadingWorker.isDone()) {
            ancestorLoadingWorker.disregard();
        }

        ancestorLoadingWorker = new SimpleWorker() {

            private List<RootedEntity> ancestors = new ArrayList<RootedEntity>();

            @Override
            protected void doStuff() throws Exception {
                List<String> uniqueIds = EntityUtils.getPathFromUniqueId(contextRootedEntity.getUniqueId());
                List<Long> entityIds = new ArrayList<Long>();
                for (String uniqueId : uniqueIds) {
                    Long entityId = EntityUtils.getEntityIdFromUniqueId(uniqueId);
                    entityIds.add(entityId);
                }
                Map<Long, Entity> entityMap = EntityUtils.getEntityMap(ModelMgr.getModelMgr().getEntityByIds(entityIds));

                for (String uniqueId : uniqueIds) {
                    Long entityId = EntityUtils.getEntityIdFromUniqueId(uniqueId);
                    Entity entity = entityMap.get(entityId);
                    if (entity != null) {
                        EntityData entityData = new EntityData();
                        entityData.setChildEntity(entity);
                        ancestors.add(new RootedEntity(uniqueId, entityData));
                    }
                }

                Collections.reverse(ancestors);
            }

            @Override
            protected void hadSuccess() {
                setRootedAncestors(ancestors);
                ancestorLoadingWorker = null;
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);

            }
        };
        ancestorLoadingWorker.execute();

        viewer.loadEntity(rootedEntity, success);
    }

    private synchronized void setRootedAncestors(List<RootedEntity> rootedAncestors) {
        this.rootedAncestors = rootedAncestors;
        StringBuilder buf = new StringBuilder();
        for (int i = rootedAncestors.size() - 1; i >= 0; i--) {
            RootedEntity ancestor = rootedAncestors.get(i);
            if (buf.length() > 0) {
                buf.append(" : ");
            }
            buf.append(ancestor.getEntity().getName());
        }
        setTitle(buf.toString());
    }

    public List<RootedEntity> getRootedAncestors() {
        return rootedAncestors;
    }
}
