package org.janelia.it.workstation.shared.util.filecache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * <p>
 * This class encapsulates the information about a cached file.
 * It supports retrieval of the file from a remote server and
 * removal of the file when evicted from the cache.
 * </p>
 * <p>
 * When a remote file is retrieved, it is saved to the local file system
 * along with a companion file that contains a serialized form of this
 * object's metadata about the file. For example retrieval of the file
 * .../separate/ConsolidatedSignalMIP.png results in creation of a companion file
 * .../separate/.ConsolidatedSignalMIP.png.jacs-cached-file.
 * Creation of these companion files allows the most important parts of the
 * in-memory cache (namely the remote URL and etag) to be persisted without
 * requiring centralized/synchronized management.
 * </p>
 *
 * @author Eric Trautman
 */
public class CachedFile {
    
    private WebDavFile webDavFile;
    private File localFile;
    private File metaFile;

    /**
     * Constructs a new instance.
     *
     * @param  webDavFile  the WebDAV information for the file's remote source.
     * @param  localFile   the local location for the file after retrieval.
     */
    public CachedFile(WebDavFile webDavFile,
                      File localFile) {
        this.webDavFile = webDavFile;
        this.localFile = localFile;
        if (! webDavFile.isDirectory()) {
            // prefix name with '.' so that the meta files are
            // hidden/obscured when 'reveal in finder' is used
            this.metaFile = new File(localFile.getParentFile(),
                                     getMetaFileName(localFile));
        }
    }

    /**
     * @return the source URL for this file.
     */
    public URL getUrl() {
        return webDavFile.getUrl();
    }

    /**
     * @return the local location of this file after retrieval.
     */
    public File getLocalFile() {
        return localFile;
    }

    /**
     * @return the local location of this file's serialized meta data.
     */
    public File getMetaFile() {
        return metaFile;
    }

    /**
     * @return the number of kilobytes in this file.
     */
    public long getKilobytes() {
        long kilobytes = 0;
        if (localFile.exists()) {
            final long len = localFile.length();
            kilobytes = len / ONE_KILOBYTE;
            if ((len % ONE_KILOBYTE) > 0) {
                kilobytes++; // round up to nearest kb
            }
        }
        return kilobytes;
    }

    @Override
    public String toString() {

        return "CachedFile{" +
                "webDavFile=" + webDavFile +
                ", localFile=" + (localFile == null ? null : localFile.getAbsolutePath()) +
                ", metaFile=" + (metaFile == null ? null : metaFile.getAbsolutePath()) +
                '}';
    }

    /**
     * Removes this file from the local file system and any empty
     * parent directories that are within the cache.  Any exceptions
     * that occur during removal are simply logged (and ignored).
     *
     * @param  activeRootDirectory  the root directory for all active files in
     *                              the cache.
     */
    public void remove(File activeRootDirectory) {
        removeFile(localFile);
        removeFile(metaFile);
        try {
            removeEmptyCacheParent(activeRootDirectory, localFile);
        } catch (IOException e) {
            LOG.warn("remove: failed to remove empty parent directories for " + localFile.getAbsolutePath(), e);
        }
    }

    private void removeFile(File file) {
        if ((file != null) && file.isFile()) {
            try {
                if (file.delete()) {
                    LOG.debug("removeFile: removed {}", file.getAbsolutePath());
                } else {
                    LOG.warn("removeFile: failed to remove {}", file.getAbsolutePath());
                }
            } catch (Throwable t) {
                LOG.warn("removeFile: failed to remove " + file.getAbsolutePath(), t);
            }
        }
    }

    /**
     * Recursively walks up the directory structure until the active root
     * directory is found.  Any empty directories found along the way
     * are removed.  Special care is taken to make sure we only touch
     * directories that are within the cache.
     *
     * @param  activeRootDirectory     the root directory for all active files in
     *                                 the cache.

     * @param  removedFileOrDirectory  previously removed file or directory whose
     *                                 parent should be removed if empty.
     * @throws IOException
     *   if canonical paths cannot be derived.
     */
    private void removeEmptyCacheParent(File activeRootDirectory,
                                        File removedFileOrDirectory)
            throws IOException {

        File parent = removedFileOrDirectory.getParentFile();
        if ((parent != null) && parent.isDirectory()) {

            final String rootPath = activeRootDirectory.getCanonicalPath();
            final int minLength = rootPath.length();
            final String path = parent.getCanonicalPath();

            boolean logRemoval = LOG.isDebugEnabled();

            if ((path.length() > minLength) && path.startsWith(rootPath)) {
                final File[] children = parent.listFiles();
                if ((children != null) && (children.length == 0)) {
                    logRemoval = false;
                    if (parent.delete()) {
                        removeEmptyCacheParent(activeRootDirectory, parent);
                    } else {
                        LOG.warn("removeEmptyCacheParent: failed to remove {}", path);
                    }
                }
            }

            if (logRemoval) {
                LOG.debug("removeEmptyCacheParent: removed {}", removedFileOrDirectory.getAbsolutePath());
            }

        }

    }

    /**
     * Writes the current meta data file if this cached file is not a directory.
     *
     * @throws IllegalStateException
     *   if the meta data cannot be written.
     */
    public void saveMetadata()
            throws IllegalStateException {

        if (metaFile != null) {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(metaFile);
                final Gson gson = getGsonInstance();
                final String jsonValue = gson.toJson(this);
                out.write(jsonValue.getBytes());
            } catch (Exception e) {
                throw new IllegalStateException(
                        "failed to save cache file meta data to " + metaFile.getAbsolutePath(), e);

            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        LOG.warn("saveMetadata: failed to close meta data file " + metaFile.getAbsolutePath(), e);
                    }
                }
            }
        }
    }

    /**
     * @param  file  the file to check.
     *
     * @return true if the specified file is a cached file metadata file; otherwise false.
     */
    public static boolean isMetaFile(File file) {
        final String name = file.getName();
        return name.endsWith(META_FILE_SUFFIX);
    }

    /**
     * @param  localFile  locally cached file.
     *
     * @return the conventional meta file name for the specified cached file.
     */
    public static String getMetaFileName(File localFile) {
        return "." + localFile.getName() + META_FILE_SUFFIX;
    }

    /**
     * Parses the the specified metdata file and returns the corresponding
     * {@link CachedFile} instance.
     *
     * @param  metaFile  metdata file location.
     *
     * @return {@link CachedFile} instance parsed from the specified file or null if parsing fails.
     */
    public static CachedFile loadPreviouslyCachedFile(File metaFile) {

        CachedFile cachedFile = null;
        FileReader reader = null;
        try {
            reader = new FileReader(metaFile);
            final Gson gson = getGsonInstance();
            cachedFile = gson.fromJson(reader, CachedFile.class);
        } catch (Exception e) {
            LOG.warn("failed to load JSON cache file meta data from " + metaFile.getAbsolutePath(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.warn("loadPreviouslyCachedFile: failed to close meta data file " + metaFile.getAbsolutePath(),
                             e);
                }
            }
        }

        return cachedFile;
    }

    private static Gson getGsonInstance() {
        if (gsonInstance == null) {
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(File.class, new FileSerializer());
            builder.registerTypeAdapter(File.class, new FileDeserializer());
            builder.registerTypeAdapter(URL.class, new URLSerializer());
            builder.registerTypeAdapter(URL.class, new URLDeserializer());
            gsonInstance = builder.create();
        }
        return gsonInstance;
    }

    private static final Logger LOG = LoggerFactory.getLogger(CachedFile.class);

    private static final long ONE_KILOBYTE = 1024;

    private static final String META_FILE_SUFFIX = ".jacs-cached-file";

    private static Gson gsonInstance;

    private static class FileSerializer implements JsonSerializer<File> {
        public JsonElement serialize(File src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getAbsolutePath());
        }
    }

    private static class FileDeserializer implements JsonDeserializer<File> {
        public File deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new File(json.getAsJsonPrimitive().getAsString());
        }
    }

    private static class URLSerializer implements JsonSerializer<URL> {
        public JsonElement serialize(URL src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toExternalForm());
        }
    }

    private static class URLDeserializer implements JsonDeserializer<URL> {
        public URL deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            final String value = json.getAsJsonPrimitive().getAsString();
            URL url;
            try {
                url = new URL(value);
            } catch (MalformedURLException e) {
                throw new JsonParseException("failed to parse URL string '" + value + "'", e);
            }
            return url;
        }
    }

}