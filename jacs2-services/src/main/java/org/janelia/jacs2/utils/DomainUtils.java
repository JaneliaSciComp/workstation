package org.janelia.jacs2.utils;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.model.BaseEntity;
import org.janelia.jacs2.model.domain.Subject;
import org.janelia.jacs2.model.domain.annotations.MongoMapping;
import org.janelia.jacs2.model.domain.sample.LSMSampleImage;
import org.janelia.jacs2.model.domain.sample.Sample;
import org.janelia.jacs2.model.domain.sample.SampleImage;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceEvent;

import java.lang.reflect.ParameterizedType;
import java.util.List;

public class DomainUtils {
    /**
     * @param subjectKey
     * @return the subject name part of a given subject key. For example, for "group:flylight", this returns "flylight".
     */
    public static String getNameFromSubjectKey(String subjectKey) {
        if (StringUtils.isBlank(subjectKey)) {
            return null;
        }
        List<String> subjectKeyComponents = getSubjectKeyComponents(subjectKey);
        return subjectKeyComponents.get(1);
    }

    /**
     * @param subjectKey
     * @return the type part of the given subject key. For example, for "group:flylight", this returns "group".
     */
    public static String getTypeFromSubjectKey(String subjectKey) {
        if (StringUtils.isBlank(subjectKey)) {
            return null;
        }
        List<String> subjectKeyComponents = getSubjectKeyComponents(subjectKey);
        return subjectKeyComponents.get(0);
    }

    private static List<String> getSubjectKeyComponents(String subjectKey) {
        List<String> subjectKeyComponents = Splitter.on(':').trimResults().splitToList(subjectKey);
        if (subjectKeyComponents.size() != 2) {
            throw new IllegalArgumentException("Invalid subject key '" + subjectKey + "' - expected format <type>:<name>");
        }
        return subjectKeyComponents;
    }

    public static boolean isAdminOrUndefined(Subject subject) {
        return subject == null || subject.isAdmin();
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getGenericParameterType(Class<?> parameterizedClass, int paramIndex) {
        return (Class<T>)((ParameterizedType)parameterizedClass.getGenericSuperclass()).getActualTypeArguments()[paramIndex];
    }

    public static Class<? extends BaseEntity> getBaseEntityClass(String entityType) {
        switch(entityType) {
            case "JacsServiceData":
                return JacsServiceData.class;
            case "JacsServiceEvent":
                return JacsServiceEvent.class;
            case Sample.ENTITY_NAME:
                return Sample.class;
            case "SampleImage":
            case SampleImage.ENTITY_NAME:
                return SampleImage.class;
            case "LSMSampleImage":
            case LSMSampleImage.ENTITY_NAME:
                return LSMSampleImage.class;
            default:
                throw new IllegalArgumentException("Unsupported or unknown entityType: " + entityType);
        }
    }

    public static Class<?> getBasePersistedEntityClass(String entityType) {
        Class<?> entityClass = getBaseEntityClass(entityType);
        MongoMapping mongoMapping = null;
        for(Class<?> clazz = entityClass; clazz != null; clazz = clazz.getSuperclass()) {
            if (clazz.isAnnotationPresent(MongoMapping.class)) {
                return clazz; // first class encountered going up the hierarchy that has a MongoMapping annotation
            }
        }
        // if no annotated class was found assume the current class is the one being persisted.
        return entityClass;
    }

    public static MongoMapping getMapping(Class<?> objectClass) {
        MongoMapping mongoMapping = null;
        for(Class<?> clazz = objectClass; clazz != null; clazz = clazz.getSuperclass()) {
            if (clazz.isAnnotationPresent(MongoMapping.class)) {
                mongoMapping = clazz.getAnnotation(MongoMapping.class);
                break;
            }
        }
        return mongoMapping;
    }

}
