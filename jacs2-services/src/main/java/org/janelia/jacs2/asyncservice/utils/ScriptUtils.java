package org.janelia.jacs2.asyncservice.utils;

import java.io.IOException;

public class ScriptUtils {

    public static void createTempDir(String cleanupFunctionName, String tempParentDir, ScriptWriter scriptWriter) throws IOException {
        scriptWriter
                .setVar("TMPDIR", tempParentDir)
                .add("mkdir -p $TMPDIR")
                .setVar("TEMP_DIR", "`mktemp -d`")
                .openFunction(cleanupFunctionName)
                .add("rm -rf $TEMP_DIR")
                .echo("Cleaned up $TEMP_DIR")
                .closeFunction(cleanupFunctionName);

    }

}
