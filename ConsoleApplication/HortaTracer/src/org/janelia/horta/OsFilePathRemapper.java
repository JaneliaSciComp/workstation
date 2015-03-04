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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper methods to reliably convert from the "standardized" linux file location
 * to a predictable file location on another operating system.
 * @author Christopher Bruns
 */
public class OsFilePathRemapper
{
    private static final Map<String,String> sWinPrefixMappings = new HashMap<>();
    private static final Map<String,String> sMacPrefixMappings = new HashMap<>();
    static {
        sWinPrefixMappings.put("/nobackup/mousebrainmicro/", "\\\\fxt\\nobackup\\mousebrainmicro\\"); // Windows
        sWinPrefixMappings.put("/groups/mousebrainmicro/mousebrainmicro/", "\\\\dm11\\mousebrainmicro\\");
        sWinPrefixMappings.put("/tier2/mousebrainmicro/mousebrainmicro/", "\\\\tier2\\mousebrainmicro\\mousebrainmicro\\");
        //prefixMappings.put("/tier2/", "//tier2/"); // Windows
        //prefixMappings.put("/groups/mousebrainmicro/mousebrainmicro/", "//dm11/mousebrainmicro/"); // Windows

        sMacPrefixMappings.put("/nobackup/mousebrainmicro/", "/Volumes/nobackup/mousebrainmicro/"); // Mac
        sMacPrefixMappings.put("/groups/mousebrainmicro/mousebrainmicro/", "/Volumes/mousebrainmicro/");
        sMacPrefixMappings.put("/tier2/mousebrainmicro/mousebrainmicro/", "/Volumes/mousebrainmicro/mousebrainmicro/");
    }
    
    public static String remapLinuxPath(String linuxPath) {
        File file = new File(linuxPath);
        // Only munge the path if the stated path does not exist
        if (file.exists())
            return linuxPath;
        
        String path = linuxPath;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            path = replaceViaMapping(path, sWinPrefixMappings);
        // path = path.replace("/tier2/mousebrainmicro/mousebrainmicro/", "X:/");
//            path = path.replace("/nobackup/mousebrainmicro/", "\\\\fxt\\nobackup\\mousebrainmicro\\");
//            path = path.replace("/groups/mousebrainmicro/mousebrainmicro/", "\\\\dm11\\mousebrainmicro\\");
//            path = path.replace("/tier2/mousebrainmicro/mousebrainmicro/", "\\\\tier2\\mousebrainmicro\\mousebrainmicro\\");
        }
        else if (os.contains("mac")) {
            path = replaceViaMapping(path, sMacPrefixMappings);
        }
        else {
            System.out.println("Unrecognized OS name " + os + " returning linux path.");
        }
            
        return path;
    }    

    private static String replaceViaMapping(String path, Map<String,String> mapping) {
        for (String key: mapping.keySet()) {
            path = path.replace(key, mapping.get(key));
        }
        return path;
    }
}
