package org.janelia.jacs2.fileservices;

import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.service.impl.JacsService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class FileCopyComputationTest {

    private String libraryPath = "testLibrary";
    private String scriptName = "testScript";
    private FileCopyComputation testComputation;
    private File testDirectory;

    @Before
    public void setUp() throws IOException {
        Logger logger = mock(Logger.class);
        testComputation = new FileCopyComputation(libraryPath, scriptName, logger);
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
        JacsService<File> testService = new JacsService<>(null, testServiceData);
        CompletableFuture<JacsService<File>> preprocessStage = testComputation.preProcessData(testService).toCompletableFuture();
        assertTrue(preprocessStage.isDone());
        assertThat(preprocessStage.get().getResult().getAbsolutePath(), equalTo(testDestFile.getAbsolutePath()));
    }

    @Test
    public void missingRequiredParameter() throws ExecutionException, InterruptedException {
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-dst", testDestFile.getAbsolutePath())
                .build();
        verifyCompletionWithException(new JacsService<>(null, testServiceData));
    }

    @Test
    public void emptySourceOrTarget() throws ExecutionException, InterruptedException {
        JacsServiceDataBuilder testServiceDataBuilder = new JacsServiceDataBuilder(null);
        verifyCompletionWithException(new JacsService<>(null,
                testServiceDataBuilder
                        .addArg("-dst", "dst") // the arg order is important here in order to capture the execution branch
                        .addArg("-src", "")
                        .build()));
        verifyCompletionWithException(new JacsService<>(null,
                testServiceDataBuilder
                        .clearArgs()
                        .addArg("-src", "src")
                        .addArg("-dst", "")
                        .build()));
   }

    private void verifyCompletionWithException(JacsService<File> testService) throws ExecutionException, InterruptedException {
        CompletableFuture<JacsService<File>> preprocessStage = testComputation.preProcessData(testService).toCompletableFuture();
        assertTrue(preprocessStage.isCompletedExceptionally());
        assertNull(testService.getResult());
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
        CompletableFuture<JacsService<File>> doneStage = testComputation.isDone(new JacsService<>(null, testServiceData)).toCompletableFuture();
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
            CompletableFuture<JacsService<File>> doneStage = testComputation.isDone(new JacsService<>(null, testServiceData)).toCompletableFuture();
            assertTrue(doneStage.isCompletedExceptionally());
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
            CompletableFuture<JacsService<File>> doneStage = testComputation.isDone(new JacsService<>(null, testServiceData)).toCompletableFuture();
            assertTrue(doneStage.isDone());
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
        JacsService<File> testService = new JacsService<>(null, testServiceData);
        List<String> args = testComputation.prepareCmdArgs(testService);
        assertThat(testService.getServiceCmd(), equalTo(scriptName));
        assertThat(args, contains(testSource, testDestFile.getAbsolutePath()));
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
        JacsService<File> testService = new JacsService<>(null, testServiceData);
        List<String> args = testComputation.prepareCmdArgs(testService);
        assertThat(testService.getServiceCmd(), equalTo(scriptName));
        assertThat(args, contains(testSource, testDestFile.getAbsolutePath(), "8"));
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
        JacsService<File> testService = new JacsService<>(null, testServiceData);
        Map<String, String> env = testComputation.prepareEnvironment(testService);
        assertThat(env, hasEntry("LD_LIBRARY_PATH", libraryPath));
    }

    @Test
    public void checkOutputErrors() {
        ImmutableMap<String, Boolean> testData =
                new ImmutableMap.Builder<String, Boolean>()
                        .put("This has an error here", true)
                        .put("This has an ERROR here", true)
                        .put("This has an exception here", true)
                        .put("No Exception", true)
                        .put("OK here", false)
                        .put("\n", false)
                    .build();
        testData.forEach((l, r) -> assertThat(testComputation.checkForErrors(l), equalTo(r)));
    }
}
