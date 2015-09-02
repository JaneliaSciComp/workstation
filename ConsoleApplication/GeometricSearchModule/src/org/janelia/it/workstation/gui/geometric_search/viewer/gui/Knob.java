package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

/**
 * Created by murphys on 9/1/2015.
 *
 *  From GNU Lesser GPL re:
 *
 *  adapted from https://code.google.com/p/synthetizer-studio/
 *
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Formatter;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class Knob extends JPanel implements MouseListener, MouseMotionListener
{
    private final Logger logger = LoggerFactory.getLogger(Knob.class);

    private static final int PREF_WIDTH=50;
    private static final int PREF_HEIGHT=50;

    private Image  background2_;
    private Image  pointer_;
    private double value_;
    private Point  dragStart_;
    private String name_;

    private double min=-1.0;
    private double max=1.0;

    boolean showValue=true;
    boolean showName=true;

    public Knob(String name)
    {
        super();
        setup(name);
    }

    public Knob(String name, double min, double max, double initialValue) {
        super();
        this.min=min;
        this.max=max;
        setValue(initialValue);
        setup(name);
    }

    private void setup(String name) {
        name_=name;
        setupGeneral();
        setupImages();
    }

    public void setShowValue(boolean showValue) {
        this.showValue=showValue;
    }

    public void setShowName(boolean showName) {
        this.showName=showName;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PREF_WIDTH, PREF_HEIGHT);
    }

    public void setupGeneral()
    {
        setOpaque(false);
        setVisible(true);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void setupImages()
    {

        try {
            background2_ = ImageIO.read(getClass().getResourceAsStream("/org/janelia/it/workstation/gui/geometric_search/viewer/images/knob.png"));
            pointer_ = ImageIO.read(getClass().getResourceAsStream("/org/janelia/it/workstation/gui/geometric_search/viewer/images/green_pointer.png"));
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }

    }

    public void paint(Graphics g)
    {

        super.paint(g);

        int panelWidth=getWidth();
        int panelHeight=getHeight();

        int imageWidth=background2_.getWidth(null);
        int imageHeight=background2_.getHeight(null);

        int widthOffset = (panelWidth-imageWidth)/2;
        int heightOffset = (panelHeight-imageHeight)/2;

        g.drawImage(background2_, widthOffset, heightOffset, null);

        // Interpolate value between 5pi/4 and -pi/4
        double angle = ((-value_ + 1.) / 2.) * ((6. * Math.PI) / 4.) - (Math.PI / 4.);

        angle = 2 * Math.PI - angle;

        g.drawImage(pointer_,
                (int) (Math.cos(angle) * 12.) + widthOffset - 4,
                (int) (Math.sin(angle) * 12.) + imageHeight/2 + heightOffset - 7,
                null);

        Font f = g.getFont();

        if (showValue) {
            g.setFont(new Font("Arial", Font.BOLD, 9));
            g.setColor(Color.lightGray);
            String valueString = new Formatter(new StringBuilder(), Locale.FRENCH).format("%+1.1f", getNumericalValue()).toString();
            g.drawString(valueString,
                    PREF_WIDTH / 2 - (g.getFontMetrics().stringWidth(name_) / 2),
                    heightOffset + imageHeight + 6);
        }

        if (showName) {
            g.setColor(Color.black);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString(name_,
                    PREF_WIDTH / 2 - (g.getFontMetrics().stringWidth(name_) / 2),
                    heightOffset + imageHeight/2 + 4);
            g.setFont(f);
        }

    }

    public void inc(double delta)
    {
        value_ += delta;
        if (value_ > 1.)
            value_ = 1.;
        if (value_ < -1.)
            value_ = -1.;
        repaint();
    }

    public void dec(double delta)
    {
        value_ -= delta;
        if (value_ < -1.)
            value_ = -1.;
        if (value_ > 1.)
            value_ = 1.;
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent arg0)
    {
    }

    @Override
    public void mouseEntered(MouseEvent arg0)
    {
    }

    @Override
    public void mouseExited(MouseEvent arg0)
    {
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        dragStart_ = e.getPoint();
    }

    @Override
    public void mouseReleased(MouseEvent arg0)
    {
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        // 1pixel drag = 0.01 value change
        inc((dragStart_.getY() - e.getPoint().getY()) / 100.);
        dragStart_ = e.getPoint();
    }

    @Override
    public void mouseMoved(MouseEvent arg0)
    {

    }

    private double getNumericalValue() {
        return ((value_+1.0)/2.0)*(max-min)+min;
    }

    public void setValue(double value) {
        value_=(((value-min)/(max-min))*2.0)-1.0;
        if (value_<-1.0) {
            value_=-1.0;
        } else if (value_>1.0) {
            value_=1.0;
        }
    }

    public double getValue() {
        return getNumericalValue();
    }

}

