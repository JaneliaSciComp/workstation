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

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 *
 * @author Christopher Bruns
 */
public class TwoDimensionalRadiusEstimator 
implements RadiusEstimator
{

    @Override
    public float estimateRadius(Point screenPoint, VolumeProjection image) 
    {
        int intensity = image.getIntensity(screenPoint);
        if (intensity <= 0) return 0f;
        RadiusCandidate radius = new RadiusCandidate(
                screenPoint.x, screenPoint.y, 
                intensity);
        radius.computeXYRadius(image);
        return radius.radiusUm;
    }
    
    // Class to help with July 20 2014 radius approach
    private class RadiusCandidate {
        public int x;
        public int y;
        // radiusUm is the best radius identified so far
        public float radiusUm = 0f;
        public int centerIntensity; // centerIntensity at CENTER
        // radiusScore is the criterion used choose the best radius
        // the product of dIntensity/dDistance, with prior expectation of neuron radius
        public float radiusScore = 0.0f; // between center and radius
        // centerIntensity range is the brightness of this neurite, and should be stored TODO
        public int intensityRange = 0;
        
        final float minRadiusUm = 0.3f; // ** ADJUSTABLE PARAMETER **
        // final float idealRadiusUm = 0.8f; // ** ADJUSTABLE PARAMETER **
        final float maxRadiusUm = 10.0f; // ** ADJUSTABLE PARAMETER **
        // padDistanceUm prevents noisy intensities near neurite center from generating giant scores
        // larger values of padDistance might result in larger radii
        final float padDistanceUm = 0.1f; // ** ADJUSTABLE PARAMETER **
        // larger values of padIntensity might result in smaller radii
        final int padIntensity = 1000; // ** AJUSTABLE PARAMETER **
        // Larger values of extremeRadius weight flatten out the prior radius distribution
        // Looks like 1.0 is too small - all radii are estimated the same
        // final float extremeRadiusWeight = 100.0f; // ** ADJUSTABLE PARAMETER? **
        // final float pixelsPerUm = image.getPixelsPerSceneUnit();
        
        public RadiusCandidate(int x, int y, int intensity) {
            this.x = x;
            this.y = y;
            this.centerIntensity = intensity;
        }
        
        public class RadiusScoreAndIntensityRange {
            public float score = 0;
            public int intensityRange = 0;
        }
        
        public RadiusScoreAndIntensityRange computeRadiusScore(
                float dxUm, 
                float dyUm,
                VolumeProjection volumeProjection) 
        {
            RadiusScoreAndIntensityRange result = new RadiusScoreAndIntensityRange();
            float pixelsPerUm = volumeProjection.getPixelsPerSceneUnit();
            int i = volumeProjection.getIntensity(new Point2D.Float(
                    x + dxUm * pixelsPerUm, 
                    y + dyUm * pixelsPerUm));
            if (i <= 0) return result;
            // slope is a critical radius estimator
            int iRange = centerIntensity - i + padIntensity;
            float slopeDistance = (float)Math.sqrt(dxUm*dxUm + dyUm*dyUm) + padDistanceUm;
            float slope = iRange / slopeDistance;
            if (slope < 0) 
                return result;
            result.intensityRange = iRange;
            result.score = slope;
            return result;
        }
        
        // Try to find a better radius estimate in a particular search direction
        // TODO - is there a way to terminate early, based on other directions results?
        // TODO - or use binary search, to minimize number of lookups
        public void searchDirection(VolumeProjection volumeProjection, int dxPx, int dyPx) 
        {
            float pixelsPerUm = volumeProjection.getPixelsPerSceneUnit();
            float dxUm = dxPx / pixelsPerUm;
            float dyUm = dyPx / pixelsPerUm;
            float stepSizeUm = (float)Math.sqrt(dxUm*dxUm + dyUm*dyUm);
            
            final boolean doRadiusSearchLinear = true;
            if (doRadiusSearchLinear) {
                // How many pixel steps to try?
                final int tMin = 1;
                final int tMax = (int)Math.ceil(maxRadiusUm / stepSizeUm);
                float localBestScore = 0; // only used for early termination condition
                for (int t = tMin; t <= tMax; t += 1) {
                    RadiusScoreAndIntensityRange si = computeRadiusScore(t*dxUm, t*dyUm, volumeProjection);
                    float score = si.score;
                    float edgeDistanceUm = (t - 0.5f)*stepSizeUm;
                    final boolean usePriorWeight = true;
                    if (usePriorWeight) {
                        // Multiply by prior expectation of neurite size
                        float prior;
                        if (edgeDistanceUm < minRadiusUm)
                            prior = 0.0f;
                        else if (edgeDistanceUm > maxRadiusUm)
                            prior = 0.0f;
                        else
                            prior = 1.0f;
                        prior = Math.max(0, prior);
                        score = score * prior;
                    }

                    if (score > radiusScore) {
                        radiusScore = score;
                        // Step back one half step, because this is the interval between voxels
                        radiusUm = edgeDistanceUm;
                        intensityRange = si.intensityRange;
                    }
                    // Early termination optimization
                    localBestScore = Math.max(localBestScore, score);
                    if ( (score > 0) && (localBestScore > 2 * score) ) // adjustable parameter?
                        break;
                }
            }
            else { // binary search for optimal radius
                float dirScale = (float)Math.sqrt(dxUm*dxUm + dyUm*dyUm);
                Point2D dir = new Point2D.Float(
                        dxUm / dirScale, 
                        dyUm / dirScale);
                ScoreBracket sb = new ScoreBracket(dir, minRadiusUm, maxRadiusUm, this,
                        volumeProjection);
                sb.refineBracket(0.3f);
                ScoreBracket.RadiusScore best = sb.middleMax;
                if (best.score > radiusScore) {
                    radiusScore = sb.middleMax.score;
                    radiusUm = sb.middleMax.radius;
                    intensityRange = sb.middleMax.intensityRange;
                }
            }
        }
        
        public void computeXYRadius(VolumeProjection volumeProjection) {
            searchDirection(volumeProjection, 1,  0);
            searchDirection(volumeProjection, 0,  1);
            searchDirection(volumeProjection,-1,  0);
            searchDirection(volumeProjection, 0, -1);
            searchDirection(volumeProjection, 1,  1);
            searchDirection(volumeProjection,-1, -1);
            searchDirection(volumeProjection, 1, -1);
            searchDirection(volumeProjection,-1,  1);
        }
    }
    
    // ScoreBracket class helps optimize radius by binary search
    // TODO - refactor all these radius helper classes...
    private class ScoreBracket {
        RadiusScore leftEdge;
        RadiusScore middleMax;
        RadiusScore rightEdge;
        final Point2D direction;
        final RadiusCandidate radiusCandidate;
        
        ScoreBracket(
                Point2D direction, 
                float minRadius, 
                float maxRadius, 
                RadiusCandidate radiusCandidate,
                VolumeProjection volumeProjection) 
        {
            this.radiusCandidate = radiusCandidate;
            this.direction = direction;
            leftEdge = new RadiusScore(minRadius, 0, 0, volumeProjection);
            rightEdge = new RadiusScore(maxRadius, 0, 0, volumeProjection);
            middleMax = leftEdge.interpolate(rightEdge);
        }
        
        class RadiusScore {
            float radius;
            float score = 0;
            int intensityRange = 0;
            private VolumeProjection volumeProjection;
            
            RadiusScore(float radius, float score, int intensityRange, VolumeProjection volumeProjection) 
            {
                this.radius = radius;
                this.score = score;
                this.intensityRange = intensityRange;
                this.volumeProjection = volumeProjection;
            }
            
            RadiusScore interpolate(RadiusScore right) {
                RadiusScore left = this;
                float r = 0.5f * (left.radius + right.radius);
                RadiusCandidate.RadiusScoreAndIntensityRange si = 
                        radiusCandidate.computeRadiusScore(
                        (float)(this.radius*direction.getX()), 
                        (float)(this.radius*direction.getY()),
                        volumeProjection);
                return new RadiusScore(r, si.score, si.intensityRange, volumeProjection);
            }
        }

        public boolean isConverged(float maxDRadius) {
            float dRadius = Math.max(
                    Math.abs(middleMax.radius - leftEdge.radius),
                    Math.abs(rightEdge.radius - middleMax.radius));
            return (dRadius <= maxDRadius);
        }
        
        public void refineBracket(float maxDRadius) {
            while(! isConverged(maxDRadius)) {
                refineBracketOneStep();
            }
        }
        
        private void refineBracketOneStep() {
            // Refine scoring from the lowest scoring edge first
            // Always keep the maximum score in the middle
            if (leftEdge.score < rightEdge.score) { // low-max-med
                RadiusScore newScore = leftEdge.interpolate(middleMax);
                if (newScore.score > middleMax.score) { // [low-max-med]-med
                    rightEdge = middleMax;
                    middleMax = newScore;
                }
                else { // low-[med-max-med]
                    leftEdge = newScore;
                }
            }
            else { // med-max-low
                RadiusScore newScore = middleMax.interpolate(rightEdge);
                if (newScore.score > middleMax.score) { // med-[med-max-low]
                    leftEdge = middleMax;
                    middleMax = newScore;
                }
                else { // [med-max-med]-low
                    rightEdge = newScore;
                }
            }
        }
    };
}
