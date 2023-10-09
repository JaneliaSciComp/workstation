package org.janelia.horta.actors;

import org.janelia.geometry3d.*;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.horta.blocks.*;
import org.janelia.horta.volume.VolumeMipMaterial;
import org.janelia.horta.volume.VolumeMipMaterial.VolumeState;
import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL3;
import javax.media.opengl.GL4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OmeZarrVolumeActor extends BasicGL3Actor implements DepthSlabClipper {
    private static final Logger LOG = LoggerFactory.getLogger(OmeZarrVolumeActor.class);

    private static OmeZarrVolumeActor singletonInstance;

    // Singleton access
    static public OmeZarrVolumeActor getInstance() {
        if (singletonInstance == null)
            singletonInstance = new OmeZarrVolumeActor();
        return singletonInstance;
    }

    private Texture2d opaqueDepthTexture = null;
    private float zNearRelative = 0.50f;
    private float zFarRelative = 150.0f; // relative z clip planes
    private ImageColorModel brightnessModel;

    private VolumeMipMaterial.VolumeState volumeState = new VolumeMipMaterial.VolumeState();

    private OmeZarrBlockTileSource source;
    private final OmeZarrTileCache dynamicTiles = new OmeZarrTileCache(null);
    private BlockChooser<OmeZarrBlockTileKey, OmeZarrBlockTileSource> chooser;
    private BlockDisplayUpdater<OmeZarrBlockTileKey, OmeZarrBlockTileSource> blockDisplayUpdater;
    
    private final BlockSorter blockSorter = new BlockSorter();    

    private OmeZarrVolumeActor() {
        super(null);
        chooser = new OmeZarrBlockChooser();
        blockDisplayUpdater = new BlockDisplayUpdater<>(chooser);
        initBlockStrategy(chooser);
        blockDisplayUpdater.getDisplayChangeObservable().addObserver((o, arg) -> dynamicTiles.updateDesiredTiles(blockDisplayUpdater.getDesiredBlocks()));
        dynamicTiles.getDisplayChangeObservable().addObserver(((o, arg) -> {
            List<Object3d> list = new ArrayList<>();
            getChildren().forEach(c -> {
                if (!dynamicTiles.getDisplayedActors().contains(c)) {
                    list.add(c);
                }
            });
            getChildren().removeAll(list);
            dynamicTiles.getDisplayedActors().forEach(a -> {
                if (!getChildren().contains(a)) {
                    addPersistentBlock(a);
                }
            });
        }));
    }
    
    //Calculate a distance from a camera to a block.
    //Centroids cannot be used to calculate distances between camera and blocks in zarr.
    //Centroids only work when all blocks in the same resolution level have the same dimensions.
    //But blocks in zarr can have arbitrary dimensions.
    private float calcDistanceFromCamera(Vector3 minp, Vector3 maxp, Matrix4 viewMatrix)
    {
        final Vector4[] corner = {
        		viewMatrix.multiply(new Vector4(minp.getX(), minp.getY(), minp.getZ(), 1.0f)),
        		viewMatrix.multiply(new Vector4(minp.getX(), minp.getY(), maxp.getZ(), 1.0f)),
        		viewMatrix.multiply(new Vector4(minp.getX(), maxp.getY(), minp.getZ(), 1.0f)),
        		viewMatrix.multiply(new Vector4(maxp.getX(), minp.getY(), minp.getZ(), 1.0f)),
        		viewMatrix.multiply(new Vector4(minp.getX(), maxp.getY(), maxp.getZ(), 1.0f)),
        		viewMatrix.multiply(new Vector4(maxp.getX(), minp.getY(), maxp.getZ(), 1.0f)),
        		viewMatrix.multiply(new Vector4(maxp.getX(), maxp.getY(), minp.getZ(), 1.0f)),
        		viewMatrix.multiply(new Vector4(maxp.getX(), maxp.getY(), maxp.getZ(), 1.0f))
        };
		
        //distances from a camera to faces
		final int[][] faces = {
				{0, 1, 2},
				{0, 2, 3},
				{0, 3, 1},
				{7, 6, 5},
				{7, 5, 4},
				{7, 4, 6}
		};
		float dmin = Float.MAX_VALUE;
		for (int f = 0; f < 6; f++)
		{
			Vector4[] p = {corner[faces[f][0]], corner[faces[f][1]], corner[faces[f][2]]};
			Vector3 v0 = new Vector3(p[0].get(0), p[0].get(1), p[0].get(2));
			Vector3 v1 = new Vector3(p[1].get(0)-p[0].get(0), p[1].get(1)-p[0].get(1), p[1].get(2)-p[0].get(2));
			Vector3 v2 = new Vector3(p[2].get(0)-p[0].get(0), p[2].get(1)-p[0].get(1), p[2].get(2)-p[0].get(2));
			Vector3 n = v1.cross(v2).normalize();
			float t = n.dot(v0);
			
			Vector3 vf = new Vector3(n.get(0)*t - v0.get(0), n.get(1)*t - v0.get(1), n.get(2)*t - v0.get(2));
			float v1len = v1.length();
			float v2len = v2.length();
			Vector3 n1 = v1.normalize();
			Vector3 n2 = v2.normalize();
			float t1 = n1.dot(vf) / v1len;
			float t2 = n2.dot(vf) / v2len;
			
			if (t1 > 0 && t1 < 1 && t2 > 0 && t2 < 1)
			{
				float dd = t * t;
				if (dd < dmin)
					dmin = dd;
			}
		}
		
		//distances from a camera to edges
		final int[][] edges = {
				{0, 1, 2, 4}, //min x -> max x
				{0, 1, 3, 5}, //min y -> max y
				{0, 2, 3, 6}  //min z -> max z
		};
		Vector3[] ev = { //ev[0]: x axis, ev[1]: y axis, ev[2]: z axis (of the block)
				new Vector3(corner[3].get(0) - corner[0].get(0), corner[3].get(1) - corner[0].get(1), corner[3].get(2) - corner[0].get(2)),
				new Vector3(corner[2].get(0) - corner[0].get(0), corner[2].get(1) - corner[0].get(1), corner[2].get(2) - corner[0].get(2)),
				new Vector3(corner[1].get(0) - corner[0].get(0), corner[1].get(1) - corner[0].get(1), corner[1].get(2) - corner[0].get(2)),
				};
		float[] dimensions = new float[3]; //dimensions[0]: width, dimensions[1]: hight, dimensions[2]: depth
		for (int i = 0; i < 3; i++)
		{
			dimensions[i] = ev[i].length();
			ev[i].normalize(); 
		}
		for (int i = 0; i < 3; i++)
		{
			for (int j = 0; j < 4; j++)
			{
				Vector3 v = new Vector3(corner[edges[i][j]].get(0), corner[edges[i][j]].get(1), corner[edges[i][j]].get(2));
				float t = -(ev[i].dot(v));
				if (t > 0 && t < dimensions[i])
				{
					Vector3 ep1 = new Vector3(ev[i].get(0)*t + v.get(0), ev[i].get(1)*t + v.get(1), ev[i].get(2)*t + v.get(2));
					float dd = ep1.lengthSquared();
					if (dd < dmin)
						 dmin = dd;
				}
			}
		}
		
		//distances from a camera to corners
		for (int i = 0; i < 8; i++)
		{
			float dd = corner[i].get(0)*corner[i].get(0) + corner[i].get(1)*corner[i].get(1) + corner[i].get(2)*corner[i].get(2);
			if (dd < dmin)
				 dmin = dd;
		}
    	
		//return the shortest distance
    	return dmin;
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        if (! isVisible())
            return;
        if (! isInitialized) init(gl);
        if (getChildren() == null)
            return;
        if (getChildren().size() < 1)
            return;
        
        gl.glEnable(GL3.GL_BLEND);
        if (volumeState.projectionMode == VolumeState.PROJECTION_OCCLUDING) {
            // Occluding
            ((GL4)gl).glBlendEquationi(0, GL4.GL_FUNC_ADD); // RGBA color target
            ((GL4)gl).glBlendFuncSeparatei(0, 
                    GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA,
                    GL4.GL_ONE, GL4.GL_ONE_MINUS_SRC_ALPHA);
        }
        else {
            ((GL4)gl).glBlendEquationi(0, GL4.GL_MAX); // RGBA color target
        }
        // Always use Maximum for blending secondary render target
        ((GL4)gl).glBlendEquationi(1, GL4.GL_MAX); // core intensity/depth target
        
        // Display Front faces only.
        gl.glEnable(GL3.GL_CULL_FACE);
        gl.glCullFace(GL3.GL_BACK);
        
        if (opaqueDepthTexture != null) {
            final int depthTextureUnit = 2;
            opaqueDepthTexture.bind(gl, depthTextureUnit);
        }
        
        Matrix4 modelViewMatrix = parentModelViewMatrix;
        if (modelViewMatrix == null)
            modelViewMatrix = camera.getViewMatrix();
        Matrix4 localMatrix = getTransformInParent();
        if (localMatrix != null)
            modelViewMatrix = new Matrix4(modelViewMatrix).multiply(localMatrix);
        
        List<SortableBlockActor> blockList = new ArrayList<>();
        List<GL3Actor> otherActorList = new ArrayList<>();
        List<Object3d> otherList = new ArrayList<>();
        for (SortableBlockActor actor : dynamicTiles.getDisplayedActors()) {
            blockList.add(actor);
        }
        for (Object3d child : getChildren()) {
            if (child instanceof SortableBlockActorSource) {
                SortableBlockActorSource source = (SortableBlockActorSource)child;
                for (SortableBlockActor block : source.getSortableBlockActors()) {
                	if (volumeState.projectionMode != VolumeState.PROJECTION_MAXIMUM) {
                		Vector3 minp = ((OmeZarrVolumeMeshActor)block).getBBoxMin();
                		Vector3 maxp = ((OmeZarrVolumeMeshActor)block).getBBoxMax();
                		((OmeZarrVolumeMeshActor)block).setDistance(calcDistanceFromCamera(minp, maxp, modelViewMatrix));
                	}
                    blockList.add(block);
                }
            }
            else if (child instanceof GL3Actor) {
                otherActorList.add((GL3Actor)child);
            }
            else {
                otherList.add(child);
            }
        }
        assert otherActorList.isEmpty();
        assert otherList.isEmpty();
        
        // Order does not matter in MIP mode
        if (volumeState.projectionMode != VolumeState.PROJECTION_MAXIMUM) {
            blockSorter.setViewMatrix(modelViewMatrix);
            Collections.sort(blockList, blockSorter);        
        }
        for (SortableBlockActor actor : blockList) {
            actor.display(gl, camera, modelViewMatrix);
        }
    }

    private void initBlockStrategy(BlockChooser<OmeZarrBlockTileKey, OmeZarrBlockTileSource> chooser) {
        dynamicTiles.setBlockStrategy(chooser);
        blockDisplayUpdater.setBlockChooser(chooser);
    }

    public void setAutoUpdate(boolean updateCache) {
        blockDisplayUpdater.setAutoUpdate(updateCache);
    }

    public VolumeMipMaterial.VolumeState getVolumeState() {
        return volumeState;
    }

    public Object3d addPersistentBlock(Object3d child) {
        return addChild(child);
    }

    @Override
    public Object3d addChild(Object3d child) {
        if (child instanceof DepthSlabClipper) {
            ((DepthSlabClipper) child).setOpaqueDepthTexture(opaqueDepthTexture);
            ((DepthSlabClipper) child).setRelativeSlabThickness(zNearRelative, zFarRelative);
        }
        return super.addChild(child);
    }

    public void addTransientBlock(BlockTileKey key) {
        dynamicTiles.addDesiredTile((OmeZarrBlockTileKey) key);
    }

    public void setVolumeState(VolumeMipMaterial.VolumeState volumeState) {
        this.volumeState = volumeState;
    }

    public void setBrightnessModel(ImageColorModel brightnessModel) {
        this.brightnessModel = brightnessModel;
    }

    public void clearAllBlocks() {
        dynamicTiles.clearAllTiles();
        getChildren().clear();
    }

    public void setHortaVantage(Vantage vantage) {
        blockDisplayUpdater.setVantage(vantage);
    }

    public void setOmeZarrTileSource(OmeZarrBlockTileSource source) {
        dynamicTiles.setSource(source);
        blockDisplayUpdater.setBlockTileSource(source);
        this.source = source;
    }

    public ObservableInterface getDynamicTileUpdateObservable() {
        return dynamicTiles.getDisplayChangeObservable();
    }

    @Override
    public void setOpaqueDepthTexture(Texture2d opaqueDepthTexture) {
        this.opaqueDepthTexture = opaqueDepthTexture;

        getChildren().forEach(c -> {
            if (c instanceof DepthSlabClipper) {
                ((DepthSlabClipper) c).setOpaqueDepthTexture(opaqueDepthTexture);
            }
        });
    }

    @Override
    public void setRelativeSlabThickness(float zNear, float zFar) {
        if (zNear > 0.5 || zFar < 150) {
            return;
        }

        zNearRelative = zNear;
        zFarRelative = zFar;

        getChildren().forEach(c -> {
            if (c instanceof DepthSlabClipper) {
                ((DepthSlabClipper) c).setRelativeSlabThickness(zNear, zFar);
            }
        });
    }
    
    private static class BlockSorter implements Comparator<SortableBlockActor> 
    {
        private Matrix4 viewMatrix;

        public BlockSorter() {
        }

        @Override
        public int compare(SortableBlockActor o1, SortableBlockActor o2) {
            // 1) If blocks are not the same resolution, sort on resolution
            BlockTileResolution res1 = o1.getResolution();
            BlockTileResolution res2 = o2.getResolution();
            if (! res1.equals(res2)) {
                return res1.compareTo(res2);
            }
            // 2) sort based on distance from centroid to camera
            if (viewMatrix == null)
                throw new UnsupportedOperationException("View Matrix is Null");
            
            float d1 = (float)((OmeZarrVolumeMeshActor)o1).getDistance();
            float d2 = (float)((OmeZarrVolumeMeshActor)o2).getDistance();
            
            return d1 > d2 ? -1 : d1 == d2 ? 0 : 1;
        }

        public void setViewMatrix(Matrix4 viewMatrix) {
            this.viewMatrix = viewMatrix;
        }
    }
}