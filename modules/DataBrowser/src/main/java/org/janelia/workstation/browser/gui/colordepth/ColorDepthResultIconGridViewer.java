package org.janelia.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.janelia.workstation.browser.actions.ExportPickedNames;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.browser.actions.ExportPickedGUIDs;
import org.janelia.workstation.browser.actions.ExportPickedLineNames;
import org.janelia.workstation.browser.actions.ExportPickedToSplitGenWebsite;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.buttons.DropDownButton;
import org.janelia.workstation.core.model.AnnotatedObjectList;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.workstation.common.gui.listview.ListViewer;
import org.janelia.workstation.common.gui.listview.ListViewerActionListener;
import org.janelia.workstation.common.gui.listview.ListViewerState;
import org.janelia.workstation.browser.gui.listview.icongrid.IconGridViewerPanel;
import org.janelia.workstation.common.gui.support.PreferenceSupport;
import org.janelia.workstation.common.gui.support.SearchProvider;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.enums.SplitHalfType;
import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.model.domain.gui.cdmip.ColorDepthResult;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * An IconGridViewer implementation for viewing color depth search results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthResultIconGridViewer 
        extends IconGridViewerPanel<ColorDepthMatch,Reference>
        implements ListViewer<ColorDepthMatch,Reference> {
    
    private static final Logger log = LoggerFactory.getLogger(ColorDepthResultIconGridViewer.class);

    // UI Components
    private JCheckBoxMenuItem showVtLineNamesCheckbox;
    private final JToggleButton editModeButton;
    private final DropDownButton editOkButton;
    
    // Configuration
    @SuppressWarnings("unused")
    private SearchProvider searchProvider;

    // State
    private PreferenceSupport preferenceSupport;
    private AnnotatedObjectList<ColorDepthMatch,Reference> matchList;
    private ChildSelectionModel<ColorDepthMatch,Reference> selectionModel;
    private ChildSelectionModel<ColorDepthMatch,Reference> editSelectionModel;
    private boolean editMode;
    
    public ColorDepthResultIconGridViewer() {
        // Customize the config options
        DropDownButton configButton = getToolbar().getConfigButton();
        configButton.removeAll();
        showVtLineNamesCheckbox = new JCheckBoxMenuItem("Show VT Lines instead of BJD Lines where available");
        showVtLineNamesCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ColorDepthResultImageModel imageModel = (ColorDepthResultImageModel)getImageModel();
                imageModel.setShowVtLineNames(showVtLineNamesCheckbox.isSelected());
                refresh();
            }
        });
        configButton.addMenuItem(showVtLineNamesCheckbox);

        // The following buttons are duplicated from DomainObjectIconGridViewer
        
        this.editModeButton = new JToggleButton("Pick");
        editModeButton.setIcon(Icons.getIcon("cart.png"));
        editModeButton.setFocusable(false);
        editModeButton.setToolTipText("Select items for export and other actions");
        editModeButton.addActionListener((e) -> {
            toggleEditMode(editModeButton.isSelected());
        });
        
        this.editOkButton = new DropDownButton();
        editOkButton.setIcon(Icons.getIcon("cart_go.png"));
        editOkButton.setFocusable(false);
        editOkButton.setVisible(false);
        editOkButton.setToolTipText("Open split generation website with selected lines");

        JMenuItem exportGuidsMenuItem = new JMenuItem("Export GUIDs (globally unique identifiers)");
        exportGuidsMenuItem.addActionListener((e) -> {
            new ExportPickedGUIDs(getPickedItems()).actionPerformed(e);
        });
        editOkButton.addMenuItem(exportGuidsMenuItem);

        JMenuItem exportNamesMenuItem = new JMenuItem("Export object names");
        exportNamesMenuItem.addActionListener((e) -> {
            new ExportPickedNames(getPickedItems()).actionPerformed(e);
        });
        editOkButton.addMenuItem(exportNamesMenuItem);

        JMenuItem exportLinesMenuItem = new JMenuItem("Export line names");
        exportLinesMenuItem.addActionListener((e) -> {
            new ExportPickedLineNames(getPickedItems()).actionPerformed(e);
        });
        editOkButton.addMenuItem(exportLinesMenuItem);

        JMenuItem splitGenMenuItem = new JMenuItem("Send to split generation website");
        splitGenMenuItem.addActionListener((e) -> {
            new ExportPickedToSplitGenWebsite(getPickedItems()).actionPerformed(e);
        });
        editOkButton.addMenuItem(splitGenMenuItem);
        
        getToolbar().addCustomComponent(editModeButton);
        getToolbar().addCustomComponent(editOkButton);
    }

    protected List<Reference> getPickedItems() {

        // Ensure that the user has selected some lines
        List<Reference> selectedIds = editSelectionModel.getSelectedIds();
        if (selectedIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Collect the user-selected split line ids to send to the website
        ColorDepthResultImageModel model = (ColorDepthResultImageModel)getImageModel();
        List<Reference> sampleRefs = new ArrayList<>();
        for(Reference filepath : selectedIds) {
            ColorDepthMatch match = model.getImageByUniqueId(filepath);
            if (match != null) {
                Sample sample = model.getSample(match);
                if (sample != null) {
                    sampleRefs.add(Reference.createFor(sample));
                }
            }
        }
        
        return sampleRefs;
    }
    
    @Override
    public void setImageModel(ImageModel<ColorDepthMatch,Reference> imageModel) {
        super.setImageModel(imageModel);
        ColorDepthResultImageModel model = (ColorDepthResultImageModel)getImageModel();
        showVtLineNamesCheckbox.setSelected(model.isShowVtLineNames());
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void setActionListener(ListViewerActionListener listener) {
    }

    @Override
    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
    }
    
    @Override
    public void setSelectionModel(ChildSelectionModel<ColorDepthMatch,Reference> selectionModel) {
        super.setSelectionModel(selectionModel);
        this.selectionModel = selectionModel;
    }
    
    @Override
    public ChildSelectionModel<ColorDepthMatch,Reference> getSelectionModel() {
        return selectionModel;
    }

    @Override
    public void setPreferenceSupport(PreferenceSupport preferenceSupport) {
        this.preferenceSupport = preferenceSupport;
    }
    
    @Override
    public PreferenceSupport getPreferenceSupport() {
        return preferenceSupport;
    }
    
    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
    
    @Override
    public int getNumItemsHidden() {
        if (this.matchList==null || this.matchList.getObjects()==null) return 0;
        int totalItems = this.matchList.getObjects().size();
        List<ColorDepthMatch> objects = getObjects();
        int totalVisibleItems = objects==null ? 0 : getObjects().size();
        return totalItems-totalVisibleItems;
    }

    @Override
    public void select(List<ColorDepthMatch> objects, boolean select, boolean clearAll, boolean isUserDriven, boolean notifyModel) {
        log.info("selectDomainObjects(objects={},select={},clearAll={},isUserDriven={},notifyModel={})", 
                DomainUtils.abbr(objects), select, clearAll, isUserDriven, notifyModel);

        if (objects.isEmpty()) {
            return;
        }

        if (select) {
            selectObjects(objects, clearAll, isUserDriven, notifyModel);
        }
        else {
            deselectObjects(objects, isUserDriven, notifyModel);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrollSelectedObjectsToCenter();
            }
        });
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        updateUI();
    }
    
    @Override
    public void show(AnnotatedObjectList<ColorDepthMatch,Reference> matchList, Callable<Void> success) {
        this.matchList = matchList;
        log.info("show(objects={})",DomainUtils.abbr(matchList.getObjects()));
        List<ColorDepthMatch> matchObjects = matchList.getObjects();
        showObjects(matchObjects, success);        
    }
    
    @Override
    public void selectEditObjects(List<ColorDepthMatch> domainObjects, boolean select) {
        log.info("selectEditObjects(domainObjects={},select={})", DomainUtils.abbr(domainObjects), select);
        if (domainObjects.isEmpty()) {
            return;
        }
        if (select) {
            editSelectionModel.select(domainObjects, true, true);
        }
    }

    @Override
    public boolean isEditMode() {
        return editMode;
    }

    @Override
    public void toggleEditMode(boolean editMode) {
        this.editMode = editMode;
        imagesPanel.setEditMode(editMode);
        if (editSelectionModel!=null) {
            editSelectionModel.reset();
        }
        editModeButton.setSelected(editMode);
        editOkButton.setVisible(editMode);
    }

    @Override
    public void refreshEditMode() {
        imagesPanel.setEditMode(editMode);
        if (editSelectionModel!=null) {
            imagesPanel.setEditSelection(editSelectionModel.getSelectedIds());
        }
    }

    @Override
    public void setEditSelectionModel(ChildSelectionModel<ColorDepthMatch,Reference> editSelectionModel) {
        this.editSelectionModel = editSelectionModel;
        imagesPanel.setEditSelectionModel(editSelectionModel);
    }

    @Override
    public ChildSelectionModel<ColorDepthMatch,Reference> getEditSelectionModel() {
        return editSelectionModel;
    }

    @Subscribe
    public void handleEditSelection(ColorDepthMatchEditSelectionEvent event) {
        // Refresh the edit checkboxes any time the edit selection model changes
        refreshEditMode();
    }
    
    @Override
    public boolean matches(ResultPage<ColorDepthMatch,Reference> resultPage, ColorDepthMatch object, String text) {

        log.trace("Searching {} for {}",object.getImageRef(),text);

        String tupper = text.toUpperCase();
        String titleUpper = getImageModel().getImageTitle(object).toUpperCase();
        String imageFilepath = getImageModel().getImageFilepath(object);
        
        // Exact matches on filename or title always work
        if (imageFilepath.contains(text) || titleUpper.contains(tupper)) {
            return true;
        }

        ColorDepthResultImageModel imageModel = (ColorDepthResultImageModel)getImageModel();
        Sample sample = imageModel.getSample(object);
        if (sample != null) {
            String line = sample.getLine();
            if (line != null && line.toUpperCase().contains(text)) return true;
            
            String vtline = sample.getVtLine();
            if (vtline != null && vtline.toUpperCase().contains(text)) return true;
            
            String alias = sample.getFlycoreAlias();
            if (alias != null && alias.toUpperCase().contains(text)) return true;
        }
        
        return false;
    }

    @Override
    public void refresh(ColorDepthMatch match) {
        refreshObject(match);
    }

    @Override
    protected ColorDepthMatchContextMenu getContextualPopupMenu() {
        return getPopupMenu(getSelectedObjects());
    }
    
    private ColorDepthMatchContextMenu getPopupMenu(List<ColorDepthMatch> selected) {
        ColorDepthResultImageModel imageModel = (ColorDepthResultImageModel)getImageModel();
        ColorDepthMatchContextMenu popupMenu = new ColorDepthMatchContextMenu(
                (ColorDepthResult)selectionModel.getParentObject(), 
                selected, 
                imageModel, 
                editMode ? editSelectionModel : null);
        popupMenu.addMenuItems();
        return popupMenu;
    }
    
    @Override
    protected JPopupMenu getAnnotationPopupMenu(ColorDepthMatch match, Annotation annotation) {
        ColorDepthResultImageModel imageModel = (ColorDepthResultImageModel)getImageModel();
        if (annotation.getName().equals(SplitHalfType.AD.getName()) 
                || annotation.getName().equals(SplitHalfType.DBD.getName())) {
            SplitHalfContextMenu menu = new SplitHalfContextMenu(
                    imageModel, match, SplitHalfType.valueOf(annotation.getName()));
            menu.addMenuItems();
            return menu;
        }
        return null;
    }

    @Override
    protected void moreAnnotationsButtonDoubleClicked(ColorDepthMatch match) {}

    @Override
    protected void objectDoubleClick(ColorDepthMatch match) {
        getPopupMenu(Collections.singletonList(match)).runDefaultAction();
    }
    
    @Override
    protected void deleteKeyPressed() {}

    @Override
    protected void customizeTitlesPressed() {}

    @Override
    protected void setMustHaveImage(boolean mustHaveImage) {}

    @Override
    protected boolean isMustHaveImage() {
        return false;
    }

    @Override
    protected void updateHud(boolean toggle) {

        if (!toggle && !ColorDepthHud.isInitialized()) return;

        ColorDepthHud hud = ColorDepthHud.getSingletonInstance();
        hud.setKeyListener(keyListener);

        try {
            List<ColorDepthMatch> selected = getSelectedObjects();
            
            if (selected.size() != 1) {
                hud.hideDialog();
                return;
            }
            
            ColorDepthMatch match = selected.get(0);

            ColorDepthResultImageModel imageModel = (ColorDepthResultImageModel)getImageModel();
            String filepath = imageModel.getImageFilepath(match);
            String title = imageModel.getImageTitle(match);
            hud.setObjectAndToggleDialog(match, imageModel, toggle);
        } 
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
    }

    @Override
    public ListViewerState saveState() {
        int maxImageWidth = imagesPanel.getMaxImageWidth();
        log.debug("Saving maxImageWidth={}",maxImageWidth);
        ColorDepthResultIconGridViewerState state = new ColorDepthResultIconGridViewerState(maxImageWidth);
        return state;
    }

    @Override
    public void restoreState(ListViewerState viewerState) {
        if (viewerState instanceof ColorDepthResultIconGridViewerState) {
            final ColorDepthResultIconGridViewerState tableViewerState = (ColorDepthResultIconGridViewerState) viewerState;
            SwingUtilities.invokeLater(new Runnable() {
                   public void run() {
                       int maxImageWidth = tableViewerState.getMaxImageWidth();
                       log.debug("Restoring maxImageWidth={}",maxImageWidth);
                       getToolbar().getImageSizeSlider().setValue(maxImageWidth);
                       // Wait until slider resizes images, then fix scroll:
                       SwingUtilities.invokeLater(() -> scrollSelectedObjectsToCenter());
                   }
               }
            );
        }
        else {
            log.warn("Cannot restore viewer state of type {}", viewerState.getClass());
        }
    }

    private List<ColorDepthMatch> getSelectedObjects() {
        try {
            ImageModel<ColorDepthMatch,Reference> imageModel = getImageModel();
            List<ColorDepthMatch> selected = new ArrayList<>();
            for(Reference id : selectionModel.getSelectedIds()) {
                ColorDepthMatch match = imageModel.getImageByUniqueId(id);
                if (match==null) {
                    throw new IllegalStateException("Image model has no object for unique id: "+id);
                }
                else {
                    selected.add(match);
                }
            }
            return selected;
        }  
        catch (Exception e) {
            FrameworkAccess.handleException(e);
            return Collections.emptyList();
        }
    }
}
