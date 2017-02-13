package org.janelia.jacs2.asyncservice.common;

import com.beust.jcommander.JCommander;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

import javax.inject.Named;

public class ServiceArgs {

    public static <A extends ServiceArgs> A parse(String[] argsList, A args) {
        new JCommander(args).parse(argsList);
        return args;
    }

    public static <A extends ServiceArgs> String usage(String serviceName, A args) {
        StringBuilder usageOutput = new StringBuilder();
        JCommander jc = new JCommander(args);
        jc.setProgramName(serviceName);
        jc.usage(usageOutput);
        return usageOutput.toString();
    }

    public static <D extends ServiceDescriptor, A extends ServiceArgs> ServiceMetaData getMetadata(Class<D> descriptorClass, A args) {
        String serviceName = descriptorClass.getAnnotation(Named.class).value();
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(serviceName);
        smd.setUsage(ServiceArgs.usage(serviceName, args));
        return smd;
    }

}
