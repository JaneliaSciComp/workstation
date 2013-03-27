package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.ActiveThreadModel;
import org.janelia.it.FlyWorkstation.gui.framework.progress_meter.ProgressMeter;
import org.janelia.it.FlyWorkstation.shared.util.MultiHash;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 1:07 PM
 */
public class ConsoleMenuBar extends JMenuBar {
    protected Browser console;   //protected so that when subclasses, the subclasses menus can have access to it.
    private MultiHash addedMenus = new MultiHash();

    protected JLabel imageLabel;
    protected ImageIcon staticIcon, animatedIcon;
    protected FileMenu fileMenu;
    protected EditMenu editMenu;
    protected BookmarkMenu bookmarkMenu;
    protected JMenu toolsMenu;
    protected JMenu servicesMenu;
    protected JMenu searchMenu;
    protected JMenu ontologyMenu;
    protected JMenu helpMenu;
    protected JMenu viewMenu;
    // todo Remove this dumb counter for animation
    private int dumbCounter = 0;
//    protected JMenu windowMenu;
    protected Component menuGlue = Box.createHorizontalGlue();


    public ConsoleMenuBar(Browser console) {
        this.console = console;
        try {
            this.setAnimationIcon(Utils.getClasspathImage("fly_progress.gif"));
            this.setStaticIcon(Utils.getClasspathImage("fly_start.gif"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        BoxLayout layout = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(layout);
        constructMenus();
        addMenus();
        ActiveThreadModel.getActiveThreadModel().addObserver(new ThreadModelObserver());
        console.addBrowserObserver(new MenuBarBrowserObserver());
    }

    private void constructMenus() {
        fileMenu = new FileMenu(console);
        editMenu = new EditMenu(console);
        bookmarkMenu = new BookmarkMenu(console);
        toolsMenu = new ToolsMenu(console);
        searchMenu = new SearchMenu(console);
        servicesMenu = new ServicesMenu(console);
        ontologyMenu = new OntologyMenu(console);
        helpMenu = new HelpMenu(console);
        viewMenu = new ViewMenu(console);
        imageLabel = new JLabel(staticIcon);
        imageLabel.addMouseListener(new MyMouseListener());
    }

    private void addMenus() {
        add(fileMenu);
        add(editMenu);
        add(searchMenu);
//        add(bookmarkMenu);
        add(toolsMenu);
        add(servicesMenu);
        add(viewMenu);
//        add(ontologyMenu);
        add(helpMenu);
        add(menuGlue);
        add(imageLabel);
    }

//    public void add(Component comp, Position pos) {
//        addedMenus.put(pos, comp);
//        redraw();
//    }

    public void remove(Component comp) {
       addedMenus.remove(comp);
//       Vector vec;
//       for (Enumeration e=addedMenus.elements();e.hasMoreElements();) {
//         vec=(Vector)e.nextElement();
//         if (vec.contains(comp)) {
//           vec.remove(comp);
//           break;
//         }
//       }
        redraw();
    }

    public void setAnimationIcon(ImageIcon animationIcon) {
        this.animatedIcon = animationIcon;
    }

    public void setStaticIcon(ImageIcon staticIcon) {
        this.staticIcon = staticIcon;
    }

//    private void setEditorSpecificMenus(JMenuItem[] menus) {
//        addedMenus.remove(EDITOR_SPECIFIC);
//        if (menus != null) for (int i = 0; i < menus.length; i++) {
//            addedMenus.put(EDITOR_SPECIFIC, menus[i]);
//        }
//        redraw();
//    }

    private void redraw() {
//        Vector menuList = new Vector();
//        Vector tmpVec;
//        if (addedMenus.containsKey(LEFT)) {
//            tmpVec = (Vector) addedMenus.get(LEFT);
//            menuList.addAll(tmpVec);
//        }
//        menuList.addElement(fileMenu);
//        if (addedMenus.containsKey(AFTER_FILE)) {
//            tmpVec = (Vector) addedMenus.get(AFTER_FILE);
//            menuList.addAll(tmpVec);
//        }
//        menuList.add(viewMenu);
//        if (addedMenus.containsKey(AFTER_VIEW)) {
//            tmpVec = (Vector) addedMenus.get(AFTER_VIEW);
//            menuList.addAll(tmpVec);
//        }
//        if (addedMenus.containsKey(EDITOR_SPECIFIC)) {
//            tmpVec = (Vector) addedMenus.get(EDITOR_SPECIFIC);
//            menuList.addAll(tmpVec);
//        }
//        menuList.add(windowMenu);
//        if (addedMenus.containsKey(AFTER_EDITOR_SPECIFIC)) {
//            tmpVec = (Vector) addedMenus.get(AFTER_EDITOR_SPECIFIC);
//            menuList.addAll(tmpVec);
//        }
//        menuList.add(menuGlue);
//        if (addedMenus.containsKey(AFTER_SPACING_GLUE)) {
//            tmpVec = (Vector) addedMenus.get(AFTER_SPACING_GLUE);
//            menuList.addAll(tmpVec);
//        }
//        if (staticImageLabel != null) if (!animation) menuList.add(staticImageLabel);
//        else menuList.add(animatedImageLabel);
//
//
//        removeAll();
//        for (int i = 0; i < menuList.size(); i++) {
//            add((Component) menuList.elementAt(i));
//        }
        validate();
        console.repaint();
    }

    // todo Remove this public access to the Progress Meter
    public void modifyImageState(boolean animated) {
        if (animated) {
            dumbCounter++;
        }
        else {
            dumbCounter--;
        }
        if (dumbCounter>0) {
            imageLabel.setIcon(animatedIcon);
        }
        else {
            imageLabel.setIcon(staticIcon);
        }
//        redraw();
        imageLabel.repaint();
        validate();
        console.repaint();
    }


//    static class Position {
//        int pos;
//
//        Position(int position) {
//            pos = position;
//        }
//    }

    class MenuBarBrowserObserver extends BrowserObserverAdapter {
       public void editorSpecificMenusChanged(JMenuItem[] menus){
//         setEditorSpecificMenus(menus);
       }
    }

    class ThreadModelObserver implements Observer {
       public void update(Observable o, Object arg) {
         if (o instanceof ActiveThreadModel)
           if (((ActiveThreadModel)o).getActiveThreadCount()>0) modifyImageState(true);
           else modifyImageState(false);
      }
    }

    class MyMouseListener implements MouseListener {
      private ProgressMeter meter = ProgressMeter.getProgressMeter();
      public void mouseClicked(MouseEvent e){}
      public void mousePressed(MouseEvent e){}
      public void mouseReleased(MouseEvent e){}
      public void mouseEntered(MouseEvent e) {
          Point loc = imageLabel.getLocation();
          meter.setLocation(new Point(loc.x-imageLabel.getWidth(), loc.y+imageLabel.getHeight()+meter.getHeight()));
          meter.setVisible(true);
          modifyImageState(true);
      }
      public void mouseExited(MouseEvent e){
          meter.setVisible(false);
          modifyImageState(false);
      }
    }

}
