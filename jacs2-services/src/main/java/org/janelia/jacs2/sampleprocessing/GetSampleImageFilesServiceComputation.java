package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.model.service.JacsServiceState;
import org.janelia.jacs2.model.service.ProcessingLocation;
import org.janelia.jacs2.service.dataservice.sample.SampleDataService;
import org.janelia.jacs2.service.impl.AbstractServiceComputation;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.JacsService;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Named("sampleImageFilesService")
public class GetSampleImageFilesServiceComputation extends AbstractServiceComputation<List<String>> {

    private final SampleDataService sampleDataService;
    private final Logger logger;

    @Inject
    GetSampleImageFilesServiceComputation(@PropertyValue(name = "service.DefaultScratchDir") String scratchLocation,
                                          SampleDataService sampleDataService,
                                          Logger logger) {

        this.sampleDataService = sampleDataService;
        this.logger = logger;
    }

    @Override
    public CompletionStage<JacsService<List<String>>> preProcessData(JacsService<List<String>> jacsService) {
        GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs args = getArgs(jacsService);
        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsService.getOwner(), args.sampleId, args.sampleObjective);
        if (anatomicalAreas.isEmpty()) {
            CompletableFuture<JacsService<List<String>>> preProcessExc = new CompletableFuture<>();
            preProcessExc.completeExceptionally(new IllegalArgumentException("No anatomical areas found for " +
                    args.sampleId +
                    (StringUtils.isBlank(args.sampleObjective) ? "" : args.sampleObjective)));
            return preProcessExc;
        }
        Path workingDirectory = getWorkingDirectory(jacsService);
        List<Number> result = anatomicalAreas.stream()
                .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
                .map(lf -> {
                    JacsServiceData retrieveImageFileService =
                            new JacsServiceDataBuilder(jacsService.getJacsServiceData())
                                    .setName("fileCopy")
                                    .addArg("-src", lf.getFilepath())
                                    .addArg("-dst", getIntermediateImageFile(workingDirectory, lf).getAbsolutePath())
                                    .setProcessingLocation(ProcessingLocation.CLUSTER) // fileCopy only works on the cluster for now
                                    .build();
                    jacsService.submitChildServiceAsync(retrieveImageFileService);
                    return retrieveImageFileService.getId();
                }).collect(Collectors.toList());
        if (result.isEmpty()) {
            jacsService.setState(JacsServiceState.CANCELED);
            CompletableFuture<JacsService<List<String>>> preProcessExc = new CompletableFuture<>();
            preProcessExc.completeExceptionally(new ComputationException(jacsService, "No LSM image found for " + jacsService.getJacsServiceData()));
            return preProcessExc;
        }
        if (result.isEmpty()) {
            jacsService.setState(JacsServiceState.CANCELED);
            CompletableFuture<JacsService<List<String>>> preProcessExc = new CompletableFuture<>();
            preProcessExc.completeExceptionally(new ComputationException(jacsService, "No LSM image found for " + jacsService.getJacsServiceData()));
            return preProcessExc;
        }
        logger.info("Created child services {} to retrieve sample images", result, jacsService.getJacsServiceData());
        return super.preProcessData(jacsService);
    }

    @Override
    public CompletionStage<JacsService<List<String>>> processData(JacsService<List<String>> jacsService) {
        // collect the files and send them to the destination
        GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs args = getArgs(jacsService);
        List<AnatomicalArea> anatomicalAreas =
                sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsService.getOwner(), args.sampleId, args.sampleObjective);
        CompletableFuture<JacsService<List<String>>> processData = new CompletableFuture<>();
        try {
            Path workingDirectory = getWorkingDirectory(jacsService);
            List<String> results = new ArrayList<>();
            anatomicalAreas.stream()
                    .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
                    .forEach(lf -> {
                        File imageFile = getIntermediateImageFile(workingDirectory, lf);
                        results.add(writeToFileOrUrl(imageFile, args.destFolder));
                    });
            jacsService.setResult(results);
            processData.complete(jacsService);
        } catch (Exception e) {
            processData.completeExceptionally(new ComputationException(jacsService, e));
        }
        return processData;
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

    @Override
    public CompletionStage<JacsService<List<String>>> isDone(JacsService<List<String>> jacsService) {
        CompletableFuture<JacsService<List<String>>> doneFuture = new CompletableFuture<>();
        try {
            GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs args = getArgs(jacsService);
            List<String> results = jacsService.getResult();
            if (CollectionUtils.isNotEmpty(results)) {
                List<AnatomicalArea> anatomicalAreas =
                        sampleDataService.getAnatomicalAreasBySampleIdAndObjective(jacsService.getOwner(), args.sampleId, args.sampleObjective);
                Path workingDirectory = getWorkingDirectory(jacsService);
                anatomicalAreas.stream()
                        .flatMap(ar -> ar.getTileLsmPairs().stream().flatMap(lsmp -> lsmp.getLsmFiles().stream()))
                        .map(lf -> getIntermediateImageFile(workingDirectory, lf).toString())
                        .filter(workingFileName -> !results.contains(workingFileName))
                        .forEach(workingFileName -> {
                            try {
                                Files.deleteIfExists(Paths.get(workingFileName));
                            } catch (IOException e) {
                                logger.warn("Error deleting working file {}", workingFileName, e);
                            }
                        });
                doneFuture.complete(jacsService);
            } else {
                doneFuture.complete(jacsService);
            }
        } catch (Exception e) {
            doneFuture.completeExceptionally(e);
        }
        return doneFuture;
    }

    private GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs getArgs(JacsService jacsService) {
        GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs args = new GetSampleImageFilesServiceDescriptor.SampleImageFilesArgs();
        new JCommander(args).parse(jacsService.getArgsArray());
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
