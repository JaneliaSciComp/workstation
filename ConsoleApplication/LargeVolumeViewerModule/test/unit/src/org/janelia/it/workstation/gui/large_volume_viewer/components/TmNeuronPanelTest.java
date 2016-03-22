package org.janelia.it.workstation.gui.large_volume_viewer.components;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiled_microscope_protobuf.TmProtobufExchanger;

/**
 * Test the TmNeuronPanel component.
 * @author fosterl
 */
public class TmNeuronPanelTest {
    public static void main(String[] args) throws Exception {
        new TmNeuronPanelTest().exec();
    }
    
    private TmNeuronPanel panel;
    public void exec() {
        final JFrame frame = new JFrame("Neuron Panel");
        frame.setSize(1000, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Build the neuron.
        //TmObjectCreationHelper helper = new TmObjectCreationHelper();        
        TmNeuron neuron = new TmNeuron();
        neuron.setId(1113388343L);
        neuron.setName("Dummy Neuron");
        neuron.setCreationDate(new java.util.Date());
        neuron.setOwnerKey("group:me");
        neuron.setWorkspaceId(2222222L);

        TmGeoAnnotation tmGeoAnno = new TmGeoAnnotation();
        tmGeoAnno.setId(9183489343L);
        tmGeoAnno.setComment("Dummy Data");
        tmGeoAnno.setRadius(2.5);
        tmGeoAnno.setX(77000.0);
        tmGeoAnno.setY(44000.0);
        tmGeoAnno.setZ(15000.0);
        tmGeoAnno.setIndex(1);
        tmGeoAnno.setNeuronId(1113388343L);
        tmGeoAnno.setParentId(1113388343L);

        neuron.getGeoAnnotationMap().put(9183489343L, tmGeoAnno);
        neuron.addRootAnnotation(9183489343L);

        panel = new TmNeuronPanel(neuron);

        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);

        final JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        JButton okButton = new JButton("OK");
        
        JPanel textInputPanel = new JPanel();
        textInputPanel.setLayout(new GridLayout(1, 2));

        final JTextField wsField = new JTextField();
        wsField.setBorder(new TitledBorder("Workspace ID"));
        final JTextField pbNeuronIdField = new JTextField();
        pbNeuronIdField.setBorder(new TitledBorder("Neuron ID"));
        textInputPanel.add(wsField);
        textInputPanel.add(pbNeuronIdField);
        inputPanel.add(textInputPanel, BorderLayout.CENTER);
        inputPanel.add(okButton, BorderLayout.EAST);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                Long neuronId = null;
                try {
                    neuronId = Long.parseLong(pbNeuronIdField.getText().trim());
                    Long workspaceId = Long.parseLong(wsField.getText().trim());
                    final TmNeuron newNeuron = loadNeuron(workspaceId, neuronId);
                    if (newNeuron == null) {
                        JOptionPane.showMessageDialog(inputPanel, "Invalid id: " + neuronId + " not found.", "Not Found", JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                        frame.setTitle(newNeuron.getName());
                        frame.remove(panel);
                        panel = new TmNeuronPanel(newNeuron);
                        frame.add(panel, BorderLayout.CENTER);
                        frame.validate();
                        frame.invalidate();
                        frame.repaint();
                    }
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(inputPanel, "Invalid id: " + neuronId, "Invalid ID", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(inputPanel, "Error " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        frame.add(inputPanel, BorderLayout.NORTH);

        frame.setVisible(true);
    }
    
    /** Hunt up neuron's tmneuron def. */
    private TmNeuron loadNeuron(Long workspaceId, Long neuronId) throws Exception {
        byte[] packagedBytes = getEJB().getB64DecodedEntityDataValue(workspaceId, neuronId, EntityConstants.ATTRIBUTE_PROTOBUF_NEURON);
        if (packagedBytes == null) {
            return null;
        }
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        return exchanger.deserializeNeuron(packagedBytes);
    }
    
    private EntityBeanRemote getEJB() throws Exception {
        Hashtable environment = new Hashtable();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        environment.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        environment.put(Context.PROVIDER_URL, "jnp://foster-ws.janelia.priv:1199");
        InitialContext context = new InitialContext(environment);
        System.out.println("-->> connected successfully to server");

        System.out.println("\n*************************************\n");
        EntityBeanRemote entityBean = (EntityBeanRemote) context.lookup("compute/EntityEJB/remote"); 
        return entityBean;
    }
}
