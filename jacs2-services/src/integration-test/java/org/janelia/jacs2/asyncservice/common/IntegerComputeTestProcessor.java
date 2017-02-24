package org.janelia.jacs2.asyncservice.common;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Date;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.stream.LongStream;

/**
 * Created by murphys on 2/23/17.
 */
public class IntegerComputeTestProcessor extends AbstractServiceProcessor<Integer> {

    public final int MATRIX_SIZE=1000;


    static class IntegerComputeTestArgs extends ServiceArgs {
        @Parameter(names = "-matrixSize", description = "Size of matrix per dimension", required = false)
        Integer matrixSize;
    }

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
    protected ServiceComputation<Integer> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        jacsServiceData.getArgs();
        Random random = new Random(new Date().getTime());
        LongStream longStream = random.longs(0L, 100L);
        PrimitiveIterator.OfLong iterator=longStream.iterator();
        long[] matrix1=new long[MATRIX_SIZE*MATRIX_SIZE];
        long[] matrix2=new long[MATRIX_SIZE*MATRIX_SIZE];
        long[] result=new long[MATRIX_SIZE*MATRIX_SIZE];
        int position=0;
        // Create matrices
        for (int i=0;i<MATRIX_SIZE;i++) {
            for (int j=0;j<MATRIX_SIZE;j++) {
                matrix1[position]=iterator.nextLong();
                matrix2[position]=iterator.nextLong();
                position++;
            }
        }
        // Do multiply
        for (int column2=0;column2<MATRIX_SIZE;column2++) {
            for (int row1=0;row1<MATRIX_SIZE;row1++) {
                long sum=0L;
                for (int column1=0;column1<MATRIX_SIZE;column1++) {
                    for (int row2=0;row2<MATRIX_SIZE;row2++) {
                        sum+=matrix1[column1*MATRIX_SIZE+row1]*matrix2[column2*MATRIX_SIZE+row2];
                    }
                }
                result[column2*MATRIX_SIZE+row1]=sum;
            }
        }
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
    public ServiceMetaData getMetadata() {
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
