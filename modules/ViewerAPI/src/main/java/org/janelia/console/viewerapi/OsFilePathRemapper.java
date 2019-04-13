package org.janelia.console.viewerapi;

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
        sWinPrefixMappings.put("/nobackup/mousebrainmicro/", "//fxt/nobackup/mousebrainmicro/"); // Windows
        sWinPrefixMappings.put("/nobackup2/mouselight/", "//fxt/nobackup2/mouselight/"); // Windows
        sWinPrefixMappings.put("/groups/mousebrainmicro/mousebrainmicro/", "//dm11/mousebrainmicro/");
        sWinPrefixMappings.put("/groups/mousebrainmicro/mousebrainmicro/from_tier2", "//dm11/mousebrainmicro/from_tier2");
        sWinPrefixMappings.put("/tier2/mousebrainmicro/mousebrainmicro/", "//tier2/mousebrainmicro/mousebrainmicro/");
        sWinPrefixMappings.put("/nrs/mltest/", "//nrs/mltest/");
        sWinPrefixMappings.put("/nrs/mouselight/", "//nrs/mouselight/");
        sWinPrefixMappings.put("/groups/jacs/", "//dm11/jacs/");

        //prefixMappings.put("/tier2/", "//tier2/"); // Windows
        //prefixMappings.put("/groups/mousebrainmicro/mousebrainmicro/", "//dm11/mousebrainmicro/"); // Windows

        sMacPrefixMappings.put("/nobackup/mousebrainmicro/", "/Volumes/nobackup/mousebrainmicro/"); // Mac
        sMacPrefixMappings.put("/nobackup2/mouselight/", "/Volumes/nobackup2/mouselight/"); // Mac
        sMacPrefixMappings.put("/groups/mousebrainmicro/mousebrainmicro/", "/Volumes/mousebrainmicro/");
        sMacPrefixMappings.put("/groups/mousebrainmicro/mousebrainmicro/from_tier2", "/Volumes/mousebrainmicro/from_tier2");
        sMacPrefixMappings.put("/tier2/mousebrainmicro/mousebrainmicro/", "/Volumes/mousebrainmicro/mousebrainmicro/");
        sMacPrefixMappings.put("/nrs/mltest/", "/Volumes/mltest/");
        sMacPrefixMappings.put("/nrs/mouselight/", "/Volumes/mouselight/");
        sMacPrefixMappings.put("/groups/jacs/", "/Volumes/jacs/");
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
