package org.janelia.workstation.webdav;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.janelia.workstation.webdav.exception.FileNotFoundException;
import org.janelia.workstation.webdav.propfind.Multistatus;
import org.janelia.workstation.webdav.propfind.Prop;
import org.janelia.workstation.webdav.propfind.PropfindResponse;
import org.janelia.workstation.webdav.propfind.Propstat;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.List;

/**
 * Created by schauderd on 6/26/15.
 */
public class BlockFileShare extends FileShare {
    String baseURL;

    @Override
    public StreamingOutput getFile(String qualifiedFilename) throws FileNotFoundException {
        final String filename = qualifiedFilename;
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                Files.copy(Paths.get(filename), output);
            }
        };
    }

    private PropfindResponse generatePropMetadata(java.nio.file.Path file, UriInfo uriInfo) {
        PropfindResponse fileMeta = new PropfindResponse();
        try {
            Propstat propstat = new Propstat();
            Prop prop = new Prop();
            prop.setCreationDate(Files.getAttribute(file, "creationTime").toString());
            prop.setGetContentType(Files.probeContentType(file));
            prop.setGetContentLength(Long.toString(Files.size(file)));
            prop.setGetLastModified(Files.getLastModifiedTime(file).toString());
            String baseURI = uriInfo.getBaseUri().toString();
            fileMeta.setHref("/Webdav" + file.toString());
            if (Files.isDirectory(file)) {
                prop.setResourceType("collection");
                fileMeta.setHref(fileMeta.getHref() + "/");
            }
            propstat.setProp(prop);
            propstat.setStatus("HTTP/1.1 200 OK");
            fileMeta.setPropstat(propstat);
        } catch (IOException e) {
            e.printStackTrace();
            // TO DO throw specific error
        }
        return fileMeta;
    }

    private void discoverFiles(Multistatus container, java.nio.file.Path file,
                               int depth, int discoveryLevel, UriInfo uriInfo) {
        if (depth<discoveryLevel) {
            if (Files.isDirectory(file)) {
                depth++;
                try (DirectoryStream<java.nio.file.Path> directoryStream = Files.newDirectoryStream(file)) {
                    for (java.nio.file.Path subpath : directoryStream) {
                        discoverFiles(container, subpath, depth, discoveryLevel, uriInfo);
                    }
                }
                catch (IOException ex) {
                    // handle issues with reading subdirectory metadata
                    ex.printStackTrace();
                }
            }
        }
        container.getResponse().add(generatePropMetadata(file, uriInfo));
    }

    @Override
    public String propFind(UriInfo uriInfo, HttpHeaders headers) throws FileNotFoundException {
        String filepath = "/" + uriInfo.getPath();

        // create Multistatus top level
        Multistatus propfindContainer = new Multistatus();

        java.nio.file.Path fileHandle = Paths.get(filepath);
        if (Files.exists(fileHandle)) {
            // check DEPTH header to check whether to get subdirectory information
            int discoveryLevel = 0;
            List<String> depth = headers.getRequestHeader("Depth");
            if (depth != null && depth.size() > 0) {
                String depthValue = depth.get(0).trim();
                if (depthValue.toLowerCase().equals("infinity")) {
                    discoveryLevel = 20; // prevents insanity
                } else {
                    discoveryLevel = Integer.parseInt(depthValue);
                }
            }
            discoverFiles(propfindContainer, fileHandle, 0, discoveryLevel, uriInfo);
        } else {
            throw new FileNotFoundException("File does not exist");
        }

        ObjectMapper xmlMapper = new XmlMapper();
        String xml = null;
        try {
            xml = xmlMapper.writeValueAsString(propfindContainer);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            // TO DO throw specific error
        }
        return xml;

    }
}
