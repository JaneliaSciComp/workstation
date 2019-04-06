package org.janelia.it.workstation.browser.gui.util;

import java.awt.Component;
import java.awt.Cursor;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.browser.gui.model.ImageDecorator;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for generically dealing with Swing and NetBeans UI.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UIUtils {

    private static final Logger log = LoggerFactory.getLogger(UIUtils.class);

    /**
     * Load an image that is found in the /images directory within the classpath.
     */
    public static ImageIcon getClasspathImage(String filename) throws FileNotFoundException {
        try {
            URL picURL = UIUtils.class.getResource("/images/" + filename);
            return new ImageIcon(picURL);
        }
        catch (Exception e) {
            throw new FileNotFoundException("/images/" + filename);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAncestorWithType(Component component, Class<T> clazz) {
        if (clazz==null) return null;
        Component c = component;
        while (c!=null) {
            log.trace("check if {} is assignable from {}",clazz.getName(),c.getClass().getName());
            if (clazz.isAssignableFrom(c.getClass())) {
                return (T)c;
            }
            c = c.getParent();
        }
        return null;
    }

    public static boolean hasAncestorWithType(Component component, Class<?> clazz) {
        if (clazz==null) return false;
        Component c = component;
        while (c!=null) {
            log.trace("check if {} is assignable from {}",clazz.getName(),c.getClass().getName());
            if (clazz.isAssignableFrom(c.getClass())) {
                return true;
            }
            c = c.getParent();
        }
        return false;
    }

    public static void setWaitingCursor(Component component) {
        JFrame mainFrame = (JFrame) WindowManager.getDefault().getMainWindow();
        if (component==mainFrame) {
            setMainFrameCursorWaitStatus(true);
            return;
        }
        component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public static void setDefaultCursor(Component component) {
        JFrame mainFrame = (JFrame) WindowManager.getDefault().getMainWindow();
        if (component == mainFrame) {
            setMainFrameCursorWaitStatus(false);
            return;
        }
        component.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Adapted from http://netbeans-org.1045718.n5.nabble.com/Setting-wait-cursor-td3026613.html
     */
    public static void setMainFrameCursorWaitStatus(final boolean isWaiting) {
        try {
            JFrame mainFrame = (JFrame) WindowManager.getDefault().getMainWindow();
            Component glassPane = mainFrame.getGlassPane();
            if (isWaiting) {
                glassPane.setVisible(true);
                glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
            else {
                glassPane.setVisible(false);
                glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
        catch (Exception e) {
            log.error("Error changing main frame cursor wait status",e);
        }
    }

    public static void queueWaitingCursor(final Component component) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setWaitingCursor(component);
            }
        });
    }

    public static void queueDefaultCursor(final Component component) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setDefaultCursor(component);
            }
        });
    }

    /**
     * Return a list of image decorators for the given domain object.
     * @param imageObject
     * @return
     */
    public static List<ImageDecorator> getDecorators(DomainObject imageObject) {
        List<ImageDecorator> decorators = new ArrayList<>();
        if (imageObject instanceof Sample) {
            Sample sample = (Sample)imageObject;
            if (sample.isSamplePurged()) {
                decorators.add(ImageDecorator.PURGED);
            }
            if (!sample.isSampleSageSynced()) {
                decorators.add(ImageDecorator.DESYNC);
            }
        }
        else if (imageObject instanceof LSMImage) {
            LSMImage lsm = (LSMImage)imageObject;
            if (!lsm.isLSMSageSynced()) {
                decorators.add(ImageDecorator.DESYNC);
            }
        }

        return decorators;
    }

    /**
     * Return a list of image decorators for the given pipeline result.
     * @param result
     * @return
     */
    public static List<ImageDecorator> getDecorators(PipelineResult result) {
        List<ImageDecorator> decorators = new ArrayList<>();
        if (result.getPurged()!=null && result.getPurged()) {
            decorators.add(ImageDecorator.PURGED);
        }
        return decorators;
    }
}
