package org.janelia.workstation.swc;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Vector;

/**
 * this static class provides the default directory for import/export swc, and it
 * records the last used import/export directory so the user can be brought there next time
 */
public final class SWCDirectorySource {

    private static File swcDirectory;

    private SWCDirectorySource() {
    }

    public static File getSwcDirectory() {
        if (swcDirectory == null) {
            swcDirectory = getDefaultSwcDirectory();
        }
        return swcDirectory;
    }

    private static File getDefaultSwcDirectory() {
        // swc imports and exports are done to one particular place in the
        //  file system right now, so default to that location; fall back to
        //  user's home dir if it's not available

        // do a brute force search, like we do for finding tile path (see QuadViewUi.loadFile());
        //  fortunately, we're checking fewer locations
        String osName = System.getProperty("os.name").toLowerCase();
        List<Path> prefixesToTry = new Vector<>();
        if (osName.contains("win")) {
            for (File fileRoot : File.listRoots()) {
                prefixesToTry.add(fileRoot.toPath());
            }
        } else if (osName.contains("os x")) {
            // for Mac, it's simpler:
            prefixesToTry.add(new File("/Volumes").toPath());
        } else if (osName.contains("lin")) {
            // Linux
            prefixesToTry.add(new File("/groups/mousebrainmicro").toPath());
        }
        boolean found = false;
        // java and its "may not have been initialized" errors...
        File testFile = new File(System.getProperty("user.home"));
        for (Path prefix: prefixesToTry) {
            // test with and without the first part
            testFile = prefix.resolve("shared_tracing/Finished_Neurons").toFile();
            if (testFile.exists()) {
                found = true;
                break;
            }
            testFile = prefix.resolve("mousebrainmicro/shared_tracing/Finished_Neurons").toFile();
            if (testFile.exists()) {
                found = true;
                break;
            }
        }
        if (!found) {
            testFile = new File(System.getProperty("user.home"));
        }
        return testFile;
    }

    public static void setSwcDirectory(File directory) {
        swcDirectory = directory;
    }

}