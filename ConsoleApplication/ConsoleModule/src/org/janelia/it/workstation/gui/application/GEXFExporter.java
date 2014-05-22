package org.janelia.it.workstation.gui.application;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import org.apache.log4j.lf5.util.StreamUtils;
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * Export an entity graph to GEXF format, usually for import into Gephi.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GEXFExporter {
	
	private static final boolean includeAnnotations = false;
	
	private static final Color lightGrey = new Color(0xCCCCCC);
	private static final Color darkGrey = new Color(0x777777);
	private static final Color blue = new Color(0x4479D4);
	private static final Color yellow = new Color(0xFFDA40);
	private static final Color olive = new Color(0x8BB42D);
	private static final Color pink = new Color(0xFC717B);
	private static final Color orange = new Color(0xFF8B3D);
	
	private EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
	private AnnotationBeanRemote annotationBean = EJBFactory.getRemoteAnnotationBean();
	
	private Set<Long> visitedNodes = new HashSet<Long>();
	
	private File outFile;
	private File nodesFile;
	private File edgesFile;

	private PrintStream nodesStream;
	private PrintStream edgesStream;
	
	public GEXFExporter(File outFile) throws IOException {
		this.outFile = outFile;
		this.nodesFile = File.createTempFile("nodes", "frag");
		this.edgesFile = File.createTempFile("edges", "frag");
		this.nodesStream = new PrintStream(nodesFile);
		this.edgesStream = new PrintStream(edgesFile);
	}
	
	public void printGEXF(String username) throws Exception {
		
		List<Entity> roots = annotationBean.getCommonRootEntities(username);
		
		for(Entity root : roots) {
//			Entity tree = annotationBean.getEntityTree(root.getId());
			System.out.println("Processing "+root.getName());
			printEntityTreeToBuffers(root);
		}

		nodesStream.close();
		edgesStream.close();
		
		printXML();
	}
	
	private void printEntityTreeToBuffers(Entity entity) {

//		if (entity.getName().contains("Screen")) return;
		if (entity.getName().contains("Single Neuron")) return;
		
		if (visitedNodes.contains(entity.getId())) {
			return;
		}
		visitedNodes.add(entity.getId());
		
		System.out.println("  Processing "+entity.getName());
		
		String type = entity.getEntityTypeName();
		Color nodeColor = getNodeColor(entity);
		int count = entity.getEntityData().size();
		int size = (int)Math.round(Math.log(count) + 1);

		nodesStream.print(getNodeXML(entity.getId().toString(), entity.getName(), nodeColor, size));

		if (EntityConstants.TYPE_SAMPLE.equals(type) || EntityConstants.TYPE_SCREEN_SAMPLE.equals(type)) {
//			printAnnotationsToBuffers(entity, getAncestors(entity));
			return;
		}
		
		if (EntityConstants.TYPE_SUPPORTING_DATA.equals(type) || 
				EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION.equals(type)) {
			if (includeAnnotations) {
				printAnnotationsToBuffers(entity, getAncestors(entity));
			}
			return;
		}
		
		Map<Long,EntityData> edges = new HashMap<Long,EntityData>(); 
		Map<Long,Integer> edgeCounts = new HashMap<Long,Integer>();
		
		for(EntityData ed : entity.getEntityData()) {
			String attr = ed.getEntityAttrName();
			if (EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE.equals(attr) || 
					EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE.equals(attr)|| 
					EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE.equals(attr)) {
				continue;
			}
			
			Entity child = ed.getChildEntity();
			
			if (child != null) {
				int edgeCount = 0;
				if (edges.containsKey(child.getId())) {
					edgeCount = edgeCounts.get(child.getId());
				}
				else {
					edges.put(child.getId(), ed);
				}
				edgeCounts.put(child.getId(), ++edgeCount);
			}
		}
		
		for(EntityData ed : edges.values()) {
			try {
				Entity child = entityBean.getEntityById(null, ed.getChildEntity().getId());
				Color edgeColor = darkGrey;
				int weight = 1 + edgeCounts.get(child.getId());
				edgesStream.print(getEdgeXML(ed.getId().toString(), ed.getEntityAttrName(), 
						entity.getId().toString(), child.getId().toString(), edgeColor, weight, "solid"));
				printEntityTreeToBuffers(child);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (includeAnnotations) {
			Set<Entity> entities = new HashSet<Entity>();
			entities.add(entity);
			printAnnotationsToBuffers(entity, entities);
		}
	}

	private void printAnnotationsToBuffers(Entity root, Set<Entity> entities) {

		try {
			List<Long> entityIds = new ArrayList<Long>();
			for(Entity entity : entities) {
				entityIds.add(entity.getId());
			}
			
			List<Entity> annotations = annotationBean.getAnnotationsForEntities(root.getOwnerKey(), entityIds);
			for(Entity annotationEntity : annotations) {
				OntologyAnnotation annotation = new OntologyAnnotation();
				annotation.init(annotationEntity);
				nodesStream.print(getNodeXML(annotation.getId().toString(), annotation.toString(), orange, 3));
				Color edgeColor = darkGrey;
				int weight = 2;
				edgesStream.print(getEdgeXML(annotation.getId().toString(), "Annotation", 
						annotation.getId().toString(), root.getId().toString(), edgeColor, weight, "dotted"));
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Set<Entity> getAncestors(Entity entity) {
        Set<Entity> items = new HashSet<Entity>();
        items.add(entity);
        for (EntityData entityData : entity.getEntityData()) {
            Entity child = entityData.getChildEntity();
            if (child != null) {
                items.addAll(getAncestors(child));
            }
        }
        return items;
    }
    
	private void printXML() throws IOException {

		PrintStream outStream = null;
		try {
			outStream = new PrintStream(outFile);
			
			outStream.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			outStream.println("<gexf xmlns=\"http://www.gexf.net/1.2draft\" version=\"1.2\">");
			outStream.println("<graph mode=\"static\" defaultedgetype=\"directed\">");
			outStream.println("<nodes>");
			
			FileInputStream nodesIn = new FileInputStream(nodesFile);
			StreamUtils.copy(nodesIn, outStream);
			nodesIn.close();
			
			outStream.println("</nodes>");
			outStream.println("<edges>");
		
			FileInputStream edgesIn = new FileInputStream(edgesFile);
			StreamUtils.copy(edgesIn, outStream);
			edgesIn.close();
			
			outStream.println("</edges>");
			outStream.println("</graph>");
			outStream.println("</gexf>");
		}
		finally {
			if (outStream!=null) outStream.close();
		}
		
		System.out.println("Finished writing "+outFile);
	}
	
	private String getNodeXML(String id, String label, Color color, int size) {
		double alpha = (double)color.getAlpha() / 255.0;
		StringBuffer sb = new StringBuffer();
		sb.append("<node id=\""+id+"\" label=\""+label+"\">\n");
		sb.append("  <color r=\""+color.getRed()+"\" g=\""+color.getGreen()+"\" b=\""+color.getBlue()+"\" a=\""+alpha+"\"/>\n");
		sb.append("  <size value=\""+size+"\"/>\n");
		sb.append("</node>\n");
		return sb.toString();
	}
	
	private String getEdgeXML(String id, String label, String source, String target, Color color, int weight, String shape) {
		double alpha = (double)color.getAlpha() / 255.0;
		StringBuffer sb = new StringBuffer();
		sb.append("<edge id=\""+id+"\" source=\""+source+"\" target=\""+target+"\" label=\""+label+"\" weight=\""+weight+"\">\n");
		sb.append("  <color r=\""+color.getRed()+"\" g=\""+color.getGreen()+"\" b=\""+color.getBlue()+"\" a=\""+alpha+"\"/>\n");
		sb.append("  <thickness value=\""+weight+"\"/>\n");
		sb.append("  <shape value=\""+shape+"\"/>\n");
		sb.append("</edge>\n");
		return sb.toString();
	}

	private Color getNodeColor(Entity entity) {
		String type = entity.getEntityTypeName();
		Color nodeColor = lightGrey;
		if (EntityConstants.TYPE_SAMPLE.equals(type) || EntityConstants.TYPE_SCREEN_SAMPLE.equals(type)) {
			nodeColor = blue;
		}
		else if (EntityConstants.TYPE_FOLDER.equals(type)) {
			nodeColor = yellow;
		}
		else if (EntityConstants.TYPE_FLY_LINE.equals(type)) {
			nodeColor = olive;
		}
//		else if (EntityConstants.TYPE_SUPPORTING_DATA.equals(type)) {
//			nodeColor = yellow;
//		}
//		else if (EntityConstants.TYPE_IMAGE_2D.equals(type)) {
//			nodeColor = olive;
//		}
//		else if (EntityConstants.TYPE_IMAGE_3D.equals(type)) {
//			nodeColor = pink;
//		}
		return nodeColor;
	}
	
	public static void main(String[] args) throws Exception {
	
		GEXFExporter e = new GEXFExporter(new File("/Users/rokickik/jacs.gexf"));
		e.printGEXF("group:flylight");
		
	}
}
