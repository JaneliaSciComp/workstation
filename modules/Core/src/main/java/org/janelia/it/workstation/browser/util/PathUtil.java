package org.janelia.it.workstation.browser.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtil {

    /**
     * Returns a standard path (Unix-style) on any platform.
     * @param path
     * @return
     */
    public static final String getStandardPath(Path path) {
        return getStandardPath(path.toString());
    }
    
    public static final String getStandardPath(String path) {
        return path.replaceAll("\\\\", "/").replaceFirst("^[A-Z]:", "");
    }
 
    public static void main(String[] args) {
        System.out.println(getStandardPath(Paths.get("C:\\Program Files\\Test")));
        System.out.println(getStandardPath(Paths.get("/long/unix/path/with/many/components/file.png")));
    }
}
