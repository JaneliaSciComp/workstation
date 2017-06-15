package org.janelia.it.workstation.browser.gui.support.buttons;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openide.util.ImageUtilities;

/**
 * A simple drop down button supporting text and/or an icon, with a rollover effect. 
 * 
 * Parts of this code were adapted from the DropDownButton in OpenIDE.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DropDownButton extends JButton {

    private JPopupMenu popupMenu;
    private PopupMenuListener menuListener;

    public DropDownButton(Icon icon, JPopupMenu popup) {

        this.popupMenu = popup;
        
        setFocusable(false);
        setRolloverEnabled(true);
        setVerticalTextPosition(AbstractButton.CENTER);
        setHorizontalTextPosition(AbstractButton.LEADING);
        setIcon(icon);
        if (icon!=null) {
            setDisabledIcon(ImageUtilities.createDisabledIcon(icon));
        }

        addMouseListener(new MouseAdapter() {
            private boolean popupMenuOperation = false;

            @Override
            public void mousePressed(MouseEvent e) {
                popupMenuOperation = false;
                if (!isEnabled()) return;
                JPopupMenu menu = getPopupMenu();
                if (menu != null && getModel() instanceof Model) {
                    Model model = (Model) getModel();
                    if (!model._isPressed()) {
                        model._press();
                        menu.addPopupMenuListener(getMenuListener());
                        menu.show(DropDownButton.this, 0, getHeight());
                        popupMenuOperation = true;
                    }
                    else {
                        model._release();
                        menu.setVisible(false);
                        menu.removePopupMenuListener(getMenuListener());
                        popupMenuOperation = true;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // If we done something with the popup menu, we should consume
                // the event, otherwise the button's action will be triggered.
                if (popupMenuOperation) {
                    popupMenuOperation = false;
                    e.consume();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                Model model = (Model) getModel();
                model._enter();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Model model = (Model) getModel();
                model._exit();
            }
        });

        setModel(new Model());
    }

    public void paint(Graphics g) {

        super.paint(g);
        Model model = (Model) getModel();
        if (model.isHover()) {
            Graphics2D g2 = (Graphics2D)g;
            int w = getWidth();
            int h = getHeight();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f));
            g2.setPaint(Color.white);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            // The arcs here are specifically tailored for Darcula theme. This effect should be moved into the DarculaLAF module somehow.
            g.fillRoundRect(1, 1, w-2, h-2, 5, 5);
            g2.dispose();
        }
    }
    
    private PopupMenuListener getMenuListener() {
        if (null == menuListener) {
            menuListener = new PopupMenuListener() {
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    // If inside the button let the button's mouse listener
                    // deal with the state. The popup menu will be hidden and
                    // we should not show it again.
                    if (getModel() instanceof Model) {
                        ((Model) getModel())._release();
                    }
                    JPopupMenu menu = getPopupMenu();
                    if (null != menu) {
                        menu.removePopupMenuListener(this);
                    }
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            };
        }
        return menuListener;
    }

    @Override
    public void setIcon(Icon icon) {
        Icon iconWithArrow = new ImageIcon(ImageUtilities.icon2Image(new IconWithArrow(icon, false)));
        super.setIcon(iconWithArrow);
    }

    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void setPopupMenu(JPopupMenu popup) {
        this.popupMenu = popup;
    }
    
    boolean hasPopupMenu() {
        return null != getPopupMenu();
    }

    private class Model extends DefaultButtonModel {
        private boolean _pressed = false;
        private boolean _hover = false;
        
        @Override
        public void setPressed(boolean b) {
            if (_pressed)
                return;
            super.setPressed(b);
        }

        public void _press() {
            if ((isPressed()) || !isEnabled()) {
                return;
            }

            stateMask |= PRESSED + ARMED;

            fireStateChanged();
            _pressed = true;
        }

        public void _release() {
            _pressed = false;
            setArmed(false);
            setPressed(false);
            setRollover(false);
            setSelected(false);
        }

        public void _enter() {
            _hover = true;
        }
        
        public void _exit() {
            _hover = false;
        }
        
        public boolean isHover() {
            return _hover;
        }
        
        public boolean _isPressed() {
            return _pressed;
        }

        @Override
        protected void fireStateChanged() {
            if (_pressed)
                return;
            super.fireStateChanged();
        }

        @Override
        public void setArmed(boolean b) {
            if (_pressed)
                return;
            super.setArmed(b);
        }

        @Override
        public void setEnabled(boolean b) {
            if (_pressed)
                return;
            super.setEnabled(b);
        }

        @Override
        public void setSelected(boolean b) {
            if (_pressed)
                return;
            super.setSelected(b);
        }

        @Override
        public void setRollover(boolean b) {
            if (_pressed)
                return;
            super.setRollover(b);
        }
    }
}
