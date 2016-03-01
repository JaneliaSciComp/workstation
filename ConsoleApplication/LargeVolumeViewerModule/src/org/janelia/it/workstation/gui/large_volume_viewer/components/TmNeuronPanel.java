package org.janelia.it.workstation.gui.large_volume_viewer.components;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;

/**
 * Panel to show contents of a TmNeuron, by data only.  There are far better
 * visualizations based on spatial interpretation of points.  This is only
 * to give a sanity check.
 * 
 * @author fosterl
 */
public class TmNeuronPanel extends JPanel {
    private static final String LINE_SEP = System.getProperty("line.separator");
    private TmNeuron neuron;
    private JTextArea textArea;
    public TmNeuronPanel(TmNeuron neuron) {
        this.neuron = neuron;
        initGUI();
    }

    private void initGUI() {
        textArea = new JTextArea();
        JLabel nameLabel = new JLabel(neuron.getName());
        StringBuilder bldr = new StringBuilder();
        bldr.append("ID: ").append(neuron.getId()).append(LINE_SEP);
        bldr.append("Workspace ID: ").append(neuron.getWorkspaceId()).append(LINE_SEP);
        bldr.append("Created: ").append(neuron.getCreationDate()).append(LINE_SEP);
        bldr.append("Owner: ").append(neuron.getOwnerKey()).append(LINE_SEP);
        bldr.append("Geo Annotations").append(LINE_SEP);
        Map<Long,TmGeoAnnotation> annoMap = neuron.getGeoAnnotationMap();
        for (Long geoAnnoId: annoMap.keySet()) {
            final TmGeoAnnotation geoAnno = annoMap.get(geoAnnoId);
            bldr.append("    ").append(geoAnnoId).append("->").append(display(geoAnno)).append(LINE_SEP);
            if (! annoMap.keySet().contains(geoAnno.getParentId())) {
                if (! geoAnno.getParentId().equals(neuron.getId())) {
                    bldr.append("*** ERROR: ").append(geoAnno.getId())
                        .append(" has invalid parent. ")
                        .append(geoAnno.getParentId())
                        .append(" Neither geo anno nor neuron.")
                        .append(LINE_SEP);
                }
            }
        }
        
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
        textArea.setText(bldr.toString());
        
        this.setLayout(new BorderLayout());
        this.add( nameLabel, BorderLayout.NORTH );
        this.add( new JScrollPane(textArea), BorderLayout.CENTER );
        
        this.setPreferredSize(new Dimension(800, 500));
    }
    
    private StringBuilder display(TmGeoAnnotation tmGeoAnno) {
        StringBuilder geoAnnoBldr = new StringBuilder();
        geoAnnoBldr.append(" XYZ=(" + tmGeoAnno.getX() + "," + tmGeoAnno.getY() + "," + tmGeoAnno.getZ() + ")");
        geoAnnoBldr.append(" Parent=" + tmGeoAnno.getParentId());
        geoAnnoBldr.append(" Radius=" + tmGeoAnno.getRadius());
        if (tmGeoAnno.isRoot()) {
            geoAnnoBldr.append(" o--");
        }
        else if (tmGeoAnno.isBranch()) {
            geoAnnoBldr.append(" --<");
        }
        else if (tmGeoAnno.isEnd()) {
            geoAnnoBldr.append(" --o");
        }
        else if (tmGeoAnno.isLink()) {
            geoAnnoBldr.append(" ---");
        }
        else {
            geoAnnoBldr.append(" ???");
        }                
                
        return geoAnnoBldr;
    }
}
