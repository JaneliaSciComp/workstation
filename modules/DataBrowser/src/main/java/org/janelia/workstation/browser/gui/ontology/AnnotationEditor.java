package org.janelia.workstation.browser.gui.ontology;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.browser.gui.dialogs.AnnotationBuilderDialog;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.model.domain.ontology.Accumulation;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.EnumText;
import org.janelia.model.domain.ontology.Interval;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.ontology.OntologyTermReference;
import org.janelia.model.domain.ontology.Text;

/**
 * Editor for annotation values.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationEditor {
    
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
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        if (keyTerm == null) {
            try {
                keyTerm = model.getOntologyTermByReference(annotation.getKeyTerm());
            }
            catch (Exception e) {
                FrameworkImplProvider.handleException(e);
            }
        }
    }

    public boolean needsEditor() {
        return (keyTerm instanceof Interval || keyTerm instanceof EnumText || keyTerm instanceof Accumulation || keyTerm instanceof Text);
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

                value = JOptionPane.showInputDialog(FrameworkImplProvider.getMainFrame(), "Value:\n", currValue);

                Interval interval = (Interval) keyTerm;
                if (StringUtils.isEmpty(value)) return null;
                try {
                    Double dvalue = Double.parseDouble(value);
                    if (dvalue < interval.getLowerBound().doubleValue() || dvalue > interval.getUpperBound().doubleValue()) {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                            "Input out of range [" + interval.getLowerBound() + "," + interval.getUpperBound() + "]");
                    return null;
                }
            }
            else if (keyTerm instanceof EnumText) {

                Long valueEnumId = ((EnumText) keyTerm).getValueEnumId();
                OntologyTermReference enumRef = new OntologyTermReference(ontology.getId(), valueEnumId);
                OntologyTerm valueEnum = model.getOntologyTermByReference(enumRef);

                if (valueEnum == null) {
                    Exception error = new Exception(keyTerm.getName() + " has no supporting enumeration.");
                    FrameworkImplProvider.handleException(error);
                    return null;
                }

                Object currValue = null;
                if (annotation != null && annotation.getValue() != null) {
                    for (OntologyTerm term : valueEnum.getTerms()) {
                        if (term.getName().equals(annotation.getValue())) {
                            currValue = term;
                        }
                    }
                }

                OntologyTerm enumTerm = (OntologyTerm) JOptionPane.showInputDialog(FrameworkImplProvider.getMainFrame(),
                        "Value:\n", keyTerm.getName(), JOptionPane.PLAIN_MESSAGE, null, valueEnum.getTerms().toArray(), currValue);
                if (enumTerm != null) {
                    value = enumTerm.getName();
                }
            }
            else if (keyTerm instanceof Accumulation) {

                String currValue = null;
                if (annotation != null) {
                    currValue = annotation.getValue();
                }

                AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
                dialog.setAnnotationValue(currValue);
                dialog.setVisible(true);
                value = dialog.getAnnotationValue();
            }
            else if (keyTerm instanceof Text) {

                String currValue = null;
                if (annotation != null) {
                    currValue = annotation.getValue();
                }

                value = JOptionPane.showInputDialog(FrameworkImplProvider.getMainFrame(), "Value:\n", currValue);
            }

            return value;
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
            return null;
        }
    }
    
}
