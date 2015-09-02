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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import org.apache.commons.io.FilenameUtils;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.horta.modelapi.SwcVertex;
import org.janelia.horta.modelapi.NeuronReconstruction;
import org.janelia.horta.modelapi.NeuronVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
public class BasicNeuronReconstruction implements NeuronReconstruction
{
    private String name = "(unnamed neuron)";
    private List<NeuronVertex> nodes = new ArrayList<>();
    private final ComposableObservable changeObservable = new ComposableObservable();
    private Color color = new Color(86, 142, 216); // default color is "neuron blue"
    private boolean visible = true;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public BasicNeuronReconstruction()
    {
    }

    public BasicNeuronReconstruction(File file) throws FileNotFoundException, IOException
    {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        // Store parent relationships for resolution after reading
        Map<Integer, Integer> childParentMap = new HashMap<>();
        Map<Integer, SwcVertex> vertexMap = new HashMap<>();
        while ((line = br.readLine()) != null) {
            if (line.startsWith("#")) // skip comments
                continue;
            // 1 2 77299.3 56354.5 22206.5 1.00 -1
            String[] fields = line.split("\\s+");
            if (fields.length < 7)
                continue; // blank line?
            int label = Integer.parseInt(fields[0]);
            int type = Integer.parseInt(fields[1]);
            float x = Float.parseFloat(fields[2]);
            float y = Float.parseFloat(fields[3]);
            float z = Float.parseFloat(fields[4]);
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
            childVertex.setParentVertex(parentVertex);
        }
        // Take name from file
        if (nodes.size() > 0) {
            this.name = FilenameUtils.getBaseName(file.getName());
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
        setChanged();
        // notifyObservers(); // commented, so delegate to a higher authority...
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
        setChanged();
        // notifyObservers(); // delegate to a higher authority...
    }
    
    @Override
    public void setChanged()
    {
        changeObservable.setChanged();
    }

    @Override
    public void notifyObservers()
    {
        changeObservable.notifyObservers();
    }

    @Override
    public void addObserver(Observer observer)
    {
        changeObservable.addObserver(observer);
    }

    @Override
    public void deleteObserver(Observer observer)
    {
        changeObservable.deleteObserver(observer);
    }

    @Override
    public void deleteObservers()
    {
        changeObservable.deleteObservers();
    }
    
}
