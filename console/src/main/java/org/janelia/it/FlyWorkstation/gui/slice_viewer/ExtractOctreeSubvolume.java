package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Iterator;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

import com.google.common.collect.Iterators;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;

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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Implementation with stronger type arguments than main() has...
	 * @throws IOException 
	 */
	private static void extractSubvolume(
			Vec3 corner1,
			Vec3 corner2,
			double resolutionMicrometers,
			File inputOctreeFolder,
			File outputTiff) throws IOException 
	{
		SharedVolumeImage wholeImage = new SharedVolumeImage();
		wholeImage.loadURL(inputOctreeFolder.toURI().toURL());
		Subvolume subvolume = Subvolume.loadSubvolumeMicrometers(corner1, corner2, resolutionMicrometers, wholeImage);
		// Write output tiff
		BufferedImage outSlices[] = subvolume.getAsBufferedImages();
		TIFFEncodeParam params = new TIFFEncodeParam();
		Iterator<BufferedImage> it = Iterators.forArray(outSlices);
		if (it.hasNext()) it.next(); // Avoid duplicate first slice
		params.setExtraImages(it); 
		OutputStream out = new FileOutputStream(outputTiff);
		ImageEncoder encoder = ImageCodec.createImageEncoder("tiff", out, params);
		encoder.encode(outSlices[0]); 
		out.close(); 
	}

	private static void usage(String [] args) {
		System.err.println("Usage:\n"
				+" java -jar ExtractOctreeSubvolume.jar <x1>"
				+" <y1> <z1> <x2> <y2> <z2> <res> <octreeFolder> <outputTiff>");
	}
}
