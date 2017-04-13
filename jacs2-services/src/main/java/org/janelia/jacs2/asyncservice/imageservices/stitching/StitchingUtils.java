package org.janelia.jacs2.asyncservice.imageservices.stitching;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StitchingUtils {

    public static List<String> getMaxTileImageGroup(Path tileGroupsFile) {
        try {
            List<String> groupsFileContent = Files.readAllLines(tileGroupsFile);
            List<List<String>> groups = new ArrayList<>();
            List<String> currentGroup = null;

            List<String> maxGroup = new ArrayList<>();
            for (String l : groupsFileContent) {
                if (StringUtils.isBlank(l)) continue;
                if (l.startsWith("# tiled image group")) {
                    if (CollectionUtils.isNotEmpty(currentGroup)) {
                        groups.add(currentGroup);
                        if (CollectionUtils.size(maxGroup) < currentGroup.size()) {
                            maxGroup = currentGroup;
                        }
                    }
                    currentGroup = new ArrayList<>();
                } else {
                    if (currentGroup == null) continue;
                    currentGroup.add(l);
                }
            }
            if (CollectionUtils.isNotEmpty(currentGroup)) {
                groups.add(currentGroup);
                if (CollectionUtils.size(maxGroup) < currentGroup.size()) {
                    maxGroup = currentGroup;
                }
            }
            return maxGroup;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
