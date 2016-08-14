package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.janelia.it.workstation.gui.util.MouseHandler;
import org.janelia.it.workstation.gui.util.WrapLayout;

/**
 * A panel that shows a bunch of tags in a loose wrapping fashion.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TagCloudPanel<T> extends JPanel {
    
    private static final int DEFAULT_MAX_TAG_LENGTH = 200;
    private static final int DEFAULT_MAX_NUM_TAGS = 30;

    private static final Border PADDING_BORDER = BorderFactory.createEmptyBorder(5, 5, 5, 5);

    private List<T> tags = new ArrayList<>();

    public TagCloudPanel() {
        setLayout(new WrapLayout());
        setOpaque(false);
    }

    public List<T> getTags() {
        return tags;
    }

    public void setTags(List<T> tags) {
        if (tags==null) {
            this.tags = new ArrayList<>();
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

    protected Border getLineBorder(T tag) {
        return BorderFactory.createLineBorder(Color.black, 1);
    }

    protected Border getBorder(T tag) {
        return BorderFactory.createCompoundBorder(getLineBorder(tag), PADDING_BORDER);
    }

    protected String getTagTitle(T tag) {
        return tag.toString();
    }
    
    protected JLabel createTagLabel(T tag) {
        JLabel tagLabel = new JLabel(getTagTitle(tag));
        tagLabel.setBorder(getBorder(tag));
        tagLabel.setFont(new Font("Sans Serif", Font.BOLD, 12));
        tagLabel.setOpaque(true);
        tagLabel.setBackground(Color.white);
        tagLabel.setForeground(Color.black);
        return tagLabel;
    }

    protected JLabel createMoreLabel(int num) {
        JLabel tagLabel = new JLabel(num+" more...");
        tagLabel.setBorder(getBorder(null));
        tagLabel.setFont(new Font("Sans Serif", Font.BOLD, 12));
        tagLabel.setOpaque(true);
//        tagLabel.setForeground(Color.black);
        return tagLabel;
    }

    private void refresh() {

        removeAll();

        List<T> displayedTags = getTags();

        // Check if the text inside the tags goes over the maximum total, and limit the number of tags first by the
        // max text length, an then to a hard number limit.
        int maxNumTags = DEFAULT_MAX_NUM_TAGS;
        int c = 0;
        for (int i = 0; i<displayedTags.size(); i++) {
            T tag = displayedTags.get(i);
            c += getTagTitle(tag).length()+5; // every tag adds a constant to account for overhead (padding, border, etc)
            if (c>DEFAULT_MAX_TAG_LENGTH) {
                maxNumTags = i-1;
                break;
            }
        }
        // Only display a subset of the tags, if there are too many. 
        
        if (maxNumTags<1) {
            displayedTags = new ArrayList<>();
        }
        else if (displayedTags.size()>maxNumTags) {
            displayedTags = displayedTags.subList(0, maxNumTags);
        }

        for (final T tag : displayedTags) {

            JLabel tagLabel = createTagLabel(tag);
            add(tagLabel);

            tagLabel.addMouseListener(new MouseHandler() {

                @Override
                public void mousePressed(MouseEvent e) {
                    super.mousePressed(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    super.mouseReleased(e);
                }

                @Override
                protected void popupTriggered(MouseEvent e) {
                    if (e.isConsumed()) {
                        return;
                    }
                    showPopupMenu(e, tag);
                }

                @Override
                protected void singleLeftClicked(MouseEvent e) {
                    if (e.isConsumed()) {
                        return;
                    }
                    tagClicked(e, tag);
                }

                @Override
                protected void doubleLeftClicked(MouseEvent e) {
                    if (e.isConsumed()) {
                        return;
                    }
                    tagDoubleClicked(e, tag);
                }
            });
            tagLabel.addMouseListener(new MouseForwarder(this, "JLabel->TagCloudPanel"));
        }

        // When there are too many tags, display a "More" button that the user can click to get everything.
        if (getTags().size()>maxNumTags) {
            JLabel moreLabel = createMoreLabel(getTags().size()-maxNumTags);
            add(moreLabel);
            moreLabel.addMouseListener(new MouseHandler() {
                @Override
                protected void doubleLeftClicked(MouseEvent e) {
                    if (e.isConsumed()) {
                        return;
                    }
                    moreDoubleClicked(e);
                }
            });
        }

        // This is to conserve space in the grid if none of the images have annotations
        if (displayedTags.isEmpty()) {
            setPreferredSize(new Dimension(0, 0));
        }
        else {
            setPreferredSize(null);
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

    protected void moreDoubleClicked(MouseEvent e) {
    }
}
