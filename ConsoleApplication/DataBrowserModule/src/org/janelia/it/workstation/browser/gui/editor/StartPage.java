package org.janelia.it.workstation.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.workstation.browser.gui.options.ApplicationOptions;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.SelectablePanel;
import org.janelia.it.workstation.browser.nb_action.NewFilterActionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.miginfocom.swing.MigLayout;

/**
 * Start Page which is automatically shown to the user on every startup (unless disabled by user preference) 
 * and allows for easy navigation to common tasks and use cases.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StartPage extends JPanel implements PropertyChangeListener {

    private static final Logger log = LoggerFactory.getLogger(StartPage.class);
    
    private static final ImageIcon DISK_USAGE_ICON = Icons.getIcon("database_400.png");
    private static final ImageIcon SAMPLE_ICON = Icons.getIcon("microscope_400.png");
    
    private static final int iconSize = 180;
    private static final String SUMMARY_MINE = " My data ";
    private static final String SUMMARY_ALL = " All data ";
    
    private final JPanel topPanel;
    private final JPanel mainPanel;
    private final JPanel searchPanel;
    private final JPanel diskSpacePanel;
    private final JPanel dataSummaryPanel;
    private final JPanel lowerPanel = new JPanel();
    private final JCheckBox openOnStartupCheckbox = new JCheckBox("Show On Startup");
    private final JTextField searchField;
    private final JButton searchButton;

    private Font titleFont;
    private Font largeFont;
    private Font mediumFont;
    
    private String searchClass;
    private JLabel spaceUsedLabel;
    private JLabel labSpaceUsedLabel;
    private JLabel spaceAvailableLabel;
    private JLabel dataSetCountLabel;
    private JLabel sampleCountLabel;
    private JLabel lsmCountLabel;
    private JLabel neuronCountLabel;
    private JComboBox<String> summaryCombo;
    
    public StartPage() {
        
        ImageIcon diskUsageIcon = getScaledIcon(DISK_USAGE_ICON, iconSize, iconSize);
        ImageIcon sampleIcon = getScaledIcon(SAMPLE_ICON, iconSize, iconSize);
        
        JLabel title = new JLabel("Welcome to the Janelia Workstation");
        
        titleFont = title.getFont().deriveFont(Font.BOLD, 20);
        largeFont = title.getFont().deriveFont(Font.BOLD, 16);
        mediumFont = title.getFont().deriveFont(Font.PLAIN, 16);
        
        // Top Panel
        
        title.setFont(titleFont);
        title.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(title, BorderLayout.CENTER);

        // Search Panel
        
        searchField = new JTextField();
        searchField.setColumns(30);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchButton.doClick();
                }
            }
        });
        
        searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchString = searchField.getText();
                NewFilterActionListener actionListener = new NewFilterActionListener(searchString, searchClass);
                actionListener.actionPerformed(e);
            }
        });

        ButtonGroup group = new ButtonGroup();
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

        JToggleButton button0 = new JToggleButton("Everything");
        button0.setSelected(true);
        button0.setMargin(new Insets(5,5,5,5));
        button0.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: this needs to search everything
                searchClass = Sample.class.getSimpleName();
            }
        });
        group.add(button0);
        buttonsPanel.add(button0);
        
        JToggleButton button1 = new JToggleButton("Confocal Samples");
        button1.setMargin(new Insets(5,5,5,5));
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchClass = Sample.class.getSimpleName();
            }
        });
        group.add(button1);
        buttonsPanel.add(button1);

        JToggleButton button2 = new JToggleButton("LSM Images");
        button2.setMargin(new Insets(5,5,5,5));
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchClass = LSMImage.class.getSimpleName();
            }
        });
        group.add(button2);
        buttonsPanel.add(button2);

        JToggleButton button3 = new JToggleButton("Mouse Samples");
        button3.setMargin(new Insets(5,5,5,5));
        button3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchClass = TmSample.class.getSimpleName();
            }
        });
        group.add(button3);
        buttonsPanel.add(button3);
        
        JLabel promptLabel = new JLabel("What would you like to search?");
        promptLabel.setFont(largeFont);
        
        searchPanel = new JPanel();
        searchPanel.setLayout(new MigLayout("gap 50, fill, wrap 2", "[grow 50]5[grow 50]", "[grow 50]5[grow 0]5[grow 75]"));
        searchPanel.add(promptLabel, "gap 10, span 2, al center bottom");
        searchPanel.add(buttonsPanel, "span 2, al center");
        searchPanel.add(searchField, "height 35, al right top");
        searchPanel.add(searchButton, "al left top");

        // Disk Space Panel

        spaceUsedLabel = getMediumLabel("4.00 TB");
        labSpaceUsedLabel = getMediumLabel("34.00 TB");
        spaceAvailableLabel = getMediumLabel("34.33 TB");
        
        diskSpacePanel = new SelectablePanel();
        diskSpacePanel.setLayout(new MigLayout("gap 50, fillx, wrap 3", "[grow 10]5[grow 0]5[grow 10]", "[]2[]5[]5[]"));
        
        diskSpacePanel.add(getLargeLabel("Disk Space Usage"), "spanx 3, gapbottom 10, al center");
        diskSpacePanel.add(new JLabel(diskUsageIcon), "spany 3, al right top");
        
        diskSpacePanel.add(getMediumLabel("Your usage:"), "al left top");
        diskSpacePanel.add(spaceUsedLabel, "al left top");

        diskSpacePanel.add(getMediumLabel("Your lab's usage:"), "al left top");
        diskSpacePanel.add(labSpaceUsedLabel, "al left top");
        
        diskSpacePanel.add(getMediumLabel("Free space available:"), "al left top");
        diskSpacePanel.add(spaceAvailableLabel, "al left top");

        // Data Summary Panel
        
        dataSetCountLabel = getMediumLabel("4");
        sampleCountLabel = getMediumLabel("2303");
        lsmCountLabel = getMediumLabel("5314");
        neuronCountLabel = getMediumLabel("1034535");

        dataSummaryPanel = new SelectablePanel();
        dataSummaryPanel.setLayout(new MigLayout("gap 50, fillx, wrap 3", "[grow 10]5[grow 0]5[grow 10]", "[]2[]5[]5[]5[]6[]"));
        
        dataSummaryPanel.add(getLargeLabel("Data Summary"), "spanx 3, gapbottom 10, al center");
        dataSummaryPanel.add(new JLabel(sampleIcon), "spany 5, al right top");
        
        dataSummaryPanel.add(getMediumLabel("Data Sets:"), "al left top");
        dataSummaryPanel.add(dataSetCountLabel, "al left top");
        
        dataSummaryPanel.add(getMediumLabel("Samples:"), "al left top");
        dataSummaryPanel.add(sampleCountLabel, "al left top");
        
        dataSummaryPanel.add(getMediumLabel("LSM Images:"), "al left top");
        dataSummaryPanel.add(lsmCountLabel, "al left top");
        
        dataSummaryPanel.add(getMediumLabel("Neurons:"), "al left top");
        dataSummaryPanel.add(neuronCountLabel, "al left top");
        
        summaryCombo = new JComboBox<>();
        summaryCombo.setEditable(false);
        summaryCombo.setToolTipText("Choose an export format");
        summaryCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED || e.getSource() instanceof JCheckBox) {
                    log.trace("Item state changed: {}", e);
                    final String summaryState = (String)summaryCombo.getSelectedItem();
                    
                    // TODO: change display
                }
            }
        });
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) summaryCombo.getModel();
        model.addElement(SUMMARY_MINE);
        model.addElement(SUMMARY_ALL);

        dataSummaryPanel.add(summaryCombo, "spanx 2, al left top");
        
        
        // Main Panel
        
        mainPanel = new JPanel();
        mainPanel.setLayout(new MigLayout("gap 50, fillx, wrap 2", "[]5[]", "[]10[]"));
        mainPanel.add(searchPanel, "span 2, al center top");
        mainPanel.add(diskSpacePanel, "al center top, grow 50");
        mainPanel.add(dataSummaryPanel, "al center top, grow 50");
        
        // Lower panel
        
        openOnStartupCheckbox.setSelected(ApplicationOptions.getInstance().isShowStartPageOnStartup());
        openOnStartupCheckbox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                ApplicationOptions.getInstance().setShowStartPageOnStartup(openOnStartupCheckbox.isSelected());
            }
        });
        
        lowerPanel.setLayout(new BorderLayout());
        lowerPanel.add(openOnStartupCheckbox);
        
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(lowerPanel, BorderLayout.SOUTH);
    }

    private ImageIcon getScaledIcon(ImageIcon icon, int width, int height) {
        Image img = icon.getImage();
        Image newimg = img.getScaledInstance(width, height,  java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(newimg);
    }
    
    private JLabel getLargeLabel() {
        return getLargeLabel(null);
    }
    
    private JLabel getLargeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(largeFont);
        return label;
    }

    private JLabel getMediumLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(mediumFont);
        return label;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ApplicationOptions.PROP_SHOW_START_PAGE_ON_STARTUP)) {
            openOnStartupCheckbox.setSelected(ApplicationOptions.getInstance().isShowStartPageOnStartup());
        }
    }
}
