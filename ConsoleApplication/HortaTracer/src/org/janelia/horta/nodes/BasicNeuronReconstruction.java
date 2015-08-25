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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.janelia.horta.modelapi.NeuronSegment;
import org.janelia.horta.modelapi.NeuronReconstruction;

/**
 *
 * @author Christopher Bruns
 */
public class BasicNeuronReconstruction implements NeuronReconstruction
{
    private String name = "(unnamed neuron)";
    private List<NeuronSegment> nodes = new ArrayList<NeuronSegment>();

    public BasicNeuronReconstruction()
    {
    }

    public BasicNeuronReconstruction(File file) throws FileNotFoundException, IOException
    {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("#")) // skip comments
                continue;
            // 1 2 77299.3 56354.5 22206.5 1.00 -1
            String[] fields = line.split("\\s+");
            if (fields.length < 7)
                continue; // blank line?
            int label = Integer.parseInt(fields[0]);
            int type = Integer.parseInt(fields[1]);
            double x = Double.parseDouble(fields[2]);
            double y = Double.parseDouble(fields[3]);
            double z = Double.parseDouble(fields[4]);
            double radius = Double.parseDouble(fields[5]);
            int parentLabel = Integer.parseInt(fields[6]);
            NeuronSegment node = new BasicNeuronSegment(x, y, z);
            node.setLabel(label);
            node.setTypeIndex(type);
            node.setRadius(radius);
            node.setParentLabel(parentLabel);
            nodes.add(node);
        }
        // Take name from file
        if (nodes.size() > 0) {
            String neuronName = FilenameUtils.getBaseName(file.getName());
            setName(neuronName);
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
    public Collection<NeuronSegment> getSegments()
    {
        return nodes;
    }
    
}
