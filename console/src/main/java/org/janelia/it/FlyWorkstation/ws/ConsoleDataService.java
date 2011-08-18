package org.janelia.it.FlyWorkstation.ws;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.janelia.it.jacs.model.entity.Entity;

@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService
public interface ConsoleDataService extends Remote {

	public abstract int reservePort(
			@WebParam(name = "clientName") String clientName) 
			throws RemoteException;

	public abstract void registerClient(
			@WebParam(name = "port") int port, 
			@WebParam(name = "endpointUrl") String endpointUrl) 
			throws RemoteException;

	public abstract Entity getEntityTree(
			@WebParam(name = "entityId") Long entityId) 
			throws RemoteException;

}