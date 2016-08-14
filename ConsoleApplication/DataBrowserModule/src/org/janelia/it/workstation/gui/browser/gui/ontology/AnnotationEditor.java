package org.janelia.it.workstation.gui.browser.gui.ontology;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.EnumText;
import org.janelia.it.jacs.model.domain.ontology.Interval;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.ontology.Text;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.gui.dialogs.AnnotationBuilderDialog;
import org.janelia.it.workstation.gui.browser.nb_action.ApplyAnnotationAction;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for annotation values.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationEditor {

    private final static Logger log = LoggerFactory.getLogger(ApplyAnnotationAction.class);

    public static final String CANCEL_VALUE = "CANCEL";

    private final Ontology ontology;
    private Annotation annotation;
    private OntologyTerm keyTerm;
    
    public AnnotationEditor(Ontology ontology, Annotation annotation) {
        this.ontology = ontology;
        this.annotation = annotation;
    }

    public AnnotationEditor(Ontology ontology, OntologyTerm keyTerm) {
        this.ontology = ontology;
        this.keyTerm = keyTerm;
    }

    public String showEditor() {
        try {
            DomainModel model = DomainMgr.getDomainMgr().getModel();
            if (keyTerm == null) {
                keyTerm = model.getOntologyTermByReference(annotation.getKeyTerm());
            }

            String value = null;
            if (keyTerm instanceof Interval) {

                String currValue = null;
                if (annotation != null) {
                    currValue = annotation.getValue();
                }

                value = JOptionPane.showInputDialog(SessionMgr.getMainFrame(), "Value:\n", currValue);

                Interval interval = (Interval) keyTerm;
                if (StringUtils.isEmpty(value)) return null;
                try {
                    Double dvalue = Double.parseDouble(value);
                    if (dvalue < interval.getLowerBound().doubleValue() || dvalue > interval.getUpperBound().doubleValue()) {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
                            "Input out of range [" + interval.getLowerBound() + "," + interval.getUpperBound() + "]");
                    return CANCEL_VALUE;
                }
            }
            else if (keyTerm instanceof EnumText) {

                Long valueEnumId = ((EnumText) keyTerm).getValueEnumId();
                OntologyTermReference enumRef = new OntologyTermReference(ontology.getId(), valueEnumId);
                OntologyTerm valueEnum = model.getOntologyTermByReference(enumRef);

                if (valueEnum == null) {
                    Exception error = new Exception(keyTerm.getName() + " has no supporting enumeration.");
                    SessionMgr.getSessionMgr().handleException(error);
                    return CANCEL_VALUE;
                }

                Object currValue = null;
                if (annotation != null && annotation.getValue() != null) {
                    for (OntologyTerm term : valueEnum.getTerms()) {
                        if (term.getName().equals(annotation.getValue())) {
                            currValue = term;
                        }
                    }
                }

                OntologyTerm enumTerm = (OntologyTerm) JOptionPane.showInputDialog(SessionMgr.getMainFrame(),
                        "Value:\n", keyTerm.getName(), JOptionPane.PLAIN_MESSAGE, null, valueEnum.getTerms().toArray(), currValue);
                if (enumTerm != null) {
                    value = enumTerm.getName();
                }
            }
            else if (keyTerm instanceof Text) {

                String currValue = null;
                if (annotation != null) {
                    currValue = annotation.getValue();
                }

                AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
                dialog.setAnnotationValue(currValue);
                dialog.setVisible(true);
                value = dialog.getAnnotationValue();
                if (value==null) return CANCEL_VALUE;
            }

            return value;
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
            return null;
        }
    }
    
}
