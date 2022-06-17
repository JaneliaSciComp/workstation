package org.janelia.workstation.ndviewer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.janelia.jacsstorage.newclient.JadeStorageService;
import org.janelia.jacsstorage.newclient.StorageLocation;
import org.janelia.jacsstorage.newclient.StorageObject;
import org.janelia.jacsstorage.newclient.StorageObjectNotFoundException;
import org.janelia.saalfeldlab.n5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Stream;

/**
 * {@link N5Reader} implementation using Jade as the storage backend.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class N5JadeReader extends AbstractGsonReader {

	private final static Logger log = LoggerFactory.getLogger(N5JadeReader.class);

	protected static final String jsonFile = "attributes.json";

	protected final JadeStorageService jadeStorage;
	protected final StorageLocation storageLocation;

	protected final String basePath;

	/**
	 * Opens an {@link N5JadeReader} at a given base path with a custom
	 * {@link GsonBuilder} to support custom attributes.
	 *
	 * @param basePath N5 base path
	 * @param gsonBuilder
	 * @throws IOException
	 *    if the base path cannot be read or does not exist,
	 *    if the N5 version of the container is not compatible with this
	 *    implementation.
	 */
	public N5JadeReader(final JadeStorageService jadeStorage,
						final String basePath,
						final GsonBuilder gsonBuilder) throws IOException {

		super(gsonBuilder);
		this.jadeStorage = jadeStorage;
		this.basePath = basePath;
		this.storageLocation = jadeStorage.getStorageObjectByPath(basePath);

		if (storageLocation == null) {
			throw new IOException("Could not find Jade location for path: "+basePath);
		}

		// TODO: version checking is disabled here because the POM reading functionality
		//       doesn't work with the NetBeans module system
//		if (exists("/")) {
//			final Version version = getVersion();
//			if (!VERSION.isCompatible(version))
//				throw new IOException("Incompatible version " + version + " (this is " + VERSION + ").");
//		}
	}

	/**
	 * Opens an {@link N5JadeReader} at a given base path.
	 *
	 * @param basePath N5 base path
	 * @throws IOException
	 *    if the base path cannot be read or does not exist,
	 *    if the N5 version of the container is not compatible with this
	 *    implementation.
	 */
	public N5JadeReader(final JadeStorageService jadeStorageService, final String basePath) throws IOException {
		this(jadeStorageService, basePath, new GsonBuilder());
	}

	/**
	 *
	 * @return N5 base path
	 */
	public String getBasePath() {
		return this.basePath;
	}

	@Override
	public boolean exists(final String pathName) {

		final Path path = Paths.get(basePath, pathName);
		String relativePath = storageLocation.getRelativePath(path.toString());
		try {
			StorageObject metadata = jadeStorage.getMetadata(storageLocation, relativePath);
			boolean exists = metadata != null && metadata.isCollection();
			log.trace("exists {} = {}", pathName, exists);
			return exists;
		}
		catch (StorageObjectNotFoundException e) {
			log.trace("exists {} = StorageObjectNotFoundException", pathName);
			return false;
		}
	}

	@Override
	public HashMap<String, JsonElement> getAttributes(final String pathName) throws IOException {

		log.trace("getAttributes "+pathName);
		final Path path = Paths.get(basePath, getAttributesPath(pathName).toString());
		String relativePath = storageLocation.getRelativePath(path.toString());

		try (Reader reader = new InputStreamReader(jadeStorage.getContent(storageLocation, relativePath))) {
			HashMap<String, JsonElement> attrs = GsonAttributesParser.readAttributes(reader, getGson());
			if (attrs==null || attrs.isEmpty())
				log.error("Empty n5 attributes: "+path);
			return attrs;
		}
		catch (Exception e) {
			log.error("Could not fetch "+path);
			return new HashMap<>();
		}
	}

	@Override
	public DataBlock<?> readBlock(
			final String pathName,
			final DatasetAttributes datasetAttributes,
			final long... gridPosition) throws IOException {

		log.trace("readBlock "+pathName);
		final Path path = Paths.get(basePath, getDataBlockPath(pathName, gridPosition).toString());
		String relativePath = storageLocation.getRelativePath(path.toString());

		try (InputStream inputStream = jadeStorage.getContent(storageLocation, relativePath)) {
			return DefaultBlockReader.readBlock(inputStream, datasetAttributes, gridPosition);
		}
		catch (Exception e) {
			log.trace("Could not fetch "+path, e);
			return null;
		}
	}

	@Override
	public String[] list(final String pathName) throws IOException {

		log.trace("list "+pathName);
		final Path path = Paths.get(basePath, pathName);
		String relativePath = storageLocation.getRelativePath(path.toString());

		try (Stream<StorageObject> stream = jadeStorage.getChildren(storageLocation, relativePath).stream()) {
			return stream
					.filter(a -> a.isCollection())
					.map(a -> path.relativize(Paths.get(a.getAbsolutePath())).toString())
					.toArray(n -> new String[n]);
		}
		catch (StorageObjectNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Constructs the path for a data block in a dataset at a given grid position.
	 *
	 * The returned path is
	 * <pre>
	 * $datasetPathName/$gridPosition[0]/$gridPosition[1]/.../$gridPosition[n]
	 * </pre>
	 *
	 * This is the file into which the data block will be stored.
	 *
	 * @param datasetPathName
	 * @param gridPosition
	 * @return
	 */
	protected static Path getDataBlockPath(
			final String datasetPathName,
			final long... gridPosition) {

		final String[] pathComponents = new String[gridPosition.length];
		for (int i = 0; i < pathComponents.length; ++i)
			pathComponents[i] = Long.toString(gridPosition[i]);

		return Paths.get(removeLeadingSlash(datasetPathName), pathComponents);
	}

	/**
	 * Constructs the path for the attributes file of a group or dataset.
	 *
	 * @param pathName
	 * @return
	 */
	protected static Path getAttributesPath(final String pathName) {

		return Paths.get(removeLeadingSlash(pathName), jsonFile);
	}

	/**
	 * Removes the leading slash from a given path and returns the corrected path.
	 * It ensures correctness on both Unix and Windows, otherwise {@code pathName} is treated
	 * as UNC path on Windows, and {@code Paths.get(pathName, ...)} fails with {@code InvalidPathException}.
	 *
	 * @param pathName
	 * @return
	 */
	protected static String removeLeadingSlash(final String pathName) {

		return pathName.startsWith("/") || pathName.startsWith("\\") ? pathName.substring(1) : pathName;
	}

	@Override
	public String toString() {
		return String.format("%s[basePath=%s]", getClass().getSimpleName(), basePath);
	}
}
