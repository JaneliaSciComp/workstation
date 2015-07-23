package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.lang.ref.WeakReference;

import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.nodes.children.OntologyChildFactory;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.Children;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyTermNode extends InternalNode<OntologyTerm> {
    
    private final static Logger log = LoggerFactory.getLogger(OntologyTermNode.class);
    
    private final WeakReference<Ontology> ontologyRef;
    
    public OntologyTermNode(Ontology ontology, OntologyTerm ontologyTerm) throws Exception {
        super(DomainUtils.isEmpty(ontologyTerm.getTerms())
                ?Children.LEAF
                :Children.create(new OntologyChildFactory(ontology, ontologyTerm), false), ontologyTerm);
        this.ontologyRef = new WeakReference<Ontology>(ontology);
    }
    
    private OntologyTerm getOntologyTerm() {
        return (OntologyTerm)getObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getOntologyTerm().getName();
    }
    
    @Override
    public String getSecondaryLabel() {
        return getOntologyTerm().getTypeName();
    }
    
    @Override
    public Image getIcon(int type) {
        String termType = getOntologyTerm().getTypeName();
        if ("Category".equals(termType)) {
            return Icons.getIcon("folder.png").getImage();
        }
        else if ("Enum".equals(termType)) {
            return Icons.getIcon("folder_page.png").getImage();
        }
        else if ("Interval".equals(termType)) {
            return Icons.getIcon("page_white_code.png").getImage();
        }
        else if ("Tag".equals(termType)) {
            return Icons.getIcon("page_white.png").getImage();
        }
        else if ("Text".equals(termType)) {
            return Icons.getIcon("page_white_text.png").getImage();
        }
        else if ("Custom".equals(termType)) {
            return Icons.getIcon("page_white_text.png").getImage();
        }
        else if ("EnumItem".equals(termType)) {
            return Icons.getIcon("page.png").getImage();
        }
        else if ("EnumText".equals(termType)) {
            return Icons.getIcon("page_go.png").getImage();
        }
        return Icons.getIcon("bullet_error.png").getImage();
    }
}
