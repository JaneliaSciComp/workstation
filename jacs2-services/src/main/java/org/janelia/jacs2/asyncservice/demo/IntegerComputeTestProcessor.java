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
import java.util.stream.LongStream;

/**
 * Created by murphys on 2/23/17.
 */
@Named("integerComputeTest")
public class IntegerComputeTestProcessor extends AbstractServiceProcessor<Long> {

    public static class IntegerComputeTestArgs extends ServiceArgs {
        @Parameter(names = "-matrixSize", description = "Size of matrix NxN", required = false)
        Integer matrixSize;
        @Parameter(names = "-iterations", description = "Iterations per matrix multiply", required = false)
        Integer iterations;
    }

    private final int DEFAULT_MATRIX_SIZE=1000;
    private final int DEFAULT_ITERATIONS=1;

    private long resultComputationTime;

    private IntegerComputeTestProcessor.IntegerComputeTestArgs getArgs(JacsServiceData jacsServiceData) {
        return IntegerComputeTestProcessor.IntegerComputeTestArgs.parse(jacsServiceData.getArgsArray(), new IntegerComputeTestArgs());
    }

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
    }

    @Override
    protected ServiceComputation<Long> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        IntegerComputeTestArgs args=getArgs(jacsServiceData);
        int matrixSize=DEFAULT_MATRIX_SIZE;
        if (args.matrixSize!=null) {
            matrixSize=args.matrixSize;
        }
        int iterations=DEFAULT_ITERATIONS;
        if (args.iterations!=null) {
            iterations=args.iterations;
        }
        long startTime=new Date().getTime();
        jacsServiceData.getArgs();
        Random random = new Random(startTime);
        LongStream longStream = random.longs(0L, 100L);
        PrimitiveIterator.OfLong iterator=longStream.iterator();
        long[] matrix1=new long[matrixSize*matrixSize];
        long[] matrix2=new long[matrixSize*matrixSize];
        long[] result=new long[matrixSize*matrixSize];
        int position=0;
        // Create matrices
        for (int i=0;i<matrixSize;i++) {
            for (int j=0;j<matrixSize;j++) {
                matrix1[position]=iterator.nextLong();
                matrix2[position]=iterator.nextLong();
                position++;
            }
        }
        // Do multiply
        for (int i=0;i<iterations;i++) {
            for (int column2=0;column2<matrixSize;column2++) {
                for (int row1 = 0; row1 < matrixSize; row1++) {
                    long sum = 0L;
                    for (int column1 = 0; column1 < matrixSize; column1++) {
                        for (int row2 = 0; row2 < matrixSize; row2++) {
                            sum += matrix1[column1 * matrixSize + row1] * matrix2[column2 * matrixSize + row2];
                        }
                    }
                    result[column2 * matrixSize + row1] = sum;
                }
            }
        }
        long doneTime=new Date().getTime();
        resultComputationTime=doneTime-startTime;
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
        return ServiceArgs.getMetadata(this.getClass(), new IntegerComputeTestArgs());
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
