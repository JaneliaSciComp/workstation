package org.janelia.it.workstation.gui.application;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Serialize and deserialize Entity object graphs to XML.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class JAXBSerializer {
	
	public static void serializeUsingJAXB(Object entity, File file) throws JAXBException {
	    JAXBContext context = JAXBContext.newInstance(entity.getClass());
	    context.createMarshaller().marshal(entity, file);
	}

	public static void seralizeCommonRoots(String username, File dir) throws Exception {
		List<Entity> roots = org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory.getRemoteAnnotationBean().getCommonRootEntities(username);
		for(Entity root : roots) {
			Entity tree = org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory.getRemoteEntityBean().getEntityTree(username, root.getId());
			File file = new File(dir, "commonRoot-"+root.getId()+".xml");
			JAXBSerializer.serializeUsingJAXB(tree, file);
		}
	}

	/**
	 * Test Harness
	 */
	public static void main(String[] args) throws Exception {

		JAXBSerializer.seralizeCommonRoots("user:rokickik", new File("/Users/rokickik/serialize"));
		
		
	}

}
