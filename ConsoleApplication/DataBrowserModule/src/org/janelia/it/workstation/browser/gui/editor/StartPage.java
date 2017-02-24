package org.janelia.it.workstation.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang.StringUtils;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.browser.gui.options.ApplicationOptions;
import org.janelia.it.workstation.browser.nb_action.NewFilterActionListener;

import net.miginfocom.swing.MigLayout;

/**
 * Start Page which is automatically shown to the user on every startup (unless disabled by user preference) 
 * and allows for easy navigation to common tasks and use cases.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StartPage extends JPanel implements PropertyChangeListener {

    private final JPanel topPanel = new JPanel();
    private final JPanel mainPanel = new JPanel();
    private final JPanel lowerPanel = new JPanel();
    private final JCheckBox openOnStartupCheckbox = new JCheckBox("Show On Startup");
    private final JTextField searchField;
    private final JButton searchButton;
    
    private String searchClass;
    
    public StartPage() {
            
        JLabel title = new JLabel("Welcome to the Janelia Workstation");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        topPanel.setLayout(new BorderLayout());
        topPanel.add(title, BorderLayout.CENTER);

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
                if (!StringUtils.isBlank(searchString)) {
                    NewFilterActionListener actionListener = new NewFilterActionListener(searchString, searchClass);
                    actionListener.actionPerformed(e);
                }
            }
        });

        JToggleButton button1 = new JToggleButton("Confocal Samples");
        button1.setSelected(true);
        button1.setMargin(new Insets(5,5,5,5));
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchClass = Sample.class.getSimpleName();
            }
        });

        JToggleButton button2 = new JToggleButton("LSM Images");
        button2.setMargin(new Insets(5,5,5,5));
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchClass = LSMImage.class.getSimpleName();
            }
        });
        
        ButtonGroup group = new ButtonGroup();
        group.add(button1);
        group.add(button2);
                
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
        buttonsPanel.add(button1);
        buttonsPanel.add(button2);
        
        JLabel promptLabel = new JLabel("What would you like to find?");
        promptLabel.setFont(promptLabel.getFont().deriveFont(Font.BOLD, 16));
        
        mainPanel.setLayout(new MigLayout("gap 50, fill, wrap 2", "[grow 50]5[grow 50]", "[grow 50]5[grow 0]5[grow 75]"));
        mainPanel.add(promptLabel, "gap 10, span 2, al center bottom");
        mainPanel.add(buttonsPanel, "span 2, al center");
        mainPanel.add(searchField, "height 35, al right top");
        mainPanel.add(searchButton, "al left top");

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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ApplicationOptions.PROP_SHOW_START_PAGE_ON_STARTUP)) {
            openOnStartupCheckbox.setSelected(ApplicationOptions.getInstance().isShowStartPageOnStartup());
        }
    }
}
