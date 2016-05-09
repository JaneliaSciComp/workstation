package org.janelia.it.workstation.gui.browser.gui.dialogs;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import net.miginfocom.swing.MigLayout;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.gui.support.DownloadItem;
import org.janelia.it.workstation.gui.browser.gui.support.DropDownButton;
import org.janelia.it.workstation.gui.browser.gui.support.FileDownloadWorker;
import org.janelia.it.workstation.gui.browser.gui.support.ResultSelectionButton;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchConfiguration;
import org.janelia.it.workstation.gui.browser.model.search.SolrSearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A dialog for exporting data. Supports exporting hierarchies of files in various ways. 
 *
 * TODO: keep track of user's favorite file naming patterns
 * TODO: better progress indication for loading large file sets
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DownloadDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(DownloadDialog.class);
    
    // Constants
    private static final Lock COPY_FILE_LOCK = new ReentrantLock();
    
    private static final Font SEPARATOR_FONT = new Font("Sans Serif", Font.BOLD, 12);
    
    private static final String ITEM_TYPE_SELF = "Selected Items";
    
    private static final String ITEM_TYPE_LSM = "LSMs";

    private static final String FILE_PATTERN_HELP =
            "<html><font color='#959595' size='-1'>The naming pattern can use any attribute on the items being downloaded.<br>"
            + "It may also use the following special attributes: {Sample Name}, {Result Name}, {File Name}, {Extension}<br>"
            + "Each attribute may include multiple names as a fallback, e.g.: {Fly Core Alias|Line}"
            + "</font></html>";

    private static final String NATIVE_EXTENSION = "No conversion";

    private static final String[] FORMAT_EXTENSIONS = {
            NATIVE_EXTENSION,
            "lsm.bz2", 
            "lsm", 
            "tif", 
            "v3draw", 
            "v3dpbd", 
            "mp4", 
            "h5j"
    };
    
    private static final String[] STANDARD_FILE_PATTERNS = {
            "{Sample Name}/{Result Name|\"Image\"}-{File Name}", 
            "{Line}/{Sample Name}.{Extension}"
    };
    private static final String FILE_PATTERN_PROP_NAME = "LAST_USER_FILE_PATTERN";

    // GUI
    private final JLabel loadingLabel;
    private final JButton cancelButton;
    private final JButton okButton;
    private final JPanel buttonPane;
    private JPanel attrPanel;
    private JList<String> expandedObjectList;
    private JLabel expandedObjectCountLabel;
    private ResultSelectionButton resultButton;

    private JComboBox<Format> formatCombo;
    private JCheckBox splitChannelCheckbox;
    private JCheckBox flattenStructureCheckbox;
    private JComboBox<String> filePatternCombo;
    private JLabel downloadItemCountLabel;
    private JList<DownloadItem> downloadItemList;

    // State
    private String currItemsToExport = ITEM_TYPE_SELF;
    private ResultDescriptor defaultResultDescriptor;
    private List<? extends DomainObject> inputObjects;
    boolean onlySampleInputs;
    private List<DomainObject> expandedObjects;
    private List<DownloadItem> downloadItems;
    private boolean listening;
    
    public DownloadDialog() {
    	super(SessionMgr.getMainFrame());
        setTitle("File Download");
        
        loadingLabel = new JLabel(Icons.getLoadingIcon());
        add(loadingLabel, BorderLayout.CENTER);
        
        cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel the export and close this window");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        okButton = new JButton("Export");
        okButton.setToolTipText("Queue export");
        okButton.setEnabled(false);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });

        buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    private void addSeparator(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(SEPARATOR_FONT);
        panel.add(label, "split 2, span, gaptop 10lp, ay top");
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "wrap, gaptop 22lp, grow");
    }

    private void addField(String label, JComponent component) {
        addField(label, component, "");
    }
    
    private void addField(String label, JComponent component, String constraints) {
        JLabel attrLabel = new JLabel(label);
        attrLabel.setLabelFor(component);
        attrPanel.add(attrLabel,"gap para, ay top");
        String compConstraints = "gap para, ay top";
        if (!StringUtils.isEmpty(constraints)) {
            compConstraints += ", "+constraints;
        }
        attrPanel.add(component,compConstraints);
    }
    
    public void showDialog(final List<? extends DomainObject> domainObjects, final ResultDescriptor defaultResultDescriptor) {

        this.inputObjects = domainObjects;
        this.defaultResultDescriptor = defaultResultDescriptor;
        this.expandedObjects = new ArrayList<>();
        this.downloadItems = new ArrayList<>();
        findObjectsToExport(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                populateUI();
                populateExpandedObjectList();
                return null;
            }
        });
        Component mainFrame = SessionMgr.getMainFrame();
        setPreferredSize(new Dimension((int) (mainFrame.getWidth() * 0.3), (int) (mainFrame.getHeight() * 0.3)));
        packAndShow();
    }
    
    private void findObjectsToExport(final Callable<Void> success) {

        log.info("findObjectsToExport(inputObjects.size={})",inputObjects.size());

        Utils.setWaitingCursor(DownloadDialog.this);
        
    	// Reset state that will be populated below
    	onlySampleInputs = true;
    	downloadItems.clear();
    	expandedObjects.clear();
    	
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                for(DomainObject domainObject : inputObjects) {
                    addObjectsToExport(new ArrayList<String>(), domainObject);
                }
                for(DownloadItem downloadItem : downloadItems) {
                    expandedObjects.add(downloadItem.getDomainObject());
                }
                log.info("Found {} objects to export",downloadItems.size());
            }

            @Override
            protected void hadSuccess() {
            	Utils.setDefaultCursor(DownloadDialog.this);
                ConcurrentUtils.invokeAndHandleExceptions(success);
            }

            @Override
            protected void hadError(Throwable error) {
            	Utils.setDefaultCursor(DownloadDialog.this);
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }
    
    private void addObjectsToExport(List<String> path, DomainObject domainObject) {
        // TODO: this should update some kind of label so the user knows what's going on during a long load
        log.info("addObjectsToExport({},{})",path,domainObject.getName());
        if (domainObject instanceof TreeNode) {
            TreeNode treeNode = (TreeNode)domainObject;
            if (treeNode.hasChildren()) {
                List<Reference> childRefs = treeNode.getChildren();
                List<DomainObject> children = DomainMgr.getDomainMgr().getModel().getDomainObjects(childRefs);
                List<String> childPath = new ArrayList<>(path);
                childPath.add(domainObject.getName());
                for(DomainObject child : children) {
                    addObjectsToExport(childPath, child);
                }
            }
        }
        else if (domainObject instanceof TreeNode) {
            TreeNode treeNode = (TreeNode)domainObject;
            List<DomainObject> children = DomainMgr.getDomainMgr().getModel().getDomainObjects(treeNode.getChildren());
            List<String> childPath = new ArrayList<>(path);
            childPath.add(domainObject.getName());
            for(DomainObject child : children) {
                addObjectsToExport(childPath, child);
            }
        }
        else if (domainObject instanceof Filter) {
            Filter filter = (Filter)domainObject;
            try {
                SearchConfiguration config = new SearchConfiguration(filter, 1000);
                SolrSearchResults searchResults = config.performSearch();
                searchResults.loadAllResults();
                for(ResultPage page : searchResults.getPages()) {
                    List<String> childPath = new ArrayList<>(path);
                    childPath.add(domainObject.getName());
                    for(DomainObject resultObject : page.getDomainObjects()) {
                        addObjectsToExport(childPath, resultObject);
                    }
                }
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
        }
        else {
            if (domainObject instanceof Sample) {
                if (currItemsToExport.equals(ITEM_TYPE_LSM)) {
                    for(LSMImage lsm : DomainMgr.getDomainMgr().getModel().getLsmsForSample((Sample)domainObject)) {
                        log.info("Adding expanded LSM: "+lsm.getName());
                        downloadItems.add(new DownloadItem(path, lsm));
                    }
                }
                else {
                    log.info("Adding Sample: "+domainObject.getName());
                    downloadItems.add(new DownloadItem(path, domainObject));
                }
            }
            else {
                onlySampleInputs = false;
                log.info("Not just Samples. Adding "+domainObject.getName());
                downloadItems.add(new DownloadItem(path, domainObject));
            }
        }
    }
    
    private void populateUI() {

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));

        addSeparator(attrPanel, "Items to Export");
        
        DefaultComboBoxModel<String> etmodel = new DefaultComboBoxModel<>();
        etmodel.setSelectedItem(currItemsToExport);
        etmodel.addElement(ITEM_TYPE_SELF);
        if (onlySampleInputs) {
            etmodel.addElement(ITEM_TYPE_LSM);
        }
        
        final JComboBox<String> exportTypeCombo = new JComboBox<>(etmodel);
        exportTypeCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange()==ItemEvent.SELECTED) {
                    currItemsToExport = (String)exportTypeCombo.getSelectedItem();
                    findObjectsToExport(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            updateResultCombo();
                            populateExpandedObjectList();
                            return null;
                        }
                    });
                }
            }
        });
        
        addField("Select type:", exportTypeCombo, "width 100:150:200, grow");
        
        expandedObjectCountLabel = new JLabel();
        addField("Item count:", expandedObjectCountLabel);
        
        expandedObjectList = new JList<>(new DefaultListModel<String>());
        expandedObjectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        expandedObjectList.setLayoutOrientation(JList.VERTICAL);
        addField("Preview items:", new JScrollPane(expandedObjectList), "width 200:300:600, height 30:100:200, grow");
        
        resultButton = new ResultSelectionButton(onlySampleInputs) {
            protected void resultChanged(ResultDescriptor resultDescriptor) {
                populateDownloadItemList();
                populateExtensions();
            }
        };
        resultButton.setResultDescriptor(defaultResultDescriptor);
        resultButton.populate(expandedObjects);
        resultButton.setVisible(true); // Show even if there is nothing to select. We'll just make it disabled.
        addField("Select result:", resultButton);
        
        updateResultCombo();
        
        addSeparator(attrPanel, "File Processing");
        
        formatCombo = new JComboBox<>();
        formatCombo.setEditable(false);
        formatCombo.setToolTipText("Choose an export format");
        addField("Output file format:", formatCombo);
        
        splitChannelCheckbox = new JCheckBox();
        addField("Split channels?", splitChannelCheckbox);
        
        addSeparator(attrPanel, "File Naming");
        
        flattenStructureCheckbox = new JCheckBox();
        addField("Flatten folder structure?", flattenStructureCheckbox);

        filePatternCombo = new JComboBox<>();
        filePatternCombo.setEditable(true);
        filePatternCombo.setToolTipText("Select a standard file naming pattern, or enter your own.");
        DefaultComboBoxModel<String> fpmodel = (DefaultComboBoxModel) filePatternCombo.getModel();
        String userFilePattern = (String)SessionMgr.getSessionMgr().getModelProperty(FILE_PATTERN_PROP_NAME);
        if (userFilePattern!=null) {
            fpmodel.addElement(userFilePattern);
        }
        for (String pattern : STANDARD_FILE_PATTERNS) {
            fpmodel.addElement(pattern);
        }

        addField("Naming pattern:", filePatternCombo, "width 200:300:600, grow");
        
        attrPanel.add(new JLabel(""), "gap para, aligny top");
        attrPanel.add(new JLabel(FILE_PATTERN_HELP), "gap para, width 200:800:1000, grow, ay top");

        downloadItemCountLabel = new JLabel();
        addField("File count:", downloadItemCountLabel);
        
        downloadItemList = new JList<>(new DefaultListModel<DownloadItem>());
        downloadItemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        downloadItemList.setLayoutOrientation(JList.VERTICAL);
        addField("Preview files:", new JScrollPane(downloadItemList), "width 200:800:1000, height 80:300:1000, grow");
        
        populateDownloadItemList();

        // Add change listeners
        formatCombo.addItemListener(changeListener);
        splitChannelCheckbox.addItemListener(changeListener);
        flattenStructureCheckbox.addItemListener(changeListener);
        filePatternCombo.addItemListener(changeListener);
        
        remove(loadingLabel);
        add(attrPanel, BorderLayout.CENTER);
        
        okButton.setEnabled(true);
        setPreferredSize(null);
        packAndShow();
    }

    private void updateResultCombo() {
    	resultButton.setEnabled(onlySampleInputs && currItemsToExport.equals(ITEM_TYPE_SELF));
    }
    
    // If anything changes, we need to recalculate the file download list
    ItemListener changeListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (listening) {
                if (e.getStateChange() == ItemEvent.SELECTED || e.getSource() instanceof JCheckBox) {
                    log.info("Item state changed: {}", e);
                    populateDownloadItemList();
                }
            }
            else {
                log.trace("Ignoring item state change: {}", e);
            }
        }
    };
    
    private void resumeListeners() {
    	listening = true;
    }
    
    private void stopListeners() {
    	listening = false;
    }
    
    private void populateExpandedObjectList() {

        stopListeners();
        
        DefaultListModel<String> eolm = (DefaultListModel)expandedObjectList.getModel();
        eolm.removeAllElements();
        for(DomainObject domainObject : expandedObjects) {
            log.info("Adding expanded object to list: "+domainObject.getName());
            eolm.addElement(domainObject.getName());
        }

        expandedObjectCountLabel.setText(expandedObjects.size()+" items");
        
        resumeListeners();
        populateDownloadItemList();
    }
    
    private void populateDownloadItemList() {

        stopListeners();
        
        final ResultDescriptor resultDescriptor = resultButton.getResultDescriptor();
        final boolean splitChannels = splitChannelCheckbox.isSelected();
        final boolean flattenStructure = flattenStructureCheckbox.isSelected();
        final String filenamePattern = (String)filePatternCombo.getSelectedItem();

        final Format format = (Format)formatCombo.getSelectedItem();
        String extension = null;
        if (format!=null && !format.getExtension().equals(NATIVE_EXTENSION)) {
            extension = format.getExtension();
        }
        final String finalExtension = extension;

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                for (DownloadItem downloadItem : downloadItems) {
                    downloadItem.init(resultDescriptor, finalExtension, splitChannels, flattenStructure, filenamePattern);
                }
            }

            @Override
            protected void hadSuccess() {

                int count = 0;
                DefaultListModel<DownloadItem> dlm = (DefaultListModel) downloadItemList.getModel();
                dlm.removeAllElements();
                for (DownloadItem downloadItem : downloadItems) {
                    count += downloadItem.getSourceFile()!=null ? 1 : 0;
                    dlm.addElement(downloadItem);
                }
                
                downloadItemCountLabel.setText(count+" files");
                
                resumeListeners();
                populateExtensions();
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                resumeListeners();
                populateExtensions();
            }
        };

        worker.execute();
    }
    
    private void populateExtensions() {
        
        stopListeners();
        
        boolean allLsms = true;
        final Multiset<String> countedExtensions = LinkedHashMultiset.create();
        for(DownloadItem downloadItem : downloadItems) {
            String extension = downloadItem.getSourceExtension();
            if (!(downloadItem.getDomainObject() instanceof LSMImage)) {
                allLsms = false;
            }
            countedExtensions.add(extension);
        }
        
        List<String> sortedExtensions = new ArrayList<>(countedExtensions.elementSet());
        Collections.sort(sortedExtensions, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                Integer i1 = countedExtensions.count(o1);
                Integer i2 = countedExtensions.count(o2);
                return i2.compareTo(i1);
            }
        });
        
        for(String extension : sortedExtensions) {
            log.trace("Extension '{}' has {} instances", extension, countedExtensions.count(extension));
        }

        DefaultComboBoxModel<Format> model = (DefaultComboBoxModel) formatCombo.getModel();
        Format currValue = (Format)model.getSelectedItem();
        
        model.removeAllElements();
        for (String extension : FORMAT_EXTENSIONS) {
            if (!allLsms && extension.startsWith("lsm")) {
                continue;
            }
            Format format = new Format(extension, false);
            model.addElement(format);
            if (extension.equals(NATIVE_EXTENSION)) {
                model.setSelectedItem(format);
            }
        }
        
        if (currValue!=null) {
            model.setSelectedItem(currValue);
        }
        
        resumeListeners();
    }
    
    private void saveAndClose() {

        String filePattern = (String)filePatternCombo.getSelectedItem();
        boolean found = false;
        for (String pattern : STANDARD_FILE_PATTERNS) {
            if (pattern.equals(filePattern)) {
                found = true;
                break;
            }
        }
        if (!found) {
            SessionMgr.getSessionMgr().setModelProperty(FILE_PATTERN_PROP_NAME, filePattern);
        }

        boolean started = false;
        for(final DownloadItem downloadItem : downloadItems) {
            if (downloadItem.getSourceFile()!=null) {
                FileDownloadWorker worker = new FileDownloadWorker(downloadItem, COPY_FILE_LOCK);
                worker.startDownload();
                started = true;
            }
        }

        if (!started) {
            JOptionPane.showMessageDialog(this, "There are no downloads to start.", "Nothing to do", JOptionPane.PLAIN_MESSAGE);
        }
        else {
            setVisible(false);
        }
    }
    
    private class Format {
        
        private final String extension;
        private final boolean isNative;
        
        public Format(String extension, boolean isNative) {
            this.extension = extension;
            this.isNative = isNative;
        }

        public String getExtension() {
            return extension;
        }

        @Override
        public String toString() {
            //return extension+(isNative?"":" (Convert)");
            return extension;
        }
    }
    
}
