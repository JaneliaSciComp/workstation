package org.janelia.workstation.controller.tileimagery;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.rendering.FileBasedRenderedVolumeLocation;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;

public class FileBasedTileLoader extends TileLoader {

    private static final Logger LOG = LoggerFactory.getLogger(FileBasedTileLoader.class);

    /**
     * Create the frame.
     */
    public FileBasedTileLoader() {
    }

        /**
         * given a string containing the canonical Linux path to the data,
         * open the data in the viewer
         *
         * @param sample
         * @return
         */
        @Override
        public boolean loadData(TmSample sample) {
            String canonicalLinuxPath = sample.getLargeVolumeOctreeFilepath();
            LOG.info("loadData from file: {}", canonicalLinuxPath);

            // on Linux, this just works, as the input path is the Linux path;
            //  for Mac and Windows, we need to guess the mount point of the
            //  shared disk and alter the path accordingly

            File testFile = new File(canonicalLinuxPath); // Maybe it just works...
            if (!testFile.exists()) {
                // must be on Mac or Windows; but first, which of the
                //  possible Linux prefixes are we looking at?
                // OK to compare as strings, because we know the
                //  input path is Linux
                String [] mbmPrefixes = {
                        "/groups/mousebrainmicro/mousebrainmicro/",
                        "/groups/mousebrainmicro/mousebrainmicro/from_tier2",
                        "/nobackup/mousebrainmicro/",
                        "/nobackup2/mouselight",
                        "/tier2/mousebrainmicro/mousebrainmicro/",
                        "/nrs/mouselight",
                        "/nrs/mltest/",
                        "/groups/jacs",
                        "/groups/dickson/dicksonlab",
                };
                Path linuxPrefix = null;
                for (String testPrefix: mbmPrefixes) {
                    if (canonicalLinuxPath.startsWith(testPrefix)) {
                        linuxPrefix = new File(testPrefix).toPath();
                    }
                }
                int pathPrefixDepth = 0;
                if (linuxPrefix != null) // Avoid NPE
                    pathPrefixDepth = linuxPrefix.getNameCount();
                Path partialPath = testFile.toPath().subpath(pathPrefixDepth, testFile.toPath().getNameCount());
                // System.out.println("linuxPrefix = " + linuxPrefix);
                // System.out.println("linuxPrefix = " + linuxPrefix);
                // System.out.println("partialPath = " + partialPath);

                // now we just need to assemble pieces: a root plus a mount name
                //  plus the partial path we just extracted; the root is OS dependent:
                String osName = System.getProperty("os.name").toLowerCase();
                List<Path> prefixesToTry = new Vector<>();
                if (osName.contains("win")) {
                    for (File fileRoot : File.listRoots()) {
                        prefixesToTry.add(fileRoot.toPath());
                    }
                } else if (osName.contains("os x")) {
                    // for Mac, it's a lot simpler:
                    prefixesToTry.add(new File("/Volumes").toPath());
                }
                // System.out.println("prefixes to try: " + prefixesToTry);

                // the last, middle piece can be nothing or one of these
                //  mounts names (nothing = enclosing dir is mounted directly
                String [] mountNames = {"", "mousebrainmicro", "mouselight",
                        "nobackup/mousebrainmicro", "nobackup2/mouselight",
                        "nobackup/mousebrainmicro/from_tier2", "mousebrainmicro/from_tier2",
                        "mousebrainmicro/mousebrainmicro", "nrs/mouselight",
                        "mltest", "dicksonlab"};

                boolean found = false;
                for (Path prefix: prefixesToTry) {
                    if (found) {
                        break;
                    }
                    for (String mount: mountNames) {
                        if (mount.length() > 0) {
                            testFile = prefix.resolve(new File(mount).toPath()).resolve(partialPath).toFile();
                        } else {
                            testFile = prefix.resolve(partialPath).toFile();
                        }
                        // System.out.println("trying " + testFile);
                        if (testFile.exists()) {
                            // System.out.println("file exists: " + testFile);
                            found = true;
                            break;
                        }
                    }
                }
            } // end if Mac or Windows
            // by now, if we ain't got the path, we ain't got the path
            if (!testFile.exists()) {
               // JOptionPane.showMessageDialog(this.getParent(),
                //        "Error opening Linux sample path " + canonicalLinuxPath +
                //                " \nIs the file share mounted?",
                 //       "Folder does not exist.",
                  //      JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // for Mac/Win, must be a dialog (symlinks work on Linux,
            //  but don't work when mounted on Mac/Win
            if (System.getProperty("os.name").contains("Mac OS X") ||
                    System.getProperty("os.name").contains("Windows")) {
                if (!testFile.isDirectory()) {
                   // JOptionPane.showMessageDialog(this.getParent(),
                     //       "Error opening Linux sample path " + canonicalLinuxPath +
                  //                  " \nAre you sure this is a dialog?",
                  //          "Not a dialog?",
                  //          JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            try {
                url = testFile.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
            return true;
        }

        @Override
        public RenderedVolumeLocation getRenderedVolumeLocation(TmSample tmSample) {
            return new FileBasedRenderedVolumeLocation(Paths.get(tmSample.getLargeVolumeOctreeFilepath()), p -> Paths.get(OsFilePathRemapper.remapLinuxPath(p.toString())));
        }
}
