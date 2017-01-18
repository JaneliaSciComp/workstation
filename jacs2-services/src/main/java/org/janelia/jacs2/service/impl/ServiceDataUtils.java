package org.janelia.jacs2.service.impl;

import com.google.common.base.Splitter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.model.service.JacsServiceData;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceDataUtils {

    public static List<File> stringToFileList(String s) {
        if (StringUtils.isNotBlank(s)) {
            return Splitter.on(",").omitEmptyStrings().trimResults()
                    .splitToList(s)
                    .stream()
                    .map(File::new)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public static String fileListToString(List<File> fileList) {
        if (CollectionUtils.isNotEmpty(fileList)) {
            return fileList.stream()
                    .filter(r -> r != null)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(","));
        } else {
            return null;
        }
    }

    public static File stringToFile(String s) {
        if (StringUtils.isNotBlank(s)) {
            return new File(s);
        } else {
            return null;
        }
    }

    public static String fileToString(File result) {
        if (result != null) {
            return result.getAbsolutePath();
        } else {
            return null;
        }
    }

}
