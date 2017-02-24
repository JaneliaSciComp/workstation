package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 * Created by murphys on 2/23/17.
 */
public class IntegerComputeTestProcessor extends AbstractServiceProcessor<Integer> {

    @Inject
    public IntegerComputeTestProcessor (
            JacsServiceEngine jacsServiceEngine,
            ServiceComputationFactory computationFactory,
            JacsServiceDataPersistence jacsServiceDataPersistence,
            @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
            @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
            @Any Instance<ExternalProcessRunner> serviceRunners,
            Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);

        if (logger == null) {
            System.out.println("IntegerComputeTestProcessor() logger is null");
        } else {
            System.out.println("IntegerComputeTestProcessor() logger is NOT null");
        }

    }

    @Override
    protected ServiceComputation<Integer> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return false;
    }

    @Override
    protected Integer retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public Integer getResult(JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public void setResult(Integer result, JacsServiceData jacsServiceData) {

    }
}
