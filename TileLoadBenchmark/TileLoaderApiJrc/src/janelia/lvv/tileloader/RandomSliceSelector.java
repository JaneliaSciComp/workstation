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

package janelia.lvv.tileloader;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class RandomSliceSelector implements LoadStrategem
{
    // private final List<SubstackInfo> slices = new ArrayList<SubstackInfo>();
    private final Random random = new Random();
    private final int sliceCount;
    private final BrickSetSource source;
    
    public RandomSliceSelector(BrickSetSource source, int sliceCount) {
        this.sliceCount = sliceCount;
        this.source = source;
    }

    @Override
    public Iterator<SubstackInfo> iterator()
    {
        List<SubstackInfo> slices = new ArrayList<SubstackInfo>();
        int blockCount = source.getBrickFolders().size();
        int slicesPerBlock = source.getBrickDepth();
        // Select slices at random
        for (int s = 0; s < sliceCount; ++s) {
            int b = random.nextInt(blockCount);
            int z = random.nextInt(slicesPerBlock);
            List<Integer> ix = new ArrayList<Integer>();
            ix.add(z);
            URL url;
            try {
                url = source.getBrickFolders().get(b).toURI().resolve(".").toURL();
                slices.add(new SubstackInfo(url, ix));
            } catch (URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            } catch (MalformedURLException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return slices.iterator();
    }

}
