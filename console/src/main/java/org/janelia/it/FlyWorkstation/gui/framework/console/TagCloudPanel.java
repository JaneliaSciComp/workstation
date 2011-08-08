package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.util.WrapLayout;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A panel that shows a bunch of tags in a loose wrapping fashion.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TagCloudPanel<T> extends JPanel {

    private List<T> tags = new ArrayList<T>();
    private Map<T, JLabel> tagLabels = new HashMap<T, JLabel>();

    public TagCloudPanel() {
        setLayout(new WrapLayout());
        setOpaque(false);
    }

    public List<T> getTags() {
        return tags;
    }

    public void setTags(List<T> tags) {
        if (tags == null) {
            this.tags = new ArrayList<T>();
        }
        else {
            this.tags = tags;
        }
        refresh();
    }

    public void removeTag(T tag) {
        tags.remove(tag);
        refresh();
    }

    public void addTag(T tag) {
        tags.add(tag);
        refresh();
    }

    public Map<T, JLabel> getTagLabels() {
        return tagLabels;
    }

    private void refresh() {

        tagLabels = new HashMap<T, JLabel>();
        removeAll();

        Border paddingBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        Border lineBorder = BorderFactory.createLineBorder(Color.black, 1);
        Border border = BorderFactory.createCompoundBorder(lineBorder, paddingBorder);

        for (final T tag : getTags()) {
            JLabel tagLabel = new JLabel(tag.toString());
            tagLabel.setBorder(border);
            tagLabel.setFont(new Font("Sans Serif", Font.BOLD, 12));
            tagLabel.setOpaque(true);
            tagLabel.setBackground(Color.white);
            tagLabel.setForeground(Color.black);
            add(tagLabel);
            tagLabels.put(tag, tagLabel);

            tagLabel.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {

                    if (e.isPopupTrigger()) {
                        showPopupMenu(e, tag);
                    }
                    // This masking is to make sure that the right button is being double clicked, not left and then right or right and then left
                    else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 && (e.getModifiersEx() | InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                        tagDoubleClicked(e, tag);
                    }
                    else if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                        tagClicked(e, tag);
                    }
                }

                public void mousePressed(MouseEvent e) {
                    // We have to also listen for mousePressed because OSX generates the popup trigger here
                    // instead of mouseReleased like any sane OS.
                    if (e.isPopupTrigger()) {
                        showPopupMenu(e, tag);
                    }
                }
            });
        }

        revalidate();
        repaint();
    }

    protected void showPopupMenu(MouseEvent e, T tag) {
    }

    protected void tagDoubleClicked(MouseEvent e, T tag) {
    }

    protected void tagClicked(MouseEvent e, T tag) {
    }
}
