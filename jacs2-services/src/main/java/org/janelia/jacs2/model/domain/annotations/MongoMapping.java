package org.janelia.jacs2.model.domain.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for mapping a class hierarchy to a given collection in MongoDB.
 * Only the top-level class should be mapped using this annotation -
 * the subclasses will be instantiated using JsonTypeInfo.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface MongoMapping {
    String collectionName();
    String label();
}
