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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class BrainTileInfoList 
implements Map<String, BrainTileInfo>, Iterable<BrainTileInfo>
{
    private final LinkedHashMap<String, BrainTileInfo> map = new LinkedHashMap<>();
    private String tilebasePath;

    public String getTilebasePath() {
        return tilebasePath;
    }
    
    public void loadYamlFile(File f) throws IOException {
        loadYamlFile(new FileInputStream(f));
    }

    public void loadYamlFile(InputStream is) throws IOException {
        // \\dm11\mousebrainmicro\stitch\2014-04-04subset\tilebase.cache.yml.filtered
        Yaml yaml = new Yaml();
        Map<String, Object> tilebase = (Map<String, Object>)yaml.load(is);
        // System.out.println(tilebase.getClass().getName());
        tilebasePath = (String) tilebase.get("path");
        List<Map<String, Object>> tiles = (List<Map<String, Object>>) tilebase.get("tiles");
        // Index tiles for easy retrieval
        for (Map<String, Object> tile : tiles) {
            String tilePath = (String) tile.get("path");
            map.put(tilePath, new BrainTileInfo(tile, tilebasePath));
            // System.out.println(tilePath);
        }
    }
    
    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public BrainTileInfo get(Object key) {
        return map.get(key);
    }

    @Override
    public BrainTileInfo put(String key, BrainTileInfo value) {
        return map.put(key, value);
    }

    @Override
    public BrainTileInfo remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends BrainTileInfo> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<BrainTileInfo> values() {
        return map.values();
    }

    @Override
    public Set<Entry<String, BrainTileInfo>> entrySet() {
        return map.entrySet();
    }

    @Override
    public Iterator<BrainTileInfo> iterator() {
        return map.values().iterator();
    }
    
}
