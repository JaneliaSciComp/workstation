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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Christopher Bruns
 */
public class BrickSetSource
{
    private List<URL> brickFolders = new ArrayList<URL>();
    private URI parentFolder;
    private int brickWidth, brickHeight, brickDepth, brickChannels, brickBitDepth;
    
    static Pattern headerPatterns[] = new Pattern[] {
        Pattern.compile("# X width in pixels: (\\d+)"), 
        Pattern.compile("# Y height in pixels: (\\d+)"), 
        Pattern.compile("# Z depth in pixels: (\\d+)"), 
        Pattern.compile("# Color channels: (\\d+)"), 
        Pattern.compile("# Bits per intensity: (\\d+)")};
    
    // Load image source metadata from text file
    public BrickSetSource(URL tileListFile) throws IOException, URISyntaxException
    {
        parentFolder = tileListFile.toURI().resolve(".");
        URLConnection connection = tileListFile.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
            if (inputLine.startsWith("#")) {
                for (int p = 0; p < headerPatterns.length; ++p) {
                    Matcher m = headerPatterns[p].matcher(inputLine);
                    if (m.matches()) {
                        int val = Integer.parseInt(m.group(1));
                        switch (p) {
                            case 0:
                                brickWidth = val;
                                break;
                            case 1:
                                brickHeight = val;
                                break;
                            case 2:
                                brickDepth = val;
                                break;
                            case 3:
                                brickChannels = val;
                                break;
                            case 4:
                                brickBitDepth = val;
                                break;
                        }
                        break;
                    }
                }
            }
            else {
                // TODO parse block definition
                URL brickUrl = parentFolder.resolve(inputLine).toURL();
                brickFolders.add(brickUrl);
            }
        }
        reader.close();
    }

    public List<URL> getBrickFolders()
    {
        return brickFolders;
    }

    public int getBrickWidth()
    {
        return brickWidth;
    }

    public int getBrickHeight()
    {
        return brickHeight;
    }

    public int getBrickDepth()
    {
        return brickDepth;
    }

    public int getBrickChannels()
    {
        return brickChannels;
    }

    public int getBrickBitDepth()
    {
        return brickBitDepth;
    }
    
    
}
