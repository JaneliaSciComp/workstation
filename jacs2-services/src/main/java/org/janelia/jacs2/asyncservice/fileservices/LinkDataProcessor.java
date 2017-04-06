package org.janelia.jacs2.asyncservice.fileservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
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

@Named("linkData")
public class LinkDataProcessor extends AbstractBasicLifeCycleServiceProcessor<File> {

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
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                LinkDataArgs args = getArgs(jacsServiceData);
                File targetFile = getTargetFile(args);
                return targetFile.exists();
            }

            @Override
            public File collectResult(JacsServiceData jacsServiceData) {
                LinkDataArgs args = getArgs(jacsServiceData);
                File targetFile = getTargetFile(args);
                return targetFile;
            }
        };
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            LinkDataArgs args = getArgs(jacsServiceData);
            if (StringUtils.isBlank(args.source) || !getSourceFile(args).exists()) {
                throw new ComputationException(jacsServiceData, "Source file name must be specified and must exist");
            } else if (StringUtils.isBlank(args.target)) {
                throw new ComputationException(jacsServiceData, "Target file name must be specified");
            } else {
                File targetFile = getTargetFile(args);
                Files.createDirectories(targetFile.getParentFile().toPath());
            }
        } catch (ComputationException e) {
            throw e;
        } catch (Exception e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenApply(sd -> {
                    try {
                        LinkDataArgs args = getArgs(jacsServiceData);
                        Files.createSymbolicLink(getSourceFile(args).toPath(), getTargetFile(args).toPath());
                        return sd;
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

    private File getSourceFile(LinkDataArgs args) {
        return new File(args.source);
    }

    private File getTargetFile(LinkDataArgs args) {
        return new File(args.target);
    }

}
