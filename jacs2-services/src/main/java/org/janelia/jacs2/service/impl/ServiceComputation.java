package org.janelia.jacs2.service.impl;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * ServiceComputationStage represents a certain service computation stage.
 *
 * @param <S> service type
 * @param <R> stage result type
 */
public interface ServiceComputation<S, R> {
    /**
     * @return the service to which this stage is related.
     */
    S getService();

    /**
     * @return the result if the stage is completed.
     */
    R get();

    ServiceComputation<S, R> applyFunction(Function<S, ? extends R> fn);

    ServiceComputation<S, R> applyComputation(ServiceComputation<S, ? extends R> fn);

    /**
     * Applies function fn after successful completion of this stage.
     * @param fn
     * @param <S1>
     * @param <R1>
     * @return
     */
    <S1, R1> ServiceComputation<S1, R1> thenApply(S1 s1, BiFunction<S1, ? super R, ? extends R1> fn);

    /**
     * When both this and the fn computation stage complete applyComputation their results
     * @param fn
     * @param <S1>
     * @param <R1>
     * @return
     */
    <S1, R1> ServiceComputation<S1, R1> thenCompose(S1 s1, BiFunction<S1, R, ServiceComputation<S1, R1>> fn);

    /**
     * Returns a stage with the same service and result that executes the action after this stage completes with any result.
     * @param action
     * @return
     */
    ServiceComputation<S, R> whenComplete(BiConsumer<? super R, ? super Throwable> action);
}
