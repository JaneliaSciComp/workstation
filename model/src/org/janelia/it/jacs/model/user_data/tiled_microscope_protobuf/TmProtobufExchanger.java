/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.user_data.tiled_microscope_protobuf;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import javax.annotation.concurrent.ThreadSafe;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;

/**
 * Exchanges data between byte array and Tile Microscope.  At time of writing,
 * all contents of a TmNeuron are supported.
 * 
 * All operations here must be thread safe.
 *
 * @author fosterl
 */
@ThreadSafe
public class TmProtobufExchanger {
    private Schema<TmNeuron> schema = null;
    
    public TmProtobufExchanger() {
        try {
            schema = RuntimeSchema.getSchema(TmNeuron.class);
        } catch (Exception ex) {
            if (schema == null) {
                throw new RuntimeException("Failed to get schema for " + TmNeuron.class.getCanonicalName());
            }
        }
    }

    /**
     * Use protobuf mechanism to turn an array of raw data into a fully
     * inflated neuron.
     * 
     * @param protobufData found from serialized source.
     * @return neuron as it should have existed prior to serialization.
     * @throws Exception from any called methods.
     */
    public TmNeuron deserializeNeuron( byte[] protobufData ) throws Exception {
		return deserializeNeuron( protobufData, null );
    }
    
    /**
     * Use protobuf mechanism to turn an array of raw data into a fully
     * inflated neuron.
     * 
     * @param protobufData found from serialized source.
	 * @param oldNeuron existing, pre-instantiated neuron to populate.
     * @return neuron as it should have existed prior to serialization.
     * @throws Exception from any called methods.
     */
    public TmNeuron deserializeNeuron( byte[] protobufData, TmNeuron oldNeuron ) throws Exception {
        TmNeuron tmNeuron = null;
        final LinkedBuffer buffer = LinkedBuffer.allocate();
        try {
			if (oldNeuron == null) {
	            tmNeuron = schema.newMessage();
			}
			else {
				tmNeuron = oldNeuron;
			}
            clearNeuronValues(tmNeuron);
            ProtobufIOUtil.mergeFrom(protobufData, tmNeuron, schema);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            buffer.clear();
        }
        return tmNeuron;
    }

    /**
     * Turn a neuron into a series of bytes.
     * 
     * @param neuron what to store
     * @return array of bytes, suitable for
     * @see #deserializeNeuron(byte[]) 
     * @throws Exception from any called methods.
     */
    public byte[] serializeNeuron( TmNeuron neuron ) throws Exception {
        byte[] protobuf = null;
        final LinkedBuffer buffer = LinkedBuffer.allocate();
        // Populate a byte array from serialized data.
            try {
                    protobuf = ProtobufIOUtil.toByteArray(neuron, schema, buffer);
        } finally {
                    buffer.clear();
                }
        return protobuf;
    }

    private void clearNeuronValues(TmNeuron tmNeuron) {
        // NOTE: merge-from will cause duplication of collections within
        // the neuron, unless clearing is done first.
        tmNeuron.getAnchoredPathMap().clear();
        tmNeuron.getGeoAnnotationMap().clear();
        tmNeuron.clearRootAnnotations();
        tmNeuron.getStructuredTextAnnotationMap().clear();
    }
    
}
