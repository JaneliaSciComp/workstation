package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.janelia.it.workstation.browser.gui.lasso.BufferedImageCanvas;
import org.janelia.it.workstation.browser.gui.lasso.BufferedImagePlus;
import org.janelia.it.workstation.browser.gui.lasso.Filler;
import org.janelia.it.workstation.browser.gui.lasso.ImagePlus;
import org.janelia.it.workstation.browser.gui.lasso.ImageProcessor;
import org.janelia.it.workstation.browser.gui.lasso.Roi;

public class TestLasso extends JPanel {
    
    List<Point> points = new ArrayList<Point>();

    ImagePlus imagePlus;
    JButton maskButton;
    
    public TestLasso() throws IOException {
        
        setLayout(new BorderLayout());
        
        BufferedImage img = ImageIO.read(new File("/Users/rokickik/Desktop/screenshot.png"));
        this.imagePlus = new BufferedImagePlus(img);
        add((BufferedImageCanvas)imagePlus.getCanvas());

        maskButton = new JButton("Mask");
        maskButton.setFocusable(false);
        maskButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                 
                Filler fill = new Filler();
                prepareProcessor(imagePlus.getImageProcessor(), imagePlus);
                fill.setup("outside", imagePlus);
                fill.run(imagePlus.getImageProcessor());
                
            }
        });
        
        add(getPanel(), BorderLayout.CENTER);
        add(maskButton, BorderLayout.SOUTH);
    }

    /** prepare an ImageProcessor by setting roi and CalibrationTable.
     */
    private void prepareProcessor(ImageProcessor ip, ImagePlus imp) {
        ImageProcessor mask = imp.getMask();
        Roi roi = imp.getRoi();
        if (roi!=null && roi.isArea())
            ip.setRoi(roi);
        else
            ip.setRoi((Roi)null);
//        if (imp.getStackSize()>1) {
//            ImageProcessor ip2 = imp.getProcessor();
//            double min1 = ip2.getMinThreshold();
//            double max1 = ip2.getMaxThreshold();
//            double min2 = ip.getMinThreshold();
//            double max2 = ip.getMaxThreshold();
//            if (min1!=ImageProcessor.NO_THRESHOLD && (min1!=min2||max1!=max2))
//                ip.setThreshold(min1, max1, ImageProcessor.NO_LUT_UPDATE);
//        }
        //float[] cTable = imp.getCalibration().getCTable();
        //ip.setCalibrationTable(cTable);
    }
    
    public JPanel getPanel() {
        return (BufferedImageCanvas)imagePlus.getCanvas();
    }
//    protected void paintComponent(Graphics g) {
//        super.paintComponent(g);
//        Graphics2D g2 = (Graphics2D) g;
//        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g2.setPaint(Color.blue);
//        for (int j = 0; j < points.size() - 1; j++) {
//            Point p = points.get(j);
//            Point next = points.get(j + 1);
//            g2.draw(new Line2D.Double(p, next));
//        }
//        g2.setPaint(Color.red);
//        for (int j = 0; j < points.size(); j++) {
//            Point p = points.get(j);
//            g2.fill(new Ellipse2D.Double(p.x - 2, p.y - 2, 4, 4));
//        }
//    }
//
//    public Dimension getPreferredSize() {
//        return new Dimension(imagePlus.getWidth(), imagePlus.getHeight());
//    }


//    private MouseListener ml = new MouseAdapter() {
//        int size = 5;
//        int PROXIMAL = 5;
//
//        public void mousePressed(MouseEvent e) {
//            Point p = e.getPoint();
//            if (points.size() == size - 1) {
//                if (points.get(0).distance(p) <= PROXIMAL) {
//                    points.add((Point) points.get(0).clone());
//                }
//                else {
//                    int retVal = JOptionPane.showConfirmDialog(null, "Do you want to keep this?", "Confirm", JOptionPane.YES_NO_OPTION);
//                    if (retVal == JOptionPane.YES_OPTION) {
//                        points.add((Point) points.get(0).clone());
//                    }
//                    else {
//                        points.clear();
//                    }
//                }
//            }
//            else if (points.size() < size) {
//                points.add(p);
//            }
//            else if (e.getClickCount() == 2) {
//                points.clear();
//            }
//            repaint();
//        }
//    };

    public static void main(String[] args) throws Exception {
        TestLasso lasso = new TestLasso();
//        lasso.addMouseListener(lasso.ml);
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add(lasso);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}