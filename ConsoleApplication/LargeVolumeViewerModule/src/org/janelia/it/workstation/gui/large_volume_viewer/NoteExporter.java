package org.janelia.it.workstation.gui.large_volume_viewer;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FilenameUtils;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;

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
 * -- as with swc files, data from more than one neuron may be included
 * -- before each neuron's notes, the neuron's ID will appear in a comment: # neuronID 123456
 * -- notes within each neuron may appear in any order
 * -- notes line contain coordinates (microns) and note text: xxxx yyyy zzzz notetext
 *
 *
 */

public class NoteExporter {

    /**
     * export the notes from the given neuron(s), with a filename derived from the
     * given swc file
     */
    public static void exportNotes(String swcPath, Long workspaceID, List<TmNeuronMetadata> neuronList) {

        // generate new file name; in principle, the swc file should already de-conflict,
        //  but if it hasn't, we'll...do what?

        // I love how Java makes this *so* easy...(not):
        File swcFile = new File(swcPath);
        String swcBase = FilenameUtils.removeExtension(swcFile.getName());
        Path notePath = swcFile.toPath().getParent().resolve(swcBase + ".json");
        File noteFile = notePath.toFile();

        if (noteFile.exists()) {
            // should do something here...but not clear what
        }



        System.out.println("pretending to output notes");


        // create initial json object; add "header" information
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("workspaceID", workspaceID);
        // rootNode.put("username", )



        // loop over neurons; loop over notes; add note to neuron; add neuron to master obj
        // count notes!

        // if # notes > 0, write json file from object

    }

    public static void exportNotes(String swcPath, Long workspaceID, TmNeuronMetadata neuron) {
        exportNotes(swcPath, workspaceID, Arrays.asList(neuron));
    }
}
