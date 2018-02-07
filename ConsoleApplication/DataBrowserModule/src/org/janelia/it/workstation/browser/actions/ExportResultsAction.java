package org.janelia.it.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileFilter;

import org.janelia.it.jacs.shared.file_chooser.FileChooser;
import org.janelia.it.jacs.shared.utils.Progress;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.it.workstation.browser.gui.table.DynamicColumn;
import org.janelia.it.workstation.browser.model.search.DomainObjectResultPage;
import org.janelia.it.workstation.browser.model.search.DomainObjectSearchResults;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.DomainObject;

/**
 * Export a table to TAB or CSV format.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExportResultsAction<T> extends AbstractAction {

    /**
     * Default directory for exports
     */
    protected static final String DEFAULT_EXPORT_DIR = System.getProperty("user.home");
    
    private DomainObjectSearchResults searchResults;
    private DomainObjectTableViewer domainObjectTableViewer;

    public ExportResultsAction(DomainObjectSearchResults searchResults, DomainObjectTableViewer domainObjectTableViewer) {
        super("Export table");
        this.searchResults = searchResults;
        this.domainObjectTableViewer = domainObjectTableViewer;
        // TODO: what to do if domainObjectTableViewer is null? 
        // We could still export all the attributes on each object.
        if (domainObjectTableViewer==null) {
            throw new IllegalStateException("Export is currently only allowed from table view");
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {

        ActivityLogHelper.logUserAction("ExportResultsAction.doAction");

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

        if (chooser.showDialog(ConsoleApp.getMainFrame(), "OK") == FileChooser.CANCEL_OPTION) {
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
                int rv = JOptionPane.showConfirmDialog(ConsoleApp.getMainFrame(), "Data was successfully exported to " + destFile + ". Open file in default viewer?",
                        "Export successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (rv == JOptionPane.YES_OPTION) {
                    OpenWithDefaultAppAction openAction = new OpenWithDefaultAppAction(destFile);
                    openAction.actionPerformed(null);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.setProgressMonitor(new ProgressMonitor(ConsoleApp.getMainFrame(), "Exporting data", "", 0, 100));
        worker.execute();
    }
    
    private void export(String destFile, Progress progress) throws Exception {

        long numFound = searchResults.getNumTotalResults();

        FileWriter writer = new FileWriter(destFile);

        StringBuffer buf = new StringBuffer();
        for (DynamicColumn column : domainObjectTableViewer.getColumns()) {
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
            DomainObjectResultPage resultPage = searchResults.getPage(page);

            for (DomainObject domainObject : resultPage.getObjects()) {

                buf = new StringBuffer();
                int i = 0;
                for (DynamicColumn column : domainObjectTableViewer.getColumns()) {
                    Object value = domainObjectTableViewer.getValue(resultPage, domainObject, column.getName());
                    if (i++ > 0) {
                        buf.append("\t");
                    }
                    if (value != null) {
                        buf.append(sanitize(value.toString()));
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
        writer.close();
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
