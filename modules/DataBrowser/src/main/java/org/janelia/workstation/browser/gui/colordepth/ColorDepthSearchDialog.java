package org.janelia.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.model.domain.gui.cdmip.ColorDepthLibrary;
import org.janelia.model.domain.gui.cdmip.ColorDepthMask;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.GroupedKeyValuePanel;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add a newly created mask to a ColorDepthSearch.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(ColorDepthSearchDialog.class);
    
    private final JLabel loadingLabel = new JLabel(Icons.getLoadingIcon());
    private final JPanel mainPanel;
    private final GroupedKeyValuePanel attrPanel;
    private final JPanel addPanel;
    private final ColorDepthSearchOptionsPanel searchOptionsPanel;
    private final JLabel alignmentSpaceLabel;
    private final JTextField maskNameField;
    private final JRadioButton existingSearchRadioButton;
    private final JLabel existingSearchLabel;
    private final JPanel searchComboPanel;
    private final JComboBox<SearchWrapper> searchComboBox;
    private final JRadioButton newSearchRadioButton;
    private final JLabel newSearchLabel;
    private final JTextField searchNameField;
    
    private ColorDepthMask mask;
    
    public ColorDepthSearchDialog() {

        setTitle("Color Depth Search");

        setLayout(new BorderLayout());

        this.alignmentSpaceLabel = new JLabel();
        
        this.maskNameField = new JTextField(60);
        
        attrPanel = new GroupedKeyValuePanel();
        attrPanel.addItem("Alignment space", alignmentSpaceLabel);
        attrPanel.addItem("Mask name", maskNameField);

        ButtonGroup group = new ButtonGroup();
        
        this.existingSearchRadioButton = new JRadioButton("Add to existing search:");
        group.add(existingSearchRadioButton);
        existingSearchRadioButton.addActionListener((ActionEvent e) -> {
            updateState();
        });
        
        this.searchComboBox = new JComboBox<>(); 
        searchComboBox.setEditable(false);
        searchComboBox.addActionListener((ActionEvent e) -> {
            updateState();
        });
        existingSearchLabel = new JLabel("Existing search");
        
        this.searchComboPanel = new JPanel(new BorderLayout());
        searchComboPanel.add(existingSearchLabel, BorderLayout.WEST);
        searchComboPanel.add(loadingLabel, BorderLayout.CENTER);
        
        this.newSearchRadioButton = new JRadioButton("Create a new search:");
        newSearchRadioButton.setSelected(true);
        group.add(newSearchRadioButton);
        newSearchRadioButton.addActionListener((ActionEvent e) -> {
            updateState();
        });
        
        this.newSearchLabel = new JLabel("Search name");
        this.searchNameField = new JTextField(35);

        this.addPanel = new JPanel();
        addPanel.setLayout(new MigLayout("wrap 2, ins 10, fillx", "[grow 0]20[grow 1]"));
        addPanel.add(existingSearchRadioButton, "");
        addPanel.add(newSearchRadioButton, "");
        addPanel.add(searchComboPanel, "gapleft 10");
        addPanel.add(searchNameField, "gapleft 10");
        
        searchOptionsPanel = new ColorDepthSearchOptionsPanel();
        
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.add(attrPanel);
        mainPanel.add(addPanel);
        mainPanel.add(searchOptionsPanel);
        
        add(mainPanel, BorderLayout.CENTER);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(e -> setVisible(false));

        JButton saveButton = new JButton("Save");
        saveButton.setToolTipText("Add mask");
        saveButton.addActionListener(e -> processSave(false));
        
        JButton saveAndSearchButton = new JButton("Save and Execute Search");
        saveAndSearchButton.setToolTipText("Add mask");
        saveAndSearchButton.addActionListener(e -> processSave(true));
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(saveButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(saveAndSearchButton);
        add(buttonPane, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(saveAndSearchButton);
    }
    
    private void updateState() {

        if (existingSearchRadioButton.isSelected()) {
            existingSearchLabel.setEnabled(true);
            searchComboBox.setEnabled(true);
            newSearchLabel.setEnabled(false);
            searchNameField.setEnabled(false);
            
            ColorDepthSearch search = getSelectedSearch();
            if (search==null) {
                searchOptionsPanel.setSearch(getNewSearch());
            }
            else {
                searchOptionsPanel.setSearch(search);
            }
        }
        else if (newSearchRadioButton.isSelected()) {
            existingSearchLabel.setEnabled(false);
            searchComboBox.setEnabled(false);
            newSearchLabel.setEnabled(true);
            searchNameField.setEnabled(true);
            
            searchOptionsPanel.setSearch(getNewSearch());
        }
        else {
            log.warn("None of the radio buttons is selected. This should never happen.");
        }
        
        searchOptionsPanel.refresh();
    }
    
    private ColorDepthSearch getNewSearch() {
        ColorDepthSearch colorDepthSearch = new ColorDepthSearch();
        colorDepthSearch.setAlignmentSpace(mask.getAlignmentSpace());
        return colorDepthSearch;
    }
    
    private void load() {
        
        String alignmentSpace = mask.getAlignmentSpace();
        
        SimpleWorker worker = new SimpleWorker() {
            
            private List<ColorDepthLibrary> libraries;
            private List<ColorDepthSearch> searches = new ArrayList<>();
                    
            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();

                libraries = model.getColorDepthLibraries(alignmentSpace);
                
                for(ColorDepthSearch search : model.getAllDomainObjectsByClass(ColorDepthSearch.class)) {
                    // TODO: this should use hasWriteAccess, once save is working server-side
                    if (ClientDomainUtils.isOwner(search)) {
                        if (search.getAlignmentSpace().equals(alignmentSpace)) {
                            searches.add(search);
                        }
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

                log.info("Adding {} values to library button", libraries.size());
                searchOptionsPanel.setLibraries(libraries);
                
                updateState();
                pack();
            }

            @Override
            protected void hadError(Throwable error) {
                setVisible(false);
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();
    }

    public void showForMask(ColorDepthMask mask) {

        this.mask = mask;
        
        alignmentSpaceLabel.setText(mask.getAlignmentSpace());
        maskNameField.setText(mask.getName());
        searchNameField.setText("Mask Search");

        ActivityLogHelper.logUserAction("AddMaskDialog.showForMask", mask);

        updateState(); // initialize UI so that it can be packed before the background load finishes
        load();
        packAndShow();
    }
    
    private ColorDepthSearch getSelectedSearch() {
        SearchWrapper wrapper = (SearchWrapper)searchComboBox.getSelectedItem();
        if (wrapper == null) return null;
        return wrapper.search;
    }
    
    private void processSave(boolean execute) {

        String maskNameStr = maskNameField.getText();
        
        if (StringUtils.isBlank(maskNameStr)) {
            JOptionPane.showMessageDialog(this, 
                    "You must specify a name for your mask", 
                    "Missing mask name", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (existingSearchRadioButton.isSelected()) {
            if (getSelectedSearch()==null) {
                JOptionPane.showMessageDialog(this, 
                        "You must select a search to add the mask to", 
                        "No search selected", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (execute) {
            if (searchOptionsPanel.getSearch().getLibraries().isEmpty()) {
                JOptionPane.showMessageDialog(this, "You need to select some color depth libraries to search against.");
                return;
            }
        }
        
        SimpleWorker worker = new SimpleWorker() {
            
            ColorDepthSearch search = searchOptionsPanel.getSearch();

            @Override
            protected void doStuff() throws Exception {

                DomainModel model = DomainMgr.getDomainMgr().getModel();

                if (!maskNameStr.equals(mask.getName())) {
                    // Rename mask
                    mask.setName(maskNameStr);
                    mask = model.save(mask);
                }

                if (newSearchRadioButton.isSelected()) {
                    search.setName(searchNameField.getText());
                }             
                
                search = searchOptionsPanel.saveChanges();
                search = model.addMaskToSearch(search, mask);
                
                if (execute) {
                    ActivityLogHelper.logUserAction("ColorDepthSearchDialog.executeSearch", search);
                    DomainMgr.getDomainMgr().getAsyncFacade().executeColorDepthService(search);
                }
                else {
                    ActivityLogHelper.logUserAction("ColorDepthSearchDialog.processSave", search);
                }
            }

            @Override
            protected void hadSuccess() {
                SwingUtilities.invokeLater(() -> {
                    DomainExplorerTopComponent.getInstance().expandNodeById(ColorDepthSearchesNode.NODE_ID);
                    DomainExplorerTopComponent.getInstance().selectAndNavigateNodeById(search.getId());
                });
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(), "Saving changes", ""));
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
