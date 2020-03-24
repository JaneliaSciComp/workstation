
package org.janelia.workstation.core.util;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2006
 * Time: 1:44:26 PM
 */
public class SystemCall {

    private Logger logger;

    public static final String UNIX_SHELL = "sh";
    public static final String UNIX_SHELL_FLAG = "-c";
    // Different flavors of Windows will need different shell and flag information
    public static final String WIN_SHELL = "cmd.exe";
    public static final String WIN_SHELL_FLAG = "/c";

    public static final String SCRATCH_DIR_PROP = "SystemCall.ScratchDir";
    public static final String SHELL_PATH_PROP = "SystemCall.ShellPath";
    public static final String STREAM_DIRECTOR_PROP = "SystemCall.StreamDirector";

    private static final String DEFAULT_SCRATCH_DIR = "/scratch/jboss/tmp_exec";
    private static final String DEFAULT_SHELL_PATH = "/bin/bash";
    private static final String DEFAULT_STREAM_DIRECTOR = ">&";

    private File scratchParent = null;
    private File scratchDir = null;
    private String shellPath = null;
    private String streamDirector = null;
    
    private StringBuffer stderr;
    private StringBuffer stdout;

    private boolean deleteExecTmpFile=true;
    
    private Random random = new Random();

    public SystemCall() {
    }
    
    public SystemCall(Logger logger) {
        this.logger = logger;
    }
    
    public SystemCall(Logger logger, StringBuffer stdout, StringBuffer stderr) {
        this.logger = logger;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public SystemCall(StringBuffer stdout, StringBuffer stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
    }
    
    public SystemCall(Properties props, File scratchParentDir, Logger logger) {
        this.logger = logger;
        configure(props, scratchParentDir);
    }

    public void setDeleteExecTmpFile(boolean deleteExecTmpFile) {
        this.deleteExecTmpFile=deleteExecTmpFile;
    }

    protected void configure(Properties props, File scratchParentDir) {
        if (props != null) {
            String scratchDirParentName = props.getProperty(SCRATCH_DIR_PROP);
            if (scratchDirParentName != null) {
                scratchParent = new File(scratchDirParentName);
            }
            else {
                scratchParent = scratchParentDir;
            }
            String shellPathString = props.getProperty(SHELL_PATH_PROP);
            if (shellPathString != null) shellPath = shellPathString;
            String streamDirectorString = props.getProperty(STREAM_DIRECTOR_PROP);
            if (streamDirectorString != null) streamDirector = streamDirectorString;
        }
        if (scratchParent == null) {
            scratchParent = scratchParentDir;
        }
        if (scratchParent == null) {
            scratchParent = new File(DEFAULT_SCRATCH_DIR);
        }
        if (shellPath == null) shellPath = DEFAULT_SHELL_PATH;
        if (streamDirector == null) streamDirector = DEFAULT_STREAM_DIRECTOR;
        if (!scratchParent.exists()) {
            if (!scratchParent.mkdirs()) {
                throw new RuntimeException("Could not create " + scratchParent.getAbsolutePath());
            }
        }
        if (!scratchParent.canWrite()) {
            throw new RuntimeException("Directory " + scratchParent.getAbsolutePath() + " can not be written to");
        }
        if (scratchDir == null) {
            this.scratchDir = new File(scratchParent, getNextIDString());
        }
        if (logger!=null) logger.info("Creating scratchDir=" + scratchDir.getAbsolutePath());
        if (!this.scratchDir.exists()) {
            if (!scratchDir.mkdirs()) {
                throw new RuntimeException("Could not create scratch directory " + this.scratchDir.getAbsolutePath());
            }
            try {
                Thread.sleep(1000);
            }
            catch (Exception ex) {/*Do nothing*/}
            if (!scratchDir.exists()) {
                throw new RuntimeException("Could not verify new scratch directory=" + this.scratchDir.getAbsolutePath());
            }
            else {
            	if (logger!=null) logger.info("New scratch directory verified=" + scratchDir.getAbsolutePath());
            }

        }
    }

    public int execute(String call, boolean captureOutput) throws IOException, InterruptedException {
        if (scratchDir.exists()) {
//            if (logger!=null) logger.info("SystemCall execute verified scratchDir="+scratchDir.getAbsolutePath());
        }
        else {
            throw new IOException("Could not verify scratchDir=" + scratchDir.getAbsolutePath());
        }
        File tmpFile;
        if (shellPath.contains("cmd")) {
            tmpFile = new File(scratchDir, "system_exec_" + getNextIDString() + ".bat");
        } else {
            tmpFile = new File(scratchDir, "system_exec_" + getNextIDString());
        }
        File outFile = new File(tmpFile.getAbsolutePath() + ".out");
        FileWriter writer = new FileWriter(tmpFile);
        try {
            if (captureOutput) {
                writer.write(call + " " + streamDirector + " " + outFile.getAbsolutePath());
            }
            else {
                writer.write(call);
            }
        }
        finally {
            writer.close();
        }
        boolean notExists;
        boolean notSize = true;
        int tries = 0;
        do {
            tries++;
            if (tries > 20)
                throw new RuntimeException("Execution file " + tmpFile.getAbsolutePath() + " was not written by operation system in time");
            Thread.sleep(100); // sleep for 100 miliseconds
            notExists = !(tmpFile.exists());
            if (!notExists) notSize = !(tmpFile.length() > 0);
        }
        while (notExists || notSize);
        if (tries > 1)
            System.err.println("Note: system required " + tries + " 100msec wait periods for system_exec file");
        String execString = shellPath + " " + tmpFile.getAbsolutePath();
        if (logger!=null) logger.info("SystemCall executing script shell=" + shellPath + " path=" + tmpFile.getAbsolutePath());
        Process process = null;
        int exitVal;
        try {
            process = Runtime.getRuntime().exec(execString);
            exitVal = Integer.MIN_VALUE;
            while (exitVal == Integer.MIN_VALUE) {
                try {
                    exitVal = process.waitFor();
                }
                catch (InterruptedException ex) {
                    System.err.println("SystemCall.execute() : intercepted InterruptedException. Continuing...");
                }
            }
        }
        finally {
            if (null != process) {
                closeStream(process.getOutputStream());
                closeStream(process.getInputStream());
                closeStream(process.getErrorStream());
                process.destroy(); // force immediate release of resources back to OS
            }
        }
        if (exitVal == 0 && deleteExecTmpFile) {
            boolean tmpDelSuccess = tmpFile.delete();
            boolean tmpOutSuccess = outFile.delete();
            if (!tmpDelSuccess) {
                System.err.println("SystemCall unable to delete temp file: " + tmpFile.getAbsolutePath());
            }
            if (!tmpOutSuccess) {
                System.err.println("SystemCall unable to delete output file: " + outFile.getAbsolutePath());
            }
            // scratchDir.delete(); We do not want to delete scratchDir because this could be reused by class - use cleanup()
        }
        return exitVal;
    }

    protected String getNextIDString() {
        return "" + new Date().getTime() + "_" + Math.abs(random.nextLong());
    }

    /**
     * This method is used when the developer wants to run a command line, verbatim.
     * OS-specific character sequences on the command line must be processed specially, and this method
     * is intended to take that into account. Runtime.exec() cannot handle os streams by itself.
     * This code was found online from a public source.
     *
     * @param desiredCommandLine - line which would be pasted into a shell for execution
     * @param isUnixStyleSystem  - this can be derived and does not need to be passed
     * @return int - the exit value of the command run
     * @throws IOException          - error executing the thread
     * @throws InterruptedException - error used to stop the wait state
     */
    public int emulateCommandLine(String desiredCommandLine, boolean isUnixStyleSystem) throws IOException, InterruptedException {
        return this.emulateCommandLine(desiredCommandLine, isUnixStyleSystem, null, null, 0);
    }

    public int emulateCommandLine(String desiredCommandLine, boolean isUnixStyleSystem, int timeoutSeconds) throws IOException, InterruptedException {
        return this.emulateCommandLine(desiredCommandLine, isUnixStyleSystem, null, null, timeoutSeconds);
    }

    public int emulateCommandLine(String desiredCommandLine, boolean isUnixStyleSystem, String[] envVariables, File directory) throws IOException, InterruptedException {
        return this.emulateCommandLine(desiredCommandLine, isUnixStyleSystem, envVariables, directory, 0);
    }

    public int emulateCommandLine(String desiredCommandLine, boolean isUnixStyleSystem, String[] envVariables, File directory, int timeoutSeconds) throws IOException, InterruptedException {

        if (logger != null && logger.isDebugEnabled()) {
            logger.debug("Executing: " + desiredCommandLine);
        }
        
        String[] args = null;
    	args = new String[]{"", "", ""};
        if (isUnixStyleSystem) {
            args[0] = UNIX_SHELL;
            args[1] = UNIX_SHELL_FLAG;
        }
        else {
            args[0] = WIN_SHELL;
            args[1] = WIN_SHELL_FLAG;
        }
        args[2] = desiredCommandLine;
        
    	return this.emulateCommandLine(args, envVariables, directory, timeoutSeconds);
    }

    /**
     * This method is used when the developer wants to run a command line, verbatim.
     * OS-specific character sequences on the command line must be processed specially, and this method
     * is intended to take that into account. Runtime.exec() cannot handle os streams by itself.
     * This code was found online from a public source.
     *
     * @param envVariables       - environment variables which can be overridden
     * @param directory          - directory to run the comand from
     * @return int - the exit value of the command run
     * @throws IOException          - error executing the thread
     * @throws InterruptedException - error used to stop the wait state
     */
    public int emulateCommandLine(String[] args, String[] envVariables, File directory, int timeoutSeconds) throws IOException, InterruptedException {
        
        Process proc = null;
        StreamGobbler errorGobbler, outputGobbler;
        int exitVal = -1;
        try {
            Runtime rt = Runtime.getRuntime();
            proc = rt.exec(args, envVariables, directory);
            // any error message?
            errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");

            // any output?
            outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT");

            // kick them off
            errorGobbler.start();
            outputGobbler.start();

            // any error??? make sure execution produces a real exit value
            if (timeoutSeconds==0) {
                exitVal = proc.waitFor();
            } else {
                Date startTime=new Date();
                boolean finishOnTime=false;
                while(!finishOnTime && (new Date().getTime() - startTime.getTime()) < timeoutSeconds*1000) {
                    Thread.sleep(1000);
                    try {
                        exitVal=proc.exitValue();
                        finishOnTime=true;
                    } catch (IllegalThreadStateException e) {}
                }
                if (!finishOnTime) {
                	if (logger!=null) logger.error("Process exceeded maximum timeout of " + timeoutSeconds + " seconds");
                    proc.destroy();
                    exitVal=1;
                }
            }
            if (logger != null && logger.isDebugEnabled()) {
                logger.debug("ExitValue: " + exitVal);
            }
        }
        finally {
            if (null != proc) {
                closeStream(proc.getOutputStream());
                closeStream(proc.getInputStream());
                closeStream(proc.getErrorStream());
                proc.destroy(); // force immediate release of resources back to OS
            }
        }
        return exitVal;

    }

    private void closeStream(Closeable c) {
        if (c != null) {
            try {
                c.close();
            }
            catch (IOException e) {
                // ignored
            }
        }
    }

    class StreamGobbler extends Thread {
        InputStream is;
        String type;

        StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        public void run() {
            BufferedReader br = null;
            try {
                InputStreamReader isr = new InputStreamReader(is);
                br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    if (logger != null && logger.isDebugEnabled()) {
                    	if ("OUTPUT".equals(type) && stdout != null) {
                    		stdout.append(line).append("\n");
                    	}
                    	else if ("ERROR".equals(type) && stderr != null) {
                    		stderr.append(line).append("\n");
                    	}
                        logger.debug(type + ">" + line);
                    }
                }
            }
            catch (IOException ioe) {
                // ioe.printStackTrace(); this was causing occasional harmless output
                if (!ioe.getMessage().equalsIgnoreCase("Stream closed")) {
                	if (logger!=null) logger.error("IOException: " + ioe.getMessage() + " (not stream closed) in SystemCall StreamGobbler type=" + type);
                }
            }
            finally {
                if (null != br) {
                    try {
                        br.close();
                    }
                    catch (IOException e) {
                    	if (logger!=null) logger.error("Error trying to close the SystemCall buffered stream reader. " + e.getMessage());
                    }
                }
            }
        }

    }

    public void cleanup() {
        if (scratchDir != null) {
            cleanDirectory(scratchDir);
            boolean scratchDirSuccess = scratchDir.delete();
            if (!scratchDirSuccess) {
                System.err.println("SystemCall unable to delete scratch dir: " + scratchDir.getAbsolutePath());
            }
        }
    }

    public File getScratchDir() {
        return scratchDir;
    }


    /**
     * Deletes all the directories and files under directoryPath but leaves
     * directoryPath alone
     *
     * @param directoryPath
     */
    private static void cleanDirectory(String directoryPath) {
        File dir = new File(directoryPath);
        cleanDirectory(dir);
    }

    /**
     * Deletes all the directories and files under dir but leaves
     * directoryPath alone
     *
     * @param dir
     */
    private static void cleanDirectory(File dir) {
        File[] dirFiles = dir.listFiles();
        if (dirFiles != null) {
            for (File dirFile : dirFiles) {
                if (dirFile.isFile()) {
                    dirFile.delete();
                }
                else if (dirFile.isDirectory()) {
                    cleanDirectory(dirFile);
                }
            }
        }
    }
}
