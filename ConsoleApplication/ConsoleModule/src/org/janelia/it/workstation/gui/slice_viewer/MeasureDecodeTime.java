package org.janelia.it.workstation.gui.slice_viewer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Decompressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4UnknownSizeDecompressor;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.IContainer;

public class MeasureDecodeTime 
{
	private boolean frameFound = false;
	private static int fileBytes;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new MeasureDecodeTime();
	}
	
	public MeasureDecodeTime() {
		testLz4();
		measureH264Time();
		measureJpegTime();
		// measureJpeg12Time();
		measurePngTime();
		measurePng16Time();
	}

	private static BufferedImage decodeImage(InputStream byteStream)
	throws IOException 
	{
		BufferedImage image = ImageIO.read(byteStream);
		return image;
	}
	
	private static ByteArrayInputStream downloadBytes(String fileName) 
	throws IOException 
	{
		//
		// First load bytes, THEN parse image (for more surgical timing measurements)
		// http://stackoverflow.com/questions/2295221/java-net-url-read-stream-to-byte
		ByteArrayOutputStream byteStream0 = new ByteArrayOutputStream();
		byte[] chunk = new byte[32768];
		int bytesRead;
		InputStream stream = new BufferedInputStream(new FileInputStream(fileName));
		while ((bytesRead = stream.read(chunk)) > 0) {
			byteStream0.write(chunk, 0, bytesRead);
		}
		byte[] byteArray = byteStream0.toByteArray();
		fileBytes = byteArray.length;
		ByteArrayInputStream byteStream = new ByteArrayInputStream(byteArray);
		return byteStream;
	}
	
	private void measureJpeg12Time() {
		System.out.println("  12-bit jpeg; quality 80");
		String fn = "/Users/brunsc/projects/quadtree_microbenchmarks/16bit/gray/250_80.jpg";			
		ByteArrayInputStream stream;
		try {
			stream = downloadBytes(fn);
			// TODO - this fails
			RenderedOp ir = JAI.create("ImageRead", stream);
			BufferedImage image = ir.getAsBufferedImage();
			System.out.println(image.getWidth());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void measureJpegTime() {
		System.out.println("*** JPEG ***");
		String fileNameBase = "/Users/brunsc/projects/quadtree_microbenchmarks/8bit/gray/250_";	
		int quality[] = {50, 80, 90, 100};
		for (int q : quality) {
			String fn = fileNameBase+q+".jpg";
			try {
				System.out.println("  Quality: "+q);
				reportLoadTime(fn);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void measurePngTime() {
		System.out.println("*** PNG ***");
		String fileNameBase = "/Users/brunsc/projects/quadtree_microbenchmarks/8bit/gray/250_";	
		int quality[] = {0, 10, 15, 95, 100};
		for (int q : quality) {
			String fn = fileNameBase+q+".png";
			try {
				System.out.println("  Quality: "+q);
				reportLoadTime(fn);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void measurePng16Time() {
		System.out.println("*** PNG 16-bit ***");
		String fileNameBase = "/Users/brunsc/projects/quadtree_microbenchmarks/16bit/gray/250_";	
		int quality[] = {0, 10, 15, 95, 100};
		for (int q : quality) {
			String fn = fileNameBase+q+".png";
			try {
				System.out.println("  Quality: "+q);
				reportLoadTime(fn);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void measureH264Time() {
		measureH264File("/Users/brunsc/projects/quadtree_microbenchmarks/16bit/gray/default.1.mp4");
		measureH264File("/Users/brunsc/projects/quadtree_microbenchmarks/8bit/gray/stack_2m.mp4");
		measureH264File("/Users/brunsc/projects/quadtree_microbenchmarks/8bit/gray/stack_32m.mp4");
	}
	
	private void measureH264File(String fileName)
	{
		System.out.println("*** H264 *** : "+fileName);
		frameFound = false;
		long startTime = System.nanoTime();
		// open video file
        IMediaReader mediaReader = ToolFactory.makeReader(fileName);
        mediaReader.addListener(new MediaListenerAdapter() {
			@Override
			public void onVideoPicture(IVideoPictureEvent event) {
				frameFound = true;
				BufferedImage image = event.getImage();
			}
        });
        mediaReader.open();
        // mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_USHORT_GRAY);
        mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
        IContainer container = mediaReader.getContainer();
        final int vidIndex = 0;
        // seek to some random frame index
        int frameNumber = 250; // or whatever
        // find nearest key frame
        container.seekKeyFrame(vidIndex, -1, 0); // rewind, required for correct seeking?
        container.seekKeyFrame(vidIndex, frameNumber, IContainer.SEEK_FLAG_FRAME);
        long seekTime = System.nanoTime();
        // get frame
        while ( (null == mediaReader.readPacket()) && (! frameFound) )
        		;
        if (frameFound) {
	        long endTime = System.nanoTime();
	        double seekElapsed = (seekTime-startTime)/(double)1e6;
	        double decodeElapsed = (endTime-seekTime)/(double)1e6;
	        System.out.println("seek: "+seekElapsed+" ms; decode: "+decodeElapsed+" ms");
        }
	}
	
	private static void reportLoadTime(String fileName)
	throws IOException
	{
		long startTime = System.nanoTime();
		ByteArrayInputStream inputStream = downloadBytes(fileName);
		long loadTime = System.nanoTime();
		BufferedImage image = decodeImage(inputStream);
		long decodeTime = System.nanoTime();
		double loadElapsed = (loadTime - startTime) / (double)1e6;
		double decodeElapsed = (decodeTime - loadTime) / (double)1e6;
		double compressionRatio = -1;
		try {
			compressionRatio = fileBytes/(double)
				((DataBufferByte) image.getData().getDataBuffer()).getData().length;
		} catch (ClassCastException e) {
			compressionRatio = 0.5 * fileBytes/(double)
			((DataBufferUShort) image.getData().getDataBuffer()).getData().length;
		}
		System.out.println("load: "+loadElapsed+" ms"
				+"; decode: "+decodeElapsed+" ms"
				+"; file bytes: "+fileBytes
				+"; compression ratio: "+compressionRatio
				);
	}
	
	private void testLz4() {
		LZ4Factory factory = LZ4Factory.fastestInstance();

		// get image bytes
		byte[] uncompressed = null;
		try {
			ByteArrayInputStream inputStream = downloadBytes(
					"/Users/brunsc/projects/quadtree_microbenchmarks/16bit/gray/250_100.png");
			BufferedImage image = decodeImage(inputStream);
			short shortData[] = ((DataBufferUShort) image.getData().getDataBuffer()).getData();
			uncompressed = new byte[shortData.length*2];
			ByteBuffer.wrap(uncompressed).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortData);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
	    final int decompressedLength = uncompressed.length;

	    // compress data
	    // LZ4Compressor compressor = factory.fastCompressor();
	    LZ4Compressor compressor = factory.highCompressor();
	    int maxCompressedLength = compressor.maxCompressedLength(decompressedLength);
	    byte[] compressed = new byte[maxCompressedLength];
	    int compressedLength = compressor.compress(uncompressed, 0, decompressedLength, compressed, 0, maxCompressedLength);

	    long startTime = System.nanoTime();
	    // decompress data
	    // - method 1: when the decompressed length is known
	    LZ4Decompressor decompressor = factory.decompressor();
	    byte[] restored = new byte[decompressedLength];
	    int compressedLength2 = decompressor.decompress(compressed, 0, restored, 0, decompressedLength);
	    // compressedLength == compressedLength2

	    long decomp1Time = System.nanoTime();
	    double decomp1Ms = (decomp1Time - startTime) / (double)1e6;
	    // - method 2: when the compressed length is known (a little slower)
	    // the destination buffer needs to be over-sized
	    LZ4UnknownSizeDecompressor decompressor2 = factory.unknwonSizeDecompressor();
	    int decompressedLength2 = decompressor2.decompress(compressed, 0, compressedLength, restored, 0);
	    // decompressedLength == decompressedLength2
	    
	    long decomp2Time = System.nanoTime();
	    double decomp2Ms = (decomp2Time - decomp1Time) / (double)1e6;
	    System.out.println("LZ4: "+decompressedLength+", "+compressedLength2+", "+decompressedLength2
	    		+", "+decomp1Ms+", "+decomp2Ms);
	}
	
}
