package org.janelia.jacs2.fileservices;

import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.service.impl.JacsService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class FileCopyComputationTest {

    private Logger logger;
    private String libraryPath = "testLibrary";
    private String scriptName = "testScript";
    private FileCopyComputation testComputation;
    private File testTargetDirectory;

    @Before
    public void setUp() throws IOException {
        logger = mock(Logger.class);
        testComputation = new FileCopyComputation(libraryPath, scriptName, logger);
        testTargetDirectory = Files.createTempDirectory("testFileCopy").toFile();
    }

    @After
    public void tearDown() throws IOException {
        Files.delete(testTargetDirectory.toPath());
    }

    @Test
    public void successfulPreprocessing() throws ExecutionException, InterruptedException {
        File testDestFile = new File(testTargetDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                    .addArg("-src", "/home/testSource")
                    .addArg("-dst", testDestFile.getAbsolutePath())
                    .build();
        JacsService<File> testService = new JacsService<>(null, testServiceData);
        CompletableFuture<JacsService<File>> preprocessStage = testComputation.preProcessData(testService).toCompletableFuture();
        assertTrue(preprocessStage.isDone());
        assertThat(preprocessStage.get().getResult().getAbsolutePath(), equalTo(testDestFile.getAbsolutePath()));
    }

    @Test
    public void missingRequiredParameter() throws ExecutionException, InterruptedException {
        File testDestFile = new File(testTargetDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-dst", testDestFile.getAbsolutePath())
                .build();
        JacsService<File> testService = new JacsService<>(null, testServiceData);
        CompletableFuture<JacsService<File>> preprocessStage = testComputation.preProcessData(testService).toCompletableFuture();
        assertTrue(preprocessStage.isCompletedExceptionally());
        assertNull(testService.getResult());
    }

    @Test
    public void cmdArgs() {
        String testSource = "/home/testSource";
        File testDestFile = new File(testTargetDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-src", testSource)
                .addArg("-dst", testDestFile.getAbsolutePath())
                .build();
        JacsService<File> testService = new JacsService<>(null, testServiceData);
        List<String> args = testComputation.prepareCmdArgs(testService);
        assertThat(testService.getServiceCmd(), equalTo(scriptName));
        assertThat(args, contains(testSource, testDestFile.getAbsolutePath()));
    }

    @Test
    public void cmdArgsWithConvertFlag() {
        String testSource = "/home/testSource";
        File testDestFile = new File(testTargetDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-src", testSource)
                .addArg("-dst", testDestFile.getAbsolutePath())
                .addArg("-mv")
                .addArg("-convert8")
                .build();
        JacsService<File> testService = new JacsService<>(null, testServiceData);
        List<String> args = testComputation.prepareCmdArgs(testService);
        assertThat(testService.getServiceCmd(), equalTo(scriptName));
        assertThat(args, contains(testSource, testDestFile.getAbsolutePath(), "8"));
    }
}
