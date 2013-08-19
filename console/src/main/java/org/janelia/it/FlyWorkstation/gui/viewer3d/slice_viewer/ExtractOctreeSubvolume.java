package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.io.File;
import java.net.MalformedURLException;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

public class ExtractOctreeSubvolume {

	/**
	 * Creates a tiff file of a subvolume from an octree on-disk volume.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 9) {
			usage(args);
			System.exit(1);
		}
		Vec3 corner1 = new Vec3(
				Double.parseDouble(args[0]),
				Double.parseDouble(args[1]),
				Double.parseDouble(args[2]));
		Vec3 corner2 = new Vec3(
				Double.parseDouble(args[3]),
				Double.parseDouble(args[4]),
				Double.parseDouble(args[5]));
		double resolutionMicrometers = Double.parseDouble(args[6]);
		File inputOctreeFolder = new File(args[7]);
		File outputTiff = new File(args[8]);
		try {
			extractSubvolume(corner1, corner2, resolutionMicrometers,
					inputOctreeFolder, outputTiff);
		} catch (MalformedURLException e) {
			System.err.println(e.getMessage());
			usage(args);
			System.exit(1);
		}
	}
	
	/**
	 * Implementation with stronger type arguments than main() has...
	 * @throws MalformedURLException 
	 */
	private static void extractSubvolume(
			Vec3 corner1,
			Vec3 corner2,
			double resolutionMicrometers,
			File inputOctreeFolder,
			File outputTiff) throws MalformedURLException 
	{
		SharedVolumeImage wholeImage = new SharedVolumeImage();
		wholeImage.loadURL(inputOctreeFolder.toURI().toURL());
		Subvolume subvolume = Subvolume.loadSubvolumeMicrometers(corner1, corner2, resolutionMicrometers, wholeImage);
		// TODO write tiff
	}

	private static void usage(String [] args) {
		System.err.println("Usage:\n"
				+" java -jar ExtractOctreeSubvolume.jar <x1>"
				+" <y1> <z1> <x2> <y2> <z2> <res> <octreeFolder> <outputTiff>");
	}
}
