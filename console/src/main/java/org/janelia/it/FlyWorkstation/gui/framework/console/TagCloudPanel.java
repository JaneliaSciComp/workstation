package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.janelia.it.FlyWorkstation.gui.util.WrapLayout;

/**
 * A panel that shows a bunch of tags in a loose wrapping fashion.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TagCloudPanel<T> extends JPanel {

    private List<T> tags = new ArrayList<T>();
    private Map<T,JLabel> tagLabels = new HashMap<T,JLabel>();
    
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

	public Map<T, JLabel> getTagLabels() {
		return tagLabels;
	}
    
    private void refresh() {

    	tagLabels = new HashMap<T,JLabel>();
        removeAll();
        
        Border paddingBorder = BorderFactory.createEmptyBorder(5,5,5,5);
        Border lineBorder = BorderFactory.createLineBorder(Color.black, 1);
        Border border = BorderFactory.createCompoundBorder(lineBorder, paddingBorder);
        
        for(final T tag : getTags()) {
            JLabel tagLabel = new JLabel(tag.toString());
            tagLabel.setBorder(border);
            tagLabel.setFont(new Font("Sans Serif", Font.BOLD, 12));
            tagLabel.setOpaque(true);
            tagLabel.setBackground(Color.white);
            tagLabel.setForeground(Color.black);
            add(tagLabel);
            tagLabels.put(tag, tagLabel);
        }
        
        revalidate();
        repaint();
    }
	
	
}
