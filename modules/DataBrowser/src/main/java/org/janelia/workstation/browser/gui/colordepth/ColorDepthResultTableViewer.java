package org.janelia.workstation.browser.gui.colordepth;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.table.TableModel;


import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainObjectAttribute;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.DynamicDomainObjectProxy;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.cdmip.ColorDepthImage;
import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.model.domain.gui.cdmip.ColorDepthResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.dialogs.TableViewerConfigDialog;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.browser.gui.listview.table.TableViewerConfiguration;
import org.janelia.workstation.browser.gui.listview.table.TableViewerPanel;
import org.janelia.workstation.common.gui.listview.ListViewer;
import org.janelia.workstation.common.gui.listview.ListViewerActionListener;
import org.janelia.workstation.common.gui.listview.ListViewerState;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.PreferenceSupport;
import org.janelia.workstation.common.gui.support.SearchProvider;
import org.janelia.workstation.common.gui.table.DynamicColumn;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.model.AnnotatedObjectList;
import org.janelia.workstation.core.model.Decorator;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.workstation.core.util.StringUtilsExtra;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A table viewer for domain objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthResultTableViewer 
        extends TableViewerPanel<ColorDepthMatch,Reference>
        implements ListViewer<ColorDepthMatch,Reference> {

    private static final Logger log = LoggerFactory.getLogger(ColorDepthResultTableViewer.class);
    
    // Configuration
    private TableViewerConfiguration config;
    private final DomainObjectAttribute ATTR_SCORE = new DomainObjectAttribute("score","Score (Pixels)",null,null,true,null,null);
    private final DomainObjectAttribute ATTR_SCORE_PCT = new DomainObjectAttribute("score_pct","Score (Percent)",null,null,true,null,null);
    private final DomainObjectAttribute ATTR_CHANNEL = new DomainObjectAttribute("match_channel","Channel Number",null,null,true,null,null);
    private final DomainObjectAttribute ATTR_OWNER = new DomainObjectAttribute("owner","Owner",null,null,true,null,null);
    private final DomainObjectAttribute ATTR_FILE_NAME = new DomainObjectAttribute("filename","File Name",null,null,true,null,null);
    private final DomainObjectAttribute ATTR_FILE_PATH = new DomainObjectAttribute("filepath","File Path",null,null,true,null,null);

    private final DomainObjectAttribute ATTR_LIBRARIES = new DomainObjectAttribute("libraries","Color Depth Libraries",null,null,true,null,null);
    private final Map<String, DomainObjectAttribute> attributeMap = new HashMap<>();

    // State
    private PreferenceSupport preferenceSupport;
    private AnnotatedObjectList<ColorDepthMatch,Reference> matchList;
    private ChildSelectionModel<ColorDepthMatch,Reference> selectionModel;
    private SearchProvider searchProvider;
    private List<DomainObjectAttribute> attrs;

    // UI state
    private String sortField;
    private boolean ascending = true;
    
    public ColorDepthResultTableViewer() {
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void setActionListener(ListViewerActionListener listener) {
        // Ignored, because this viewer does not hide objects
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
    public int getNumItemsHidden() {
        int totalItems = this.matchList.getObjects().size();
        int totalVisibleItems = getObjects().size();
        return totalItems-totalVisibleItems;
    }
        
    @Override
    public void select(List<ColorDepthMatch> objects, boolean select, boolean clearAll, boolean isUserDriven, boolean notifyModel) {
        log.info("selectDomainObjects(objects={},select={},clearAll={},isUserDriven={},notifyModel={})", 
                DomainUtils.abbr(objects), select, clearAll, isUserDriven, notifyModel);

        if (objects.isEmpty()) {
            return;
        }

        selectObjects(objects, select, clearAll, isUserDriven, notifyModel);
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        updateUI();
    }

    @Override
    public void show(final AnnotatedObjectList<ColorDepthMatch,Reference> matchList, final Callable<Void> success) {

        try {
            this.config = TableViewerConfiguration.loadConfig();
        } catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }

        this.matchList = matchList;
        log.debug("show(matchList={})",DomainUtils.abbr(matchList.getObjects()));

        attributeMap.clear();

        List<ColorDepthMatch> matchObjects = matchList.getObjects();

        ColorDepthResultImageModel model = (ColorDepthResultImageModel)getImageModel();
        List<DomainObject> samples = new ArrayList<>(model.getSamples());
        
        attrs = DomainUtils.getDisplayAttributes(samples);
        attrs.add(0, ATTR_SCORE);
        attrs.add(0, ATTR_SCORE_PCT);
        attrs.add(0, ATTR_OWNER);
        attrs.add(0, ATTR_FILE_PATH);
        attrs.add(0, ATTR_FILE_NAME);
        attrs.add(0, ATTR_LIBRARIES);
        attrs.add(0, ATTR_CHANNEL);

        getDynamicTable().clearColumns();
        for(DomainObjectAttribute attr : attrs) {
            attributeMap.put(attr.getName(), attr);
            boolean visible = config.isColumnVisible(attr.getName());
            // TODO: we can implement sorting, but first there needs to be a way to return to the default sort
            boolean sortable = false;
            getDynamicTable().addColumn(attr.getName(), attr.getLabel(), visible, false, true, sortable);
        }

        showObjects(matchObjects, success);
        setSortCriteria(searchProvider.getSortField());
    }

    @Override
    public void refresh() {
        showObjects(matchList.getObjects(), null);
    }

    @Override
    public void refresh(ColorDepthMatch object) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected ColorDepthMatchContextMenu getContextualPopupMenu() {

        try {
            List<ColorDepthMatch> selected = getSelectedObjects();
            log.info("Selected objects: "+selected);
            ColorDepthResultImageModel imageModel = (ColorDepthResultImageModel)getImageModel();
            ColorDepthMatchContextMenu popupMenu = new ColorDepthMatchContextMenu(
                    (ColorDepthResult)selectionModel.getParentObject(), selected, imageModel, null);
            
            JTable table = getTable();
            ListSelectionModel lsm = table.getSelectionModel();
            if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) {
                String value = table.getValueAt(table.getSelectedRow(), table.getSelectedColumn()).toString();
                final String label = StringUtils.isEmpty(value) ? "Empty value" : value;

                JMenuItem titleMenuItem = new JMenuItem(label);
                titleMenuItem.setEnabled(false);
                popupMenu.add(titleMenuItem);

                JMenuItem copyMenuItem = new JMenuItem("Copy Value To Clipboard");
                copyMenuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Transferable t = new StringSelection(label);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
                    }
                });
                popupMenu.add(copyMenuItem);
                popupMenu.addSeparator();
            }

            popupMenu.addMenuItems();

            return popupMenu;
        } 
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
            return null;
        }
    }

    protected JPopupMenu getColumnPopupMenu(final int col) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem hideItem = new JMenuItem("Hide this column");
        hideItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DynamicColumn column = getDynamicTable().getVisibleColumn(col);
                log.info("Hiding column {} ({})",column.getLabel(),col);
                try {
                    config.getHiddenColumns().add(column.getName());
                    config.save();
                    column.setVisible(false);
                    updateTableModel();
                }
                catch (Exception ex) {
                    FrameworkAccess.handleException(ex);
                }
            }
        });
        menu.add(hideItem);
        return menu;
    }

    @Override
    protected void objectDoubleClicked(ColorDepthMatch object) {
        try {
            getContextualPopupMenu().runDefaultAction();
        } catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
    }

    @Override
    protected void chooseColumnsButtonPressed() {
        TableViewerConfigDialog configDialog = new TableViewerConfigDialog(attrs, config);
        if (configDialog.showDialog(this)==1) {
            for(String attrName : attributeMap.keySet()) {
                boolean visible = config.isColumnVisible(attrName);
                getColumn(attrName).setVisible(visible);
            }
            updateTableModel();
            updateUI();
            log.info("Updated table model1");
        }
    }

    @Override
    protected void deleteKeyPressed() {
        // TODO: implement this?
    }

    @Override
    protected void exportButtonPressed() {
        searchProvider.export();
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    @Override
    public boolean isEditMode() {
        return false;
    }

    @Override
    public void toggleEditMode(boolean editMode) {

    }

    @Override
    public void refreshEditMode() {
    }

    @Override
    public void setEditSelectionModel(ChildSelectionModel<ColorDepthMatch,Reference> editSelectionModel) {
    }

    @Override
    public ChildSelectionModel<ColorDepthMatch,Reference> getEditSelectionModel() {
        return null;
    }

    @Override
    public void selectEditObjects(List<ColorDepthMatch> objects, boolean select) {
    }

    @Override
    public AnnotatedObjectList<ColorDepthMatch,Reference> getObjectList() {
        return matchList;
    }
   
    @Override
    public boolean matches(ResultPage<ColorDepthMatch,Reference> resultPage, ColorDepthMatch object, String text) {
        
        log.trace("Searching {} for {}",object.getImageRef(),text);

        String tupper = text.toUpperCase();
        String titleUpper = getImageModel().getImageTitle(object).toUpperCase();
        String imageFilepath = getImageModel().getImageFilepath(object);

        // Matches on filename or title always work, no matter if they're visible or not
        if (imageFilepath.contains(text) || titleUpper.contains(tupper)) {
            return true;
        }

        for(DynamicColumn column : getColumns()) {
            if (column.isVisible()) {
                log.trace("Searching column "+column.getLabel());
                Object value = getValue(resultPage, object, column.getName());
                if (value != null) {
                    if (value.toString().toUpperCase().contains(tupper)) {
                        log.trace("Found match in column {}: {}",column.getLabel(),value);
                        return true;
                    }
                }
            }
            else {
                log.trace("Skipping invisible column {}",column.getLabel());
            }
        }
        return false;
    }

    public Object getValue(AnnotatedObjectList<ColorDepthMatch,Reference> domainObjectList, ColorDepthMatch match, String columnName) {
        ColorDepthResultImageModel model = (ColorDepthResultImageModel)getImageModel();

        if (ATTR_SCORE.getName().equals(columnName)) {
            return match.getScore();
        }
        else if (ATTR_SCORE_PCT.getName().equals(columnName)) {
            return MaskUtils.getFormattedScorePct(match);
        }
        else if (ATTR_FILE_NAME.getName().equals(columnName)) {
            return model.getImage(match).getName();
        }
        else if (ATTR_FILE_PATH.getName().equals(columnName)) {
            return model.getImage(match).getFilepath();
        }
        else if (ATTR_LIBRARIES.getName().equals(columnName)) {
            ColorDepthImage image = model.getImage(match);
            if (image!=null) {
                return StringUtilsExtra.getCommaDelimited(image.getLibraries());
            }
        }
        else if (ATTR_OWNER.getName().equals(columnName)) {
            return model.getImage(match).getOwnerName();
        }
        else if (ATTR_CHANNEL.getName().equals(columnName)) {
            return model.getImage(match).getChannelNumber();
        }
        else {
            DomainObjectAttribute attr = attributeMap.get(columnName);
            if (attr==null) {
                throw new IllegalStateException("No attribute found for column: "+columnName);
            }

            Sample sample = model.getSample(match);
            if (sample==null) {
                return null;
            }
            else {
                DynamicDomainObjectProxy proxy = new DynamicDomainObjectProxy(sample);
                return proxy.get(attr.getLabel());
            }
        }
        return null;
    }

    @Override
    protected void updateTableModel() {
        super.updateTableModel();
        getTable().setRowSorter(new DomainObjectRowSorter());
    }
    
    @Override
    protected void updateHud(boolean toggle) {

        if (!toggle && !Hud.isInitialized()) return;
        
        Hud hud = Hud.getSingletonInstance();
        hud.setKeyListener(keyListener);

        try {
            List<ColorDepthMatch> selected = getSelectedObjects();
            
            if (selected.size() != 1) {
                hud.hideDialog();
                return;
            }
            
            ColorDepthMatch match = selected.get(0);

            ImageModel<ColorDepthMatch,Reference> imageModel = getImageModel();
            String filepath = imageModel.getImageFilepath(match);
            String title = imageModel.getImageTitle(match);
            hud.setFilepathAndToggleDialog(filepath, title, toggle, false);
        } 
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
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
            return null;
        }
    }

    private void setSortCriteria(String sortCriteria) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(sortCriteria)) {
            setSortColumn(null, true);
        }
        else {
            this.sortField = (sortCriteria.startsWith("-") || sortCriteria.startsWith("+")) ? sortCriteria.substring(1) : sortCriteria;
            this.ascending = !sortCriteria.startsWith("-");
            log.info("Setting sort column: {}",sortCriteria);
            setSortColumn(sortField, ascending);
        }
    }

    protected class DomainObjectRowSorter extends RowSorter<TableModel> {

        private List<SortKey> sortKeys = new ArrayList<>();

        public DomainObjectRowSorter() {
            List<DynamicColumn> columns = getDynamicTable().getDisplayedColumns();
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).getName().equals(sortField)) {
                    sortKeys.add(new SortKey(i, ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING));
                    break;
                }
            }
        }

        @Override
        public void toggleSortOrder(int columnNum) {
            List<DynamicColumn> columns = getDynamicTable().getDisplayedColumns();
            DynamicColumn column = columns.get(columnNum);
            if (!column.isVisible() || !column.isSortable()) {
                return;
            }
            SortOrder newOrder = SortOrder.ASCENDING;
            if (!sortKeys.isEmpty()) {
                SortKey currentSortKey = sortKeys.get(0);
                if (currentSortKey.getColumn() == columnNum) {
                    // Reverse the sort
                    if (currentSortKey.getSortOrder() == SortOrder.ASCENDING) {
                        newOrder = SortOrder.DESCENDING;
                    }
                }
                sortKeys.clear();
            }

            sortKeys.add(new SortKey(columnNum, newOrder));
            sortField = column.getName();
            ascending = (newOrder != SortOrder.DESCENDING);
            String prefix = ascending ? "+":"-";
            getSearchProvider().setSortField(prefix + sortField);
            getSearchProvider().search();
        }

        @Override
        public void setSortKeys(List<? extends SortKey> sortKeys) {
            this.sortKeys = new ArrayList<>(sortKeys);
        }

        @Override
        public void rowsUpdated(int firstRow, int endRow, int column) {
        }

        @Override
        public void rowsUpdated(int firstRow, int endRow) {
        }

        @Override
        public void rowsInserted(int firstRow, int endRow) {
        }

        @Override
        public void rowsDeleted(int firstRow, int endRow) {
        }

        @Override
        public void modelStructureChanged() {
        }

        @Override
        public int getViewRowCount() {
            return getTable().getModel().getRowCount();
        }

        @Override
        public List<? extends SortKey> getSortKeys() {
            return sortKeys;
        }

        @Override
        public int getModelRowCount() {
            return getTable().getModel().getRowCount();
        }

        @Override
        public TableModel getModel() {
            return getDynamicTable().getTableModel();
        }

        @Override
        public int convertRowIndexToView(int index) {
            return index;
        }

        @Override
        public int convertRowIndexToModel(int index) {
            return index;
        }

        @Override
        public void allRowsChanged() {
        }
    };
    
    @Override
    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
    }

    public SearchProvider getSearchProvider() {
        return searchProvider;
    }

    @Override
    public ListViewerState saveState() {
        JScrollPane scrollPane = getDynamicTable().getScrollPane();
        
        int horizontalScrollValue = scrollPane.getHorizontalScrollBar().getModel().getValue();
        log.debug("Saving horizontalScrollValue={}",horizontalScrollValue);
        
        int verticalScrollValue = scrollPane.getVerticalScrollBar().getModel().getValue();
        log.debug("Saving verticalScrollValue={}",verticalScrollValue);
        
        return new ColorDepthResultTableViewerState(horizontalScrollValue, verticalScrollValue);
    }

    @Override
    public void restoreState(ListViewerState viewerState) {
        if (viewerState instanceof ColorDepthResultTableViewerState) {
            final ColorDepthResultTableViewerState tableViewerState = (ColorDepthResultTableViewerState)viewerState;
            final JScrollPane scrollPane = getDynamicTable().getScrollPane();
            SwingUtilities.invokeLater(() -> {
                
                int horizontalScrollValue = tableViewerState.getHorizontalScrollValue();
                log.debug("Restoring horizontalScrollValue={}",horizontalScrollValue);
                scrollPane.getHorizontalScrollBar().setValue(horizontalScrollValue);

                int verticalScrollValue = tableViewerState.getVerticalScrollValue();
                log.debug("Restoring verticalScrollValue={}",verticalScrollValue);
                scrollPane.getVerticalScrollBar().setValue(verticalScrollValue);
            }
            );
        }
        else {
            log.warn("Cannot restore viewer state of type {}", viewerState.getClass());
        }
    }

}
