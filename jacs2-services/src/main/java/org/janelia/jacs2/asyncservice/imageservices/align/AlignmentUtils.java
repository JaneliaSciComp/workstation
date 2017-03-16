package org.janelia.jacs2.asyncservice.imageservices.align;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.Iterator;

public class AlignmentUtils {

    public static AlignmentConfiguration parseAlignConfig(String configFile) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(AlignmentConfiguration.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (AlignmentConfiguration) jaxbUnmarshaller.unmarshal(new File(configFile));
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static ImageCoordinates parseCoordinates(String coordStr) {
        ImageCoordinates coord = new ImageCoordinates();
        Iterator<String> coordItr = Splitter.on('x').split(coordStr).iterator();
        if (coordItr.hasNext()) {
            coord.x = Double.parseDouble(coordItr.next());
        }
        if (coordItr.hasNext()) {
            coord.y = Double.parseDouble(coordItr.next());
        }
        if (coordItr.hasNext()) {
            coord.z = Double.parseDouble(coordItr.next());
        }
        return coord;
    }

    public static AlignmentInput parseInput(String inputStr) {
        AlignmentInput alignmentInput = new AlignmentInput();

        if (StringUtils.isBlank(inputStr)) return alignmentInput;

        Iterator<String> inputItr = Splitter.on(',').split(inputStr).iterator();
        if (inputItr.hasNext()) {
            alignmentInput.name = inputItr.next();
        }
        if (inputItr.hasNext()) {
            alignmentInput.channels = inputItr.next();
        }
        if (inputItr.hasNext()) {
            alignmentInput.ref = inputItr.next();
        }
        if (inputItr.hasNext()) {
            alignmentInput.res = inputItr.next();
        }
        if (inputItr.hasNext()) {
            alignmentInput.dims = inputItr.next();
        }
        return alignmentInput;
    }

}
