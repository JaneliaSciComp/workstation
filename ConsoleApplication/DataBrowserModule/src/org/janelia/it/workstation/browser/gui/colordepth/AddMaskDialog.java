package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
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

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.browser.gui.support.GroupedKeyValuePanel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleAlignmentResult;

/**
 * Add a newly created mask to a ColorDepthSearch.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AddMaskDialog extends ModalDialog {

    private final JLabel loadingLabel = new JLabel(Icons.getLoadingIcon());
    private final GroupedKeyValuePanel mainPanel;
    private final JLabel alignmentSpaceLabel;
    private final JTextField maskNameField;
    private final JRadioButton existingSearchRadioButton;
    private final JLabel existingSearchLabel;
    private final JPanel searchComboPanel;
    private final JComboBox<SearchWrapper> searchComboBox;
    private final JRadioButton newSearchRadioButton;
    private final JLabel newSearchLabel;
    private final JTextField searchNameField;
    
    private BufferedImage imageMask;
    private Sample sample;
    private SampleAlignmentResult alignment;
    private String filepath;
    private boolean cancelled = false;
    private ColorDepthMask mask;
    
    public AddMaskDialog() {

        setTitle("Add Mask");

        setLayout(new BorderLayout());
        //setPreferredSize(new Dimension(300, 200));

        ButtonGroup group = new ButtonGroup();
        this.mainPanel = new GroupedKeyValuePanel();

        this.alignmentSpaceLabel = new JLabel();
        mainPanel.addItem("Alignment space", alignmentSpaceLabel);
        
        this.maskNameField = new JTextField(40);
        mainPanel.addItem("Mask name", maskNameField);

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
                cancelled = true;
                setVisible(false);
            }
        });

        JButton okButton = new JButton("Add");
        okButton.setToolTipText("Add mask to color depth search");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelled = false;
                processAdd();
            }
        });
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(cancelButton);
        add(buttonPane, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);
    }
    
    private void updateRadioSelection() {
        if (existingSearchRadioButton.isSelected()) {
            existingSearchLabel.setEnabled(true);
            searchComboBox.setEnabled(true);
            newSearchLabel.setEnabled(false);
            searchNameField.setEnabled(false);
        }
        else {
            existingSearchLabel.setEnabled(false);
            searchComboBox.setEnabled(false);
            newSearchLabel.setEnabled(true);
            searchNameField.setEnabled(true);
        }
    }
    
    public void load() {
        
        SimpleWorker worker = new SimpleWorker() {

            private List<ColorDepthSearch> searches = new ArrayList<>(); 
                    
            @Override
            protected void doStuff() throws Exception {
                for(ColorDepthSearch search : DomainMgr.getDomainMgr().getModel().getAllDomainObjectsByClass(ColorDepthSearch.class)) {
                    searches.add(search);
                }
            }

            @Override
            protected void hadSuccess() {
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
                    for(ColorDepthSearch search : searches) {
                        SearchWrapper wrapper = new SearchWrapper();
                        wrapper.search = search;
                        model.addElement(wrapper);
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
    
    public ColorDepthMask showForMask(BufferedImage imageMask, Sample sample, SampleAlignmentResult alignment, String filepath) {
        
        this.imageMask = imageMask;
        this.sample = sample;
        this.alignment = alignment;
        this.filepath = filepath;
        
        maskNameField.setText("Mask derived from "+sample.getName());
        alignmentSpaceLabel.setText(alignment.getAlignmentSpace());

        load();
        packAndShow();
        
        if (cancelled) {
            return null;
        }
        
        return mask;
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
        else {
            String searchName = searchNameField.getText();
            search = new ColorDepthSearch();
            search.setAlignmentSpace(alignment.getAlignmentSpace());
            search.setName(searchName);
        }
         
        // TODO: ask the user to specify using a thresholding algorithm
        int maskThreshold = 50;
        final ColorDepthSearch finalSearch = search;
        
        Utils.setWaitingCursor(this);
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                DomainModel model = DomainMgr.getDomainMgr().getModel();
                
                String maskName = maskNameStr;
                if (!maskName.matches("#\\d+$")) {
                    List<ColorDepthMask> masks = model.getDomainObjectsAs(ColorDepthMask.class, finalSearch.getMasks());
                    maskName = ClientDomainUtils.getNextNumberedName(masks, maskName, false);
                }
                
                mask = new ColorDepthMask();
                mask.setFilepath(filepath);
                mask.setName(maskName);
                mask.setMaskThreshold(maskThreshold);
                mask = model.save(mask);
                                
                finalSearch.addMask(Reference.createFor(mask));
                model.save(finalSearch);
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(AddMaskDialog.this);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setDefaultCursor(AddMaskDialog.this);
                setVisible(false);
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }


    /**
     * Wrapper to present ColorDepthSearch with names in a combo box.
     */
    private class SearchWrapper {
        
        ColorDepthSearch search;

        @Override
        public String toString() {
            if (search==null) {
                return "";
            }
            return search.getName();
        }
    }
       
}
