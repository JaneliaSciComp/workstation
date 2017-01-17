package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.model.service.JacsServiceState;
import org.janelia.jacs2.model.service.ProcessingLocation;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.service.dataservice.sample.SampleDataService;
import org.janelia.jacs2.service.impl.AbstractServiceProcessor;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.JacsServiceDispatcher;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceComputationFactory;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Named("sampleImageFilesService")
public class GetSampleImageFilesServiceProcessor extends AbstractServiceProcessor<List<String>> {

    private final SampleDataService sampleDataService;

    public GetSampleImageFilesServiceProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                                               ServiceComputationFactory computationFactory,
                                               JacsServiceDataPersistence jacsServiceDataPersistence,
                                               @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                               SampleDataService sampleDataService,
                                               Logger logger) {
        super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.sampleDataService = sampleDataService;
    }

    @Override
    public List<String> getResult(JacsServiceData jacsServiceData) {
        if (StringUtils.isNotBlank(jacsServiceData.getStringifiedResult())) {
            return Splitter.on(",").omitEmptyStrings().trimResults().splitToList(jacsServiceData.getStringifiedResult());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void setResult(List<String> result, JacsServiceData jacsServiceData) {
        if (CollectionUtils.isNotEmpty(result)) {
            jacsServiceData.setStringifiedResult(result.stream()
                    .filter(s -> StringUtils.isNotBlank(s))
                    .collect(Collectors.joining(",")));
        } else {
            jacsServiceData.setStringifiedResult(null);
        }
    }

    @Override
    protected ServiceComputation<List<File>> preProcessData(JacsServiceData jacsServiceData) {
        GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs args = getArgs(jacsServiceData);
        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsServiceData.getOwner(), args.sampleId, args.sampleObjective);
        if (anatomicalAreas.isEmpty()) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "No anatomical areas found for " +
                    args.sampleId +
                    (StringUtils.isBlank(args.sampleObjective) ? "" : args.sampleObjective)));
        }
        Path workingDirectory = getWorkingDirectory(jacsServiceData);
        // invoke child file copy services for all LSM files
        List<ServiceComputation<?>> fcs = anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
                .map(lf -> {
                    JacsServiceData retrieveImageFileServiceData =
                            new JacsServiceDataBuilder(jacsServiceData)
                                    .setName("fileCopy")
                                    .addArg("-src", lf.getFilepath())
                                    .addArg("-dst", getIntermediateImageFile(workingDirectory, lf).getAbsolutePath())
                                    .setProcessingLocation(ProcessingLocation.CLUSTER) // fileCopy only works on the cluster for now
                                    .build();
                    this.submitChildService(jacsServiceData, retrieveImageFileServiceData);
                    return this.waitForCompletion(retrieveImageFileServiceData);
                })
                .collect(Collectors.toList());
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenCombineAll(fcs, (sd, results) -> results.stream().map(r -> (File)r).collect(Collectors.toList()));
    }

    @Override
    protected ServiceComputation<List<String>> localProcessData(Object preprocessingResults, JacsServiceData jacsServiceData) {
        // collect the files and send them to the destination
        GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs args = getArgs(jacsServiceData);
        List<File> intermediateFiles = (List<File>) preprocessingResults;
        List<String> results = new ArrayList<>();
        intermediateFiles.forEach(imageFile -> {
                results.add(writeToFileOrUrl(imageFile, args.destFolder));
        });
        setResult(results, jacsServiceData);
        return computationFactory.newCompletedComputation(results);
    }

    protected ServiceComputation<List<String>> postProcessData(List<String> processingResult, JacsServiceData jacsServiceData) {
        try {
            GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs args = getArgs(jacsServiceData);
            if (CollectionUtils.isNotEmpty(processingResult)) {
                List<AnatomicalArea> anatomicalAreas =
                        sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsServiceData.getOwner(), args.sampleId, args.sampleObjective);
                Path workingDirectory = getWorkingDirectory(jacsServiceData);
                anatomicalAreas.stream()
                        .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
                        .map(lf -> getIntermediateImageFile(workingDirectory, lf).toString())
                        .filter(workingFileName -> !processingResult.contains(workingFileName)) // only delete the file if it's not an actual result
                        .forEach(workingFileName -> {
                            try {
                                Files.deleteIfExists(Paths.get(workingFileName));
                            } catch (IOException e) {
                                logger.warn("Error deleting working file {}", workingFileName, e);
                            }
                        });
            }
            return computationFactory.newCompletedComputation(processingResult);
        } catch (Exception e) {
            return computationFactory.newFailedComputation(e);
        }
    }

    private String writeToFileOrUrl(File imageFile, String destFolder) {
        if (destFolder.startsWith("http://")) { // for now only consider http (no https)
            // HTTP POST to destination
            try {
                return writeToHttpUrl(imageFile, new URL(destFolder));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid URL: " + destFolder, e);
            }
        } else if (destFolder.startsWith("file://")) {
            URI destURL;
            try {
                destURL = new URL(destFolder).toURI();
                Path destPath = Paths.get(destURL);
                return copyFileToFolder(imageFile, destPath);
            } catch (MalformedURLException | URISyntaxException e) {
                throw new IllegalArgumentException("Invalid URL: " + destFolder, e);
            }
        } else {
            return copyFileToFolder(imageFile, Paths.get(destFolder));
        }
    }

    private String writeToHttpUrl(File imageFile, URL url) {
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            OutputStream os = conn.getOutputStream();
            Files.copy(imageFile.toPath(), os);
            int statusCode = conn.getResponseCode();
            if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                throw new IllegalStateException("Invalid HTTP request for sending " + imageFile + " to " + url);
            }
            is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader((is)));
            // consume the response
            String serverOutput;
            while ((serverOutput = br.readLine()) != null) {
                logger.debug(serverOutput);
            }
            return url.toString();
        } catch (Exception e) {
            logger.error("Error writing {} to {}", imageFile, url, e);
            throw new IllegalStateException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    logger.warn("Error closing the response stream", e);
                }
            }
            if (conn != null)
                conn.disconnect();
        }
    }

    private String copyFileToFolder(File imageFile, Path destFolder) {
        String fileName = imageFile.getName();
        Path destFile = destFolder.resolve(fileName);
        try {
            Files.createDirectories(destFolder); // ensure the destination folder exists
            Files.copy(imageFile.toPath(), destFile);
            return destFile.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs getArgs(JacsServiceData jacsServiceData) {
        GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs args = new GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private File getIntermediateImageFile(Path workingDir, Image image) {
        String fileName = new File(image.getFilepath()).getName();
        if (fileName.endsWith(".bz2")) {
            fileName = fileName.substring(0, fileName.length() - ".bz2".length());
        } else if (fileName.endsWith(".gz")) {
            fileName = fileName.substring(0, fileName.length() - ".gz".length());
        }
        return new File(workingDir.toFile(), fileName);
    }
}
