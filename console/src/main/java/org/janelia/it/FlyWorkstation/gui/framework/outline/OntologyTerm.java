/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/16/11
 * Time: 12:37 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyTermType;

/**
 * An ontology term backed by an Entity. Has a type and an associated action.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyTerm {

    private Entity entity;
    private OntologyTerm parentTerm;
    private Action action;
    private OntologyTermType type;

    public OntologyTerm(Entity entity, OntologyTerm parentTerm) {
        this.entity = entity;
        this.parentTerm = parentTerm;

        String typeName = entity.getValueByAttributeName(EntityConstants.ATTR_NAME_ONTOLOGY_TERM_TYPE);
        if (!Utils.isEmpty(typeName)) {
            type = OntologyTermType.createTypeByName(typeName);
            if (type != null) type.init(entity);
        }
    }

    public String getName() {
        return entity.getName();
    }

    public Long getId() {
        return entity.getId();
    }

    public Entity getEntity() {
        return entity;
    }

    public OntologyTerm getParentTerm() {
		return parentTerm;
	}

	public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public OntologyTermType getType() {
        return type;
    }

    public void setType(OntologyTermType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OntologyTerm)) return false;
        OntologyTerm that = (OntologyTerm) o;
        return entity.getId().equals(that.getEntity().getId());
    }

    @Override
    public int hashCode() {
        return entity.hashCode();
    }
}
