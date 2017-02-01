package org.janelia.jacs2.asyncservice.fileservices;

import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.JacsServiceDispatcher;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileCopyProcessorTest {

    private JacsServiceDispatcher jacsServiceDispatcher;
    private ServiceComputationFactory serviceComputationFactory;
    private JacsServiceDataPersistence jacsServiceDataPersistence;
    private Instance<ExternalProcessRunner> serviceRunners;
    private String libraryPath = "testLibrary";
    private String scriptName = "testScript";
    private String defaultWorkingDir = "testWorking";
    private String executablesBaseDir = "testTools";

    private FileCopyProcessor testProcessor;
    private File testDirectory;

    @Before
    public void setUp() throws IOException {
        ExecutorService executor = mock(ExecutorService.class);

        when(executor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        });

        serviceComputationFactory = new ServiceComputationFactory(executor);
        Logger logger = mock(Logger.class);
        testProcessor = new FileCopyProcessor(
                jacsServiceDispatcher,
                serviceComputationFactory,
                jacsServiceDataPersistence,
                defaultWorkingDir,
                executablesBaseDir,
                serviceRunners,
                libraryPath,
                scriptName,
                logger);
        testDirectory = Files.createTempDirectory("testFileCopy").toFile();
    }

    @After
    public void tearDown() throws IOException {
        Files.delete(testDirectory.toPath());
    }

    @Test
    public void successfulPreprocessing() throws ExecutionException, InterruptedException {
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                    .addArg("-src", "/home/testSource")
                    .addArg("-dst", testDestFile.getAbsolutePath())
                    .build();
        ServiceComputation<File> preprocessStage = testProcessor.preProcessData(testServiceData);
        assertTrue(preprocessStage.isDone());
        assertThat(preprocessStage.get().getAbsolutePath(), equalTo(testDestFile.getAbsolutePath()));
    }

    @Test
    public void missingRequiredParameter() throws ExecutionException, InterruptedException {
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-dst", testDestFile.getAbsolutePath())
                .build();
        verifyCompletionWithException(testServiceData);
    }

    @Test
    public void emptySourceOrTarget() throws ExecutionException, InterruptedException {
        JacsServiceDataBuilder testServiceDataBuilder = new JacsServiceDataBuilder(null);
        verifyCompletionWithException(testServiceDataBuilder
                        .addArg("-dst", "dst") // the arg order is important here in order to capture the execution branch
                        .addArg("-src", "")
                        .build());
        verifyCompletionWithException(testServiceDataBuilder
                        .clearArgs()
                        .addArg("-src", "src")
                        .addArg("-dst", "")
                        .build());
   }

    private void verifyCompletionWithException(JacsServiceData testServiceData) throws ExecutionException, InterruptedException {
        ServiceComputation<File> preprocessStage = testProcessor.preProcessData(testServiceData);

        assertTrue(preprocessStage.isCompletedExceptionally());
    }

    @Test
    public void deleteSourceWhenDone() throws IOException {
        Path testSourcePath = Files.createTempFile(testDirectory.toPath(), "testFileCopySource", ".test");
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-src", testSourcePath.toString())
                .addArg("-dst", testDestFile.getAbsolutePath())
                .addArg("-mv")
                .addArg("-convert8")
                .build();
        assertTrue(Files.exists(testSourcePath));
        ServiceComputation<File> doneStage = testProcessor.postProcessData(testSourcePath.toFile(), testServiceData);
        assertTrue(doneStage.isDone());
        assertTrue(Files.notExists(testSourcePath));
    }

    @Test
    public void cannotDeleteSourceWhenDone() throws IOException {
        Path testSourcePath = Files.createTempFile(testDirectory.toPath(), "testFileCopySource", ".test");
        try {
            File testDestFile = new File(testDirectory, "testDest");
            JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                    .addArg("-src", testDirectory.getAbsolutePath()) // pass in a non-empty dir so that it cannot be deleted
                    .addArg("-dst", testDestFile.getAbsolutePath())
                    .addArg("-mv")
                    .addArg("-convert8")
                    .build();
            ServiceComputation<File> postProcessing = testProcessor.postProcessData(testDestFile, testServiceData);
            assertTrue(postProcessing.isCompletedExceptionally());
            assertTrue(Files.exists(testSourcePath));
        } finally {
            Files.deleteIfExists(testSourcePath);
        }
    }

    @Test
    public void doNotDeleteSourceWhenDone() throws IOException {
        Path testSourcePath = Files.createTempFile(testDirectory.toPath(), "testFileCopySource", ".test");
        try {
            File testDestFile = new File(testDirectory, "testDest");
            JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                    .addArg("-src", testSourcePath.toString())
                    .addArg("-dst", testDestFile.getAbsolutePath())
                    .addArg("-convert8")
                    .build();
            ServiceComputation<File> postProcessing = testProcessor.postProcessData(testDestFile, testServiceData);
            assertTrue(postProcessing.isDone());
            assertTrue(Files.exists(testSourcePath));
        } finally {
            Files.deleteIfExists(testSourcePath);
        }
    }

    @Test
    public void cmdArgs() {
        String testSource = "/home/testSource";
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-src", testSource)
                .addArg("-dst", testDestFile.getAbsolutePath())
                .build();
        ExternalCodeBlock copyScript = testProcessor.prepareExternalScript(testServiceData);
        assertThat(copyScript.toString(),
                equalTo(executablesBaseDir + "/" + scriptName + " " + testSource + " " + testDestFile.getAbsolutePath() + " \n"));
    }

    @Test
    public void cmdArgsWithConvertFlag() {
        String testSource = "/home/testSource";
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-src", testSource)
                .addArg("-dst", testDestFile.getAbsolutePath())
                .addArg("-mv")
                .addArg("-convert8")
                .build();
        ExternalCodeBlock copyScript = testProcessor.prepareExternalScript(testServiceData);
        assertThat(copyScript.toString(),
                equalTo(executablesBaseDir + "/" + scriptName + " " + testSource + " " + testDestFile.getAbsolutePath() + " 8 \n"));
    }

    @Test
    public void prepareEnv() {
        String testSource = "/home/testSource";
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-src", testSource)
                .addArg("-dst", testDestFile.getAbsolutePath())
                .addArg("-mv")
                .addArg("-convert8")
                .build();
        Map<String, String> env = testProcessor.prepareEnvironment(testServiceData);
        assertThat(env, hasEntry(equalTo("LD_LIBRARY_PATH"), containsString(libraryPath)));
    }

}
