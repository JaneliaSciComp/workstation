package org.janelia.jacs2.fileservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.impl.AbstractLocalProcessComputation;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Named("fileCopyService")
public class FileCopyComputation extends AbstractLocalProcessComputation {

    private static final String DY_LIBRARY_PATH_VARNAME = "LD_LIBRARY_PATH";

    @PropertyValue(name = "Executables.ModuleBase")
    @Inject
    private String executablesBaseDir;
    @PropertyValue(name = "VAA3D.LibraryPath")
    @Inject
    private String libraryPath;
    @PropertyValue(name = "Convert.ScriptPath")
    @Inject
    private String scriptName;

    static class FileCopyArgs {
        @Parameter(names = "-src", description = "Source file name", required = true)
        String sourceFilename;
        @Parameter(names = "-dst", description = "Destination file name or location", required = true)
        String targetFilename;
        @Parameter(names = "-mv", arity = 0, description = "If used the file will be moved to the target", required = false)
        boolean deleteSourceFile = false;
    }


    @Override
    protected List<String> prepareCommandLine(JacsServiceData jacsServiceData) {
        FileCopyArgs fileCopyArgs = new FileCopyArgs();
        new JCommander(fileCopyArgs).parse(jacsServiceData.getArgsAsArray());

        ImmutableList.Builder cmdLineBuilder = new ImmutableList.Builder<>();
        cmdLineBuilder.add(jacsServiceData.getServiceCmd());
        cmdLineBuilder.add(jacsServiceData.getId().toString());
        cmdLineBuilder.add(jacsServiceData.getName());
        if (CollectionUtils.isNotEmpty(jacsServiceData.getArgs())) {
            cmdLineBuilder.addAll(jacsServiceData.getArgs());
        }
        return cmdLineBuilder.build();
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData si) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedLibraryPath());
    }

    private String getUpdatedLibraryPath() {
        Optional<String> currentLibraryPath = getEnvVar(DY_LIBRARY_PATH_VARNAME);
        if (currentLibraryPath.isPresent()) {
            return libraryPath + ":" + currentLibraryPath.get();
        } else {
            return libraryPath;
        }
    }

}
