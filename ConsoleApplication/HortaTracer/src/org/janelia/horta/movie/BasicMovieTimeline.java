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

package org.janelia.horta.movie;

import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brunsc
 */
class BasicMovieTimeline<T extends ViewerState> 
implements Timeline<T>
{
    private final Deque<KeyFrame<T>> keyFrames = new ConcurrentLinkedDeque<>();
    private final Interpolator<T> interpolator;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public BasicMovieTimeline(Interpolator<T> interpolator) {
        this.interpolator = interpolator;
    }

    @Override
    public float getTotalDuration(boolean doLoop) {
        Deque<KeyFrame<T>> f = this;
        float totalDuration = 0;
        for (KeyFrame<T> keyFrame : f) {
            totalDuration += keyFrame.getFollowingIntervalDuration();
        }
        // Don't include final frame duration, unless movie is a loop
        if ( (! doLoop) && (f.size() > 0) ) {
            KeyFrame<T> finalFrame = f.getLast();
            totalDuration -= finalFrame.getFollowingIntervalDuration();
        }
        return totalDuration;
    }

    @Override
    public void addFirst(KeyFrame<T> e) {
        keyFrames.addFirst(e);
    }

    @Override
    public void addLast(KeyFrame<T> e) {
        keyFrames.addLast(e);
    }

    @Override
    public boolean offerFirst(KeyFrame<T> e) {
        return keyFrames.offerFirst(e);
    }

    @Override
    public boolean offerLast(KeyFrame<T> e) {
        return keyFrames.offerLast(e);
    }

    @Override
    public KeyFrame<T> removeFirst() {
        return keyFrames.removeFirst();
    }

    @Override
    public KeyFrame<T> removeLast() {
        return keyFrames.removeLast();
    }

    @Override
    public KeyFrame<T> pollFirst() {
        return keyFrames.pollFirst();
    }

    @Override
    public KeyFrame<T> pollLast() {
        return keyFrames.pollLast();
    }

    @Override
    public KeyFrame<T> getFirst() {
        return keyFrames.getFirst();
    }

    @Override
    public KeyFrame<T> getLast() {
        return keyFrames.getLast();
    }

    @Override
    public KeyFrame<T> peekFirst() {
        return keyFrames.peekFirst();
    }

    @Override
    public KeyFrame<T> peekLast() {
        return keyFrames.peekLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return keyFrames.removeFirstOccurrence(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return keyFrames.removeLastOccurrence(o);
    }

    @Override
    public boolean add(KeyFrame<T> e) {
        return keyFrames.add(e);
    }

    @Override
    public boolean offer(KeyFrame<T> e) {
        return keyFrames.offer(e);
    }

    @Override
    public KeyFrame<T> remove() {
        return keyFrames.remove();
    }

    @Override
    public KeyFrame<T> poll() {
        return keyFrames.poll();
    }

    @Override
    public KeyFrame<T> element() {
        return keyFrames.element();
    }

    @Override
    public KeyFrame<T> peek() {
        return keyFrames.peek();
    }

    @Override
    public void push(KeyFrame<T> e) {
        keyFrames.push(e);
    }

    @Override
    public KeyFrame<T> pop() {
        return keyFrames.pop();
    }

    @Override
    public boolean remove(Object o) {
        return keyFrames.remove(o);
    }

    @Override
    public boolean contains(Object o) {
        return keyFrames.contains(o);
    }

    @Override
    public int size() {
        return keyFrames.size();
    }

    @Override
    public Iterator<KeyFrame<T>> iterator() {
        return keyFrames.iterator();
    }

    @Override
    public Iterator<KeyFrame<T>> descendingIterator() {
        return keyFrames.descendingIterator();
    }

    @Override
    public boolean isEmpty() {
        return keyFrames.isEmpty();
    }

    @Override
    public Object[] toArray() {
        return keyFrames.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return keyFrames.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return keyFrames.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends KeyFrame<T>> c) {
        return keyFrames.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return keyFrames.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return keyFrames.retainAll(c);
    }

    @Override
    public void clear() {
        keyFrames.clear();
    }

    @Override
    public int hashCode() {
        return keyFrames.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return keyFrames.equals(obj);
    }

    @Override
    public String toString() {
        return keyFrames.toString();
    }
    
    @Override
    public T viewerStateForTime(float timeInSeconds, boolean doLoop) 
    {
        if (isEmpty())
            return null; // There are no keyframes to interpolate
        
        // Find four frame neighborhood for interpolation
        KeyFrame<T> k0, k1, k2, k3;
        k0 = k1 = k2 = k3 = null;
        
        float currentKeyFrameHeadPosition = 0;
        float partialFrameInterval = 0; // seconds into inter-frame interval
        Iterator<KeyFrame<T>> it = this.iterator();
        KeyFrame<T> previousKeyFrame = null;
        KeyFrame<T> previousKeyFrame2 = null;
        
        // This loop finds first three key frames
        boolean doBreak = false;
        while (it.hasNext()) {
            KeyFrame<T> keyFrame = it.next();
            if (currentKeyFrameHeadPosition > timeInSeconds) {
                k2 = keyFrame;
                k1 = previousKeyFrame;
                k0 = previousKeyFrame2;
                if (k1 != null) {
                    partialFrameInterval = k1.getFollowingIntervalDuration() - currentKeyFrameHeadPosition + timeInSeconds;
                }
                break;
            }
            else if (currentKeyFrameHeadPosition == timeInSeconds) {
                k1 = keyFrame;
                k0 = previousKeyFrame;
                if (it.hasNext())
                    k2 = it.next();
                partialFrameInterval = 0;
                previousKeyFrame2 = previousKeyFrame;
                previousKeyFrame = keyFrame;
                break;
            }
            else {
                currentKeyFrameHeadPosition += keyFrame.getFollowingIntervalDuration();
                previousKeyFrame2 = previousKeyFrame;
                previousKeyFrame = keyFrame;
                continue;
            }
        }
        // get fourth key frame
        if (it.hasNext())
            k3 = it.next();
        
        // maybe requested time is in duration of final frame
        if (k2 == null) {
            k1 = previousKeyFrame; // final frame
            k0 = previousKeyFrame2;
            if (k1 != null) {
                partialFrameInterval = k1.getFollowingIntervalDuration() - currentKeyFrameHeadPosition + timeInSeconds;
            }
        }
        
        assert(k1 != null);
        
        // Looping requires interpolation between final and initial key frames
        if ((k1 == getFirst()) && (doLoop)) {
            k0 = getLast();
        }        
        if ((k1 == getLast()) && (doLoop)) {
            k2 = getFirst();
        }
        
        // Pad empty ends with duplicates
        if (k0 == null)
            k0 = k1;        
        if (k2 == null)
            k2 = k1;
        if (k3 == null)
            k3 = k2;
        
        if (k1 == k2)
            partialFrameInterval = 0;
        
        // Compute the offset parameter between frames k1 and k2
        float t = 0;
        if (k1.getFollowingIntervalDuration() > 0)
            t = partialFrameInterval / k1.getFollowingIntervalDuration();
        // sanity checks
        if (t < 0) {
            t = 0;
        }
        if (t > 1) {
            t = 1;
        }

        // logger.info("partialFrameInterval = " + partialFrameInterval);
        
        // Compute local time stamps for each key frame
        double t0 = 0;
        double t1 = t0 + k0.getFollowingIntervalDuration();
        if (k0 == k1)
            t1 = t0;
        double t2 = t1 + k1.getFollowingIntervalDuration();
        if (k1 == k2)
            t2 = t1;
        double t3 = t2 + k2.getFollowingIntervalDuration();
        if (k2 == k3)
            t3 = t2;
        
        T result = interpolator.interpolate(t, 
                k0.getViewerState(), k1.getViewerState(), k2.getViewerState(), k3.getViewerState(),
                t0, t1, t2, t3);
        
        return result;
    }

    @Override
    public JsonObject serializeKeyFrame(KeyFrame<T> state) {
        return state.serializeJson();
    }

    @Override
    public KeyFrame<T> deserializeKeyFrame(JsonObject json) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
