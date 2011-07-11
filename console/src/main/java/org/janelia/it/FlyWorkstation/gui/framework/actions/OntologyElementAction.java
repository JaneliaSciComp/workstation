/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/22/11
 * Time: 1:35 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.actions;

import org.janelia.it.jacs.model.ontology.OntologyElement;

/**
 * An abstract base class for actions dealing with entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class OntologyElementAction implements Action {

    private OntologyElement element;

    public void init(OntologyElement element) {
        this.element = element;
    }
    
    public OntologyElement getOntologyElement() {
        return element;
    }

    @Override
    public String getName() {
        return element.getName();
    }

    @Override
    public abstract void doAction();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OntologyElementAction)) return false;
        OntologyElementAction that = (OntologyElementAction) o;
        if (!element.equals(that.element)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }
}
