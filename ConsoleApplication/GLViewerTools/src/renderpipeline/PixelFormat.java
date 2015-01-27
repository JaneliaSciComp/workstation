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

package renderpipeline;

import java.util.HashMap;
import java.util.Map;
import javax.media.opengl.GL3;

/**
 *
 * @author Christopher Bruns
 */
class PixelFormat {
    
    // STATIC
    private static final Map<Integer, PixelFormat> formatForCode = new HashMap<Integer, PixelFormat>();
    private static final Map<String, PixelFormat> formatForName = new HashMap<String, PixelFormat>();
    
    static final public PixelFormat RGBA8 = new PixelFormat("RGBA8", GL3.GL_RGBA8, 4, 8);
    
    static public PixelFormat get(String name) {return formatForName.get(name);}
    static public PixelFormat get(int code) {return formatForCode.get(code);}

    // INSTANCE
    private final String name;
    private final int glCode;
    private final int numChannels;
    private final int bitsPerChannel;

    protected PixelFormat(String name, int glCode, int numChannels, int bitsPerChannel) 
    {
        this.name = name;
        this.glCode = glCode;
        this.numChannels = numChannels;
        this.bitsPerChannel = bitsPerChannel;
        formatForCode.put(glCode, this);
        formatForName.put(name, this);
    }

    public String getName() {
        return name;
    }

    public int getGlCode() {
        return glCode;
    }

    public int getNumChannels() {
        return numChannels;
    }

    public int getBitsPerChannel() {
        return bitsPerChannel;
    }
    
}
