package org.janelia.workstation.browser.gui.editor;

import com.google.common.eventbus.Subscribe;
import net.miginfocom.swing.MigLayout;
import org.janelia.model.domain.report.DatabaseSummary;
import org.janelia.model.domain.report.DiskUsageSummary;
import org.janelia.model.domain.report.QuotaUsage;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.workstation.browser.actions.NewFilterActionListener;
import org.janelia.workstation.browser.gui.support.SelectablePanel;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.ViewerToolbar;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.core.options.OptionConstants;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.util.HelpTextUtils;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.annotation.Annotation;
import java.util.Map;

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
    private static final ImageIcon COLOR_DEPTH_ICON = Icons.getIcon("color_depth_brain.png");
    
    private static final int iconSize = 128;
    
    private final JPanel topPanel;
    private final JPanel mainPanel;
    private final JPanel searchPanel;
    private final JPanel diskSpacePanel;
    private final JPanel dataSummaryPanel;
    private final JPanel colorDepthPanel;
    private final JPanel lowerPanel = new JPanel();
    private final JCheckBox openOnStartupCheckbox = new JCheckBox("Show On Startup");
    private final JTextField searchField;
    private final JButton searchButton;

    private Font titleFont;
    private Font largeFont;
    private Font mediumFont;
    
    private Class<?> searchClass;
    private JLabel spaceUsedLabel;
    private JLabel labSpaceUsedLabel;
    private JLabel spaceAvailableLabel;
    private JLabel dataSetCountLabel;
    private JLabel sampleCountLabel;
    private JLabel lsmCountLabel;
    private JLabel annotationCountLabel;

    private DiskUsageSummary diskUsageSummary;
    private DatabaseSummary dataSummary;
    private ImageIcon diskUsageIcon;
    private ImageIcon sampleIcon;
    private ImageIcon colorDepthIcon;
    
    public StartPage() {
        
        diskUsageIcon = getScaledIcon(DISK_USAGE_ICON, iconSize, iconSize);
        sampleIcon = getScaledIcon(SAMPLE_ICON, iconSize, iconSize);
        colorDepthIcon = COLOR_DEPTH_ICON;
        
        JLabel titleLabel = new JLabel("Welcome to the Janelia Workstation");
        //titleLabel.setForeground(UIManager.getColor("textInactiveText"));
        
        titleFont = titleLabel.getFont().deriveFont(Font.BOLD, 20);
        largeFont = titleLabel.getFont().deriveFont(Font.BOLD, 16);
        mediumFont = titleLabel.getFont().deriveFont(Font.PLAIN, 16);
        
        // Top Panel
        
        titleLabel.setFont(titleFont);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        topPanel = new ViewerToolbar() {
            @Override
            protected void refresh() {
                StartPage.this.refresh();
            }
        };
        
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
        searchButton.setFont(mediumFont);
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewFilterActionListener actionListener = new NewFilterActionListener(searchField.getText(), searchClass);
                actionListener.actionPerformed(e);
            }
        });

        ButtonGroup group = new ButtonGroup();
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

//        JToggleButton button0 = new JToggleButton("Everything");
//        button0.setSelected(true);
//        button0.setMargin(new Insets(5,5,5,5));
//        button0.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                // TODO: this needs to search everything
//                searchClass = Sample.class.getSimpleName();
//            }
//        });
//        group.add(button0);
//        buttonsPanel.add(button0);
                
        JToggleButton button1 = new JToggleButton("Confocal Samples");
        button1.setFont(mediumFont);
        button1.setMargin(new Insets(5,5,5,5));
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchClass = Sample.class;
            }
        });
        group.add(button1);

        JToggleButton button2 = new JToggleButton("LSM Images");
        button2.setFont(mediumFont);
        button2.setMargin(new Insets(5,5,5,5));
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchClass = LSMImage.class;
            }
        });
        group.add(button2);

        JToggleButton button3 = new JToggleButton("Mouse Samples");
        button3.setFont(mediumFont);
        button3.setMargin(new Insets(5,5,5,5));
        button3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchClass = TmSample.class;
            }
        });
        group.add(button3);

        buttonsPanel.add(button1);
        buttonsPanel.add(Box.createRigidArea(new Dimension(5,0)));
        buttonsPanel.add(button2);
        buttonsPanel.add(Box.createRigidArea(new Dimension(5,0)));
        buttonsPanel.add(button3);
        
        // Default search button
        button1.setSelected(true);
        searchClass = Sample.class;
        
        JLabel promptLabel = new JLabel("What would you like to search?");
        promptLabel.setFont(largeFont);
        
        searchPanel = new JPanel();
        searchPanel.setLayout(new MigLayout(
                "gap 50, fill, wrap 2", // Layout constraints
                "[grow 50]5[grow 50]", // Column constraints
                "[grow 50]5[grow 0]5[grow 0]5[grow 75, fill]")); // Row constraints
        searchPanel.add(titleLabel, "gap 10, span 2, al center bottom");
        searchPanel.add(promptLabel, "gap 10, span 2, al center bottom");
        searchPanel.add(buttonsPanel, "span 2, al center");
        searchPanel.add(searchField, "height 35, al right top");
        searchPanel.add(searchButton, "al left top");

        // Disk Space Panel

        diskSpacePanel = new SelectablePanel();
        diskSpacePanel.setLayout(new MigLayout("gap 50, fillx, wrap 3", "[grow 10]5[grow 0]5[grow 10]", "[]2[]5[]5[]"));
        
        // Data Summary Panel
        dataSummaryPanel = new SelectablePanel();
        dataSummaryPanel.setLayout(new MigLayout("gap 50, fillx, wrap 3", "[grow 10]5[grow 0]5[grow 10]", "[]2[]5[]5[]5[]5[]"));

        // Data Summary Panel
        colorDepthPanel = new SelectablePanel();
        colorDepthPanel.setLayout(new MigLayout("gap 50, fillx, wrap 4", "[grow 10]5[grow 0]5[grow 0]5[grow 10]", "[]2[]0[]"));
        
        
        // Main Panel
        
        mainPanel = new JPanel();
        mainPanel.setLayout(new MigLayout("gap 50, fillx, wrap 2", "[]5[]", "[]10[]"));
        mainPanel.add(searchPanel, "span 2, al center top");
        mainPanel.add(diskSpacePanel, "al center top, width 50%");
        mainPanel.add(dataSummaryPanel, "al center top, width 50%");
        mainPanel.add(colorDepthPanel, "span 2, al center top, width 100%");
        
        // Lower panel
        
        openOnStartupCheckbox.setSelected(ApplicationOptions.getInstance().isShowStartPageOnStartup());
        openOnStartupCheckbox.addItemListener(e -> ApplicationOptions.getInstance().setShowStartPageOnStartup(openOnStartupCheckbox.isSelected()));
        
        lowerPanel.setLayout(new BorderLayout());
        lowerPanel.add(openOnStartupCheckbox);
        
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(lowerPanel, BorderLayout.SOUTH);
        
        refresh();
    }
    
    public JTextField getSearchField() {
        return searchField;
    }

    private void refresh() {
        
        log.info("Refreshing start page");

        diskSpacePanel.removeAll();
        diskSpacePanel.add(getLargeLabel("Disk Space Usage"), "spanx 3, gapbottom 10, al center");
        diskSpacePanel.add(new JLabel(diskUsageIcon), "al right top, w 50%");
        diskSpacePanel.add(new JLabel(Icons.getLoadingIcon()), "spanx 2, al center center");

        dataSummaryPanel.removeAll();
        dataSummaryPanel.add(getLargeLabel("Confocal Data Summary"), "spanx 3, gapbottom 10, al center");
        dataSummaryPanel.add(new JLabel(sampleIcon), "al right top, w 50%");
        dataSummaryPanel.add(new JLabel(Icons.getLoadingIcon()), "spanx 2, al center center");

        String colorDepthTxt = "<html>To search the color depth projection libraries, you first need to create a search mask.<br>"
                + "To begin, right-click any Color Depth Projection and select "+HelpTextUtils.getBoldedLabel("Create Mask for Color Depth Search")+".<br>"
                + "You can also upload a custom mask using the "+HelpTextUtils.getMenuItemLabel("File","Upload","Color Depth Mask")+"  menu option.<br>";
        
        JPanel colorDepthTitlePanel = new JPanel();
        colorDepthTitlePanel.setLayout(new BoxLayout(colorDepthTitlePanel, BoxLayout.LINE_AXIS));
                
        colorDepthTitlePanel.add(getLargeLabel("Color Depth Mask Search"));
        //colorDepthTitlePanel.add(getHighlightLabel("NEW"));
        
        JButton userManualButton = new JButton("Learn more in the User Manual");
        userManualButton.setFont(mediumFont);
        userManualButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String manualUrl = ConsoleProperties.getInstance().getProperty("manual.color.depth.url", null);
                if (manualUrl==null) {
                    JOptionPane.showMessageDialog(
                            FrameworkAccess.getMainFrame(),
                            "No color depth user manual can be found.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE,
                            null
                    );
                }
                else {
                    Utils.openUrlInBrowser(manualUrl);
                }
            }
        });
        
        colorDepthPanel.removeAll();
        colorDepthPanel.add(colorDepthTitlePanel, "spanx 4, gapbottom 10, al center");
        colorDepthPanel.add(Box.createHorizontalGlue(), "spany2");
        colorDepthPanel.add(new JLabel(colorDepthIcon), "spany 2, gapright 10, al right top");
        colorDepthPanel.add(new JLabel(colorDepthTxt), "al left top");
        colorDepthPanel.add(Box.createHorizontalGlue(), "spany2");
        colorDepthPanel.add(userManualButton, "al left");
        
        
        mainPanel.updateUI();

        diskSpacePanel.setVisible(false);
        dataSummaryPanel.setVisible(false);

        if (!AccessManager.loggedIn()) {
            return;
        }
        
        SimpleWorker worker = new SimpleWorker() {

            private DiskUsageSummary summary;
            
            @Override
            protected void doStuff() throws Exception {
                summary = DomainMgr.getDomainMgr().getDomainFacade().getDiskUsageSummary();   
            }

            @Override
            protected void hadSuccess() {
                diskUsageSummary = summary;
                populateDiskView(diskUsageSummary);
            }

            @Override
            protected void hadError(Throwable e) {
                FrameworkAccess.handleException(e);
                diskUsageSummary = null;
                populateDiskView(diskUsageSummary);
            }
        };

        worker.execute();
        
        SimpleWorker worker2 = new SimpleWorker() {

            private DatabaseSummary summary;
            
            @Override
            protected void doStuff() throws Exception {
                summary = DomainMgr.getDomainMgr().getDomainFacade().getDatabaseSummary();
            }

            @Override
            protected void hadSuccess() {
                dataSummary = summary;
                populateDataView(dataSummary);
            }

            @Override
            protected void hadError(Throwable e) {
                FrameworkAccess.handleException(e);
                dataSummary = null;
                populateDataView(dataSummary);
            }
        };

        worker2.execute();
    }

    private void populateDiskView(DiskUsageSummary dataSummary) {
        
        // Reset components
        diskSpacePanel.removeAll();
        
        spaceUsedLabel = getMediumLabel("");
        labSpaceUsedLabel = getMediumLabel("");
        spaceAvailableLabel = getMediumLabel("");
        
        diskSpacePanel.add(getLargeLabel("Disk Space Usage"), "spanx 3, gapbottom 10, al center");
        diskSpacePanel.add(new JLabel(diskUsageIcon), "spany 3, al right top");
        
        diskSpacePanel.add(getMediumLabel("Your usage:"), "al left top, width 30pt");
        diskSpacePanel.add(spaceUsedLabel, "al left top, width 30pt");
        
        diskSpacePanel.add(getMediumLabel("Lab's usage:"), "al left top");
        diskSpacePanel.add(labSpaceUsedLabel, "al left top");
        
        diskSpacePanel.add(getMediumLabel("Free space:"), "al left top");
        diskSpacePanel.add(spaceAvailableLabel, "al left top");
        
        if (dataSummary==null) return;
        
        Double userDataSetsTB = dataSummary.getUserDataSetsTB();
        if (userDataSetsTB!=null) {
            spaceUsedLabel.setText(String.format("%2.2f TB", userDataSetsTB));
        }
        
        QuotaUsage quotaUsage = dataSummary.getQuotaUsage();
        if (quotaUsage!=null) {
            Double spaceUsedTB = quotaUsage.getSpaceUsedTB();
            Double totalSpaceTB = quotaUsage.getTotalSpaceTB();
            if (spaceUsedTB!=null) {
                labSpaceUsedLabel.setText(String.format("%2.2f TB", spaceUsedTB));
                if (totalSpaceTB!=null) {
                    double spaceAvailable = totalSpaceTB - spaceUsedTB;
                    spaceAvailableLabel.setText(String.format("%2.2f TB", spaceAvailable));
                }
            }
        }
        
        diskSpacePanel.setVisible(true);
        
    }
    
    private void populateDataView(DatabaseSummary dataSummary) {

        // Reset components
        dataSummaryPanel.removeAll();

        dataSetCountLabel = getMediumLabel("");
        sampleCountLabel = getMediumLabel("");
        lsmCountLabel = getMediumLabel("");
        annotationCountLabel = getMediumLabel("");

        dataSummaryPanel.add(getLargeLabel("Confocal Data Summary"), "spanx 3, gapbottom 10, al center");
        dataSummaryPanel.add(new JLabel(sampleIcon), "spany 5, al right top");
        
        dataSummaryPanel.add(getMediumLabel("Data Sets:"), "al left top");
        dataSummaryPanel.add(dataSetCountLabel, "al left top");
        
        dataSummaryPanel.add(getMediumLabel("Samples:"), "al left top");
        dataSummaryPanel.add(sampleCountLabel, "al left top");
        
        dataSummaryPanel.add(getMediumLabel("LSM Images:"), "al left top");
        dataSummaryPanel.add(lsmCountLabel, "al left top");
        
        dataSummaryPanel.add(getMediumLabel("Annotations:"), "al left top");
        dataSummaryPanel.add(annotationCountLabel, "al left top");
        
        if (dataSummary==null) return;

        Map<String, Long> counts = dataSummary.getUserCounts(); 

        if (counts!=null) {
            dataSetCountLabel.setText(counts.get(DataSet.class.getSimpleName())+"");
            sampleCountLabel.setText(counts.get(Sample.class.getSimpleName())+"");
            lsmCountLabel.setText(counts.get(LSMImage.class.getSimpleName())+"");
            annotationCountLabel.setText(counts.get(Annotation.class.getSimpleName())+"");
        }
        
        dataSummaryPanel.setVisible(true);
    }
    
    private ImageIcon getScaledIcon(ImageIcon icon, int width, int height) {
        Image img = icon.getImage();
        Image newimg = img.getScaledInstance(width, height,  java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(newimg);
    }
    
    private JLabel getLargeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(largeFont);
        return label;
    }

    private JLabel getHighlightLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(largeFont.deriveFont(10));
        label.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 0));
        label.setForeground(Color.red);
        return label;
    }
    
    private JLabel getMediumLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(mediumFont);
        return label;
    }

    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {
        if (event.isTotalInvalidation()) {
            log.debug("Total invalidation detected, refreshing...");
            refresh();
        }
    }
    
    @Override
    public boolean requestFocusInWindow() {
        boolean success = super.requestFocusInWindow();
        if (getSearchField()!=null) {
            success = getSearchField().requestFocusInWindow();
        }
        return success;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(OptionConstants.SHOW_START_PAGE_ON_STARTUP)) {
            openOnStartupCheckbox.setSelected(ApplicationOptions.getInstance().isShowStartPageOnStartup());
        }
    }
}
