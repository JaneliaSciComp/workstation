package org.janelia.it.workstation.browser.gui.dialogs;

import static org.janelia.it.workstation.browser.gui.options.OptionConstants.LAST_SHOWN_RELEASE_NOTES;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.gui.options.ApplicationOptions;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog for viewing Release Notes for the system.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ReleaseNotesDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(ReleaseNotesDialog.class);
    
    private static final String CURR_RELEASE_FILE = "resources/release_notes/curr_release.txt";
    private static final String PREV_RELEASES_FILE = "resources/release_notes/prev_releases.txt";
    
    private final JLabel titleLabel;
    private final JCheckBox showAfterUpdate;
    private final JTextArea textArea;
    private final JButton prevButton;
    private final JButton nextButton;
    
    private List<ReleaseNotes> releaseNotesList;
    private int currIndex = 0;

    private JScrollPane scrollPane;
    
    public ReleaseNotesDialog() {

        setTitle("Release Notes");

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setText("Loading...");
        
        this.titleLabel = new JLabel("Release Notes");
        add(titleLabel, BorderLayout.NORTH);
        
        scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        
        this.showAfterUpdate = new JCheckBox("Show release notes after update");
        
        prevButton = new JButton("Loading...");
        prevButton.setEnabled(false);
        prevButton.setToolTipText("See release notes for the previous version");
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currIndex++;
                updatePrevNextButtons();
                showReleaseNotes();
            }
        });
        
        nextButton = new JButton("Next");
        nextButton.setEnabled(false);
        nextButton.setToolTipText("See release notes for the next version");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currIndex--;
                updatePrevNextButtons();
                showReleaseNotes();
            }
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(showAfterUpdate);
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(prevButton);
        buttonPane.add(nextButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    private void updatePrevNextButtons() {
        prevButton.setEnabled(currIndex<releaseNotesList.size()-1);
        nextButton.setEnabled(currIndex>0);
    }
    
    public void showIfFirstRunSinceUpdate() {
        if (!ApplicationOptions.getInstance().isShowReleaseNotes()) return;
        String appVersion = ConsoleApp.getConsoleApp().getApplicationVersion();
        if ("DEV".equals(appVersion)) return; // Never show release notes in normal development
        if (!appVersion.equals(getLastShownReleaseNotes())) {
            setLastShownReleaseNotes(appVersion);
            showCurrentReleaseNotes();
        }
    }
    
    public void showCurrentReleaseNotes() {
        showAfterUpdate.setSelected(ApplicationOptions.getInstance().isShowReleaseNotes());
        
        boolean firstRun = releaseNotesList==null;
        
        if (firstRun) {
            releaseNotesList = new ArrayList<>();
            loadReleaseNotes(CURR_RELEASE_FILE);
        }
        
        this.currIndex = 0;
        updatePrevNextButtons();
        showReleaseNotes();
        
        if (firstRun) {
            SimpleWorker worker = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {
                    loadReleaseNotes(PREV_RELEASES_FILE);
                    Thread.sleep(2000);
                }

                @Override
                protected void hadSuccess() {
                    prevButton.setText("Previous");
                    updatePrevNextButtons();
                }

                @Override
                protected void hadError(Throwable error) {
                    ConsoleApp.handleException(error);
                }

            };
            worker.execute();
        }
        
        packAndShow();
    }
    
    private void loadReleaseNotes(String filepath) {

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filepath); BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String version = null;
            StringBuilder notes = new StringBuilder();
            
            boolean ignoreEmptyLines = true;
            
            String inputLine = null;
            while ((inputLine = br.readLine()) != null) {
                                
                if (inputLine.startsWith("//")) {
                    // Starting next release notes set, do we have one already being built?
                    if (notes.length()>0 && version!=null) {
                        releaseNotesList.add(new ReleaseNotes(version, notes.toString()));
                        notes = new StringBuilder();
                    }
                    version = inputLine.replace("//", "").trim();
                    ignoreEmptyLines = true;
                }
                else {
                    if (StringUtils.isBlank(inputLine) && ignoreEmptyLines) {
                        // Ignored...
                    }
                    else {
                        notes.append(inputLine).append("\n");
                        ignoreEmptyLines = false; // Got our first non-blank line, go back to not ignoring them
                    }
                }
            }
            
            if (version==null) {
                log.error("Release notes in "+filepath+" contain a section with no identifiable version number. Each section must start with a line containing '// VERSION_NUMBER'");
                version = "Unknown";
            }
            
            releaseNotesList.add(new ReleaseNotes(version, notes.toString()));
            
        }
        catch (IOException e) {
            log.error("Error reading "+CURR_RELEASE_FILE);
        }
    }

    private void showReleaseNotes() {
        if (currIndex < 0 || currIndex > releaseNotesList.size()-1) {
            throw new IllegalStateException("Illegal release note index: "+currIndex);
        }
        
        ReleaseNotes releaseNotes = releaseNotesList.get(currIndex);
        titleLabel.setText("Release Notes for Janelia Workstation Version "+releaseNotes.getVersion());
        textArea.setText(releaseNotes.getNotes());
        
        // Reset scrollbar
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                scrollPane.getVerticalScrollBar().setValue(0);
            }
         });
    }
    
    private void saveAndClose() {
        ApplicationOptions.getInstance().setShowReleaseNotes(showAfterUpdate.isSelected());
        setVisible(false);
    }
    
    public static String getLastShownReleaseNotes() {
        return (String) FrameworkImplProvider.getModelProperty(LAST_SHOWN_RELEASE_NOTES);
    }
    
    private static void setLastShownReleaseNotes(String value) {
        FrameworkImplProvider.setModelProperty(LAST_SHOWN_RELEASE_NOTES, value);  
    }
    
    private class ReleaseNotes {
        private String version;
        private String notes;
        
        public ReleaseNotes(String version, String notes) {
            super();
            this.version = version;
            this.notes = notes;
        }
        public String getVersion() {
            return version;
        }
        public String getNotes() {
            return notes;
        }
        
    }
}
