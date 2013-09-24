package org.janelia.it.FlyWorkstation.tracing;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.Subvolume;
import org.janelia.it.FlyWorkstation.raster.VoxelIndex;

import com.google.common.collect.Lists;

/**
 * Maybe implement my own version of AStar.
 * http://en.wikipedia.org/wiki/A*_search_algorithm
 * 
 * @author brunsc
 *
 */
public class AStar {
    // Cached values
    private double minStepCost = Double.NaN;
    private Subvolume volume;
    private Map<Integer, Double> pathCostForIntensity = new HashMap<Integer, Double>();
    private double meanIntensity = Double.NaN;
    private double stdDevIntensity = Double.NaN;
    
    public List<VoxelIndex> trace(
            VoxelIndex start, 
            VoxelIndex goal,
            Subvolume volume) 
    {
        clearCachedValues();
        this.volume = volume;
        // The set of nodes already evaluated
        Set<VoxelIndex> closedSet = new HashSet<VoxelIndex>();
        // The set of tentative nodes to be evaluated, initially containing the start node.
        Set<VoxelIndex> openSet = new HashSet<VoxelIndex>();
        openSet.add(start);
        // The map of navigated nodes
        Map<VoxelIndex, VoxelIndex> cameFrom = new HashMap<VoxelIndex, VoxelIndex>();
        // Cost from start along best known path
        Map<VoxelIndex, Double> gScore = new HashMap<VoxelIndex, Double>();
        Map<VoxelIndex, Double> fScore = new HashMap<VoxelIndex, Double>();
        gScore.put(start, 0.0);
        fScore.put(start, gScore.get(start) + heuristicCostEstimate(start, goal));
        
        while (openSet.size() > 0) {
            // Get node with lowest fScore in openSet
            VoxelIndex current = null;
            for (VoxelIndex n : openSet) {
                if (current == null) {
                    current = n;
                    continue;
                }
                if (fScore.get(n) < fScore.get(current))
                    current = n;
            }
            if (current.equals(goal))
                return reconstructPath(cameFrom, start, goal);
            // Remove current from openSet
            openSet.remove(current);
            closedSet.add(current);
            for (VoxelIndex neighbor : getNeighbors(current)) 
            {
                double tentativeGScore = gScore.get(current)
                        + distanceBetween(current, neighbor);
                if ( closedSet.contains(neighbor)
                       && (tentativeGScore >= gScore.get(neighbor) ) ) {
                    continue;
                }
                if ( (! openSet.contains(neighbor)) 
                       || (tentativeGScore < gScore.get(neighbor)) )
                {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    fScore.put(neighbor, gScore.get(neighbor) + heuristicCostEstimate(neighbor, goal));
                    openSet.add(neighbor);
                }
            }
        }
        return null;
    }
    
    private double distanceBetween(VoxelIndex first, VoxelIndex second) {
        // Set distance to cost of second node.
        int intensity = volume.getIntensityLocal(second, 0);
        double pathScore = getPathStepCostForIntensity(intensity);
        return pathScore;
    }
    
    private List<VoxelIndex> getNeighbors(VoxelIndex center) {
        // For performance, don't step to diagonals
        // Thus, up to six neighbors in 3D
        List<VoxelIndex> result = new Vector<VoxelIndex>();
        //
        if (center.getX() > 0)
            result.add(new VoxelIndex(center.getX()-1, center.getY(), center.getZ()));
        if (center.getY() > 0)
            result.add(new VoxelIndex(center.getX(), center.getY()-1, center.getZ()));
        if (center.getZ() > 0)
            result.add(new VoxelIndex(center.getX(), center.getY(), center.getZ()-1));
        //
        if (center.getX() < volume.getExtent().getX() - 1)
            result.add(new VoxelIndex(center.getX()+1, center.getY(), center.getZ()));
        if (center.getX() < volume.getExtent().getY() - 1)
            result.add(new VoxelIndex(center.getX(), center.getY()+1, center.getZ()));
        if (center.getX() < volume.getExtent().getZ() - 1)
            result.add(new VoxelIndex(center.getX(), center.getY(), center.getZ()+1));
        //
        return result;
    }
    
    private List<VoxelIndex> reconstructPath(
            Map<VoxelIndex, VoxelIndex> cameFrom, 
            VoxelIndex start,
            VoxelIndex goal) 
    {
        List<VoxelIndex> result = new Vector<VoxelIndex>();
        VoxelIndex p = goal;
        while (! p.equals(start)) {
            result.add(p);
            p = cameFrom.get(p);
        }
        result.add(start);
        return Lists.reverse(result);
    }

    void clearCachedValues() {
        volume = null;
        minStepCost = Double.NaN;
        pathCostForIntensity = new HashMap<Integer, Double>();
        meanIntensity = Double.NaN;
        stdDevIntensity = Double.NaN;
    }
    
    // Compute mean, standard deviation, and minimum path score
    void computeIntensityStats() {
        double sumIntensity = 0;
        long intensityCount = 0;
        int maxIntensity = Integer.MIN_VALUE;
        ByteBuffer intensityBytes = volume.getByteBuffer();
        // Mean and min path
        if (volume.getBytesPerIntensity() == 2) {
            // two bytes per value ushort
            ShortBuffer shorts = intensityBytes.asShortBuffer();
            shorts.rewind();
            while (shorts.hasRemaining()) {
                int intensity = shorts.get() & 0xffff;
                maxIntensity = Math.max(intensity, maxIntensity);
                sumIntensity += intensity;
                intensityCount += 1;
            }
        }
        else { // one byte per value ubyte
            intensityBytes.rewind();
            while (intensityBytes.hasRemaining()) {
                int intensity = intensityBytes.get() & 0xff;
                maxIntensity = Math.max(intensity, maxIntensity);
                sumIntensity += intensity;
                intensityCount += 1;
            }
        }
        meanIntensity = 0.0;
        if (intensityCount > 0)
            meanIntensity = sumIntensity / (double)intensityCount;
        // Standard deviation
        double delta = 0;
        if (volume.getBytesPerIntensity() == 2) {
            // two bytes per value ushort
            ShortBuffer shorts = intensityBytes.asShortBuffer();
            shorts.rewind();
            while (shorts.hasRemaining()) {
                int intensity = shorts.get() & 0xffff;
                double di = meanIntensity - intensity;
                delta += di * di;
            }
        }
        else { // one byte per value ubyte
            intensityBytes.rewind();
            while (intensityBytes.hasRemaining()) {
                int intensity = intensityBytes.get() & 0xff;
                double di = meanIntensity - intensity;
                delta += di * di;
            }
        }
        stdDevIntensity = 1.0;
        if (intensityCount > 0) 
            stdDevIntensity = Math.sqrt(delta/(double)intensityCount);
        // minStepCost must be computed AFTER mean/stddev
        minStepCost = getPathStepCostForIntensity(maxIntensity);
    }
    
    // fractional error in math formula less than 1.2 * 10 ^ -7.
    // although subject to catastrophic cancellation when z in very close to 0
    // from Chebyshev fitting formula for erf(z) from Numerical Recipes, 6.2
    public static double erf(double z) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

        // use Horner's method
        double ans = 1 - t * Math.exp( -z*z   -   1.26551223 +
                                            t * ( 1.00002368 +
                                            t * ( 0.37409196 + 
                                            t * ( 0.09678418 + 
                                            t * (-0.18628806 + 
                                            t * ( 0.27886807 + 
                                            t * (-1.13520398 + 
                                            t * ( 1.48851587 + 
                                            t * (-0.82215223 + 
                                            t * ( 0.17087277))))))))));
        if (z >= 0) return  ans;
        else        return -ans;
    }

    double getMinStepCost() {
        // Calculate the value once, then cache it for future use
        if (Double.isNaN(minStepCost))
            computeIntensityStats();
        return minStepCost;
    }
    
    // Let path step cost be the probability that this intensity could 
    // occur by chance, given the intensity statistics.
    private double getPathStepCostForIntensity(int intensity) {
        double result;
        if (pathCostForIntensity.containsKey(intensity))
            result = pathCostForIntensity.get(intensity);
        else {
            if (Double.isNaN(meanIntensity))
                computeIntensityStats();
            double zScore = (intensity - meanIntensity) / stdDevIntensity;
            result = 0.5 * (1.0 - erf(zScore));
            // Store computed value for future use
            pathCostForIntensity.put(intensity, result);
        }
        return result;
    }

    // Must not overestimate actual cost of path to goal
    double heuristicCostEstimate(VoxelIndex v1, VoxelIndex v2) {
        int distance = 0;
        // Use Manhattan distance, and prohibit diagonal moves (for performance)
        distance += Math.abs(v1.getX() - v2.getX());
        distance += Math.abs(v1.getY() - v2.getY());
        distance += Math.abs(v1.getZ() - v2.getZ());
        return distance * getMinStepCost();
    }
    
}
