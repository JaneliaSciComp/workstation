package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.gui.support.DownloadItem;
import org.janelia.it.workstation.gui.browser.gui.support.FileDownloadWorker;
import org.janelia.it.workstation.gui.browser.gui.support.ResultSelectionButton;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchConfiguration;
import org.janelia.it.workstation.gui.browser.model.search.SolrSearchResults;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

import net.miginfocom.swing.MigLayout;

/**
 * A dialog for exporting data. Supports exporting hierarchies of files in various ways. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileExportDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadWorker.class);
    
    // Constants
    private static final Lock COPY_FILE_LOCK = new ReentrantLock();
    private static final Font SEPARATOR_FONT = new Font("Sans Serif", Font.BOLD, 12);
    private static final String ITEM_TYPE_SELF = "Selected Items";
    private static final String ITEM_TYPE_LSM = "LSMs";
    
    private static final String[] FORMAT_EXTENSIONS = {
            "lsm.bz2", 
            "lsm", 
            "tif", 
            "v3draw", 
            "v3dpbd", 
            "mp4", 
            "h5j"
    };
    
    private static final String[] STANDARD_FILE_PATTERNS = {
            "{Sample Name}/{Result Name}-{File Name}", 
            "{Line}/{Sample Name}.{Extension}"
    };

    // GUI
    private final JLabel loadingLabel;
    private final JButton cancelButton;
    private final JButton okButton;
    private final JPanel buttonPane;
    private JPanel attrPanel;
    private JList<String> expandedObjectList;
    private ResultSelectionButton resultButton;
    private JComboBox<Format> formatCombo;
    private JCheckBox splitChannelCheckbox;
    private JCheckBox flattenStructureCheckbox;
    private JComboBox<String> filePatternCombo;
    private JList<DownloadItem> downloadItemList;

    // State
    private String currItemsToExport = ITEM_TYPE_SELF;
    private ResultDescriptor defaultResultDescriptor;
    private List<DomainObject> inputObjects;
    boolean onlySampleInputs = true;
    private List<DomainObject> expandedObjects;
    private List<DownloadItem> downloadItems;
    
    public FileExportDialog() {
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
        panel.add(label, "split 2, span, gaptop 10lp, aligny top");
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "gaptop 10lp, grow, wrap");
    }

    private void addField(String label, JComponent component) {
        addField(label, component, "");
    }
    
    private void addField(String label, JComponent component, String constraints) {
        JLabel attrLabel = new JLabel(label);
        attrLabel.setLabelFor(component);
        attrPanel.add(attrLabel,"gap para, aligny top");
        String compConstraints = "gap para, aligny top";
        if (!StringUtils.isEmpty(constraints)) {
            compConstraints += ", "+constraints;
        }
        attrPanel.add(component,compConstraints);
    }
    
    public void showDialog(final List<DomainObject> domainObjects, final ResultDescriptor defaultResultDescriptor) {

        this.inputObjects = domainObjects;
        this.expandedObjects = new ArrayList<>();
        this.defaultResultDescriptor = defaultResultDescriptor;
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

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                for(DomainObject domainObject : inputObjects) {
                    addObjectsToExport(domainObject, new ArrayList<String>());
                }
                for(DownloadItem downloadItem : downloadItems) {
                    expandedObjects.add(downloadItem.getDomainObject());
                }
                log.info("Found {} objects to export",downloadItems.size());
            }

            @Override
            protected void hadSuccess() {
                ConcurrentUtils.invokeAndHandleExceptions(success);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }
    
    private void addObjectsToExport(DomainObject domainObject, List<String> path) {
        // TODO: this should update some kind of label so the user knows what's going on during a long load
        if (domainObject instanceof TreeNode) {
            TreeNode treeNode = (TreeNode)domainObject;
            if (treeNode.hasChildren()) {
                List<Reference> childRefs = treeNode.getChildren();
                List<DomainObject> children = DomainMgr.getDomainMgr().getModel().getDomainObjects(childRefs);
                List<String> childPath = new ArrayList<>(path);
                childPath.add(domainObject.getName());
                for(DomainObject child : children) {
                    addObjectsToExport(child, childPath);
                }
            }
        }
        else if (domainObject instanceof ObjectSet) {
            ObjectSet objectSet = (ObjectSet)domainObject;
            List<DomainObject> children = DomainMgr.getDomainMgr().getModel().getDomainObjects(objectSet.getClassName(), objectSet.getMembers());
            List<String> childPath = new ArrayList<>(path);
            childPath.add(domainObject.getName());
            for(DomainObject child : children) {
                addObjectsToExport(child, childPath);
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
                        addObjectsToExport(resultObject, childPath);
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
                    for(LSMImage lsm : DomainMgr.getDomainMgr().getModel().getLsmsForSample(domainObject.getId())) {
                        downloadItems.add(new DownloadItem(path, lsm));
                    }
                }
                else {
                    downloadItems.add(new DownloadItem(path, domainObject));
                }
            }
            else {
                onlySampleInputs = false;
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
                currItemsToExport = (String)exportTypeCombo.getSelectedItem();
                findObjectsToExport(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        populateExpandedObjectList();
                        return null;
                    }
                });
            }
        });
        
        addField("What To Export?", new JScrollPane(exportTypeCombo), "width 100:150:200, grow");
        
        expandedObjectList = new JList<>(new DefaultListModel<String>());
        expandedObjectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        expandedObjectList.setLayoutOrientation(JList.VERTICAL);
        addField("Items To Export", new JScrollPane(expandedObjectList), "width 200:300:600, height 30:100:200, grow");
        
        resultButton = new ResultSelectionButton(onlySampleInputs) {
            protected void resultChanged(ResultDescriptor resultDescriptor) {
                populateDownloadItemList();
                populateExtensions();
            }
        };
        resultButton.setResultDescriptor(defaultResultDescriptor);
        resultButton.populate(expandedObjects);
        addField("Result", resultButton);
        
        addSeparator(attrPanel, "Format");
        
        
        formatCombo = new JComboBox<>();
        formatCombo.setEditable(false);
        formatCombo.setToolTipText("Choose an export format");
        addField("File Format", formatCombo);      
        
        splitChannelCheckbox = new JCheckBox();
        addField("Split Channels", splitChannelCheckbox);
        
        flattenStructureCheckbox = new JCheckBox();
        addField("Flatten Folder Structure", flattenStructureCheckbox);
        
        addSeparator(attrPanel, "File Names");

        filePatternCombo = new JComboBox<>();
        filePatternCombo.setEditable(true);
        filePatternCombo.setToolTipText("Substitution pattern for file naming");
        DefaultComboBoxModel<String> fpmodel = (DefaultComboBoxModel) filePatternCombo.getModel();
        for (String pattern : STANDARD_FILE_PATTERNS) {
            fpmodel.addElement(pattern);
        }
        
        addField("File Name Pattern", filePatternCombo, "width 200:300:600, grow");

        addSeparator(attrPanel, "Files To Export");
        
        downloadItemList = new JList<>(new DefaultListModel<DownloadItem>());
        downloadItemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        downloadItemList.setLayoutOrientation(JList.VERTICAL);
        addField("File Paths", new JScrollPane(downloadItemList), "width 600:800:1000, height 100:150:200, grow");
        
        populateDownloadItemList();
        addListeners();
        
        remove(loadingLabel);
        add(attrPanel, BorderLayout.CENTER);
        
        okButton.setEnabled(true);
        setPreferredSize(null);
        pack();
    }

    // If anything changes, we need to recalculate the file download list
    ItemListener changeListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            populateDownloadItemList();
        }
    };
    
    private void addListeners() {
        formatCombo.addItemListener(changeListener);
        splitChannelCheckbox.addItemListener(changeListener);
        flattenStructureCheckbox.addItemListener(changeListener);
        filePatternCombo.addItemListener(changeListener);
        
    }
    
    private void removeListeners() {
        formatCombo.removeItemListener(changeListener);
        splitChannelCheckbox.removeItemListener(changeListener);
        flattenStructureCheckbox.removeItemListener(changeListener);
        filePatternCombo.removeItemListener(changeListener);
    }
    
    private void populateExpandedObjectList() {

        removeListeners();
        
        DefaultListModel<String> eolm = (DefaultListModel)expandedObjectList.getModel();
        eolm.removeAllElements();
        for(DomainObject domainObject : expandedObjects) {
            eolm.addElement(domainObject.getName());
        }

        addListeners();
        populateDownloadItemList();
    }
    
    private void populateDownloadItemList() {

        removeListeners();
        
        ResultDescriptor resultDescriptor = resultButton.getResultDescriptor();
        Format format = (Format)formatCombo.getSelectedItem();
        String extension = format==null?null:format.getExtension();
        boolean splitChannels = splitChannelCheckbox.isSelected();
        boolean flattenStructure = flattenStructureCheckbox.isSelected();
        String filenamePattern = (String)filePatternCombo.getSelectedItem();
        
        for(DownloadItem downloadItem : downloadItems) {
            downloadItem.init(resultDescriptor, extension, splitChannels, flattenStructure, filenamePattern);
        }
        
        DefaultListModel<DownloadItem> dlm = (DefaultListModel)downloadItemList.getModel();
        dlm.removeAllElements();
        for(DownloadItem downloadItem : downloadItems) {
            dlm.addElement(downloadItem);
        }

        addListeners();
        populateExtensions();
    }
    
    private void populateExtensions() {
        
        removeListeners();
        
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
        
        String maxCountExtension = sortedExtensions.get(0);

        DefaultComboBoxModel<Format> model = (DefaultComboBoxModel) formatCombo.getModel();
        model.removeAllElements();
        Format currValue = (Format)model.getSelectedItem();
                
        for (String extension : FORMAT_EXTENSIONS) {
            if (!allLsms && extension.startsWith("lsm")) {
                continue;
            }
            Format format = new Format(extension, extension.equals(maxCountExtension));
            model.addElement(format);
            
            if (extension.equals(maxCountExtension)) {
                model.setSelectedItem(format);
            }
        }
        
        if (currValue!=null) {
            model.setSelectedItem(currValue);
        }
        
        addListeners();
    }
    
    private void saveAndClose() {
        
        for(final DownloadItem downloadItem : downloadItems) {
            FileDownloadWorker worker = new FileDownloadWorker(downloadItem, COPY_FILE_LOCK);
            worker.startDownload();
        }

        setVisible(false);
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
            return extension+" "+(isNative?"":" (Convert)");
        }
    }
    
}
