package org.janelia.workstation.jfs;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.janelia.workstation.jfs.exception.FileNotFoundException;
import org.janelia.workstation.jfs.exception.FileUploadException;
import org.janelia.workstation.jfs.exception.PermissionsFailureException;
import org.janelia.workstation.jfs.fileshare.FileShare;
import org.janelia.workstation.jfs.security.Permission;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.janelia.workstation.jfs.CommandLineApplication.Command.*;

public class CommandLineApplication {
    boolean checksum = false;
    boolean compress = false;
    boolean validate = false;
    File diskFile;
    String key;
    String fileservice;
    Command command;

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public boolean isChecksum() {
        return checksum;
    }

    public void setChecksum(boolean checksum) {
        this.checksum = checksum;
    }

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    public boolean isValidate() {
        return validate;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public File getDiskFile() {
        return diskFile;
    }

    public void setDiskFile(File diskFile) {
        this.diskFile = diskFile;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getFileservice() {
        return fileservice;
    }

    public void setFileservice(String fileservice) {
        this.fileservice = fileservice;
    }

    public enum Command {
        read, write, delete;
    }

    public CommandLineApplication() {

    }

    public static void main(String[] args) {
        CommandLineApplication cl = new CommandLineApplication();
        String[] fileList = new String[0];
        for (int i=0; i<args.length; i++) {
            if (args[i].startsWith("-fileservice")) {
                String service = args[i+1];
                if (service==null) {
                    System.out.println ("fileservice needs an argument");
                    System.exit(1);
                }
                cl.setFileservice(service);
            } else if (args[i].startsWith("-file")) {
                String file = args[i+1];
                try {
                    File diskFile = new File(file);
                    if (diskFile!=null) {
                        cl.setDiskFile(diskFile);
                    }
                } catch (Exception e) {
                    System.out.println ("Problem creating/reading from disk file");
                    System.exit(1);
                }
            } else if (args[i].startsWith("-checksum")) {
                cl.setChecksum(true);
            } else if (args[i].startsWith("-compress")) {
                cl.setCompress(true);
            } else if (args[i].startsWith("-validate")) {
                cl.setValidate(true);
            } else if (args[i].startsWith("-key")) {
                String key = args[i+1];
                if (key==null) {
                    System.out.println ("key needs an argument");
                    System.exit(1);
                }
                cl.setKey(key);
            }  else if (args[i].startsWith("-command")) {
                String command = args[i+1];
                if (command==null) {
                    System.out.println ("command needs an argument");
                    System.exit(1);
                }
                cl.setCommand(valueOf(command));
            }
        }

        if (cl.getCommand()==null || cl.getFileservice()==null || cl.getKey()==null) {
            System.out.println ("Both Key and Fileservice are required parameters");
            System.exit(1);
        } else {
            if (cl.getCommand()!= delete && cl.getDiskFile()==null) {
                System.out.println ("File is required parameter for read/write operations");
                System.exit(1);
            }
        }


        // read configuration file
        ServicesConfiguration sc = new ServicesConfiguration();
        sc.contextInitialized(null);

        int status = 0;
        try {
            status = cl.processCommand();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(status);

    }

    public int processCommand() {
        // find the Mapping
        FileShare mapping = Common.mapResource(this.getFileservice());
        if (mapping==null) {
            System.out.println ("This fileservice was not found in the directory of services.");
            return 1;
        }

        switch (command) {
            case read:
                if (!mapping.getPermissions().contains(Permission.READ)) {
                    System.out.println("not allowed to read from this fileservice");
                    return 1;
                }
                try {
                    Response.Status status = mapping.readFileToStream(this.getFileservice() + "/" + this.getKey(), new FileOutputStream(this.getDiskFile()), true);
                    if (status != Response.Status.CREATED) {
                        return 1;
                    }
                    System.out.println("Successfully got file " + getKey() + " and wrote to " + this.getDiskFile());
                }
                catch (java.io.FileNotFoundException e) {
                    e.printStackTrace();
                    System.out.println("Unable to create local file to write to.");
                    return 1;
                }
                break;
            case write:
                if (!mapping.getPermissions().contains(Permission.WRITE)) {
                    System.out.println("not allowed to write from this fileservice");
                    return 1;
                }
                try {
                    mapping.putFile(null, new FileInputStream(this.getDiskFile()), this.getFileservice() + "/" + this.getKey(), isChecksum(), true);
                    System.out.println("Successfully wrote file " + getKey() + " to fileservice " + getFileservice());
                }
                catch (FileUploadException e) {
                    e.printStackTrace();
                    System.out.println("Internal error trying to upload the file into this fileservice");
                    return 1;
                }
                catch (java.io.FileNotFoundException e) {
                    e.printStackTrace();
                    System.out.println("Unable to find local file to read from.");
                    return 1;
                }
                break;
            case delete:
                if (!mapping.getPermissions().contains(Permission.DELETE)) {
                    System.out.println("not allowed to delete from this fileservice");
                    return 1;
                }
                try {
                    mapping.deleteFile(this.getFileservice() + "/" + this.getKey(), true);
                    System.out.println("Successfully deleted file " + getKey() + " from fileservice " + getFileservice());
                }
                catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Problem deleting file from this fileservice");
                    return 1;
                }
                break;
        }
        return 0;
    }


}
