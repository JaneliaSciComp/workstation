package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractServiceProcessor<T> implements ServiceProcessor<T> {

    protected final JacsServiceEngine jacsServiceEngine;
    protected final ServiceComputationFactory computationFactory;
    protected final String defaultWorkingDir;
    protected final Logger logger;

    public AbstractServiceProcessor(JacsServiceEngine jacsServiceEngine,
                                    ServiceComputationFactory computationFactory,
                                    String defaultWorkingDir,
                                    Logger logger) {
        this.jacsServiceEngine = jacsServiceEngine;
        this.computationFactory= computationFactory;
        this.defaultWorkingDir = defaultWorkingDir;
        this.logger = logger;
    }

    protected JacsServiceData submit(JacsServiceData jacsServiceData) {
        return jacsServiceEngine.submitSingleService(jacsServiceData);
    }

    @Override
    public JacsServiceData createServiceData(ServiceExecutionContext executionContext, ServiceArg... args) {
        ServiceMetaData smd = getMetadata();
        JacsServiceDataBuilder jacsServiceDataBuilder =
                new JacsServiceDataBuilder(executionContext.getParentServiceData())
                        .setName(smd.getServiceName())
                        .setProcessingLocation(executionContext.getProcessingLocation())
                        .setDescription(executionContext.getDescription());
        if (executionContext.getParentServiceData() != null) {
            jacsServiceDataBuilder.setWorkspace(getWorkingDirectory(executionContext.getParentServiceData()).toString());
        }
        jacsServiceDataBuilder.addArg(Stream.of(args).flatMap(arg -> Stream.of(arg.toStringArray())).toArray(String[]::new));
        if (executionContext.getServiceState() != null) {
            jacsServiceDataBuilder.setState(executionContext.getServiceState());
        }
        executionContext.getWaitFor().forEach(sd -> jacsServiceDataBuilder.addDependency(sd));
        if (executionContext.getParentServiceData() != null) {
            executionContext.getParentServiceData().getDependeciesIds().forEach(did -> jacsServiceDataBuilder.addDependencyId(did));
        }
        return jacsServiceDataBuilder.build();
    }

    protected <U> ServiceComputation<U> createComputation(U data) {
        return computationFactory.newCompletedComputation(data);
    }

    protected <U> ServiceComputation<U> createFailure(Throwable exc) {
        return computationFactory.newFailedComputation(exc);
    }

    protected Path getWorkingDirectory(JacsServiceData jacsServiceData) {
        String workingDir;
        if (StringUtils.isNotBlank(jacsServiceData.getWorkspace())) {
            workingDir = jacsServiceData.getWorkspace();
        } else if (StringUtils.isNotBlank(defaultWorkingDir)) {
            workingDir = defaultWorkingDir;
        } else {
            workingDir = System.getProperty("java.io.tmpdir");
        }
        return getServicePath(workingDir, jacsServiceData);
    }

    protected Path getServicePath(String baseDir, JacsServiceData jacsServiceData, String... more) {
        List<String> pathElems = new ImmutableList.Builder<String>()
                .add(jacsServiceData.getName())
                .add(jacsServiceData.getId().toString())
                .addAll(Arrays.asList(more))
                .build();
        return Paths.get(baseDir, pathElems.toArray(new String[0])).toAbsolutePath();
    }

}
