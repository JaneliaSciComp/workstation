package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.AbstractTextureLoadAdapter.MissingTileException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.AbstractTextureLoadAdapter.TileLoadError;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;

import com.sun.media.jai.codec.ImageDecoder;

/**
 * Convert block tiff octree render artifacts to slice pam/lz4 artifacts.
 * @author brunsc
 *
 */
public class TiffToPam {
	
	private BlockTiffOctreeLoadAdapter loadAdapter;
	private boolean recursive = false;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			usage();
			System.exit(1);
		}
		File inputTiffFolder = new File(args[0]);
		File outputPamFolder = new File(args[1]);
		TiffToPam tiffToPam = new TiffToPam();
		tiffToPam.loadAdapter = new BlockTiffOctreeLoadAdapter();
		try {
			tiffToPam.loadAdapter.setTopFolder(inputTiffFolder);
			tiffToPam.convertFolder(inputTiffFolder, outputPamFolder);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Recursively convert a folder and all of it's descendants.
	 * @param inputFolder
	 * @param outputFolder
	 * @return
	 */
	private boolean convertFolder(File inputFolder, File outputFolder) {
		boolean result = true; // start optimistic
		if (! inputFolder.exists())
			return false;
		if (! outputFolder.exists()) {
			if (! outputFolder.mkdirs()) // create output folder
				return false;
		}
		System.out.println("Converting folder "+inputFolder+" to "+outputFolder);
		// Open tiff files, one per channel
		ImageDecoder decoder[];
		try {
			// TODO - also non-z slices
			decoder = loadAdapter.createImageDecoders(inputFolder, CoordinateAxis.Z);
		} catch (MissingTileException e) {
			return false;
		} catch (TileLoadError e) {
			return false;
		}
		// Convert files
		TileFormat tileFormat = loadAdapter.getTileFormat();
		int sz = tileFormat.getTileSize()[2];
		NumberFormat format = new DecimalFormat("00000");
		// LZ4 does not work well with Liver images...
		/*
		LZ4Factory factory = LZ4Factory.fastestInstance();
		LZ4Compressor compressor = factory.highCompressor();
		 */
		for (int z = 0; z < sz; ++z) {
			try {
				TextureData2dGL tex = loadAdapter.loadSlice(z, decoder);
				// Write pam file
				File pamFile = new File(outputFolder, "slice_"+format.format(z)+".pam");
				// System.out.println(pamFile.getAbsolutePath());
				FileOutputStream pamStream0 = new FileOutputStream(pamFile);
				/*
				OutputStream pamStream2 = new LZ4BlockOutputStream(
						pamStream0
						, 65536
						, compressor);
						*/
				// Write header of pam file
				PrintWriter writer = new PrintWriter(pamStream0);
				/*	 
					P7
					WIDTH 227
					HEIGHT 149
					DEPTH 3
					MAXVAL 255
					TUPLTYPE RGB
					ENDHDR
				*/
				writer.write("P7\n");
				writer.write("WIDTH "+tex.getWidth()+"\n");
				writer.write("HEIGHT "+tex.getHeight()+"\n");
				writer.write("DEPTH "+tex.getChannelCount()+"\n");
				int maxVal = 255;
				if (tex.getBitDepth() > 8)
					maxVal = 65535;
				writer.write("MAXVAL "+maxVal+"\n");
				// Image might be padded
				writer.write("# USEDWIDTH "+tex.getUsedWidth()+"\n");
				writer.write("ENDHDR\n");
				writer.flush();
				// Write data of pam file
				byte[] bb = tex.getPixels().array();
				// Result must be big endian, texture is probably little endian
				if ( (tex.getPixels().order() == ByteOrder.LITTLE_ENDIAN)
					&& (tex.getBitDepth() > 8) )
				{
					// System.out.println("swapping bytes");
					ShortBuffer in = ByteBuffer.wrap(bb).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
					byte[] swap = new byte[bb.length];
					ShortBuffer out = ByteBuffer.wrap(swap).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
					out.put(in);
					bb = swap;
				}
				// bb = PamOctreeLoadAdapter.unpackChannels(bb, tex.getChannelCount(), tex.getBitDepth()/8);
				// bb = PamOctreeLoadAdapter.packChannels(bb, tex.getChannelCount(), tex.getBitDepth()/8); // control
				pamStream0.write(bb);
				// TODO - write as lz4 pam
				pamStream0.flush();
				pamStream0.close();
			} catch (TileLoadError e) {
				result = false;
			} catch (FileNotFoundException e) {
				result = false;
			} catch (IOException e) {
				result = false;
			}
		}
		
		if (recursive) {
			// Convert eight possible octree subfolders
			for (int ix = 1; ix <= 8; ++ix) {
				String ixString = ""+ix;
				File inFolder2 = new File(inputFolder, ixString);
				if (! inFolder2.exists())
					continue;
				File outFolder2 = new File(outputFolder, ixString);
				if (! convertFolder(inFolder2, outFolder2))
					result = false;
			}
		}
		
		return result;
	}
	
	private static void usage() {
		System.out.println("Usage: java "+TiffToPam.class.getName()+" <input_tiff_folder> <output_pam_folder>");
	}

}
