package org.janelia.jacs2.asyncservice.fileservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.VoidServiceResultHandler;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Named("fileRemove")
public class FileRemoveProcessor extends AbstractBasicLifeCycleServiceProcessor<Void, Void> {

    public static class FileMoveArgs extends ServiceArgs {
        @Parameter(names = {"-file"}, description = "File name", required = true)
        String file;
    }

    @Inject
    FileRemoveProcessor(ServiceComputationFactory computationFactory,
                        JacsServiceDataPersistence jacsServiceDataPersistence,
                        @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                        Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new FileMoveArgs());
    }

    @Override
    public ServiceResultHandler<Void> getResultHandler() {
        return new VoidServiceResultHandler();
    }

    @Override
    protected ServiceComputation<JacsServiceResult<Void>> processing(JacsServiceResult<Void> depResults) {
        return computationFactory.newCompletedComputation(depResults)
                .thenApply(pd -> {
                    try {
                        FileMoveArgs args = getArgs(pd.getJacsServiceData());
                        Path filePath = getFile(args);
                        if (!Files.exists(filePath)) {
                            Files.delete(filePath);
                        }
                        return pd;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private FileMoveArgs getArgs(JacsServiceData jacsServiceData) {
        FileMoveArgs fileMoveArgs = new FileMoveArgs();
        new JCommander(fileMoveArgs).parse(jacsServiceData.getArgsArray());
        return fileMoveArgs;
    }

    private Path getFile(FileMoveArgs args) {
        return Paths.get(args.file);
    }
}
