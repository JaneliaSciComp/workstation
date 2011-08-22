package org.janelia.it.FlyWorkstation.ws;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

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

	public Entity getCurrentOntology();
	
    public Entity getEntityById(
    		@WebParam(name = "entityId") long entityId);

	public Entity getEntityTree(
			@WebParam(name = "entityId") long entityId) 
			throws RemoteException;

    public EntityData[] getParentEntityDataArray(
    		@WebParam(name = "childEntityId") long childEntityId);
}