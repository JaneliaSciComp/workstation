package org.janelia.it.workstation.browser.filecache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TestFileUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TestFileUtils.class);
    private static final String TIMESTAMP_PATTERN = "yyyyMMdd-HHmmssSSS";
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat(TIMESTAMP_PATTERN);
    private static int fileCount = 0;

    /**
     * @return a new timestamp directory name based on the current time.
     */
    synchronized static String buildTimestampName() {
        fileCount++;
        return TIMESTAMP_FORMAT.format(new Date()) + "-" + fileCount;
    }

    /**
     * Utility to create a uniquely named test file with the specified length.
     *
     * @param  numberOfKilobytes  size of file in kilobytes.
     *
     * @return new file of specified size.
     *
     * @throws IOException
     *   if the file cannot be created.
     */
    static File createFile(File parentDirectory, long numberOfKilobytes) throws IOException {
        final long numberOfBytes = numberOfKilobytes * 1024;
        final String name = "test-" + buildTimestampName() + ".txt";
        File file = new File(parentDirectory, name);

        final int lineLength = name.length() + 1;
        FileWriter out = null;
        try {
            out = new FileWriter(file);
            long i = lineLength;
            for (; i < numberOfBytes; i += lineLength) {
                out.write(name);
                out.write('\n');
            }
            long remainder = numberOfBytes % lineLength;
            for (i = 1; i < remainder; i++) {
                out.write('.');
            }
            if (remainder > 0) {
                out.write('\n');
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }

        LOG.info("createFile: created " + numberOfKilobytes +
                "Kb file " + file.getAbsolutePath());

        return file;
    }

    /**
     * Utility to delete the specified file.
     *
     * @param  file  file to delete.
     */
    static void deleteFile(File file) {
        if (file != null && file.exists()) {
            if (file.delete()) {
                LOG.info("deleteFile: deleted " + file.getAbsolutePath());
            } else {
                LOG.info("deleteFile: failed to delete " + file.getAbsolutePath());
            }
        }
    }

}
