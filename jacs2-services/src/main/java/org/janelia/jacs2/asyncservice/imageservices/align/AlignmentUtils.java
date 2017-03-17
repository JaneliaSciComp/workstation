package org.janelia.jacs2.asyncservice.imageservices.align;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

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

    public static void convertAffineMatToInsightMat(Path affineMatFile, Path insightMatFile) {
        PrintWriter insightWriter = null;
        try  {
            Matrix<Double> affineMat = readAffineMat(affineMatFile);
            insightWriter = new PrintWriter(Files.newBufferedWriter(insightMatFile));
            insightWriter.println("#Insight Transform File V1.0");
            insightWriter.println("#Transform 0");
            insightWriter.println("Transform: MatrixOffsetTransformBase_double_3_3");
            insightWriter.printf("Parameters: %f %f %f %f1 %f %f %f %f %f 0 0 0\n",
                    affineMat.getElem(0, 0), affineMat.getElem(1, 0), affineMat.getElem(2, 0),
                    affineMat.getElem(0, 1), affineMat.getElem(1, 1), affineMat.getElem(2, 1),
                    affineMat.getElem(0, 2), affineMat.getElem(1, 2), affineMat.getElem(2, 2));
            insightWriter.println("FixedParameters: 0 0 0");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (insightWriter != null) {
                insightWriter.close();
            }
        }
    }

    private static Matrix<Double> readAffineMat(Path affineMatFile) {
        try {
            Matrix<Double> affineMat = new Matrix<>(4, 4);
            List<String> rowLines = Files.readAllLines(affineMatFile);
            if (rowLines.size() < 4) {
                throw new IllegalArgumentException("Expected to read 4 lines from the affine matrix file");
            }
            Iterator<String> rowItr = rowLines.iterator();
            for (int row = 0; row < 4 && rowItr.hasNext(); ) {
                String rowLine = rowItr.next().trim();
                if (StringUtils.isEmpty(rowLine)) {
                    continue;
                }
                Iterator<String> colItr = Splitter.on(" ").omitEmptyStrings().split(rowLine).iterator();
                for (int col = 0; col < 4 && colItr.hasNext(); col++) {
                    affineMat.setElem(row, col, Double.parseDouble(colItr.next()));
                }
                row++;
            }
            return affineMat;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
