/* 
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.horta;

import Jama.Matrix;
import org.apache.commons.httpclient.methods.GetMethod;
import org.janelia.geometry3d.Box3;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.texture.Texture3d;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.horta.volume.VoxelIndex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.compression.CompressedFileResolverI;
import org.janelia.it.jacs.shared.img_3d_loader.FileByteSource;
import org.janelia.it.jacs.shared.img_3d_loader.FileStreamSource;
import org.janelia.it.jacs.shared.lvv.HttpDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents Mouse Brain tile information entry from tilebase.cache.yml file.
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class BrainTileInfo 
implements BrickInfo
{
    // e.g.
    //path: /nobackup/mousebrainmicro/data/2014-04-04/Tiling
    //tiles:
    //- aabb:
    //    ori: [84934200, 17379900, 9909023]
    //    shape: [386670, 532776, 200000]
    //  path: /2014-04-14/01/01659
    //  shape:
    //    dims: [1024, 2048, 201, 2]
    //    type: u16
    //  transform: [-377.607422, 0.0, 0.0, 0.0, 85320872.0, 0.0, -260.144531, 0.0, 0.0,
    //    17912676.0, 0.0, 0.485852, 995.024902, 0.0, 9909023.0, 0.0, 0.0, 0.0, 1.0, 0.0,
    //    0.0, 0.0, 0.0, 0.0, 1.0]
    final int[] bbOriginNanometers = new int[3];
    final int[] bbShapeNanometers = new int[3];
    private String localPath;
    private String parentPath;
    final int[] pixelDims = new int[4]; // includes color channel count
    String intensityType;
    Matrix transform; // converts voxels to stage coordinates in nanometers
    private Matrix texCoord_X_stageUm; // cached transform inverse; after conversion to micrometers
    
    // TODO  colorChannelIndex is a temporary hack that should be removed when we can show more than one channel at once
    private int colorChannelIndex = 0;
    private boolean leverageCompressedFiles;
    private Logger log = LoggerFactory.getLogger(BrainTileInfo.class);
    
    public BrainTileInfo(Map<String, Object> yamlFragment, String parentPath, boolean leverageCompressedFiles) throws ParseException 
    {
        //log.info("BrainTileInfo() parentPath="+parentPath);
        this.parentPath = parentPath;
        this.leverageCompressedFiles = leverageCompressedFiles;
        Map<String, Object> aabb = (Map<String, Object>)yamlFragment.get("aabb");
        List<Integer> ori = (List<Integer>)aabb.get("ori");
        List<Integer> bbshape = (List<Integer>)aabb.get("shape");
        for (int i : new int[]{0,1,2}) {
            bbOriginNanometers[i] = ori.get(i);
            bbShapeNanometers[i] = bbshape.get(i);
        }
        localPath = (String) yamlFragment.get("path");
        Map<String, Object> shape = (Map<String, Object>)yamlFragment.get("shape");
        List<Integer> dims = (List<Integer>)shape.get("dims");
        for (int i : new int[]{0,1,2,3})
            pixelDims[i] = dims.get(i);
        intensityType = (String)shape.get("type");
        List<Double> td = (List<Double>)yamlFragment.get("transform");
        if (td.size() != 25)
            throw new ParseException("Unexpected raw tile transform size "+td.size(), 25);
        double[][] dd = new double[5][5];
        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 5; ++j) {
                dd[i][j] = td.get(5*i + j);
            }
        }
        transform = new Matrix(dd, 5, 5);
        // transform.print(11, 3);
    }
    
    public String getLocalPath() {
        return localPath;
    }

    public String getParentPath() {
        return parentPath;
    }

    /**
     * Construct a matrix to help convert spatial positions to texture
     * coordinates; for use in efficient ray casting.
     * 
     * @return Matrix that maps microscope stage coordinates, in micrometers,
     * to normalized 3D texture coordinates in tile.
     */
    public Matrix getTexCoord_X_stageUm() {
        // Compute matrix just-in-time
        if (texCoord_X_stageUm == null) {
            // Standard transform converts voxel positions into microscope stage coordinates (in nanometers)
            // Remove weird channel dimension, lowering size from 5x5 to 4x4
            Matrix m5 = transform;
            // m5.print(5, 5);
            Matrix stageNm_X_voxel = new Matrix(new double[][] {
                {m5.get(0, 0), m5.get(0, 1), m5.get(0, 2), m5.get(0, 4)},
                {m5.get(1, 0), m5.get(1, 1), m5.get(1, 2), m5.get(1, 4)},
                {m5.get(2, 0), m5.get(2, 1), m5.get(2, 2), m5.get(2, 4)},
                {m5.get(4, 0), m5.get(4, 1), m5.get(4, 2), m5.get(4, 4)}});
            // stageNm_X_voxel.print(16, 12);
            // Invert, to turn microscope stage coordinates into voxel positions:
            Matrix voxel_X_stageNm = stageNm_X_voxel.inverse();
            // voxel_X_stageNm.print(16, 12);
            // Convert nanometers to micrometers:
            Matrix nm_X_um = new Matrix(new double[][] {
                {1000, 0, 0, 0},
                {0, 1000, 0, 0},
                {0, 0, 1000, 0},
                {0, 0, 0, 1}});
            Matrix voxel_X_stageUm = voxel_X_stageNm.times(nm_X_um);
            // For ray casting, convert from stageUm to texture coordinates (i.e. normalized voxels)
            // voxel_X_stageUm.print(16, 12);
            Matrix tc_X_vx = new Matrix(new double[][] {
                {1.0/pixelDims[0], 0, 0, 0},
                {0, 1.0/pixelDims[1], 0, 0},
                {0, 0, 1.0/pixelDims[2], 0},
                {0, 0, 0, 1}});
            texCoord_X_stageUm = tc_X_vx.times(voxel_X_stageUm);
            // texCoord_X_stageUm.print(16, 12);
        }
        return texCoord_X_stageUm;
    }

    /**
     * 
     * @return resolution in nanometers
     */
    float getMinResolutionNanometers() {
        float resolution = Float.MAX_VALUE;
        for (int xyz = 0; xyz < 3; ++xyz) {
            float res = bbShapeNanometers[xyz] / (float)pixelDims[xyz];
            if (res < resolution)
                resolution = res;
        }
        return resolution;
    }

    @Override
    public List<? extends ConstVector3> getCornerLocations() 
    {
        List<ConstVector3> result = new ArrayList<>();
        for (int pz : new int[]{0, pixelDims[2]}) {
            for (int py : new int[]{0, pixelDims[1]}) {
                for (int px : new int[]{0, pixelDims[0]}) {
                    Matrix corner = new Matrix(new double[]{px, py, pz, 0, 1}, 5);
                    Matrix um = transform.times(corner).times(1.0 / 1000.0);
                    ConstVector3 v = new Vector3(
                            (float) um.get(0, 0),
                            (float) um.get(1, 0),
                            (float) um.get(2, 0));
                    result.add(v);
                }
            }
        }
        return result;
    }

    @Override
    public List<Vector3> getValidCornerLocations() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Vector3> getTilingSubsetLocations() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public VoxelIndex getRasterDimensions() {
        return new VoxelIndex(pixelDims[0], pixelDims[1], pixelDims[2]);
    }

    @Override
    public int getChannelCount() {
        return pixelDims[3];
    }

    @Override
    public int getBytesPerIntensity() {
        if ( intensityType.equals("u16") )
            return 2;
        else
            return 1;
    }

    @Override
    public double getResolutionMicrometers() {
        return getMinResolutionNanometers() / 1000.0;
    }

    @Override
    public Box3 getBoundingBox() {
        Box3 result = new Box3();
        Vector3 bbOrigin = new Vector3(
                bbOriginNanometers[0],
                bbOriginNanometers[1],
                bbOriginNanometers[2]);
        Vector3 bbSize = new Vector3(
                bbShapeNanometers[0],
                bbShapeNanometers[1],
                bbShapeNanometers[2]);
        bbOrigin = bbOrigin.multiplyScalar(1e-3f); // Convert nm to um
        bbSize = bbSize.multiplyScalar(1e-3f); // Convert nm to um
        result.include(bbOrigin);
        result.include(bbOrigin.add(bbSize));
        return result;
    }

    public boolean folderExists() {
        // OS specific path should have already been translated in MouseLightYamlBrickSource
        File folderPath = new File(parentPath, localPath);
        return folderPath.exists();
    }

    // TODO - remove this hack after we can show more than one channel at a time
    public int getColorChannelIndex() {
        return this.colorChannelIndex;
    }
    
    public void setColorChannelIndex(int index) {
        this.colorChannelIndex = index;
    }
    
    public Texture3d loadBrick(double maxEdgePadWidth, int colorChannel) throws IOException
    {
        setColorChannelIndex(colorChannel);
        return loadBrick(maxEdgePadWidth);
    }

    @Override
    public Texture3d loadBrick(double maxEdgePadWidth) throws IOException
    {
        // OS specific path should have already been translated in MouseLightYamlBrickSource

        //log.info("BrainTileInfo loadBrick() parentPath="+parentPath+" localPath="+localPath);

        File folderPath = new File(parentPath, localPath);


        //log.info("BrainTileInfo loadBrick() using folderPath=" + folderPath.getAbsolutePath());

        File tileFile = null;

        Texture3d texture = new Texture3d();

        if (!HttpDataSource.useHttp()) {

            //System.out.println(folderPath.getAbsolutePath());
            if (!folderExists())
                throw new IOException("no such tile folder " + folderPath.getAbsolutePath());

            CompressedFileResolverI resolver = FrameworkImplProvider.getCompressedFileResolver();

            if (leverageCompressedFiles && resolver == null) {
                throw new IOException("Failed to find compression resolver.");
            }

            // That path is just a folder. Now find the actual files.
            // TODO - this just loads the first channel.
            String imageSuffix = "." + Integer.toString(colorChannelIndex); // + ".tif";
            File compressedTileFile = null;
            for (File file : folderPath.listFiles()) {
                if (leverageCompressedFiles && file.getName().contains(imageSuffix) && resolver.canDecompress(file)) {
                    File decompressedName = resolver.getDecompressedNameForFile(file);
                    if (decompressedName != null && decompressedName.getName().endsWith(imageSuffix)) {
                        log.info("Starting with compressed version of file {}.", file);
                        compressedTileFile = file;
                        break;
                    }
                }
                // Use the first channel file
                if (file.getName().endsWith(imageSuffix + ".tif")) {
                    log.info("Using never-compressed version of file {}.", file);
                    tileFile = file;
                    break;
                }
            }
            if (compressedTileFile != null) {
                try {
                    log.info("Decompressing...");
                    tileFile = resolver.decompressToFile(compressedTileFile);
                    log.info("Decompressed as {}.", tileFile);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    throw new IOException("Decompression step failed. " + compressedTileFile, ex);
                }
            }
            if (tileFile == null){
                throw new IOException("No channel tiff file found");
            } else {
                System.out.println("BrainTileInfo loadBrick() - using tileFile="+tileFile.getAbsolutePath());
            }

        } else {
            tileFile=new File(folderPath, "default."+colorChannelIndex);
            log.info("loadBrick() http using tileFile="+tileFile.getAbsolutePath());

            texture.setOptionalFileStreamSource(new FileStreamSource() {
                @Override
                public GetMethod getStreamForFile(String filepath) throws Exception {
                    return HttpDataSource.getMouseLightTiffStream(filepath);
                }
            });

//            texture.setOptionalFileByteSource(new FileByteSource() {
//                @Override
//                public byte[] loadBytesForFile(String filepath) throws Exception {
//                    return HttpDataSource.getMouseLightTiffBytes(filepath);
//                }
//            });

        }

        texture.loadTiffStack(tileFile);

        return texture;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.localPath);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BrainTileInfo other = (BrainTileInfo) obj;
        return Objects.equals(this.localPath, other.localPath);
    }

    @Override
    public boolean isSameBrick(BrickInfo other) {
        if (!(other instanceof BrainTileInfo)) {
            return false;
        }
        BrainTileInfo rhs = (BrainTileInfo) other;
        if (rhs.getColorChannelIndex() != this.getColorChannelIndex())
            return false;
        
        return rhs.parentPath.equals(parentPath) && rhs.localPath.equals(localPath);

    }
    
    
}
