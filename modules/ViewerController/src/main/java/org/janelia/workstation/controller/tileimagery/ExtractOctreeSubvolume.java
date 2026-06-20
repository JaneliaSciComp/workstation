package org.janelia.workstation.controller.tileimagery;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.integration.util.FrameworkAccess;

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
			FrameworkAccess.handleException(e);
		}
	}

	/**
	 * Implementation with stronger type arguments than main() has...
	 * @throws IOException
	 */
	public static void extractSubvolume(
			Vec3 corner1,
			Vec3 corner2,
			double resolutionMicrometers,
			File inputOctreeFolder,
			File outputTiff) throws IOException
	{
		SharedVolumeImage wholeImage = new SharedVolumeImage();
		wholeImage.loadURL(inputOctreeFolder.toURI().toURL());
		Subvolume subvolume = new Subvolume(corner1, corner2, resolutionMicrometers, wholeImage);
		// Write multi-page output tiff using ImageIO + TwelveMonkeys
		BufferedImage outSlices[] = subvolume.getAsBufferedImages();
		try (FileOutputStream fos = new FileOutputStream(outputTiff);
		     ImageOutputStream ios = ImageIO.createImageOutputStream(fos)) {
			ImageWriter writer = TiffImageIOHelper.getTiffWriter();
			writer.setOutput(ios);
			writer.prepareWriteSequence(null);
			for (BufferedImage outSlice : outSlices) {
				writer.writeToSequence(new IIOImage(outSlice, null, null), null);
			}
			writer.endWriteSequence();
			writer.dispose();
		}
	}

	private static void usage(String [] args) {
		System.err.println("Usage:\n"
				+" java -jar ExtractOctreeSubvolume.jar <x1>"
				+" <y1> <z1> <x2> <y2> <z2> <res> <octreeFolder> <outputTiff>");
	}
}
