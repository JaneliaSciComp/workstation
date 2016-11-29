package org.janelia.it.workstation.browser.nb_action;

import static org.janelia.it.workstation.browser.gui.editor.FilterEditorPanel.DEFAULT_SEARCH_CLASS;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.browser.gui.support.DropDownButton;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;

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

        try {
            ActivityLogHelper.logUserAction("SetPublishingNameActionListener.actionPerformed");
    
            String consensusLine = null;
            for(Sample sample : samples) {
                if (consensusLine==null) {
                    consensusLine = sample.getLine();
                }
                else if (!consensusLine.equals(sample.getLine())) {
                    JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), 
                            "In order to annotate the published line name for multiple samples, "
                            + "they must all share the same line name.");
                    return;
                }
            }
            
            PublishingNameDialog dialog = new PublishingNameDialog();
            dialog.load();
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

        private final JPanel mainPanel = new JPanel(new BorderLayout());
        private DropDownButton publishingNameButton;
        
        private String returnValue;
        
        public PublishingNameDialog() {

            setLayout(new BorderLayout());
            
            add(new JLabel(Icons.getLoadingIcon()), BorderLayout.CENTER);
            
            JButton okButton = new JButton("OK");
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

            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    returnValue = null;
                }
            });
        }
        
        public void load() {

            final DomainModel model = DomainMgr.getDomainMgr().getModel();
            
            // Save the filter and select it in the explorer so that it opens
            SimpleWorker worker = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {

                    String currValue = getCurrValue();
                    returnValue = currValue;

                    publishingNameButton = new DropDownButton();
                    
                    List<String> possibleNames = null;//TODO: implement restful client
                    
                    ButtonGroup typeGroup = new ButtonGroup();
                    for (final String publishingName : possibleNames) {
                        JMenuItem menuItem = new JRadioButtonMenuItem(publishingName, publishingName.equals(currValue));
                        menuItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                returnValue = publishingName;
                            }
                        });
                        typeGroup.add(menuItem);
                        publishingNameButton.getPopupMenu().add(menuItem);
                    }
                }

                @Override
                protected void hadSuccess() {
                    add(mainPanel, BorderLayout.CENTER);
                }

                @Override
                protected void hadError(Throwable error) {
                    ConsoleApp.handleException(error);
                }
            };

            worker.execute();
        }
        
        public String showDialog() {
            packAndShow();
            // Blocks until dialog is no longer visible, and then:
            removeAll();
            dispose();
            return returnValue;
        }
        
    }
    
    private String getCurrValue() throws Exception {
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
