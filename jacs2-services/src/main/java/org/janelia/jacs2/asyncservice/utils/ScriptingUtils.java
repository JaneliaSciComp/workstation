package org.janelia.jacs2.asyncservice.utils;

import java.util.Random;

public class ScriptingUtils {

    private static final int DEFAULT_RETRIES = 10;

    public static String echoHostname() {
        return "echo \"Running on \"`hostname`\n";
    }

    /**
     * Generate a script that starts screen capture process based on X11 - xvfb
     * @param displayPort
     * @param resolution
     * @return
     */
    public static String startScreenCapture(String displayPort, String resolution) {
        StringBuilder cmdBuilder = new StringBuilder();

        // Skip ports that are currently in use, or "locked"
        cmdBuilder.append(echoHostname());
        cmdBuilder.append("echo \"Finding a port for Xvfb, starting at "+displayPort+"...\"\n");
        cmdBuilder.append(
                "PORT=" + displayPort + " " +
                "COUNTER=0 RETRIES=" + DEFAULT_RETRIES+"\n");

        // Clean up Xvfb on any exit
        cmdBuilder.append("function cleanXvfb {\n");
        cmdBuilder.append("    kill $MYPID\n");
        cmdBuilder.append("    rm -f /tmp/.X${PORT}-lock\n");
        cmdBuilder.append("    rm -f /tmp/.X11-unix/X${PORT}\n");
        cmdBuilder.append("    echo \"Cleaned up Xvfb\"\n");
        cmdBuilder.append("}\n");
        cmdBuilder.append("trap cleanXvfb EXIT\n");

        cmdBuilder.append("while [ \"$COUNTER\" -lt \"$RETRIES\" ]; do\n");
        cmdBuilder.append("    while (test -f \"/tmp/.X${PORT}-lock\") || (test -f \"/tmp/.X11-unix/X${PORT}\") || (netstat -atwn | grep \"^.*:${PORT}.*:\\*\\s*LISTEN\\s*$\")\n");
        cmdBuilder.append("        do PORT=$(( ${PORT} + 1 ))\n");
        cmdBuilder.append("    done\n");
        cmdBuilder.append("    echo \"Found the first free port: $PORT\"\n");

        // Run Xvfb (virtual framebuffer) on the chosen port
        cmdBuilder.append("    /usr/bin/Xvfb :${PORT} -screen 0 " + resolution + " -fp /usr/share/X11/fonts/misc > Xvfb.${PORT}.log 2>&1 &\n");
        cmdBuilder.append("    echo \"Started Xvfb on port $PORT\"\n");

        // Save the PID so that we can kill it when we're done
        cmdBuilder.append("    MYPID=$!\n");
        cmdBuilder.append("    export DISPLAY=\"localhost:${PORT}.0\"\n");

        // Wait some time and check to make sure Xvfb is actually running, and retry if not.
        cmdBuilder.append("    sleep 3\n");
        cmdBuilder.append("    if kill -0 $MYPID >/dev/null 2>&1; then\n");
        cmdBuilder.append("        echo \"Xvfb is running as $MYPID\"\n");
        cmdBuilder.append("        break\n");
        cmdBuilder.append("    else\n");
        cmdBuilder.append("        echo \"Xvfb died immediately, trying again...\"\n");
        cmdBuilder.append("        cleanXvfb\n");
        cmdBuilder.append("        PORT=$(( ${PORT} + 1 ))\n");
        cmdBuilder.append("    fi\n");

        cmdBuilder.append("    COUNTER=\"$(( $COUNTER + 1 ))\"\n");
        cmdBuilder.append("done\n\n");

        return cmdBuilder.toString();
    }


    /**
     * Take a screenshot with quadratically increase latency.
     * For example, when secs=5, the time between screenshots works out to the quadratic sequence 2.5t^2+2.5t
     * which means that screenshots are taken at 5 seconds, 15 seconds, 30 seconds, 50 seconds, etc.
     * @param outputDir
     * @param portVarName
     * @param pidVarName
     * @param secs
     * @param maxSecs
     * @return
     */
    public static String screenCaptureLoop(String outputDir, String portVarName, String pidVarName, int secs, int maxSecs) {
        StringBuilder script = new StringBuilder();

        script.append("XVFB_SCREENSHOT_DIR=\"").append(outputDir).append("\"\n");
        script.append("mkdir -p $XVFB_SCREENSHOT_DIR\n");
        script.append("ssinc=" + secs + "\n"); // increment in how often to take a screenshot
        script.append("freq=$ssinc\n"); // how often to take a screenshot
        script.append("inc=5\n"); // how often to wake up and check if the process is still running
        script.append("t=0\n"); // time counter
        script.append("nt=$freq\n");

        script.append("while kill -0 $" + pidVarName + " 2> /dev/null; do\n");
        script.append("  sleep $inc\n");
        script.append("  t=$((t+inc))\n");

        // Take a screenshot with an incrementing delay
        script.append("  if [ \"$t\" -eq \"$nt\" ]; then\n");
        script.append("    freq=$((freq+ssinc))\n");
        script.append("    nt=$((t+freq))\n");
        script.append("    DISPLAY=:$" + portVarName + " import -window root $XVFB_SCREENSHOT_DIR/screenshot_$t.png\n");
        script.append("  fi\n");

        // Don't allow it to run for more than a set amount of time, in case it hangs due to requiring user input
        script.append("  if [ \"$t\" -gt "+maxSecs+" ]; then\n");
        script.append("    echo \"Killing Xvfb session which has been running for over "+maxSecs+" seconds\"\n");
        script.append("    kill -9 $" + pidVarName + " 2> /dev/null\n");
        script.append("  fi\n");

        script.append("done\n");

        return script.toString();
    }

    public static int getRandomPort(int startPort) {
        return ((int)(100.0 * Math.random()) + startPort);
    }

}
