package org.janelia.it.FlyWorkstation.gui.application;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * Export an entity graph to Neo4j's batch-importer tab-delimited format.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Neo4jBatchExporter {
	
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
	
	public Neo4jBatchExporter(File outFile) throws IOException {
		this.outFile = outFile;
		this.nodesFile = new File(outFile,"nodes.csv");
		this.edgesFile = new File(outFile,"edges.csv");
		this.nodesStream = new PrintStream(nodesFile);
		this.edgesStream = new PrintStream(edgesFile);
	}
	
	public void printCSV(String username) throws Exception {
		
		List<Entity> roots = annotationBean.getCommonRootEntities(username);
		
		for(Entity root : roots) {
			System.out.println("Processing "+root.getName());
			printEntityTreeToBuffers(root);
		}

		nodesStream.print("Id\tName\tType\n");
		edgesStream.print("Source\tTarget\tType\tWeight\n");

		nodesStream.close();
		edgesStream.close();
	}
	
	private void printEntityTreeToBuffers(Entity entity) {

		if (entity.getName().contains("Screen")) return;
//		if (entity.getName().contains("Single Neuron")) return;
		
		if (visitedNodes.contains(entity.getId())) {
			return;
		}
		visitedNodes.add(entity.getId());
		
//		System.out.println("  Processing "+entity.getName());
		
		String type = entity.getEntityTypeName();
		Color nodeColor = getNodeColor(entity);
		int count = entity.getEntityData().size();
		int size = (int)Math.round(Math.log(count) + 1);

		nodesStream.print(getNodeCSV(entity.getId().toString(), entity.getName(), type, nodeColor, size));

//		if (EntityConstants.TYPE_SAMPLE.equals(type) || EntityConstants.TYPE_SCREEN_SAMPLE.equals(type)) {
//			return;
//		}
		
//		if (EntityConstants.TYPE_SUPPORTING_DATA.equals(type) || 
//				EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION.equals(type)) {
//			return;
//		}
		
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
				edgesStream.print(getEdgeCSV(ed.getId().toString(), ed.getEntityAttrName(), 
						entity.getId().toString(), child.getId().toString(), edgeColor, weight, "solid"));
				printEntityTreeToBuffers(child);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
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
    
	private String getNodeCSV(String id, String label, String type, Color color, int size) {
		double alpha = (double)color.getAlpha() / 255.0;
		StringBuffer sb = new StringBuffer();
		sb.append(id);
		sb.append("\t");
		sb.append(label);
		sb.append("\t");
		sb.append(type);
		sb.append("\n");
		return sb.toString();
	}
	
	private String getEdgeCSV(String id, String label, String source, String target, Color color, int weight, String shape) {
		double alpha = (double)color.getAlpha() / 255.0;
		StringBuffer sb = new StringBuffer();
		sb.append(source);
		sb.append("\t");
		sb.append(target);
		sb.append("\t");
		sb.append(label);
		sb.append("\t");
		sb.append(weight);
		sb.append("\n");
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
	
		Neo4jBatchExporter e = new Neo4jBatchExporter(new File("/Users/rokickik"));
		e.printCSV("group:flylight");
		
	}
}
