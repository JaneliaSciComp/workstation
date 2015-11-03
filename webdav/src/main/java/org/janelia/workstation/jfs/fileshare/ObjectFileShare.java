package org.janelia.workstation.jfs.fileshare;

import java.io.File;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.janelia.workstation.jfs.mongo.MongoConfiguration;
import org.janelia.workstation.jfs.security.Principal;
import org.janelia.workstation.jfs.ServicesConfiguration;
import org.janelia.workstation.jfs.exception.FileNotFoundException;
import org.janelia.workstation.jfs.exception.FileUploadException;
import org.janelia.workstation.jfs.mongo.JOSObject;
import org.janelia.workstation.jfs.propfind.Multistatus;
import org.janelia.workstation.jfs.propfind.Prop;
import org.janelia.workstation.jfs.propfind.PropfindResponse;
import org.janelia.workstation.jfs.propfind.Propstat;
import org.jongo.MongoCursor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

/**
 * Created by schauderd on 6/26/15.
 * For now, this delegates mostly to JOSS.  At some point, we should probably merge all this functionality into one web-app.
 */
public class ObjectFileShare extends FileShare implements Runnable {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 2;
    private static final long MEGABYTE = 1000*1000;
    private String ring;
    private ScalityProvider scality;
    private MongoConfiguration mongo;
    private boolean syncDelete;

    public ScalityProvider getScality() {
        return scality;
    }

    public void setScality(ScalityProvider scality) {
        this.scality = scality;
    }

    public String getRing() {
        return ring;
    }

    public void setRing(String ring) {
        this.ring = ring;
    }

    public MongoConfiguration getMongo() {
        return mongo;
    }

    public void setMongo(MongoConfiguration mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run() {
        MongoCursor<JOSObject> cursor = mongo.getObjectCollection().find("{deleted:#}",true).as(JOSObject.class);
        List<String> paths = new ArrayList<>();
        for (JOSObject obj : cursor) {
            if (obj.getPath()!=null) {
               paths.add(obj.getPath());
            }
        }

        int c = 0;
        for(String path : paths) {
            if (scality.delete(path, ring)) {
                mongo.getObjectCollection().remove("{path:#}",path);
                c++;
            }
        }

        if (c>0) {
            //log.info("Purged {} objects marked for deletion",c);
        }
    }

    @Override
    public void init() {
        scality = (ScalityProvider)ServicesConfiguration.getProviders().get("scality");

    }

    @Override
    public String propFind(HttpHeaders headers, String path) throws FileNotFoundException, IOException {
        // there is no hierarchical concept in Scality, so always only return existing file info
        String filepath = "/" + path;

        // create Multistatus top level
        Multistatus propfindContainer = new Multistatus();
        PropfindResponse fileMeta = new PropfindResponse();
        propfindContainer.getResponse().add(fileMeta);
        Propstat propstat = new Propstat();
        Prop prop = new Prop();

        final JOSObject obj = getObject(path, false);
        prop.setGetContentLength(obj.getNumBytes().toString());
        propstat.setProp(prop);
        propstat.setStatus("HTTP/1.1 200 OK");
        fileMeta.setPropstat(propstat);
        fileMeta.setHref("/Webdav" + this.getMapping() + filepath);

        ObjectMapper xmlMapper = new XmlMapper();
        String xml = null;
        try {
            xml = xmlMapper.writeValueAsString(propfindContainer);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IOException("Problem parsing out proper meta content");
        }
        return xml;

    }

    @Override
    public StreamingOutput getFile (HttpServletResponse response, String path) throws FileNotFoundException {
        final JOSObject obj = getObject(path, false);

        Map<String,String> headerMap = scality.head(path, ring);
        // for now assume not bzipped
        // final boolean bzipped = decompress==null?obj.isBzipped():decompress;
        final InputStream input = scality.get(path, obj.getNumBytes(), ring);

        StreamingOutput output = new StreamingOutput() {
            public void write(OutputStream output) throws IOException, WebApplicationException {
                CountingOutputStream counted = new CountingOutputStream(output);
                IOUtils.copy(input, counted);
                counted.flush();

                if (counted.getByteCount()!=obj.getNumBytes()) {
                    // add some warning with fast logger
                    //log.warn("Streamed {} bytes, but object has {}",counted.getByteCount(),obj.getNumBytes());
                }
            }
        };

      /* settle on whether headers are important first before restoring this code
        Response.ResponseBuilder rb = Response.ok(output);

       // rb.header(JOSS_NAME, obj.getName());
        //rb.header(JOSS_OWNER, obj.getOwner());
        if (obj.getNumBytes()!=null) {
          //  rb.header(JOSS_SIZE, obj.getNumBytes().toString());
            rb.header(org.apache.http.HttpHeaders.CONTENT_LENGTH, obj.getNumBytes().toString());
        }
        return rb.build();*/
        return output;
    }

    public void deleteFile (String path) throws IOException {
        JOSObject obj = getObject(path, false);

        WriteResult wr = mongo.getObjectCollection().update("{path:#}",path).with("{$set:{deleted:#}}", true);
        if (wr.getN()<1) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (syncDelete) {
            if (scality.delete(path, ring)) {
                mongo.getObjectCollection().remove("{path:#}",path);
            }
        }
        else {
            //log.info("Marked object for deletion: {}", path);
        }
    }

    @Override
    public void putFile(HttpServletRequest request, HttpServletResponse response, InputStream binaryStream, String path) throws FileUploadException, WebApplicationException {
        boolean compress = false;
        try {
            boolean bzipped = compress;

            JOSObject obj = getObject(path, true);
            long contentLength = scality.put(binaryStream, path, getRing());
            if (contentLength==0) {
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }

            if (obj==null) obj = new JOSObject();

            File file = new File(path);
            String name = file.getName();
            String extension = name.substring(name.lastIndexOf('.')+1);
            obj.setName(name);
            obj.setPath(path);
            obj.setParentPath(file.getParent());
            obj.setFileType(extension);
            Principal owner = (Principal)request.getSession().getAttribute("principal");
            if (owner!=null)
                obj.setOwner(owner.getUsername());
            obj.setNumBytes(contentLength);
            obj.setBzipped(bzipped);
            mongo.getObjectCollection().save(obj);
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileUploadException("Problem creating new file in joss");
        }
    }


    @Override
    public Object getInfo (HttpServletResponse response, String path) throws FileNotFoundException {
        final String finalPath = getFormattedPath(path);
        JOSObject obj = mongo.getObjectCollection().findOne("{path:#}",finalPath).as(JOSObject.class);
        return obj;
    }

    @Override
    public StreamingOutput searchFile (HttpServletResponse response, String path) throws FileNotFoundException {
         ScalityProvider provider = (ScalityProvider) ServicesConfiguration.getProviders().get("scality");
         final String name = path;
         return new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                MongoCursor<JOSObject> cursor = mongo.getObjectCollection().find("{name:#}",name).as(JOSObject.class);
                // add Jackson write value
            }
        };
    }

    @Override
    public Map<String,String> generateUsageReports (String store) throws FileNotFoundException {
        List<DBObject> results = mongo.getObjectCollection().aggregate("{\"$group\" : {_id:\"$owner\", totalMb:{$sum:{$divide:[\"$numBytes\","+MEGABYTE + "]}}}}").as(DBObject.class);
        Map<String,String> usage = new HashMap<>();

        DecimalFormat df = new DecimalFormat("0.0000 MB");

        for (DBObject result : results) {
            Double sum = (Double)result.get("totalMb");
            usage.put(result.get("_id").toString(), df.format(sum));
        }

        return usage;
    }

    @Override
    public void registerFile(HttpServletRequest request, String path) throws FileNotFoundException {
        try {
            JOSObject existingObj = getObject(path, true);

            System.out.println ("Registering path " + path + " in scality");
            Map<String,String> headerMap = scality.head(path, ring);

            if (headerMap==null) {
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }

            JOSObject obj = existingObj==null ? new JOSObject() : existingObj;
            File file = new File(path);
            String name = file.getName();
            String extension = name.substring(name.lastIndexOf('.')+1);

            String contentLengthStr = headerMap.get(org.apache.http.HttpHeaders.CONTENT_LENGTH);
            if (contentLengthStr!=null) {
                long contentLength = Long.parseLong(contentLengthStr);
                obj.setNumBytes(contentLength);
            }
            else {
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }

            String checksum = headerMap.get("X-Scal-Usermd");
            if (checksum == null || checksum.trim().length()==0) {
                checksum = generateChecksum(scality.get(path, obj.getNumBytes(), ring), path);
            }

            obj.setChecksum(checksum);
            obj.setName(name);
            obj.setPath(path);
            obj.setParentPath(file.getParent());
            obj.setFileType(extension);
            Principal owner = (Principal)request.getSession().getAttribute("principal");
            if (owner!=null)
                obj.setOwner(owner.getUsername());
            obj.setBzipped(false);

            mongo.getObjectCollection().save(obj);
        } catch (IOException e) {
            e.printStackTrace();
            throw new FileNotFoundException("problem finding file in scality");
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String,String> registerBulkFiles(HttpServletRequest request, String filestore, List<String> objects) throws FileNotFoundException {
        Map<String, String> bulkReport = new HashMap<String, String>();
        for (int i=0; i<objects.size(); i++) {
            try {
                registerFile(request, filestore + "/" + objects.get(i));
                bulkReport.put(objects.get(i), "Successfully added to the file store");
            }
            catch (Exception e) {
                bulkReport.put(objects.get(i), e.getMessage());
            }
        }
        return bulkReport;
    }

    private String getFormattedPath(String path) {
        String formattedPath = path.endsWith("/") ? path.substring(0, path.length()-1) : path;
        return formattedPath;
    }

    private String generateChecksum (InputStream is, String filepath) throws NoSuchAlgorithmException, IOException {
        byte[] buffer = new byte[1024];

        MessageDigest md = MessageDigest.getInstance("MD5");
        int read;
        do {
            read = is.read(buffer);
            if (read > 0) {
                md.update(buffer, 0, read);
            }
        } while (read != -1);
        StringBuffer hexString = new StringBuffer();
        byte[] hash = md.digest();
        is.close();

        for (int j = 0; j < hash.length; j++) {
            if ((0xff & hash[j]) < 0x10) {
                hexString.append("0"
                        + Integer.toHexString((0xFF & hash[j])));
            } else {
                hexString.append(Integer.toHexString(0xFF & hash[j]));
            }
        }
        return hexString.toString();
    }

    protected JOSObject getObject(String path, boolean nullOk) throws WebApplicationException {
        JOSObject obj = mongo.getObjectCollection().findOne("{path:#}", path).as(JOSObject.class);
        if (obj==null) {
            if (nullOk)
                return null;
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        // for now don't enforce file-level ownership until we can map out subject keys
        // if (!owner.equals(obj.getOwner())) {
        //throw new WebApplicationException(Response.Status.FORBIDDEN);
        //}
        if (obj.isDeleted()) {
            throw new WebApplicationException(Response.Status.GONE);
        }

        return obj;
    }

    @Override
    public Object clone() {
        try {
            ObjectFileShare userCopy = this.getClass().newInstance();
            userCopy.setPath(this.getPath());
            userCopy.setMapping(this.getMapping());
            userCopy.setAuthorizer(this.getAuthorizer());
            userCopy.setPermissions(this.getPermissions());
            userCopy.setMongo(this.getMongo());
            userCopy.setRing(this.getRing());
            userCopy.setScality(this.getScality());
            return userCopy;
        } catch (InstantiationException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
