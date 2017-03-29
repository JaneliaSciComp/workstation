package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

public abstract class AbstractServiceProcessor<T> implements ServiceProcessor<T> {

    protected final String defaultWorkingDir;
    protected final Logger logger;

    public AbstractServiceProcessor(String defaultWorkingDir, Logger logger) {
        this.defaultWorkingDir = defaultWorkingDir;
        this.logger = logger;
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
        if (StringUtils.isNotBlank(executionContext.getOutputPath())) {
            jacsServiceDataBuilder.setOutputPath(executionContext.getOutputPath());
        }
        if (StringUtils.isNotBlank(executionContext.getErrorPath())) {
            jacsServiceDataBuilder.setErrorPath(executionContext.getErrorPath());
        }
        jacsServiceDataBuilder.copyResourcesFrom(executionContext.getResources());
        executionContext.getWaitFor().forEach(jacsServiceDataBuilder::addDependency);
        if (executionContext.getParentServiceData() != null) {
            executionContext.getParentServiceData().getDependeciesIds().forEach(jacsServiceDataBuilder::addDependencyId);
        }
        return jacsServiceDataBuilder.build();
    }

    protected Path getWorkingDirectory(JacsServiceData jacsServiceData) {
        if (StringUtils.isNotBlank(jacsServiceData.getWorkspace())) {
            return Paths.get(jacsServiceData.getWorkspace());
        } else if (StringUtils.isNotBlank(defaultWorkingDir)) {
            return getServicePath(defaultWorkingDir, jacsServiceData);
        } else {
            return getServicePath(System.getProperty("java.io.tmpdir"), jacsServiceData);
        }
    }

    protected void setOutputPath(JacsServiceData jacsServiceData) {
        if (StringUtils.isBlank(jacsServiceData.getOutputPath())) {
            jacsServiceData.setOutputPath(getServicePath(
                    getWorkingDirectory(jacsServiceData).toString(),
                    jacsServiceData,
                    String.format("%s-stdout.txt", jacsServiceData.getName(), jacsServiceData.hasId() ? "-" + jacsServiceData.getId() : "")).toString());
        }
    }

    protected void setErrorPath(JacsServiceData jacsServiceData) {
        if (StringUtils.isBlank(jacsServiceData.getErrorPath())) {
            jacsServiceData.setErrorPath(getServicePath(
                    getWorkingDirectory(jacsServiceData).toString(),
                    jacsServiceData,
                    String.format("%s-stderr.txt", jacsServiceData.getName(), jacsServiceData.hasId() ? "-" + jacsServiceData.getId() : "")).toString());
        }
    }

    protected Path getServicePath(String baseDir, JacsServiceData jacsServiceData, String... more) {
        ImmutableList.Builder<String> pathElemsBuilder = ImmutableList.<String>builder()
                .add(jacsServiceData.getName());
        if (jacsServiceData.hasId()) {
            pathElemsBuilder.add(jacsServiceData.getId().toString());
        }
        pathElemsBuilder.addAll(Arrays.asList(more));
        return Paths.get(baseDir, pathElemsBuilder.build().toArray(new String[0])).toAbsolutePath();
    }

}
