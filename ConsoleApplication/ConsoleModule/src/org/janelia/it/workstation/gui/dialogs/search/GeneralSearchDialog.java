package org.janelia.it.workstation.gui.dialogs.search;

import org.janelia.it.jacs.compute.api.support.SolrQueryBuilder;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.file_chooser.FileChooser;
import org.janelia.it.jacs.shared.solr.EntityDocument;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicRow;
import org.janelia.it.workstation.gui.framework.table.DynamicTable;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.model.utils.FolderUtils;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog for performing general searches. Allows for saving the results to the entity model,
 * or exporting them to a tab-delimited file.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GeneralSearchDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(GeneralSearchDialog.class);
    
    /**
     * Default directory for exports
     */
    protected static final String DEFAULT_EXPORT_DIR = System.getProperty("user.home");

    /**
     * How many results to load at a time when exporting
     */
    protected static final int EXPORT_PAGE_SIZE = 1000;

    protected final SearchConfiguration searchConfig;

    // UI Elements
    protected final SearchParametersPanel paramsPanel;
    protected final SearchResultsPanel resultsPanel;
    protected final JTextField folderNameField;
    protected final JButton exportButton;

    public GeneralSearchDialog(SearchConfiguration searchConfig) {

        this.searchConfig = searchConfig;

        setLayout(new BorderLayout());
        setTitle("Search");

        paramsPanel = new SearchParametersPanel() {
            @Override
            public void performSearch(boolean clear) {
                super.performSearch(clear);
                resultsPanel.performSearch(clear, clear, true);
            }
        };
        paramsPanel.init(searchConfig);
        searchConfig.addConfigurationChangeListener(paramsPanel);
        add(paramsPanel, BorderLayout.NORTH);

        resultsPanel = new SearchResultsPanel(paramsPanel) {
            @Override
            protected void populateResultView(ResultPage resultPage) {
                super.populateResultView(resultPage);
                exportButton.setEnabled(!resultPage.getSolrResults().getResultList().isEmpty());
            }

            @Override
            protected JPopupMenu getPopupMenu(List<Entity> selectedEntites, String label) {
                return GeneralSearchDialog.this.getPopupMenu(selectedEntites, label);
            }

        };
        resultsPanel.init(searchConfig);
        searchConfig.addConfigurationChangeListener(resultsPanel);
        add(resultsPanel, BorderLayout.CENTER);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        exportButton = new JButton("Export All Results To File");
        exportButton.setToolTipText("Export all the results to a tab-delimited file");
        exportButton.setEnabled(false);
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportResults();
            }
        });
        buttonPane.add(exportButton);

        buttonPane.add(Box.createHorizontalGlue());

        JLabel folderNameLabel = new JLabel("Save selected objects to folder: ");
        buttonPane.add(folderNameLabel);

        folderNameField = new JTextField(30);
        folderNameField.setToolTipText("Enter the folder name to save the results in");
        folderNameField.setMaximumSize(new Dimension(400, 25));
        buttonPane.add(folderNameField);

        JButton okButton = new JButton("Save Selected");
        okButton.setToolTipText("Save the selected rows to a folder");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveResults();
            }
        });
        buttonPane.add(okButton);

        JButton cancelButton = new JButton("Close");
        cancelButton.setToolTipText("Close this dialog without saving results");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        buttonPane.add(cancelButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    protected void init() {

        if (StringUtils.isEmpty(folderNameField.getText()) || folderNameField.getText().matches(".*Search Results #(\\d+)$")) {
            SimpleWorker worker = new SimpleWorker() {

                private String folderName;

                @Override
                protected void doStuff() throws Exception {
                    folderName = getNextFolderName();
                }

                @Override
                protected void hadSuccess() {
                    folderNameField.setText(folderName);
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };

            worker.execute();
        }

        Component browser = SessionMgr.getMainFrame();
        setPreferredSize(new Dimension((int) (browser.getWidth() * 0.8), (int) (browser.getHeight() * 0.8)));

        paramsPanel.getInputField().requestFocus();

        resultsPanel.performSearch(false, false, true);

        getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "enterAction");
        getRootPane().getActionMap().put("enterAction", new AbstractAction("enterAction") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (folderNameField.hasFocus()) {
                    saveResults();
                }
                else {
                    paramsPanel.performSearch(true);
                }
            }
        });

        packAndShow();
    }

    public void showDialog() {
        paramsPanel.setSearchRoot(null);
        init();
    }

    public void showDialog(Entity entity) {
        paramsPanel.setSearchRoot(entity);
        init();
    }

    protected SearchResultContextMenu getPopupMenu(List<Entity> selectedEntities, String label) {
        return new SearchResultContextMenu(resultsPanel, selectedEntities, label);
    }

    public void setSearchHistory(List<String> searchHistory) {
        paramsPanel.setSearchHistory(searchHistory);
    }

    public List<String> getSearchHistory() {
        return paramsPanel.getSearchHistory();
    }

    protected synchronized void saveResults() {

        final SearchResults searchResults = resultsPanel.getSearchResults();
        final DynamicTable table = searchResults.getResultTreeMapping() == null ? resultsPanel.getResultsTable() : resultsPanel.getMappedResultsTable();
        
        if (table.getRows().isEmpty()) {
            JOptionPane.showMessageDialog(GeneralSearchDialog.this, "There are no results to save");
            return;
        }
        
        if (table.getSelectedRows().isEmpty()) {
            table.getTable().getSelectionModel().setSelectionInterval(0, table.getRows().size() - 1);
        }
                 
        final String folderNameValue = folderNameField.getText();
        if (StringUtils.isEmpty(folderNameValue)) {
            JOptionPane.showMessageDialog(GeneralSearchDialog.this, "Enter a folder name or path tin which to save the results");
            return;
        }
        
        SimpleWorker worker = new SimpleWorker() {

            private RootedEntity saveFolder;

            @Override
            protected void doStuff() throws Exception {
                
                Long workspaceId = ModelMgr.getModelMgr().getCurrentWorkspaceId();
                if (workspaceId==null) throw new IllegalStateException("No workspace is selected");
                Entity workspace = ModelMgr.getModelMgr().getEntityById(workspaceId);
                
                RootedEntity searchResultsRE = new RootedEntity(workspace);
                
                int i = 0;
                for(String folderName : folderNameValue.split("/")) {
                    RootedEntity parentRE = searchResultsRE;
                    searchResultsRE = parentRE.getChildByName(folderName);
                    
                    if (searchResultsRE==null) {
                        Entity newFolder =null;
                        if (i==0) {
                            newFolder = ModelMgr.getModelMgr().createCommonRoot(folderName);
                            if (folderName.equals(EntityConstants.NAME_SEARCH_RESULTS)) {
                                ModelMgr.getModelMgr().setAttributeAsTag(newFolder, EntityConstants.ATTRIBUTE_IS_PROTECTED);
                            }
                        }
                        else {
                            newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
                            ModelMgr.getModelMgr().addEntityToParent(parentRE.getEntity(), newFolder, parentRE.getEntity().getMaxOrderIndex() + 1, EntityConstants.ATTRIBUTE_ENTITY);
                        }
                        searchResultsRE = parentRE.getChildByName(folderName);
                    }
                    i++;
                }

                if (searchResultsRE==null) {
                    throw new IllegalStateException("Could not create result folder");
                }
                
                List<Long> childIds = new ArrayList<Long>();
                for (DynamicRow row : table.getSelectedRows()) {
                    Object o = row.getUserObject();
                    Entity entity = null;
                    if (o instanceof Entity) {
                        entity = (Entity) o;
                    }
                    else if (o instanceof EntityDocument) {
                        entity = ((EntityDocument) o).getEntity();
                    }
                    else {
                        throw new IllegalStateException("Unrecognized object type: "+o.getClass().getName());
                    }
                    childIds.add(entity.getId());
                }
                
                saveFolder = FolderUtils.saveEntitiesToFolder(searchResultsRE, childIds);
            }

            @Override
            protected void hadSuccess() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, saveFolder.getUniqueId(), true);
                        setVisible(false);
                    }
                });
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(GeneralSearchDialog.this);
            }
        };

        Utils.setWaitingCursor(this);
        worker.execute();
    }

    /**
     * Looks for folders in the entity tree, and creates a new result name which does not already exist.
     *
     * @return
     */
    protected String getNextFolderName() throws Exception {
        Long workspaceId = ModelMgr.getModelMgr().getCurrentWorkspaceId();
        if (workspaceId==null) {
            return "";
        }
        
        Entity workspace = ModelMgr.getModelMgr().getEntityById(workspaceId);
        Entity searchResults = EntityUtils.findChildWithNameAndType(workspace, EntityConstants.NAME_SEARCH_RESULTS, EntityConstants.TYPE_FOLDER);
        if (searchResults==null) {
            return EntityConstants.NAME_SEARCH_RESULTS+"/Search Results #1";
        }
        
        ModelMgr.getModelMgr().loadLazyEntity(searchResults, false);
        
        int maxNum = 0;
        
        for (EntityData ed : ModelMgrUtils.getAccessibleEntityDatasWithChildren(searchResults)) {
            Entity topLevelFolder = ed.getChildEntity();
            Pattern p = Pattern.compile("^Search Results #(\\d+)$");
            Matcher m = p.matcher(topLevelFolder.getName());
            if (m.matches()) {
                String num = m.group(1);
                if (num != null && !"".equals(num)) {
                    int n = Integer.parseInt(num);
                    if (n > maxNum) {
                        maxNum = n;
                    }
                }
            }
        }
        return EntityConstants.NAME_SEARCH_RESULTS+"/Search Results #" + (maxNum + 1);
    }

    protected synchronized void exportResults() {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File Destination");
        chooser.setFileSelectionMode(FileChooser.FILES_ONLY);
        File defaultFile = new File(DEFAULT_EXPORT_DIR, "WorkstationSearchResults.xls");

        int i = 1;
        while (defaultFile.exists() && i < 10000) {
            defaultFile = new File(DEFAULT_EXPORT_DIR, "WorkstationSearchResults_" + i + ".xls");
            i++;
        }

        chooser.setSelectedFile(defaultFile);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return "Tab-delimited Files (*.xls, *.txt)";
            }

            @Override
            public boolean accept(File f) {
                return !f.isDirectory();
            }
        });

        if (chooser.showDialog(GeneralSearchDialog.this, "OK") == FileChooser.CANCEL_OPTION) {
            return;
        }

        final String destFile = chooser.getSelectedFile().getAbsolutePath();
        if ((destFile == null) || destFile.equals("")) {
            return;
        }

        final SolrQueryBuilder builder = resultsPanel.getQueryBuilder(false);
        final ResultTreeMapping projection = resultsPanel.getSearchResults().getResultTreeMapping();

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                FileWriter writer = new FileWriter(destFile);

                StringBuffer buf = new StringBuffer();
                for (DynamicColumn column : resultsPanel.getResultsTable().getDisplayedColumns()) {
                    if (buf.length() > 0) {
                        buf.append("\t");
                    }
                    buf.append(column.getLabel());
                }
                if (projection != null) {
                    for (DynamicColumn column : resultsPanel.getMappedResultsTable().getDisplayedColumns()) {
                        buf.append("\t");
                        buf.append(column.getLabel());
                    }
                }
                buf.append("\n");
                writer.write(buf.toString());

                SearchResults searchResults = new SearchResults();

                long numProcessed = 0;
                int page = 0;
                while (true) {
                    SolrResults results = resultsPanel.performSearch(builder, page, EXPORT_PAGE_SIZE);
                    long numFound = results.getResponse().getResults().getNumFound();

                    ResultPage resultPage = new ResultPage(results);
                    searchResults.addPage(resultPage);

                    if (projection != null) {
                        resultPage.projectResults(projection, searchResults);
                    }

                    for (EntityDocument entityDoc : results.getEntityDocuments()) {

                        List<Entity> mappedDocs = null;
                        if (projection != null) {
                            mappedDocs = resultPage.getMappedEntities(entityDoc.getEntity().getId());

                            if (mappedDocs.isEmpty()) {
                                buf = new StringBuffer();
                                int i = 0;
                                for (DynamicColumn column : resultsPanel.getResultsTable().getDisplayedColumns()) {
                                    Object value = searchConfig.getValue(entityDoc, column.getName());
                                    if (i++ > 0) {
                                        buf.append("\t");
                                    }
                                    if (value != null) {
                                        buf.append(value.toString());
                                    }

                                }
                                buf.append("\n");
                                writer.write(buf.toString());
                            }
                            else {
                                for (Entity mappedDoc : mappedDocs) {

                                    buf = new StringBuffer();
                                    int i = 0;
                                    for (DynamicColumn column : resultsPanel.getResultsTable().getDisplayedColumns()) {
                                        Object value = searchConfig.getValue(entityDoc, column.getName());
                                        if (i++ > 0) {
                                            buf.append("\t");
                                        }
                                        if (value != null) {
                                            buf.append(value.toString());
                                        }

                                    }

                                    if (projection != null) {
                                        for (DynamicColumn column : resultsPanel.getMappedResultsTable().getDisplayedColumns()) {
                                            Object value = searchConfig.getValue(mappedDoc, column.getName());
                                            buf.append("\t");
                                            if (value != null) {
                                                buf.append(value.toString());
                                            }
                                        }
                                    }

                                    buf.append("\n");
                                    writer.write(buf.toString());
                                }
                            }
                        }
                        else {
                            buf = new StringBuffer();
                            int i = 0;
                            for (DynamicColumn column : resultsPanel.getResultsTable().getDisplayedColumns()) {
                                Object value = searchConfig.getValue(entityDoc, column.getName());
                                if (i++ > 0) {
                                    buf.append("\t");
                                }
                                if (value != null) {
                                    buf.append(value.toString());
                                }

                            }
                            buf.append("\n");
                            writer.write(buf.toString());
                        }

                        numProcessed++;
                        setProgress((int) numProcessed, (int) numFound);
                    }

                    if (numProcessed >= numFound) {
                        break;
                    }
                    page++;
                }
                writer.close();
            }

            @Override
            protected void hadSuccess() {
                int rv = JOptionPane.showConfirmDialog(GeneralSearchDialog.this, "Data was successfully exported to " + destFile + ". Open file in default viewer?",
                        "Export successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (rv == JOptionPane.YES_OPTION) {
                    OpenWithDefaultAppAction openAction = new OpenWithDefaultAppAction(destFile);
                    openAction.doAction();
                }
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.setProgressMonitor(new ProgressMonitor(GeneralSearchDialog.this, "Exporting data", "", 0, 100));
        worker.execute();
    }
}
