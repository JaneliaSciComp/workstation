package org.janelia.it.FlyWorkstation.gui.dialogs.choose;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


/**
 * A chooser that displays an arbitrary list of files.
 *
 * @author Eric Trautman
 */
public class ArbitraryFileChooser
        extends AbstractChooser<File> {

    private JList displayedList;

    public ArbitraryFileChooser(String title,
                                String description,
                                String okButtonText,
                                String okToolTipText,
                                List<File> fileList) {
        super(okButtonText, okToolTipText);
        setTitle(title);

        JPanel panel = new JPanel(new BorderLayout());

        JLabel descriptionLabel = new JLabel(description);
        panel.add(descriptionLabel, BorderLayout.NORTH);

        Vector<File> model = new Vector<File>(fileList.size());
        model.addAll(fileList);
        this.displayedList  = new JList(model);
        final JScrollPane scrollPane = new JScrollPane(this.displayedList);
        panel.add(scrollPane, BorderLayout.CENTER);

        addChooser(panel);
    }

    @Override
    protected List<File> choosePressed() {
        Object[] selectedValues = displayedList.getSelectedValues();
        List<File> selectedFiles = new ArrayList<File>(selectedValues.length);
        for (Object value : selectedValues) {
            selectedFiles.add((File) value);
        }
        return selectedFiles;
    }

}
