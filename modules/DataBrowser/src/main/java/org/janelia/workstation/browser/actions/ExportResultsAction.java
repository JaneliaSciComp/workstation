package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileFilter;

import org.janelia.model.domain.Reference;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.it.jacs.shared.file_chooser.FileChooser;
import org.janelia.it.jacs.shared.utils.Progress;
import org.janelia.workstation.common.gui.table.DynamicColumn;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.workstation.core.model.search.SearchResults;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.browser.gui.listview.table.TableViewerPanel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Export a table to TAB or CSV format.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExportResultsAction<T,S> extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(ExportResultsAction.class);
    
    /**
     * Default directory for exports
     */
    protected static final String DEFAULT_EXPORT_DIR = System.getProperty("user.home");
    
    private SearchResults<T,S> searchResults;
    private TableViewerPanel<T,S> tableViewer;

    public ExportResultsAction(SearchResults<T,S> searchResults, TableViewerPanel<T,S> tableViewer) {
        super("Export table");
        this.searchResults = searchResults;
        this.tableViewer = tableViewer;
        // TODO: what to do if tableViewer is null? 
        // We could still export all the attributes on each object.
        if (tableViewer==null) {
            throw new IllegalStateException("Export is currently only allowed from table view");
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {

        ActivityLogHelper.logUserAction("ExportResultsAction.doAction");

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File Destination");
        chooser.setFileSelectionMode(FileChooser.FILES_ONLY);
        File defaultFile = Utils.getOutputFile(DEFAULT_EXPORT_DIR, "WorkstationSearchResults", "xls");

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

        if (chooser.showDialog(FrameworkAccess.getMainFrame(), "OK") == FileChooser.CANCEL_OPTION) {
            return;
        }

        final String destFile = chooser.getSelectedFile().getAbsolutePath();
        if ((destFile == null) || destFile.equals("")) {
            return;
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                export(destFile, this);
            }

            @Override
            protected void hadSuccess() {
                int rv = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(), "Data was successfully exported to " + destFile + ". Open file in default viewer?",
                        "Export successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (rv == JOptionPane.YES_OPTION) {
                    OpenWithDefaultAppAction openAction = new OpenWithDefaultAppAction(destFile);
                    openAction.actionPerformed(null);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.setProgressMonitor(new ProgressMonitor(FrameworkAccess.getMainFrame(), "Exporting data", "", 0, 100));
        worker.execute();
    }
    
    private void export(String destFile, Progress progress) throws Exception {

        long numFound = searchResults.getNumTotalResults();

        try (FileWriter writer = new FileWriter(destFile)) {
    
            StringBuffer buf = new StringBuffer();
            for (DynamicColumn column : tableViewer.getColumns()) {
                if (buf.length() > 0) {
                    buf.append("\t");
                }
                buf.append(column.getLabel());
            }
            buf.append("\n");
            writer.write(buf.toString());
    
    
            long numProcessed = 0;
            int page = 0;
            while (true) {
                ResultPage<T, S> resultPage = searchResults.getPage(page);
    
                if (resultPage==null) {
                    log.warn("Could not retrieve result page {}. Ending export.", page);
                    break;
                }
                
                for (T object : resultPage.getObjects()) {
    
                    buf = new StringBuffer();
                    int i = 0;
                    for (DynamicColumn column : tableViewer.getColumns()) {
                        Object value = tableViewer.getValue(resultPage, object, column.getName());
                        if (i++ > 0) {
                            buf.append("\t");
                        }
                        if (value != null) {
                            if (column.getName().equals("id")) {
                                // Excel truncates longs, so we prefix the type to force it into string mode
                                String type = (String)tableViewer.getValue(resultPage, object, "type");
                                Reference ref = Reference.createFor(type, Long.parseLong(value.toString()));
                                buf.append(ref.toString());
                            }
                            else {
                                buf.append(sanitize(value.toString()));
                            }
                        }
    
                    }
                    buf.append("\n");
                    writer.write(buf.toString());
                    numProcessed++;
                    progress.setProgress((int) numProcessed, (int) numFound);
                }
    
                if (numProcessed >= numFound) {
                    break;
                }
                page++;
            }
        }
    }
    
    /**
     * Sanitize a string value before exporting to tab-delimited format.
     * @param s
     * @return
     */
    private String sanitize(String s) {
        // Replace all whitespace with a single space. This gets rid of newlines and tabs which will mess up the tab-delimited format.
        return s.replaceAll("\\s+", " ");
    }
}
