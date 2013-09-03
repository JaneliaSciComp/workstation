package org.janelia.it.FlyWorkstation.shared.annotations;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 8/28/13
 * Time: 3:08 PM
 *
 * Annotate with this to indicate that a class or method is NOT safe to be run under multiple threads.  Look
 * at/for this annotation if you are thinking of running multiple threads against a given class, or if you are trying
 * to debug something multithreaded.  Specifically NOT inherited. If "why" was given, and the problem is solved
 * at a lower granularity, OK, Then.  Likewise, if a caller undertakes to be multithreaded, it must ensure that
 * the problem highlighted (on this annotated callee) becomes obviated through its own implementation.
 *
 * This was suggested by Brian Goetz in his book Effective Concurrent Java.
 */
@Retention(RetentionPolicy.SOURCE)
@Target( { ElementType.TYPE, ElementType.METHOD } )
public @interface NotThreadSafe {
    String why() default "";
}
