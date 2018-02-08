package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.browser.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.browser.gui.support.GroupedKeyValuePanel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.workspace.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add a newly created mask to a ColorDepthSearch.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AddMaskDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(AddMaskDialog.class);
    
    private final JLabel loadingLabel = new JLabel(Icons.getLoadingIcon());
    private final GroupedKeyValuePanel mainPanel;
    private final JLabel alignmentSpaceLabel;
    private final JTextField maskNameField;
    private final JRadioButton noSearchRadioButton;
    private final JRadioButton existingSearchRadioButton;
    private final JLabel existingSearchLabel;
    private final JPanel searchComboPanel;
    private final JComboBox<SearchWrapper> searchComboBox;
    private final JRadioButton newSearchRadioButton;
    private final JLabel newSearchLabel;
    private final JTextField searchNameField;
    
    private String filepath;
    private String alignmentSpace;
    private int maskThreshold;
    private Sample sample;
    
    private ColorDepthMask mask;
    
    public AddMaskDialog() {

        setTitle("Add Mask");

        setLayout(new BorderLayout());

        ButtonGroup group = new ButtonGroup();
        this.mainPanel = new GroupedKeyValuePanel();

        this.alignmentSpaceLabel = new JLabel();
        mainPanel.addItem("Alignment space", alignmentSpaceLabel);
        
        this.maskNameField = new JTextField(40);
        mainPanel.addItem("Mask name", maskNameField);

        mainPanel.addSeparator();
        
        this.noSearchRadioButton = new JRadioButton("Do not add to a search");
        group.add(noSearchRadioButton);
        noSearchRadioButton.addActionListener((ActionEvent e) -> {
            updateRadioSelection();
        });
        mainPanel.addItem(noSearchRadioButton);
        
        mainPanel.addSeparator();
        
        this.existingSearchRadioButton = new JRadioButton("Add to existing search:");
        group.add(existingSearchRadioButton);
        existingSearchRadioButton.addActionListener((ActionEvent e) -> {
            updateRadioSelection();
        });
        mainPanel.addItem(existingSearchRadioButton);
        
        this.searchComboPanel = new JPanel(new BorderLayout());
        searchComboPanel.add(loadingLabel, BorderLayout.CENTER);
        this.searchComboBox = new JComboBox<>(); 
        searchComboBox.setEditable(false);
        existingSearchLabel = mainPanel.addItem("Existing search", searchComboPanel);

        mainPanel.addSeparator();
        
        this.newSearchRadioButton = new JRadioButton("Create a new search:");
        newSearchRadioButton.setSelected(true);
        group.add(newSearchRadioButton);
        newSearchRadioButton.addActionListener((ActionEvent e) -> {
            updateRadioSelection();
        });
        mainPanel.addItem(newSearchRadioButton);
        
        this.searchNameField = new JTextField(40);
        newSearchLabel = mainPanel.addItem("Search name", searchNameField);
        
        add(mainPanel, BorderLayout.CENTER);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JButton okButton = new JButton("Add");
        okButton.setToolTipText("Add mask");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processAdd();
            }
        });
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(okButton);
        add(buttonPane, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);
    }
    
    private void updateRadioSelection() {

        if (noSearchRadioButton.isSelected()) {
            existingSearchLabel.setEnabled(false);
            searchComboBox.setEnabled(false);
            newSearchLabel.setEnabled(false);
            searchNameField.setEnabled(false);
        }
        else if (existingSearchRadioButton.isSelected()) {
            existingSearchLabel.setEnabled(true);
            searchComboBox.setEnabled(true);
            newSearchLabel.setEnabled(false);
            searchNameField.setEnabled(false);
        }
        else if (newSearchRadioButton.isSelected()) {
            existingSearchLabel.setEnabled(false);
            searchComboBox.setEnabled(false);
            newSearchLabel.setEnabled(true);
            searchNameField.setEnabled(true);
        }
        else {
            log.warn("None of the radio buttons is selected. This should never happen.");
        }
    }
    
    private void load() {
        
        SimpleWorker worker = new SimpleWorker() {

            private List<ColorDepthSearch> searches = new ArrayList<>(); 
                    
            @Override
            protected void doStuff() throws Exception {
                for(ColorDepthSearch search : DomainMgr.getDomainMgr().getModel().getAllDomainObjectsByClass(ColorDepthSearch.class)) {
                    if (alignmentSpace==null || search.getAlignmentSpace().equals(alignmentSpace)) {
                        searches.add(search);
                    }
                }
            }

            @Override
            protected void hadSuccess() {
                
                String searchName = ClientDomainUtils.getNextNumberedName(searches, alignmentSpace+" Search", true);
                searchNameField.setText(searchName);
                
                searchComboPanel.removeAll();
                searchComboPanel.add(searchComboBox);
                
                if (searches.isEmpty()) {
                    newSearchRadioButton.setSelected(true);
                    existingSearchRadioButton.setEnabled(false);
                    searchComboBox.setEnabled(false);
                }
                else {
                    existingSearchRadioButton.setSelected(true);
                    DefaultComboBoxModel<SearchWrapper> model = (DefaultComboBoxModel<SearchWrapper>) searchComboBox.getModel();
                    SearchWrapper wrapper = null;
                    for(ColorDepthSearch search : searches) {
                        wrapper = new SearchWrapper(search);
                        model.addElement(wrapper);
                    }
                    if (wrapper!=null) {
                        model.setSelectedItem(wrapper); // select the last item by default
                    }
                }
                
                updateRadioSelection();
                revalidate();
                repaint();
            }

            @Override
            protected void hadError(Throwable error) {
                setVisible(false);
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }
    
    public void showForMask(String filepath, String alignmentSpace, int maskThreshold, Sample sample) {

        this.filepath = filepath;
        this.alignmentSpace = alignmentSpace;
        this.maskThreshold = maskThreshold;
        this.sample = sample;
        
        alignmentSpaceLabel.setText(alignmentSpace);
        
        if (sample!=null) {
            maskNameField.setText("Mask derived from "+sample.getLine());
        }
        else {
            maskNameField.setText((new File(filepath)).getName());
        }
        
        maskNameField.setEditable(true);
        searchNameField.setText("Mask Search");
        
        load();
        packAndShow();
    }

    public void showForMask(ColorDepthMask mask) {

        this.mask = mask;
        this.filepath = mask.getFilepath();
        this.alignmentSpace = mask.getAlignmentSpace();
        this.maskThreshold = mask.getMaskThreshold();
        this.sample = null;
        
        alignmentSpaceLabel.setText(mask.getAlignmentSpace());
        maskNameField.setText(mask.getName());
        maskNameField.setEditable(false);
        searchNameField.setText("Mask Search");
        
        load();
        packAndShow();
    }
    
    private void processAdd() {

        String maskNameStr = maskNameField.getText();
        
        if (StringUtils.isBlank(maskNameStr)) {
            JOptionPane.showMessageDialog(this, 
                    "You must specify a name for your mask", 
                    "Missing mask name", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        ColorDepthSearch search = null;
        
        if (existingSearchRadioButton.isSelected()) {
            SearchWrapper wrapper = (SearchWrapper)searchComboBox.getSelectedItem();
            if (wrapper.search==null) {
                JOptionPane.showMessageDialog(this, 
                        "You must select a search to add the mask to", 
                        "No search selected", JOptionPane.ERROR_MESSAGE);
                return;
            }
            search = wrapper.search;
        }
         
        final ColorDepthSearch finalSearch = search;
        
        Utils.setWaitingCursor(this);
        SimpleWorker worker = new SimpleWorker() {
            
            ColorDepthSearch colorDepthSearch = finalSearch;
            TreeNode masksFolder;
            
            @Override
            protected void doStuff() throws Exception {

                DomainModel model = DomainMgr.getDomainMgr().getModel();
                
                if (mask==null) {
                    String maskName = maskNameStr;
                    if (colorDepthSearch != null && !maskName.matches("#\\d+$")) {
                        // Add mask numbering automatically
                        List<ColorDepthMask> masks = model.getDomainObjectsAs(ColorDepthMask.class, colorDepthSearch.getMasks());
                        maskName = ClientDomainUtils.getNextNumberedName(masks, maskName, false);
                    }
                    
                    mask = model.createColorDepthMask(maskName, alignmentSpace, filepath, maskThreshold, sample);
                }

                if (colorDepthSearch == null && newSearchRadioButton.isSelected()) {
                    colorDepthSearch = model.createColorDepthSearch(searchNameField.getText(), alignmentSpace);
                }
                
                if (colorDepthSearch == null) {
                    masksFolder = model.getDefaultWorkspaceFolder(DomainConstants.NAME_COLOR_DEPTH_MASKS);
                }
                else {
                    model.addMaskToSearch(colorDepthSearch, mask);
                }
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(AddMaskDialog.this);
                setVisible(false);
                if (colorDepthSearch==null) {
                    DomainExplorerTopComponent.getInstance().selectAndNavigateNodeById(masksFolder.getId());
                }
                else {
                    DomainExplorerTopComponent.getInstance().selectAndNavigateNodeById(colorDepthSearch.getId());
                }
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setDefaultCursor(AddMaskDialog.this);
                setVisible(false);
                FrameworkImplProvider.handleException(error);
            }
        };

        worker.execute();
    }

    /**
     * Wrapper to present ColorDepthSearch with names in a combo box.
     */
    private class SearchWrapper {
        
        ColorDepthSearch search;
        
        SearchWrapper(ColorDepthSearch search) {
            this.search = search;
        }

        @Override
        public String toString() {
            if (search==null) {
                return "";
            }
            return search.getName();
        }
    }
       
}
