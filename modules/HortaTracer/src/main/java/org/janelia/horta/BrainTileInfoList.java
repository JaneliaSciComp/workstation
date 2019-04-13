
package org.janelia.horta;

import org.janelia.console.viewerapi.OsFilePathRemapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.shared.lvv.HttpDataSource;
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
    
    public void loadYamlFile(File f) throws IOException, ParseException {
        loadYamlFile(new FileInputStream(f));
    }

    public void loadYamlFile(InputStream is) throws IOException, ParseException {
        // \\dm11\mousebrainmicro\stitch\2014-04-04subset\tilebase.cache.yml.filtered
        Yaml yaml = new Yaml();
        Map<String, Object> tilebase = (Map<String, Object>)yaml.load(is);
        // System.out.println(tilebase.getClass().getName());
        tilebasePath = (String) tilebase.get("path");

        if (!HttpDataSource.useHttp()) {
            tilebasePath = OsFilePathRemapper.remapLinuxPath(tilebasePath); // Convert to OS-specific file path
            System.out.println("BrainTileInfoList - changed tilebasePath="+tilebasePath);
        } else {
            System.out.println("BrainTileInfoList - using http, keeping original tilebasePath="+tilebasePath);
        }

        List<Map<String, Object>> tiles = (List<Map<String, Object>>) tilebase.get("tiles");
        // Index tiles for easy retrieval
        for (Map<String, Object> tile : tiles) {
            String tilePath = (String) tile.get("path");
            map.put(tilePath, new BrainTileInfo(tile, tilebasePath, false));
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
