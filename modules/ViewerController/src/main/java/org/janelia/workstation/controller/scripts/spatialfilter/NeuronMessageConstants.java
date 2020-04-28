package org.janelia.workstation.controller.scripts.spatialfilter;

/**
 * Created by schauderd on 11/14/17.
 */
public class NeuronMessageConstants {
    public static class Headers {
        public static final String USER = "user";
        public static final String TARGET_USER = "target_user";
        public static final String NEURONIDS = "targetIds";
        public static final String WORKSPACE = "workspace";
        public static final String TYPE = "msgType";
        public static final String DECISION = "decision";
        public static final String DESCRIPTION = "description";
    }

    public enum MessageType {
        NEURON_CREATE,
        NEURON_SAVE_NEURONDATA,
        NEURON_DELETE,
        REQUEST_NEURON_OWNERSHIP,
        REQUEST_NEURON_ASSIGNMENT,
        NEURON_OWNERSHIP_DECISION,
        ERROR_PROCESSING
    }
}
