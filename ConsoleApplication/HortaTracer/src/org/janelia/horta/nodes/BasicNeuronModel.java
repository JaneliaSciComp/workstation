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

package org.janelia.horta.nodes;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.model.BasicNeuronVertexCreationObservable;
import org.janelia.console.viewerapi.model.BasicNeuronVertexDeletionObservable;
import org.janelia.console.viewerapi.model.BasicNeuronVertexUpdateObservable;
import org.janelia.geometry3d.Vector3;
import org.janelia.console.viewerapi.model.NeuronEdge;
import org.janelia.horta.modelapi.SwcVertex;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.NeuronVertexCreationObservable;
import org.janelia.console.viewerapi.model.NeuronVertexDeletionObservable;
import org.janelia.console.viewerapi.model.NeuronVertexUpdateObservable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
public class BasicNeuronModel implements NeuronModel
{
    private String name = "(unnamed neuron)";
    private List<NeuronVertex> nodes = new ArrayList<>();
    private List<NeuronEdge> edges = new ArrayList<>();
    private final ObservableInterface colorChangeObservable = new ComposableObservable();
    private final ObservableInterface geometryChangeObservable = new ComposableObservable();
    private final ObservableInterface visibilityChangeObservable = new ComposableObservable();
    private final NeuronVertexCreationObservable membersAddedObservable;
    private final NeuronVertexUpdateObservable vertexUpdatedObservable;
    private final NeuronVertexDeletionObservable membersRemovedObservable;
    private Color color = new Color(86, 142, 216); // default color is "neuron blue"
    private boolean visible = true;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final NeuronSet parentSet;

    
    /*
     * Creates an empty neuron model
    */
    public BasicNeuronModel(String modelName, NeuronSet parentSet)    
    {
        this.membersAddedObservable = new BasicNeuronVertexCreationObservable();
        this.membersRemovedObservable = new BasicNeuronVertexDeletionObservable();
        this.vertexUpdatedObservable = new BasicNeuronVertexUpdateObservable();
        this.name = modelName;
        this.parentSet = parentSet;
    }
    
    public BasicNeuronModel() {
        this("Unnamed Neuron", null);
    }
    
    public BasicNeuronModel(File swcFile, NeuronSet parentSet) throws FileNotFoundException, IOException
    {
        this(new BufferedInputStream(new FileInputStream(swcFile)), swcFile.getName(), parentSet);
    }
    
    public BasicNeuronModel(InputStream swcStream, String fileName, NeuronSet parentSet) throws IOException
    {
        this.parentSet = parentSet;
        this.membersAddedObservable = new BasicNeuronVertexCreationObservable();
        this.membersRemovedObservable = new BasicNeuronVertexDeletionObservable();
        this.vertexUpdatedObservable = new BasicNeuronVertexUpdateObservable();
        BufferedReader br = new BufferedReader(new InputStreamReader(swcStream));
        String line;
        
        // Parse origin offset from header
        // # OFFSET 77277.017247 44351.723117 24137.592725
        Vector3 originOffset = new Vector3(0, 0, 0);
        String sepRx = "[ ,]+";
        String floatRx = "(-?[0-9\\.]+)";
        Pattern offsetPattern = Pattern.compile("^\\s*#\\s*OFFSET\\s+"
                +floatRx // x
                +sepRx
                +floatRx // y
                +sepRx
                +floatRx // z
                +"\\s*$");
        
        // Parse neuron color from header
        // # COLOR 0.000000,1.000000,0.000000
        Pattern colorPattern = Pattern.compile("^\\s*#\\s*COLOR\\s+"
                +floatRx // red
                +sepRx
                +floatRx // green
                +sepRx
                +floatRx // blue
                +"\\s*$");
        
        // Store parent relationships for resolution after reading
        Map<Integer, Integer> childParentMap = new HashMap<>();
        Map<Integer, SwcVertex> vertexMap = new HashMap<>();
        while ((line = br.readLine()) != null) {
            if (line.startsWith("#")) { 
                // Parse origin offset
                Matcher m = offsetPattern.matcher(line);
                if (m.matches()) {
                    float ox = Float.parseFloat(m.group(1));
                    float oy = Float.parseFloat(m.group(2));
                    float oz = Float.parseFloat(m.group(3));
                    originOffset.setX(ox);
                    originOffset.setY(oy);
                    originOffset.setZ(oz);
                }
                // Parse color
                m = colorPattern.matcher(line);
                if (m.matches()) {
                    float cr = Float.parseFloat(m.group(1));
                    float cg = Float.parseFloat(m.group(2));
                    float cb = Float.parseFloat(m.group(3));
                    color = new Color(cr, cg, cb);
                }
                continue; // skip comments
            }
            // 1 2 77299.3 56354.5 22206.5 1.00 -1
            String[] fields = line.split("\\s+");
            if (fields.length < 7)
                continue; // blank line?
            int label = Integer.parseInt(fields[0]);
            int type = Integer.parseInt(fields[1]);
            float x = Float.parseFloat(fields[2]) + originOffset.getX();
            float y = Float.parseFloat(fields[3]) + originOffset.getY();
            float z = Float.parseFloat(fields[4]) + originOffset.getZ();
            float radius = Float.parseFloat(fields[5]);
            int parentLabel = Integer.parseInt(fields[6]);
            SwcVertex node = new BasicSwcVertex(x, y, z);
            node.setLabel(label);
            node.setTypeIndex(type);
            node.setRadius(radius);
            vertexMap.put(label, node);
            if (parentLabel >= 0)
                childParentMap.put(label, parentLabel);
            // node.setParentLabel(parentLabel);
            nodes.add(node);
        }
        // Assign parents  
        // ...after full load, in case node order is imperfect
        for (int childLabel : childParentMap.keySet()) {
            int parentLabel = childParentMap.get(childLabel);
            if (parentLabel < 0)
                continue;
            SwcVertex parentVertex = vertexMap.get(parentLabel);
            if (parentVertex == null)
                continue;
            SwcVertex childVertex = vertexMap.get(childLabel); 
            assert(childVertex != null);
            assert(childVertex != parentVertex);
            edges.add(new BasicNeuronEdge(parentVertex, childVertex));
        }
        // Take name from file
        if (nodes.size() > 0) {
            this.name = FilenameUtils.getBaseName(fileName);
        }
    }

    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
        // TODO: name change observable?
    }

    @Override
    public Collection<NeuronVertex> getVertexes()
    {
        return nodes;
    }

    @Override
    public Color getColor()
    {
        return color;
    }

    @Override
    public void setColor(Color color)
    {
        // logger.info("Neuron color set to "+color);
        if (color.equals(this.color))
            return;
        this.color = color;
        getColorChangeObservable().setChanged();
        // notifyObservers(); // commented, so delegate to a higher authority, such as the ReconstructionNode
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible == this.visible)
            return;
        this.visible = visible;
        getVisibilityChangeObservable().setChanged();
        // notifyObservers(); // delegate to a higher authority, such as the ReconstructionNode
    }

    @Override
    public Collection<NeuronEdge> getEdges()
    {
        return edges;
    }

    @Override
    public ObservableInterface getColorChangeObservable()
    {
        return colorChangeObservable;
    }

    @Override
    public ObservableInterface getGeometryChangeObservable()
    {
        return geometryChangeObservable;
    }

    @Override
    public ObservableInterface getVisibilityChangeObservable()
    {
        return visibilityChangeObservable;
    }

    @Override
    public NeuronVertexCreationObservable getVertexCreatedObservable()
    {
        return membersAddedObservable;
    }

    @Override
    public NeuronVertexDeletionObservable getVertexesRemovedObservable()
    {
        return membersRemovedObservable;
    }

    @Override
    public NeuronVertex appendVertex(NeuronVertex parentVertex, float[] micronXyz, float radius) {
        NeuronVertex newVertex = new BasicSwcVertex(micronXyz[0], micronXyz[1], micronXyz[2]);
        newVertex.setRadius(radius);
        nodes.add(newVertex);
        if (parentVertex != null) {
            edges.add(new BasicNeuronEdge(parentVertex, newVertex));
        }
        getVertexCreatedObservable().setChanged();
        return newVertex;
    }

    @Override
    public boolean deleteVertex(NeuronVertex doomedVertex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean mergeNeurite(NeuronVertex source, NeuronVertex target) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NeuronVertexUpdateObservable getVertexUpdatedObservable() {
        return vertexUpdatedObservable;
    }

    @Override
    public boolean moveVertex(NeuronVertex vertex, float[] micronXyz) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean updateVertexRadius(NeuronVertex vertex, float micronRadius) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public NeuronVertex getVertexByGuid(Long guid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean splitNeurite(NeuronVertex anchor1, NeuronVertex anchor2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean transferNeurite(NeuronVertex anchor) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
