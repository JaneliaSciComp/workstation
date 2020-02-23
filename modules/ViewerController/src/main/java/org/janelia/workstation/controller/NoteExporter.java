package org.janelia.workstation.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FilenameUtils;
import org.janelia.it.jacs.shared.swc.SWCDataConverter;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmStructuredTextAnnotation;

/**
 * this class exports the notes attached to points in a neuron; the notes are exported
 * alongside an swc file, not within it (swc files have no provision for notes)
 *
 * format details:
 *
 * -- filename will match swc filename, with .swc replaced by .txt
 * -- # introduces comment (non-data) lines
 * -- all fields will be tab-delimited (because notes text may contain spaces)
 * -- one comment will be: # workspaceID 2344566
 * -- one comment will be: # username myusername
 * -- one comment may be: # offset 123 234 345
 * --- this contains the same neuron center offset as in the swc file;
 *      if absent, assume 0, 0, 0
 * -- as with swc files, data from more than one neuron may be included
 * -- before each neuron's notes, the neuron's ID will appear in a comment: # neuronID 123456
 * -- notes within each neuron may appear in any order
 * -- notes line contain coordinates (microns) and note text: xxxx yyyy zzzz notetext
 *
 * yes, the way this is wired to and interoperating with the SWCData stuff is not great
 *
 */

public class NoteExporter {

    /**
     * export the notes from the given neuron(s), with a filename derived from the
     * given swc file
     */
    public static void exportNotes(String swcPath, Long workspaceID, double[] offset,
       List<TmNeuronMetadata> neuronList, SWCDataConverter converter) {

        // generate new file name; in principle, the swc file should already de-conflict,
        //  but if it hasn't, we'll...do what?

        // I love how Java makes this *so* easy...(not):
        File swcFile = new File(swcPath);
        String swcBase = FilenameUtils.removeExtension(swcFile.getName());
        Path notePath = swcFile.toPath().getParent().resolve(swcBase + ".json");
        File noteFile = notePath.toFile();

        // if the notes file already exists, overwrite it; the implication is
        //  that the user has already (most likely) chosen to overwrite the
        //  corresponding swc file at this point

        // create initial json object; add "header" information
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("workspaceID", workspaceID);
        rootNode.put("username", AccessManager.getAccessManager().getActualSubject().getName());



        // loop over neurons; loop over notes; add note to neuron; add neuron to master obj
        ArrayNode neuronNode = mapper.createArrayNode();
        boolean hasNotes = false;
        for (TmNeuronMetadata neuron: neuronList) {
            if (neuron.getStructuredTextAnnotationMap().size() > 0) {
                hasNotes = true;
                ObjectNode neuronRoot = mapper.createObjectNode();
                neuronRoot.put("neuronID", neuron.getId());
                ArrayNode notesNode = mapper.createArrayNode();
                for (Long annID: neuron.getStructuredTextAnnotationMap().keySet()) {
                    TmGeoAnnotation ann = neuron.getGeoAnnotationMap().get(annID);
                    TmStructuredTextAnnotation textAnnotation = neuron.getStructuredTextAnnotationMap().get(annID);
                    JsonNode jsonNode = textAnnotation.getData().path("note");
                    if (!jsonNode.isMissingNode()) {
                        ArrayNode notesArray = mapper.createArrayNode();
                        // transform coordinates to microns, and subtract the offset,
                        //  so the coords match the swc file
                        double[] externalCoords =
                                converter.getExchanger().getExternal(
                                        new double[]{ann.getX(), ann.getY(), ann.getZ()}
                                );
                        for (int i=0; i<3; i++) {
                            notesArray.add(externalCoords[i] - offset[i]);
                        }
                        notesArray.add(jsonNode.asText());
                        notesNode.add(notesArray);
                    }
                }
                neuronRoot.set("notes", notesNode);
                neuronNode.add(neuronRoot);

            }
        }
        rootNode.set("neurons", neuronNode);

        ArrayNode offsetNode = mapper.createArrayNode();
        for (int i=0; i<3; i++) {
            offsetNode.add(offset[i]);
        }
        rootNode.set("offset", offsetNode);

        if (hasNotes) {
            ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
            try {
                writer.writeValue(noteFile, rootNode);
            }
            catch (IOException e) {

                // decide how to handle this


                System.out.println("note export exception");
                e.printStackTrace();
            }
        }

    }

    public static void exportNotes(String swcPath, Long workspaceID, double[] neuronCenter,
        TmNeuronMetadata neuron, SWCDataConverter converter) {
        exportNotes(swcPath, workspaceID, neuronCenter, Arrays.asList(neuron), converter);
    }
}
