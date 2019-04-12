package org.janelia.workstation.common.gui.support.buttons;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.janelia.workstation.common.gui.support.JScrollPopupMenu;
import org.openide.util.ImageUtilities;


/**
 * A simple drop down button supporting text and/or an icon, with a rollover effect, and a scrolling menu. 
 * 
 * Parts of this code were adapted from the DropDownButton in OpenIDE.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DropDownButton extends JButton {

    private List<Component> components = new ArrayList<>();
    private int maxVisibleElements = 30;
    
    private JScrollPopupMenu popupMenu;
    private PopupMenuListener menuListener;

    public DropDownButton() {
        this(null, null);
    }

    public DropDownButton(String title) {
        this(title, null);
    }

    public DropDownButton(Icon icon) {
        this(null, icon);
    }
    
    public DropDownButton(String text, Icon icon) {
        
        setText(text);
        setIcon(icon);
        setFocusable(false);
        setRolloverEnabled(true);
        setVerticalTextPosition(AbstractButton.CENTER);
        setHorizontalTextPosition(AbstractButton.LEADING);
        if (icon!=null) {
            setDisabledIcon(ImageUtilities.createDisabledIcon(icon));
        }

        addMouseListener(new MouseAdapter() {
            private boolean popupMenuOperation = false;

            @Override
            public void mousePressed(MouseEvent e) {
                popupMenuOperation = false;
                if (!isEnabled()) return;
                if (getModel() instanceof Model) {
                    synchronized (DropDownButton.this) {
                        Model model = (Model) getModel();
                        if (!model._isPressed()) {
                            model._press();
                            if (popupMenu != null) {
                                // This should never happen, but just in case..
                                popupMenu.removeAll();
                            }
                            JScrollPopupMenu popupMenu = new JScrollPopupMenu();
                            for (Component component : components) {
                                popupMenu.add(component);
                            }
                            popupMenu.addPopupMenuListener(getMenuListener());
                            popupMenu.setMaximumVisibleRows(maxVisibleElements);
                            popupMenu.show(DropDownButton.this, 0, getHeight());
                            DropDownButton.this.popupMenu = popupMenu;
                        }
                        else {
                            model._release();
                            if (popupMenu != null) {
                                popupMenu.removeAll();
                                popupMenu.setVisible(false);
                                popupMenu = null;
                            }
                        }
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

    public int getMaxVisibleElements() {
        return maxVisibleElements;
    }

    public void setMaxVisibleElements(int maxVisibleElements) {
        this.maxVisibleElements = maxVisibleElements;
    }


    private PopupMenuListener getMenuListener() {
        if (null == menuListener) {
            menuListener = new PopupMenuListener() {
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    synchronized (DropDownButton.this) {
                        if (getModel() instanceof Model) {
                            ((Model) getModel())._release();
                        }
                        if (popupMenu != null) {
                            popupMenu.removeAll();
                            popupMenu.removePopupMenuListener(this);
                            popupMenu = null;
                        }
                    }
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            };
        }
        return menuListener;
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
    
    @Override
    public void setIcon(Icon icon) {
        Icon iconWithArrow = new ImageIcon(ImageUtilities.icon2Image(new IconWithArrow(icon, false)));
        super.setIcon(iconWithArrow);
    }

    public void removeAll() {
        components.clear();
    }
    
    public void addMenuItem(Component component) {
        components.add(component);
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
