package org.janelia.it.FlyWorkstation.gui.slice_viewer.skeleton;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.janelia.it.FlyWorkstation.geom.Vec3;

public class SkeletonSwcExporter {
	private Anchor rootAnchor;

	public SkeletonSwcExporter(Anchor rootAnchor)
	{
		this.rootAnchor = rootAnchor;
	}
	
	public boolean dialogAndExport(Component parent) {
		FileDialog fileDialog = new FileDialog(new Frame(), "Save SWC file", FileDialog.SAVE);
		fileDialog.setFilenameFilter(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".swc"))
					return true;
				if (name.endsWith(".SWC"))
					return true;
				return false;
			}
		});
		fileDialog.setFile("Neuron.swc");
		fileDialog.setVisible(true);
		String fileName = fileDialog.getFile();
		if (fileName == null)
			return false; // user cancelled?
		OutputStream fileOutputStream;
		try {
			fileOutputStream = new FileOutputStream(new File(fileDialog.getDirectory(), fileDialog.getFile()));
			export(fileOutputStream);
			fileOutputStream.flush();
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(parent, 
					"ERROR: SWC file creation failed.",
					"SWC file error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(parent, 
					"ERROR: SWC file creation failed.",
					"SWC file error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	
	public Set<Anchor> getAllAnchors() {
		Set<Anchor> result = new HashSet<Anchor>();
		includeAnchorChildren(rootAnchor, result);
		return result;
	}
	
	private void includeAnchorChildren(Anchor anchor, Set<Anchor> alreadyIncluded) {
		if (alreadyIncluded.contains(anchor))
			return;
		alreadyIncluded.add(anchor);
		for (Anchor child : anchor.getNeighbors()) {
			includeAnchorChildren(child, alreadyIncluded);
		}
	}
	
	public Map<Anchor, Integer> export(OutputStream outputStream) {
		Map<Anchor, Integer> usedAnchors = new HashMap<Anchor, Integer>();
		PrintWriter writer = new PrintWriter(outputStream);
		if (writer.checkError())
			return usedAnchors;
		// To view in Vaa3d, need to recenter if values are large.
		Vec3 center = new Vec3(0,0,0);
		Set<Anchor> anchors = getAllAnchors();
		for (Anchor a : anchors) {
			center = center.plus(a.getLocation());
		}
		if (anchors.size() > 0)
			center = center.times(1.0 / anchors.size());
		//
		writer.println("# ORIGINAL_SOURCE Janelia Workstation Slice Viewer");
        writer.println("# CREATURE mouse");
        writer.println("# REGION brain");
        writer.println("# FIELD/LAYER");
        writer.println("# TYPE");
        writer.println("# CONTRIBUTOR");
        writer.println("# REFERENCE");
        writer.println("# RAW");
        writer.println("# EXTRAS");
        writer.println("# SOMA_AREA");
        writer.println("# SHINKAGE_CORRECTION 1.0 1.0 1.0");
        writer.println("# VERSION_NUMBER 1.0");
        writer.println("# VERSION_DATE 2013-07-11");
        writer.println("# SCALE 1.0 1.0 1.0");
        writer.println("# OFFSET "+center.x()+" "+center.y()+" "+center.z());
        writer.println("");
        //
        if (writer.checkError())
			return usedAnchors;
        //
        exportAnchor(1, rootAnchor, null, usedAnchors, writer, center);
        writer.flush();
		return usedAnchors;
	}

	/**
	 * 
	 * @param index
	 * @param anchor
	 * @param parentAnchor
	 * @param usedAnchors
	 * @param writer
	 * @return index of final anchor exported
	 */
	private int exportAnchor(int index, Anchor anchor, 
			Anchor parentAnchor, Map<Anchor, Integer> usedAnchors, 
			PrintWriter writer, Vec3 center) {
		if (usedAnchors.containsKey(anchor))
			return index;
		// Output anchor line:
		// From http://research.mssm.edu/cnic/swc.html:
		/*
		 n T x y z R P

		 n is an integer label that identifies the current point and increments by one from one line to the next.

		 T is an integer representing the type of neuronal segment, such as soma, axon, apical dendrite, etc. The standard accepted integer values are given below.

		     0 = undefined
		     1 = soma
		     2 = axon
		     3 = dendrite
		     4 = apical dendrite
		     5 = fork point
		     6 = end point
		     7 = custom

		 x, y, z gives the cartesian coordinates of each node.

		 R is the radius at that node.
		 P indicates the parent (the integer label) of the current point or -1 to indicate an origin (soma).
		 */

		int parentIndex = -1;
		if (parentAnchor != null)
			parentIndex = usedAnchors.get(parentAnchor);
		// Deduce element type...
		Anchor.Type elementType = anchor.getAnchorType();
		if ((elementType == Anchor.Type.UNDEFINED) && (anchor.getNeighbors().size() == 1))
			elementType = Anchor.Type.END_POINT;
		if ((elementType == Anchor.Type.UNDEFINED) && (anchor.getNeighbors().size() > 2))
			elementType = Anchor.Type.FORK_POINT;
		// n T x y z R P
		writer.print(index); // n
		writer.print(" "+elementType.ordinal()); // T
		NumberFormat numberFormat = new DecimalFormat("0.000000");
		Vec3 v = anchor.getLocation().minus(center);
		writer.print(" "+numberFormat.format(v.x())); // x
		writer.print(" "+numberFormat.format(v.y())); // y
		writer.print(" "+numberFormat.format(v.z())); // z
		writer.print(" "+numberFormat.format(anchor.getRadius())); // R
		writer.println(" "+parentIndex); // P
		//
		usedAnchors.put(anchor, index);
		int nextIndex = index;
		for (Anchor child : anchor.getNeighbors()) {
			if (usedAnchors.containsKey(child))
				continue;
			nextIndex = nextIndex + 1;
			nextIndex = exportAnchor(nextIndex, child, anchor, usedAnchors, writer, center);
		}
		return nextIndex;
	}
}
