package org.janelia.workstation.core.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.janelia.model.domain.DomainObject;

/**
 * Utilities for extracting information from the domain model for view purposes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainModelViewUtils {

    private final static DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd hh:mma");

    private static final String OLD_MODEL_PATTERN = "org\\.janelia.it\\.jacs\\.model";
    private static final String NEW_MODEL = "org.janelia.model";
    private static final String OLD_SECURITY_MODEL_PATTERN = "org\\.janelia\\.it\\.jacs\\.model\\.domain\\.subjects"; 
    private static final String NEW_SECURITY_MODEL = "org.janelia.model.security";
    private static final String OLD_BROWSER_MODEL_PATTERN = "org\\.janelia\\.it\\.workstation\\.browser\\.model";
    private static final String NEW_BROWSER_MODEL = "org.janelia.workstation.core.model";
    
    /**
     * Convert any occurrences of the old model packages to the new organization.
     */
    public static String convertModelPackages(String str) {
        if (str==null) return null;
        return str
                .replaceAll(OLD_MODEL_PATTERN, NEW_MODEL)
                .replaceAll(OLD_SECURITY_MODEL_PATTERN, NEW_SECURITY_MODEL)
                .replaceAll(OLD_BROWSER_MODEL_PATTERN,NEW_BROWSER_MODEL);
    }
    
    public static String getDateString(Date date) {
        if (date==null) return "";
        return dateFormatter.format(date).toLowerCase();
    }

    public static List<DomainObject> map(DomainObject domainObject, MappingType targetType) throws Exception {
        DomainObjectMapper mapper = new DomainObjectMapper(Arrays.asList(domainObject));
        return mapper.map(targetType);
    }
}
