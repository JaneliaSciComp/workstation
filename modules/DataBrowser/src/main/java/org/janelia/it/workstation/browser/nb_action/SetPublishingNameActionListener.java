package org.janelia.it.workstation.browser.nb_action;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.api.web.SageRestClient;
import org.janelia.it.workstation.browser.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;

import net.miginfocom.swing.MigLayout;

/**
 * Business logic for setting a line publishing name on one or more Samples.  
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class SetPublishingNameActionListener implements ActionListener {

    private final static Logger log = LoggerFactory.getLogger(SetPublishingNameActionListener.class);
    
    public static final String PUBLICATION_OWNER = "group:workstation_users";
    public static final String PUBLICATION_ONTOLOGY_NAME = "Publication";
    public static final String ANNOTATION_PUBLISHING_NAME = "PublishingName";

    private final DomainModel model = DomainMgr.getDomainMgr().getModel();
    private List<Sample> samples;
    
    public SetPublishingNameActionListener(List<Sample> samples) {
        this.samples = samples;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (samples==null || samples.isEmpty()) {
            JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                    "In order to annotate the published line name, first select some Samples.");
            return;
        }
        
        try {
            ActivityLogHelper.logUserAction("SetPublishingNameActionListener.actionPerformed");
    
            String consensusLine = null;
            for(Sample sample : samples) {
                if (consensusLine==null) {
                    consensusLine = sample.getLine();
                }
                else if (!consensusLine.equals(sample.getLine())) {
                    JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                            "In order to annotate the published line name for multiple Samples, "
                            + "they must all share the same line name.");
                    return;
                }
            }
            

            if (consensusLine==null) {
                JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                        "Selected samples have no associated line information", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            PublishingNameDialog dialog = new PublishingNameDialog(consensusLine);
            final String publishedName = dialog.showDialog();
            if (StringUtils.isEmpty(publishedName)) {
                return;
            }
    
            // Save the filter and select it in the explorer so that it opens
            SimpleWorker worker = new SimpleWorker() {
    
                @Override
                protected void doStuff() throws Exception {

                    Ontology publicationOntology = getPublicationOntology();
                    OntologyTerm publishingNameTerm = getPublishedTerm(publicationOntology, ANNOTATION_PUBLISHING_NAME);
                    
                    ApplyAnnotationAction action = ApplyAnnotationAction.get();
                    action.setObjectAnnotations(samples, publishingNameTerm, publishedName, this);
                }
    
                @Override
                protected void hadSuccess() {
                }
    
                @Override
                protected void hadError(Throwable error) {
                    ConsoleApp.handleException(error);
                }
            };
    
            worker.execute();
        }
        catch (Exception ex) {
            FrameworkImplProvider.handleException("Problem setting publishing name", ex);
        }
    }
    

    private class PublishingNameDialog extends ModalDialog {

        private final JLabel loadingLabel = new JLabel(Icons.getLoadingIcon());
        private final JPanel mainPanel;
        private final JComboBox<String> comboBox; 
        
        private String lineName;
        private String returnValue;
        
        public PublishingNameDialog(String lineName) {

            this.lineName = lineName;
            
            setTitle("Choose Publishing Name");
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(300, 200));

            this.mainPanel = new JPanel(new MigLayout("wrap 2, ins 10, fill", "[grow 0][grow 1]"));
            
            this.comboBox = new JComboBox<>(); 
            comboBox.setEditable(false);
            comboBox.setToolTipText("Choose a publishing name for this line");
            comboBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange()==ItemEvent.SELECTED) {
                        String value = (String)comboBox.getSelectedItem();
                        if (!StringUtils.isEmpty(value)) {
                            returnValue = value;
                        }
                    }
                }
            });
            
            add(loadingLabel, BorderLayout.CENTER);
            
            JButton okButton = new JButton("Apply To "+samples.size()+" Samples");
            okButton.setToolTipText("Apply changes and close");
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });

            JButton cancelButton = new JButton("Cancel");
            cancelButton.setToolTipText("Close without saving changes");
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    returnValue = null;
                    setVisible(false);
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
            
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    returnValue = null;
                }
            });
        }
        
        public void load() {
            
            SimpleWorker worker = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {

                    String currValue = getCurrAnnotationValue();

                    final SageRestClient sageClient = DomainMgr.getDomainMgr().getSageClient();
                    
                    Collection<String> possibleNames = sageClient.getPublishingNames(lineName);
                    log.info("Creating drop down menu with possible names: "+possibleNames);

                    if (possibleNames.isEmpty()) {
                        mainPanel.add(new JLabel("No publishing names are available for this line."));
                    }
                    else {
                        boolean selected = false;
                        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) comboBox.getModel();
                        model.removeAllElements();
                        for (final String publishingName : possibleNames) {
                            model.addElement(publishingName);
                            if (publishingName.equals(currValue)) {
                                selected = true;
                            }
                        }
                        if (selected && currValue!=null) {
                            model.setSelectedItem(currValue);
                            returnValue = currValue;
                        }
                        
                        mainPanel.add(new JLabel("Line Name:"), "gap para");
                        mainPanel.add(new JLabel(lineName), "gap para, ax left");
                        mainPanel.add(new JLabel("Publishing Name:"), "gap para");
                        mainPanel.add(comboBox, "gap para, ax left");
                    }
                }

                @Override
                protected void hadSuccess() {
                    remove(loadingLabel);
                    add(mainPanel, BorderLayout.CENTER);
                    // Repack to fit everything
                    setPreferredSize(null);
                    packAndShow();
                }

                @Override
                protected void hadError(Throwable error) {
                    setVisible(false);
                    ConsoleApp.handleException(error);
                }
            };

            worker.execute();
        }
        
        public String showDialog() {
            load();
            packAndShow();
            // Blocks until dialog is no longer visible, and then:
            removeAll();
            dispose();
            log.info("Returning value: "+returnValue);
            return returnValue;
        }
        
    }
    
    private String getCurrAnnotationValue() throws Exception {
        ListMultimap<Long,Annotation> annotationMap = DomainUtils.getAnnotationsByDomainObjectId(model.getAnnotations(DomainUtils.getReferences(samples)));                    
        for(Sample sample : samples) {
            for (Annotation annotation : annotationMap.get(sample.getId())) {
                if (annotation.getKey().equals(ANNOTATION_PUBLISHING_NAME)) {
                    return annotation.getValue();
                }
            }
        }
        return null;
    }

    private Ontology getPublicationOntology() throws Exception {
        Ontology publicationOntology = null;
        for(Ontology ontology : model.getOntologies()) {
            if (ontology.getOwnerKey().equals(PUBLICATION_OWNER) && ontology.getName().equals(PUBLICATION_ONTOLOGY_NAME)) {
                if (publicationOntology==null) {
                    publicationOntology = ontology;
                }
                else {
                    log.warn("More than one ontology found! Make sure that "+PUBLICATION_OWNER+" only has a single Ontology named "+PUBLICATION_ONTOLOGY_NAME);
                }
            }
        }
        
        if (publicationOntology!=null) { 
            return publicationOntology;
        }
        else {
            throw new IllegalStateException("No publication ontology found. Make sure that "+PUBLICATION_OWNER+" has an Ontology named "+PUBLICATION_ONTOLOGY_NAME);
        }
    }
    
    private OntologyTerm getPublishedTerm(Ontology publicationOntology, String termName) {
        OntologyTerm publishedTerm = publicationOntology.findTerm(termName);
        
        if (publishedTerm==null) {
            throw new IllegalStateException("No ontology term owned by "+PUBLICATION_OWNER+" was found with name '"+termName+"'");
        }
        
        return publishedTerm;
    }
}
