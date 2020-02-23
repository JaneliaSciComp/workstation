package org.janelia.workstation.controller.model;

import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Rotation3d;
import org.janelia.it.jacs.shared.geom.UnitVec3;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.model.access.domain.IdSource;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates "realistic" snake-like random neurons in the LVV for benchmarking and testing purposes.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RandomNeuronGenerator {

    private static final Logger log = LoggerFactory.getLogger(RandomNeuronGenerator.class);

    private static final UnitVec3 UNIT_X = new UnitVec3(CoordinateAxis.X);
    private static final UnitVec3 UNIT_Y = new UnitVec3(CoordinateAxis.Y);
    private static final UnitVec3 UNIT_Z = new UnitVec3(CoordinateAxis.Z);
    private static final double MIN_JUMP_SIZE = 200;
    private static final double MAX_JUMP_SIZE = 300;

    private IdSource idSource;
    private BoundingBox3d boundingBox;
    private int meanPointsPerNeuron;
    private double branchProbability;

    private ThreadLocalRandom random = ThreadLocalRandom.current(); // ThreadLocalRandom supports ranges, which Random does not

    public RandomNeuronGenerator(IdSource idSource, BoundingBox3d boundingBox, int meanPointsPerNeuron, double branchProbability) {
        this.idSource = idSource;
        this.boundingBox = boundingBox;
        this.meanPointsPerNeuron = meanPointsPerNeuron;
        this.branchProbability = branchProbability;
    }
    
    public void generateArtificialNeuronData(TmNeuronMetadata neuronMetadata) {
        int points = meanPointsPerNeuron==1 ? 1 : (int)getRandomBoundedDouble(1, (double)meanPointsPerNeuron*2);
        generateArtificialNeuronData(neuronMetadata, points);
    }
        
    public void generateArtificialNeuronData(TmNeuronMetadata neuronMetadata, int numberOfPoints) {

        Long neuronId = neuronMetadata.getId();
        log.trace("Generating random neuron {} with {} points", neuronMetadata.getName(), numberOfPoints);
        
        // Choose a starting point for the neuron
        Vec3 startingPoint = getRandomPoint();
        
        log.trace("  Random starting point: {}", startingPoint);

        // Current direction of every branch
        Map<TmGeoAnnotation, Vec3> branchDirections = new HashMap<>();

        // Current set of neuron end points which can be extended  
        List<TmGeoAnnotation> endPoints = new LinkedList<>();
        
        // Create root annotation at the starting point
        Map<Long, TmGeoAnnotation> map = neuronMetadata.getGeoAnnotationMap();
        TmGeoAnnotation rootAnnotation = new TmGeoAnnotation();
        rootAnnotation.setId(nextGuid());
        rootAnnotation.setNeuronId(neuronId);
        rootAnnotation.setParentId(neuronId);
        rootAnnotation.setCreationDate(new Date());
        rootAnnotation.setX(startingPoint.getX());
        rootAnnotation.setY(startingPoint.getY());
        rootAnnotation.setZ(startingPoint.getZ());

        // Choose a direction and start tracing in that direction
        branchDirections.put(rootAnnotation, getRandomUnitVector());
        endPoints.add(rootAnnotation);
        map.put(rootAnnotation.getId(), rootAnnotation);
        neuronMetadata.addRootAnnotation(rootAnnotation);

        int i = 1;
        while(i < numberOfPoints)  {

            // Choose an end point to extend
            int branchIndex = (int) (endPoints.size() * Math.random());
            TmGeoAnnotation parent = endPoints.get(branchIndex);

            Float rot = null;
            if (random.nextDouble()<branchProbability && i>10) {
                // Branch at the grandparent so that each branch has at least one segment
                TmGeoAnnotation grandparent = map.get(map.get(parent.getId()).getParentId());
                if (grandparent!=null) {
                    // Start at grandparent and branch
                    parent = grandparent;
                    // Rotate more when branching
                    rot = 0.20f;
                }
                else {
                    // No grandparent could be found. This is probably because we went out 
                    // of bounds and restarted at the root. That's okay, it just means 
                    // we can't branch here. Logic will fall through to the normal end point
                    // processing. 
                }
            }

            if (rot==null) {
                // Rotate very slightly
                rot = 0.05f;
                // Parent is no longer an end point
                endPoints.remove(parent);
            }
            
            // End point we're extending
            Vec3 lastPoint = new Vec3(parent.getX(), parent.getY(), parent.getZ());
            Vec3 direction = branchDirections.get(parent);
            
            // Calculate next direction by slightly perturbing the last direction
            direction = rotateSlightly(direction, rot);

            // Calculate next point location
            double jumpSize = getRandomBoundedDouble(MIN_JUMP_SIZE, MAX_JUMP_SIZE);
            Vec3 point = lastPoint.plus(direction.times(jumpSize));

            if (!boundingBox.contains(point)) {
                log.trace("Out of bounds, starting over: "+neuronMetadata.getName());
                // Out of bounds, stop extending this branch
                endPoints.remove(parent);
                // If there are no other possible end points, start again at the root, in a new direction
                if (endPoints.isEmpty()) {
                    endPoints.add(rootAnnotation);
                    branchDirections.put(rootAnnotation, getRandomUnitVector());
                }
                continue;   
            }
            
            // Create annotation
            TmGeoAnnotation geoAnnotation = new TmGeoAnnotation();
            geoAnnotation.setId(nextGuid());
            geoAnnotation.setNeuronId(neuronId);
            geoAnnotation.setParentId(parent.getId());
            geoAnnotation.setX(point.getX());
            geoAnnotation.setY(point.getY());
            geoAnnotation.setZ(point.getZ());
            geoAnnotation.setCreationDate(new Date());

            branchDirections.put(geoAnnotation, direction);
            endPoints.add(geoAnnotation);
            map.put(geoAnnotation.getId(), geoAnnotation);
            parent.addChild(geoAnnotation);
            
            i++;
        }
    }
    
    private double getRandomDouble() {
        double g = (random.nextGaussian()+2)/4f; // Move from (-1,1) to (-2,2) and then (0,1)
        // If it's out of range, use a uniform distribution instead
        if (g<0) return random.nextDouble();
        if (g>1) return random.nextDouble();
        return g;
    }
    
    private Vec3 getRandomPoint() {
        return new Vec3(
                boundingBox.getMinX() + boundingBox.getWidth()*getRandomDouble(),
                boundingBox.getMinY() + boundingBox.getHeight()*getRandomDouble(),
                boundingBox.getMinZ() +  boundingBox.getDepth()*getRandomDouble()
        );
    }

    private Vec3 getRandomUnitVector() {
        Vec3 v = new Vec3(
                random.nextGaussian(),
                random.nextGaussian(),
                random.nextGaussian()
        );
        // Normalize to unit vector
        double mag = getMagnitude(v);
        v.multEquals(1/mag);
        return v;
    }
    
    private double getMagnitude(Vec3 v) {
        return Math.sqrt(Math.pow(v.x(), 2) + Math.pow(v.y(), 2) + Math.pow(v.z(), 2));
    }
    
    private double getRandomBoundedDouble(double min, double max) {
        double r = min + random.nextGaussian()*((max - min)/2);
        // If it's out of range, use a uniform distribution instead
        if (r<min || r>max) return random.nextDouble(min, max + 1);
        return r;
    }

    private Vec3 rotateSlightly(Vec3 v, double radMax) {
        v = rotateSlightly(UNIT_X, v, radMax);
        v = rotateSlightly(UNIT_Y, v, radMax);
        v = rotateSlightly(UNIT_Z, v, radMax);
        return v;
    }
    
    private Vec3 rotateSlightly(UnitVec3 rotationAxis, Vec3 v, double radMax) {
        double rotationAngle = radMax * Math.PI * random.nextGaussian(); // Very small angle adjustments
        Rotation3d rotation = new Rotation3d().setFromAngleAboutUnitVector(rotationAngle, rotationAxis);
        return rotation.times(v);
    }
    
    private long nextGuid() {
        return idSource.next();
    }

}
