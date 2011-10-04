package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
import org.janelia.it.FlyWorkstation.gui.util.WrapLayout;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
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

    private static final Border paddingBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
    private static final Border lineBorder = BorderFactory.createLineBorder(Color.black, 1);
    private static final Border border = BorderFactory.createCompoundBorder(lineBorder, paddingBorder);

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

    protected JLabel createTagLabel(T tag) {
        JLabel tagLabel = new JLabel(tag.toString());
        tagLabel.setBorder(border);
        tagLabel.setFont(new Font("Sans Serif", Font.BOLD, 12));
        tagLabel.setOpaque(true);
        tagLabel.setBackground(Color.white);
        tagLabel.setForeground(Color.black);
        return tagLabel;
    }
    
    private void refresh() {

        tagLabels = new HashMap<T, JLabel>();
        removeAll();

        for (final T tag : getTags()) {
            
        	JLabel tagLabel = createTagLabel(tag);
            add(tagLabel);
            tagLabels.put(tag, tagLabel);

            tagLabel.addMouseListener(new MouseHandler() {

				@Override
				protected void popupTriggered(MouseEvent e) {
                    showPopupMenu(e, tag);
				}

				@Override
				protected void singleLeftClicked(MouseEvent e) {
                    tagClicked(e, tag);
				}

				@Override
				protected void doubleLeftClicked(MouseEvent e) {
                    tagDoubleClicked(e, tag);
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
