/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.application.SplashPanel;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationSession;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This panel shows images for annotation. It may show a bunch of images at once, or a single image.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconDemoPanel extends JPanel {

    private static final String JACS_DATA_PATH_MAC = ConsoleProperties.getString("remote.defaultMacPath");
    private static final String JACS_DATA_PATH_LINUX = ConsoleProperties.getString("remote.defaultLinuxPath");

    private AnnotationSession session;

    private SplashPanel splashPanel;
    private JToolBar toolbar;
    private JToggleButton showTitlesButton;
    private JToggleButton showTagsButton;
    private JSlider slider;
    private JToggleButton invertButton;
    private ImagesPanel imagesPanel;
    private JScrollPane scrollPane;
    private ImageDetailPanel imageDetailPanel;

    private List<Entity> entities;
    private Entity currentEntity;
    private boolean viewingSingleImage = true;

    // Listen for key strokes and execute the appropriate key bindings
    private KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (KeymapUtil.isModifier(e)) return;

                KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);
                if (SessionMgr.getKeyBindings().executeBinding(shortcut)) return;

                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    previousEntity();
                    updateUI();
                }
                else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    nextEntity();
                    updateUI();
                }
            }
        }
    };

    public IconDemoPanel() {

        setBackground(Color.white);
        setLayout(new BorderLayout());
        setFocusable(true);

        splashPanel = new SplashPanel();
        add(splashPanel);

        toolbar = createToolbar();
        imagesPanel = new ImagesPanel(this);
        imageDetailPanel = new ImageDetailPanel(this);

        scrollPane = new JScrollPane();
        scrollPane.setViewportView(imagesPanel);

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                double imageSizePercent = (double) source.getValue() / (double) 100;
                imagesPanel.rescaleImages(imageSizePercent);
                imagesPanel.recalculateGrid();
            }
        });

        scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {

                int value = e.getValue();
                // TODO: load/unload images as they move out of the viewport?

            }
        });

        this.addKeyListener(getKeyListener());
    }

    private void setTitleVisbility() {
        for (AnnotatedImageButton button : getImagesPanel().getButtons().values()) {
            button.setTitleVisible(showTitlesButton.isSelected());
        }
    }

    private void setTagVisbility() {
        for (AnnotatedImageButton button : getImagesPanel().getButtons().values()) {
            button.setTagsVisible(showTagsButton.isSelected());
        }
    }
    
    private JToolBar createToolbar() {

        JToolBar toolBar = new JToolBar("Still draggable");
        toolBar.setFloatable(true);
        toolBar.setRollover(true);

        toolBar.add(new JLabel("Show:"));

        showTitlesButton = new JToggleButton("Titles");
        showTitlesButton.setSelected(true);
        showTitlesButton.setToolTipText("Show the image title above each image.");
        showTitlesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	setTitleVisbility();
                getImagesPanel().recalculateGrid();
            }
        });
        toolBar.add(showTitlesButton);

        showTagsButton = new JToggleButton("Tags");
        showTagsButton.setSelected(true);
        showTagsButton.setToolTipText("Show tags below each images");
        showTagsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	setTagVisbility();
                getImagesPanel().recalculateGrid();
            }
        });
        toolBar.add(showTagsButton);

        toolBar.addSeparator();

        invertButton = new JToggleButton("Invert colors");
        invertButton.setToolTipText("Invert the color space on all images");
        invertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.setWaitingCursor(IconDemoPanel.this);
                try {
                    for (AnnotatedImageButton button : getImagesPanel().getButtons().values()) {
                        button.setInvertedColors(invertButton.isSelected());
                    }
                    getImagesPanel().repaint();
                }
                finally {
                    Utils.setDefaultCursor(IconDemoPanel.this);
                }
            }
        });
        toolBar.add(invertButton);

        final JToggleButton hideCompletedButton = new JToggleButton("Hide completed");
        hideCompletedButton.setToolTipText("Hide images which have been annotated completely according to the annotation session's ruleset.");
        hideCompletedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: implement this
            }
        });
        toolBar.add(hideCompletedButton);

        toolBar.addSeparator();

        slider = new JSlider(1, 100, 100);
        slider.setFocusable(false);
        slider.setToolTipText("Image size percentage");
        toolBar.add(slider);

        return toolBar;
    }

    public boolean isInverted() {
        return invertButton.isSelected();
    }

    // TODO: need a more general way of doing this
    public String convertImagePath(String filepath) {
        return filepath.replace(JACS_DATA_PATH_LINUX, JACS_DATA_PATH_MAC);
    }

    public void loadImageEntities(final AnnotationSession session) {

        this.session = session;

        SimpleWorker loadingWorker = new SimpleWorker() {

            private Map<Long, List<Entity>> annotationMap;

            protected void doStuff() throws Exception {
                List<Entity> loadedEntities = new ArrayList<Entity>();
                List<Entity> allEntities = session.getEntities();
                for (Entity entity : allEntities) {
                    if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_TIF_2D)) {
                        // Ignore things we can't display
                        continue;
                    }
                    loadedEntities.add(entity);
                }
                setEntities(loadedEntities);
                annotationMap = session.getAnnotationMap();
            }

            protected void hadSuccess() {
                imagesPanel.load(getEntities());
                imagesPanel.loadAnnotations(annotationMap);
                setTitleVisbility();
                setTagVisbility();
                showAllEntities();
            }

            protected void hadError(Throwable error) {
                error.printStackTrace();
                if (getEntities() != null) {
                    JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error", JOptionPane.ERROR_MESSAGE);
                    imagesPanel.load(getEntities());
                    showAllEntities();
                    // TODO: set read-only mode
                }
                else {
                    JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading session", "Data Loading Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        loadingWorker.execute();
    }

    public void reloadAnnotations() {

        session.clearDerivedProperties();

        SimpleWorker loadingWorker = new SimpleWorker() {

            private Map<Long, List<Entity>> annotationMap;

            protected void doStuff() throws Exception {
                annotationMap = session.getAnnotationMap();
            }

            protected void hadSuccess() {
                imagesPanel.loadAnnotations(annotationMap);
            }

            protected void hadError(Throwable error) {
                error.printStackTrace();
                JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        loadingWorker.execute();
    }

    public synchronized void showAllEntities() {
        if (viewingSingleImage) {
            viewingSingleImage = false;
            removeAll();
            add(toolbar, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }
        updateUI();
    }

    public synchronized void showCurrentEntityDetails() {
        imageDetailPanel.load(currentEntity, null);
        if (!viewingSingleImage) {
            viewingSingleImage = true;
            removeAll();
            add(imageDetailPanel);
        }
        updateUI();

        // Focus on the panel so that it can receive keyboard input
        requestFocusInWindow();
    }

    public boolean previousEntity() {
        List<Entity> entities = getEntities();
        int i = entities.indexOf(currentEntity);
        if (i < 1) {
            // Already at the beginning
            return false;
        }
        setCurrentEntity(entities.get(i - 1));
        if (viewingSingleImage) {
            showCurrentEntityDetails();
        }
        return true;
    }

    public boolean nextEntity() {
        List<Entity> entities = getEntities();
        int i = entities.indexOf(currentEntity);
        if (i > entities.size() - 2) {
            // Already at the end
            return false;
        }
        setCurrentEntity(entities.get(i + 1));
        if (viewingSingleImage) {
            showCurrentEntityDetails();
        }
        return true;
    }

    public synchronized List<Entity> getEntities() {
        return entities;
    }

    public synchronized void setEntities(List<Entity> entities) {
        this.entities = entities;
    }

    public synchronized Entity getCurrentEntity() {
        return currentEntity;
    }

    public synchronized void setCurrentEntity(Entity entity) {
        this.currentEntity = entity;
        imagesPanel.setSelectedImage(currentEntity);
    }

    public ImagesPanel getImagesPanel() {
        return imagesPanel;
    }

    public ImageDetailPanel getImageDetailPanel() {
        return imageDetailPanel;
    }

    public KeyListener getKeyListener() {
        return keyListener;
    }
}
