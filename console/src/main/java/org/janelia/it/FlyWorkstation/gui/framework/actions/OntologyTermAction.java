/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/22/11
 * Time: 1:35 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.actions;

import org.janelia.it.FlyWorkstation.gui.framework.outline.OntologyTerm;

/**
 * An abstract base class for actions dealing with entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class OntologyTermAction implements Action {

    private OntologyTerm term;

    public OntologyTermAction(OntologyTerm term) {
        this.term = term;
    }

    public OntologyTerm getOntologyTerm() {
        return term;
    }

    @Override
    public String getName() {
        return term.getName();
    }

    @Override
    public abstract void doAction();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OntologyTermAction)) return false;
        OntologyTermAction that = (OntologyTermAction) o;
        if (!term.equals(that.term)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return term.hashCode();
    }
}
