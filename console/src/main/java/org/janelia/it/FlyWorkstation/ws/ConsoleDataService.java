package org.janelia.it.FlyWorkstation.ws;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.janelia.it.FlyWorkstation.gui.framework.keybind.OntologyKeyBindings;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationSession;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * The Console server interface for clients to call in order to request data. This implementation accepts and returns
 * SOAP-friendly types like arrays instead of collection interfaces like Lists, which are not supported by JAX-WS.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface ConsoleDataService extends Remote {

	public int reservePort(
			@WebParam(name = "clientName") String clientName) 
			throws RemoteException;

	public void registerClient(
			@WebParam(name = "port") int port, 
			@WebParam(name = "endpointUrl") String endpointUrl) 
			throws RemoteException;

    public void selectOutlineEntity(
			@WebParam(name = "uniqueId") String uniqueId,
			@WebParam(name = "clearAll") boolean clearAll)
    		throws RemoteException;

    public void deselectOutlineEntity(
			@WebParam(name = "uniqueId") String uniqueId)
    		throws RemoteException;
    
    public void selectEntity(
			@WebParam(name = "entityId") long entityId,
			@WebParam(name = "clearAll") boolean clearAll)
    		throws RemoteException;

    public void deselectEntity(
			@WebParam(name = "entityId") long entityId)
    		throws RemoteException;
    
    public void createAnnotation(
    		@WebParam(name = "annotation") OntologyAnnotation annotation) 
    		throws RemoteException;

    public void removeAnnotation(
    		@WebParam(name = "annotationId") long annotationId) 
    		throws RemoteException;
    	
    public Entity[] getAnnotationsForEntity(
			@WebParam(name = "entityId") long entityId)
    		throws RemoteException;

    public Entity[] getAnnotationsForEntities(
			@WebParam(name = "entityIds") Long[] entityIds)
    		throws RemoteException;

	public Entity getOntology(
			@WebParam(name = "rootId") long rootId)
			throws RemoteException;

	public AnnotationSession getAnnotationSession(
			@WebParam(name = "sessionId") long sessionId)
			throws RemoteException;
	
	public OntologyKeyBindings getKeybindings(
			@WebParam(name = "ontologyId") long ontologyId)
			throws RemoteException;
	
    public Entity getEntityById(
    		@WebParam(name = "entityId") long entityId)
    		throws RemoteException;

	public Entity getEntityTree(
			@WebParam(name = "entityId") long entityId) 
			throws RemoteException;

    public EntityData[] getParentEntityDataArray(
    		@WebParam(name = "childEntityId") long childEntityId)
    		throws RemoteException;
    
    public String getUserAnnotationColor(
    		@WebParam(name = "username") String username)
    		throws RemoteException;
    
}