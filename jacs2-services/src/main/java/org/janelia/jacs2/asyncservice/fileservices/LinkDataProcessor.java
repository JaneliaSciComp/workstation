package org.janelia.jacs2.asyncservice.fileservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractSingleFileServiceResultHandler;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Named("linkData")
public class LinkDataProcessor extends AbstractBasicLifeCycleServiceProcessor<Void, File> {

    public static class LinkDataArgs extends ServiceArgs {
        @Parameter(names = {"-input", "-source"}, description = "Source name", required = true)
        String source;
        @Parameter(names = {"-target"}, description = "Target name or location", required = true)
        String target;
    }

    @Inject
    LinkDataProcessor(ServiceComputationFactory computationFactory,
                      JacsServiceDataPersistence jacsServiceDataPersistence,
                      @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                      Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new LinkDataArgs());
    }

    @Override
    public ServiceResultHandler<File> getResultHandler() {
        return new AbstractSingleFileServiceResultHandler() {
            @Override
            public boolean isResultReady(JacsServiceResult<?> depResults) {
                return getTargetFile(getArgs(depResults.getJacsServiceData())).toFile().exists();
            }

            @Override
            public File collectResult(JacsServiceResult<?> depResults) {
                return getTargetFile(getArgs(depResults.getJacsServiceData())).toFile();
            }
        };
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            LinkDataArgs args = getArgs(jacsServiceData);
            if (StringUtils.isBlank(args.source)) {
                throw new ComputationException(jacsServiceData, "Source file name must be specified");
            } else if (StringUtils.isBlank(args.target)) {
                throw new ComputationException(jacsServiceData, "Target file name must be specified");
            } else {
                Path targetFile = getTargetFile(args);
                Files.createDirectories(targetFile.getParent());
            }
        } catch (ComputationException e) {
            throw e;
        } catch (Exception e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected ServiceComputation<JacsServiceResult<Void>> processing(JacsServiceResult<Void> depResults) {
        return computationFactory.newCompletedComputation(depResults)
                .thenApply(pd -> {
                    try {
                        LinkDataArgs args = getArgs(pd.getJacsServiceData());
                        Path sourcePath = getSourceFile(args);
                        Path targetPath = getTargetFile(args);
                        if (!Files.isSameFile(sourcePath, targetPath)) {
                            Files.createSymbolicLink(sourcePath, targetPath);
                        }
                        return pd;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private LinkDataArgs getArgs(JacsServiceData jacsServiceData) {
        LinkDataArgs linkDataArgs = new LinkDataArgs();
        new JCommander(linkDataArgs).parse(jacsServiceData.getArgsArray());
        return linkDataArgs;
    }

    private Path getSourceFile(LinkDataArgs args) {
        return Paths.get(args.source);
    }

    private Path getTargetFile(LinkDataArgs args) {
        return Paths.get(args.target);
    }

}
