package org.janelia.workstation.site.hortacloud.gui;

import com.google.common.eventbus.Subscribe;
import net.miginfocom.swing.MigLayout;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.browser.actions.NewFilterActionListener;
import org.janelia.workstation.common.gui.support.ViewerToolbar;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.core.options.OptionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Start Page which is automatically shown to the user on every startup (unless disabled by user preference) 
 * and allows for easy navigation to common tasks and use cases.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StartPage extends JPanel implements PropertyChangeListener {

    private static final Logger log = LoggerFactory.getLogger(StartPage.class);

    private final JPanel topPanel;
    private final JPanel mainPanel;
    private final JPanel searchPanel;
    private final JPanel lowerPanel = new JPanel();
    private final JCheckBox openOnStartupCheckbox = new JCheckBox("Show On Startup");
    private final JTextField searchField;
    private final JButton searchButton;

    private Font titleFont;
    private Font largeFont;
    private Font mediumFont;
    
    private Class<?> searchClass;

    public StartPage() {

        JLabel titleLabel = new JLabel("Welcome to janeliaHortaCloud");
        
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
        searchButton.addActionListener(e -> {
            NewFilterActionListener actionListener = new NewFilterActionListener(searchField.getText(), searchClass);
            actionListener.actionPerformed(e);
        });

        ButtonGroup group = new ButtonGroup();
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
                
        JToggleButton button1 = new JToggleButton("Samples");
        button1.setFont(mediumFont);
        button1.setMargin(new Insets(5,5,5,5));
        button1.addActionListener(e -> searchClass = TmSample.class);
        group.add(button1);

        JToggleButton button2 = new JToggleButton("Workspaces");
        button2.setFont(mediumFont);
        button2.setMargin(new Insets(5,5,5,5));
        button2.addActionListener(e -> searchClass = TmWorkspace.class);
        group.add(button2);

        buttonsPanel.add(button1);
        buttonsPanel.add(Box.createRigidArea(new Dimension(5,0)));
        buttonsPanel.add(button2);
        
        // Default search button
        button1.setSelected(true);
        searchClass = TmSample.class;

        searchPanel = new JPanel();
        searchPanel.setLayout(new MigLayout(
                "gap 50, fill, wrap 2", // Layout constraints
                "[grow 50]5[grow 50]", // Column constraints
                "[grow 50]5[grow 0]5[grow 0]5[grow 75, fill]")); // Row constraints
        searchPanel.add(titleLabel, "gap 10, span 2, al center bottom");
        searchPanel.add(buttonsPanel, "span 2, al center");
        searchPanel.add(searchField, "height 35, al right top");
        searchPanel.add(searchButton, "al left top");

        // Main Panel
        
        mainPanel = new JPanel();
        mainPanel.setLayout(new MigLayout("gap 50, fillx, wrap 2", "[]5[]", "[]10[]"));
        mainPanel.add(searchPanel, "span 2, al center top");

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

        mainPanel.updateUI();

        if (!AccessManager.loggedIn()) {
            return;
        }
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
