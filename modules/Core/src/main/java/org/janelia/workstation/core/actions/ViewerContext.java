package org.janelia.workstation.core.actions;

import java.util.List;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;

/**
 * Current viewer context which can be used to construct context menus.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerContext {

    private Object contextObject;
    private ChildSelectionModel<DomainObject, Reference> editSelectionModel;
    private List<DomainObject> domainObjectList;
    private ArtifactDescriptor resultDescriptor;
    private String typeName;

    public ViewerContext(Object contextObject, List<DomainObject> domainObjectList,
                                   ArtifactDescriptor resultDescriptor, String typeName,
                         ChildSelectionModel<DomainObject,Reference> editSelectionModel) {
        this.contextObject = contextObject;
        this.domainObjectList = domainObjectList;
        this.resultDescriptor = resultDescriptor;
        this.typeName = typeName;
        this.editSelectionModel = editSelectionModel;
    }

    public Object getContextObject() {
        return contextObject;
    }

    public ChildSelectionModel<DomainObject, Reference> getEditSelectionModel() {
        return editSelectionModel;
    }

    public List<DomainObject> getDomainObjectList() {
        return domainObjectList;
    }

    public DomainObject getDomainObject() {
        return domainObjectList.size() == 1 ? domainObjectList.get(0) : null;
    }

    public boolean isMultiple() {
        return domainObjectList.size() > 1;
    }

    public ArtifactDescriptor getResultDescriptor() {
        return resultDescriptor;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public String toString() {
        return "ViewerContext[" +
                "contextObject=" + contextObject +
                ", editSelectionModel=" + editSelectionModel +
                ", domainObjectList=" + domainObjectList +
                ", resultDescriptor=" + resultDescriptor +
                ", typeName='" + typeName + '\'' +
                ']';
    }
}
