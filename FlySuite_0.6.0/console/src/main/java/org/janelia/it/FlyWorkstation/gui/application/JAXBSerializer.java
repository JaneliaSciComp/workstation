package org.janelia.it.FlyWorkstation.gui.application;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
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

	public static void seralizeCommonRoots(String username, File dir) throws JAXBException {
		AnnotationBeanRemote annotationBean = EJBFactory.getRemoteAnnotationBean();
		List<Entity> roots = annotationBean.getCommonRootEntitiesByTypeName(username, "Folder");
		for(Entity root : roots) {
			Entity tree = annotationBean.getEntityTree(root.getId());
			File file = new File(dir, "commonRoot-"+root.getId()+".xml");
			JAXBSerializer.serializeUsingJAXB(tree, file);
		}
	}

	/**
	 * Test Harness
	 */
	public static void main(String[] args) throws Exception {

		JAXBSerializer.seralizeCommonRoots("rokickik", new File("/Users/rokickik/serialize"));
		
		
	}

}
