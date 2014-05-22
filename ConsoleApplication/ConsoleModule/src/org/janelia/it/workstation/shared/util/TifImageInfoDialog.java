package org.janelia.it.workstation.shared.util;

import loci.formats.tiff.TiffParser;

import javax.swing.*;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/17/11
 * Time: 5:00 PM
 */
public class TifImageInfoDialog extends JDialog {
    public TifImageInfoDialog(JFrame parentFrame, String imageFilePath) {
        super(parentFrame, "Image Info", true);
        try {
            TiffParser parser = new TiffParser(imageFilePath);
//            parser.
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

