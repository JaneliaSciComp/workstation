package org.janelia.jacs2.asyncservice.demo;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.*;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

/**
 * Created by murphys on 3/2/17.
 */
@Named("floatComputeTest")
public class FloatComputeTestProcessor extends AbstractServiceProcessor<Long> {

    public static class FloatComputeTestArgs extends ServiceArgs {
        @Parameter(names = "-matrixSize", description = "Size of matrix NxN", required = false)
        Integer matrixSize;
        @Parameter(names = "-iterations", description = "Iterations per matrix multiply", required = false)
        Integer iterations;
    }

    private final int DEFAULT_MATRIX_SIZE=700;
    private final int DEFAULT_ITERATIONS=10;

    private long resultComputationTime;

    private FloatComputeTestProcessor.FloatComputeTestArgs getArgs(JacsServiceData jacsServiceData) {
        return FloatComputeTestProcessor.FloatComputeTestArgs.parse(jacsServiceData.getArgsArray(), new FloatComputeTestProcessor.FloatComputeTestArgs());
    }

    @Inject
    public FloatComputeTestProcessor (
            JacsServiceEngine jacsServiceEngine,
            ServiceComputationFactory computationFactory,
            JacsServiceDataPersistence jacsServiceDataPersistence,
            @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
            @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
            @Any Instance<ExternalProcessRunner> serviceRunners,
            Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
    }

    @Override
    protected ServiceComputation<Long> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        logger.debug("localProcessData() start");
        FloatComputeTestProcessor.FloatComputeTestArgs args=getArgs(jacsServiceData);
        int matrixSize=DEFAULT_MATRIX_SIZE;
        if (args.matrixSize!=null) {
            matrixSize=args.matrixSize;
        }
        int iterations=DEFAULT_ITERATIONS;
        if (args.iterations!=null) {
            iterations=args.iterations;
        }
        logger.debug("matrixSize="+matrixSize+", iterations="+iterations);
        long startTime=new Date().getTime();
        jacsServiceData.getArgs();
        Random random = new Random(startTime);
        DoubleStream doubleStream = random.doubles(0L, 100L);
        PrimitiveIterator.OfDouble iterator=doubleStream.iterator();
        double[] matrix1=new double[matrixSize*matrixSize];
        double[] matrix2=new double[matrixSize*matrixSize];
        double[] result=new double[matrixSize*matrixSize];
        int position=0;
        // Create matrices
        logger.debug("Creating matrices");
        for (int i=0;i<matrixSize;i++) {
            for (int j=0;j<matrixSize;j++) {
                matrix1[position]=iterator.nextDouble();
                matrix2[position]=iterator.nextDouble();
                position++;
            }
        }
        // Do multiply
        logger.debug("Doing matrix multiply");
        for (int i=0;i<iterations;i++) {
            logger.debug("Starting iteration "+i+" of "+iterations);
            for (int column2=0;column2<matrixSize;column2++) {
                for (int row1 = 0; row1 < matrixSize; row1++) {
                    long sum = 0L;
                    int row2=0;
                    for (int column1=0; column1 < matrixSize; column1++) {
                        sum += matrix1[row1 * matrixSize + column1] * matrix2[row2 * matrixSize + column2];
                        row2++;
                    }
                    result[row1 * matrixSize + column2] = sum;
                }
            }
        }
        long doneTime=new Date().getTime();
        resultComputationTime=doneTime-startTime;
        logger.debug("localProcessData() end, elapsed time ms="+resultComputationTime);
        return computationFactory.newCompletedComputation(resultComputationTime);
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return true;
    }

    @Override
    protected Long retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new FloatComputeTestProcessor.FloatComputeTestArgs());
    }

    @Override
    public Long getResult(JacsServiceData jacsServiceData) {
        return new Long(jacsServiceData.getStringifiedResult());
    }

    @Override
    public void setResult(Long result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(result.toString());
    }
}
