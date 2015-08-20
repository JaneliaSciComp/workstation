package org.janelia.it.workstation.gui.browser.api.facade.interfaces;

import java.util.Collection;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;

/**
 * Interface for client implementations providing domain object access. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainFacade {

    public List<Subject> getSubjects();
        
    public DomainObject getDomainObject(Class<? extends DomainObject> domainClass, Long id);
    
    public DomainObject getDomainObject(Reference reference);
    
    public List<DomainObject> getDomainObjects(List<Reference> references);
    
    public List<DomainObject> getDomainObjects(String type, Collection<Long> ids);
    
    public List<Annotation> getAnnotations(Collection<Long> targetIds);
    
    public Workspace getDefaultWorkspace();
    
    public Collection<Workspace> getWorkspaces();
    
    public Collection<Ontology> getOntologies();
    
    public void changePermissions(String type, Collection<Long> ids, String granteeKey, String rights, boolean grant) throws Exception;
    
    public void save(TreeNode treeNode) throws Exception;
    
    public void save(Filter filter) throws Exception;
    
    public void save(ObjectSet objectSet) throws Exception;
            
    public void reorderChildren(TreeNode treeNode, int[] order) throws Exception;       
    
    public void addChildren(TreeNode treeNode, Collection<Reference> references) throws Exception;
    
    public void removeChildren(TreeNode treeNode, Collection<Reference> references) throws Exception;
    
    public void addMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception;
    
    public void removeMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception;
    
    public void updateProperty(DomainObject domainObject, String propName, String propValue);
    
}
