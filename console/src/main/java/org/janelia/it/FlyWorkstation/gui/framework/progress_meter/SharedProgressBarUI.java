package org.janelia.it.FlyWorkstation.gui.framework.progress_meter;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.swing.DefaultLookup;

/**
 * A shared implementation of ProgressBarUI which maintains a single timer for updating all indeterminate progress 
 * bars, conserving resources and synchronizing animations in cases where many progress bars are needed.
 * 
 * Code copied and modified from BasicProgressBarUI.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SharedProgressBarUI extends BasicProgressBarUI {

    private static final Logger log = LoggerFactory.getLogger(SharedProgressBarUI.class);
    
    private static ConcurrentHashSet<JProgressBar> progressBars = new ConcurrentHashSet<JProgressBar>();

    private boolean initialized = false;

    private static int cachedPercent;
    private static Animator animator;

    private static ChangeListener sharedChangeListener;
    private static Handler handler;

    /**
     * The current state of the indeterminate animation's cycle. 0, the initial
     * value, means paint the first frame. When the progress bar is
     * indeterminate and showing, the default animation thread updates this
     * variable by invoking incrementAnimationIndex() every repaintInterval
     * milliseconds.
     */
    private static int animationIndex = 0;

    /**
     * The number of frames per cycle. Under the default implementation, this
     * depends on the cycleTime and repaintInterval. It must be an even number
     * for the default painting algorithm. This value is set in the
     * initIndeterminateValues method.
     */
    private static int numFrames; // 0 1|numFrames-1 ... numFrames/2

    /**
     * Interval (in ms) between repaints of the indeterminate progress bar. The
     * value of this method is set (every time the progress bar changes to
     * indeterminate mode) using the "ProgressBar.repaintInterval" key in the
     * defaults table.
     */
    private static int repaintInterval;

    /**
     * The number of milliseconds until the animation cycle repeats. The value
     * of this method is set (every time the progress bar changes to
     * indeterminate mode) using the "ProgressBar.cycleTime" key in the defaults
     * table.
     */
    private static int cycleTime; // must be repaintInterval*2*aPositiveInteger

    // performance stuff
    private static boolean ADJUSTTIMER = false; // makes a BIG difference;
    // make this false for
    // performance tests

    /**
     * Used to hold the location and size of the bouncing box (returned by
     * getBox) to be painted.
     * 
     * @since 1.5
     */
    private static Rectangle boxRect;

    /**
     * The rectangle to be updated the next time the animation thread calls
     * repaint. For bouncing-box animation this rect should include the union of
     * the currently displayed box (which needs to be erased) and the box to be
     * displayed next. This rectangle's values are set in the setAnimationIndex
     * method.
     */
    private static Rectangle nextPaintRect;

    // cache
    /** The component's painting area, not including the border. */
    private static Rectangle componentInnards; // the current painting area
    private static Rectangle oldComponentInnards; // used to see if the size
                                                  // changed

    /** For bouncing-box animation, the change in position per frame. */
    private static double delta = 0.0;

    private static int maxPosition = 0; // maximum X (horiz) or Y box location

    public static ComponentUI createUI(JComponent x) {
        return new SharedProgressBarUI();
    }

    private void trackProgressBar(JProgressBar c) {
        synchronized (SharedProgressBarUI.class) {
            progressBars.add((JProgressBar) c);
        }
    }

    private void untrackProgressBar(JComponent c) {
        synchronized (SharedProgressBarUI.class) {
            progressBars.remove((JProgressBar) c);
            if (progressBars.isEmpty()) {
                stopSharedAnimationTimer();
            }
        }
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        installListeners();
        if (progressBar.isIndeterminate()) {
            synchronized (SharedProgressBarUI.class) {
                if (!initialized) {
                    initIndeterminateValues();
                    initialized = true;
                    // start the animation thread if necessary
                    ensureSharedAnimationTimerIsRunning();
                }
            }
        }
        trackProgressBar(progressBar);
    }

    public void uninstallUI(JComponent c) {
        if (progressBar.isIndeterminate()) {
            cleanUpIndeterminateValues();
        }
        uninstallListeners();
        untrackProgressBar(progressBar);
        super.uninstallUI(c);
    }

    protected void installListeners() {
        // Listen for changes in the progress bar's data.
        sharedChangeListener = getSharedHandler();
        progressBar.addChangeListener(sharedChangeListener);

        // Listen for changes between determinate and indeterminate state.
        progressBar.addPropertyChangeListener(getSharedHandler());
    }

    private Handler getSharedHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    /**
     * Starts the animation thread, creating and initializing it if necessary.
     * This method is invoked when an indeterminate progress bar should start
     * animating. Reasons for this may include:
     * <ul>
     * <li>The progress bar is determinate and becomes displayable
     * <li>The progress bar is displayable and becomes determinate
     * <li>The progress bar is displayable and determinate and this UI is
     * installed
     * </ul>
     * If you implement your own animation thread, you must override this
     * method.
     * 
     * @since 1.4
     * @see #stopAnimationTimer
     */
    protected synchronized void startAnimationTimer() {
    }

    private synchronized void ensureSharedAnimationTimerIsRunning() {
        synchronized (SharedProgressBarUI.class) {
            if (animator == null) {
                log.info("Starting shared animation thread");
                animator = new Animator();
                animator.start(getRepaintInterval());
            }
        }
    }

    /**
     * Stops the animation thread. This method is invoked when the indeterminate
     * animation should be stopped. Reasons for this may include:
     * <ul>
     * <li>The progress bar changes to determinate
     * <li>The progress bar is no longer part of a displayable hierarchy
     * <li>This UI in uninstalled
     * </ul>
     * If you implement your own animation thread, you must override this
     * method.
     * 
     * @since 1.4
     * @see #startAnimationTimer
     */
    protected void stopAnimationTimer() {
    }

    private void stopSharedAnimationTimer() {
        if (animator != null) {
            log.info("Stopping shared animation thread");
            animator.stop();
            animator = null;
        }
    }

    /**
     * Removes all listeners installed by this object.
     */
    protected void uninstallListeners() {
        progressBar.removeChangeListener(sharedChangeListener);
        progressBar.removePropertyChangeListener(getSharedHandler());
        handler = null;
    }

    private int getCachedPercent() {
        return cachedPercent;
    }

    private void setCachedPercent(int cachedPercent) {
        this.cachedPercent = cachedPercent;
    }

    /**
     * Stores the position and size of the bouncing box that would be painted
     * for the current animation index in <code>r</code> and returns
     * <code>r</code>. Subclasses that add to the painting performed in this
     * class's implementation of <code>paintIndeterminate</code> -- to draw an
     * outline around the bouncing box, for example -- can use this method to
     * get the location of the bouncing box that was just painted. By overriding
     * this method, you have complete control over the size and position of the
     * bouncing box, without having to reimplement
     * <code>paintIndeterminate</code>.
     * 
     * @param r
     *            the Rectangle instance to be modified; may be
     *            <code>null</code>
     * @return <code>null</code> if no box should be drawn; otherwise, returns
     *         the passed-in rectangle (if non-null) or a new rectangle
     * 
     * @see #setAnimationIndex
     * @since 1.4
     */
    protected Rectangle getBox(Rectangle r) {
        int currentFrame = getAnimationIndex();
        int middleFrame = numFrames / 2;

        if (sizeChanged() || delta == 0.0 || maxPosition == 0.0) {
            updateSizes();
        }

        r = getGenericBox(r);

        if (r == null) {
            return null;
        }
        if (middleFrame <= 0) {
            return null;
        }

        // assert currentFrame >= 0 && currentFrame < numFrames
        if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
            if (currentFrame < middleFrame) {
                r.x = componentInnards.x + (int) Math.round(delta * (double) currentFrame);
            } else {
                r.x = maxPosition - (int) Math.round(delta * (currentFrame - middleFrame));
            }
        } else { // VERTICAL indeterminate progress bar
            if (currentFrame < middleFrame) {
                r.y = componentInnards.y + (int) Math.round(delta * currentFrame);
            } else {
                r.y = maxPosition - (int) Math.round(delta * (currentFrame - middleFrame));
            }
        }
        return r;
    }

    /**
     * Updates delta, max position. Assumes componentInnards is correct (e.g.
     * call after sizeChanged()).
     */
    private void updateSizes() {
        int length = 0;

        if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
            length = getBoxLength(componentInnards.width, componentInnards.height);
            maxPosition = componentInnards.x + componentInnards.width - length;

        } else { // VERTICAL progress bar
            length = getBoxLength(componentInnards.height, componentInnards.width);
            maxPosition = componentInnards.y + componentInnards.height - length;
        }

        // If we're doing bouncing-box animation, update delta.
        delta = 2.0 * (double) maxPosition / (double) numFrames;
    }

    /**
     * Assumes that the component innards, max position, etc. are up-to-date.
     */
    private Rectangle getGenericBox(Rectangle r) {
        if (r == null) {
            r = new Rectangle();
        }

        if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
            r.width = getBoxLength(componentInnards.width, componentInnards.height);
            if (r.width < 0) {
                r = null;
            } else {
                r.height = componentInnards.height;
                r.y = componentInnards.y;
            }
            // end of HORIZONTAL

        } else { // VERTICAL progress bar
            r.height = getBoxLength(componentInnards.height, componentInnards.width);
            if (r.height < 0) {
                r = null;
            } else {
                r.width = componentInnards.width;
                r.x = componentInnards.x;
            }
        } // end of VERTICAL

        return r;
    }

    /**
     * Gets the index of the current animation frame.
     * 
     * @since 1.4
     */
    protected int getAnimationIndex() {
        return animationIndex;
    }

    /**
     * Sets the index of the current animation frame to the specified value and
     * requests that the progress bar be repainted. Subclasses that don't use
     * the default painting code might need to override this method to change
     * the way that the <code>repaint</code> method is invoked.
     * 
     * @param newValue
     *            the new animation index; no checking is performed on its value
     * @see #incrementAnimationIndex
     * 
     * @since 1.4
     */
    protected void setAnimationIndex(int newValue) {
        if (animationIndex != newValue) {
            if (sizeChanged()) {
                animationIndex = newValue;
                maxPosition = 0; // needs to be recalculated
                delta = 0.0; // needs to be recalculated
                repaintAllIndeterminate();
                return;
            }

            // Get the previous box drawn.
            nextPaintRect = getBox(nextPaintRect);

            // Update the frame number.
            animationIndex = newValue;

            // Get the next box to draw.
            if (nextPaintRect != null) {
                boxRect = getBox(boxRect);
                if (boxRect != null) {
                    nextPaintRect.add(boxRect);
                }
            }
        } else { // animationIndex == newValue
            return;
        }
        
        repaintAllIndeterminate();
    }
    
    private void repaintAllIndeterminate() {
        for(JProgressBar progressBar : progressBars) {
            if (progressBar.isIndeterminate()) {
                if (nextPaintRect != null) {
                    progressBar.repaint(nextPaintRect);
                } else {
                    progressBar.repaint();
                }
            }
        }
    }

    private boolean sizeChanged() {
        if ((oldComponentInnards == null) || (componentInnards == null)) {
            return true;
        }

        oldComponentInnards.setRect(componentInnards);
        componentInnards = SwingUtilities.calculateInnerArea(progressBar, componentInnards);
        return !oldComponentInnards.equals(componentInnards);
    }

    /**
     * Sets the index of the current animation frame, to the next valid value,
     * which results in the progress bar being repainted. The next valid value
     * is, by default, the current animation index plus one. If the new value
     * would be too large, this method sets the index to 0. Subclasses might
     * need to override this method to ensure that the index does not go over
     * the number of frames needed for the particular progress bar instance.
     * This method is invoked by the default animation thread every <em>X</em>
     * milliseconds, where <em>X</em> is specified by the
     * "ProgressBar.repaintInterval" UI default.
     * 
     * @see #setAnimationIndex
     * @since 1.4
     */
    protected void incrementAnimationIndex() {
        int newValue = getAnimationIndex() + 1;

        if (newValue < numFrames) {
            setAnimationIndex(newValue);
        } else {
            setAnimationIndex(0);
        }
    }

    /**
     * Returns the desired number of milliseconds between repaints. This value
     * is meaningful only if the progress bar is in indeterminate mode. The
     * repaint interval determines how often the default animation thread's
     * timer is fired. It's also used by the default indeterminate progress bar
     * painting code when determining how far to move the bouncing box per
     * frame. The repaint interval is specified by the
     * "ProgressBar.repaintInterval" UI default.
     * 
     * @return the repaint interval, in milliseconds
     */
    private int getRepaintInterval() {
        return repaintInterval;
    }

    private int initRepaintInterval() {
        repaintInterval = DefaultLookup.getInt(progressBar, this, "ProgressBar.repaintInterval", 50);
        return repaintInterval;
    }

    /**
     * Returns the number of milliseconds per animation cycle. This value is
     * meaningful only if the progress bar is in indeterminate mode. The cycle
     * time is used by the default indeterminate progress bar painting code when
     * determining how far to move the bouncing box per frame. The cycle time is
     * specified by the "ProgressBar.cycleTime" UI default and adjusted, if
     * necessary, by the initIndeterminateDefaults method.
     * 
     * @return the cycle time, in milliseconds
     */
    private int getCycleTime() {
        return cycleTime;
    }

    private int initCycleTime() {
        cycleTime = DefaultLookup.getInt(progressBar, this, "ProgressBar.cycleTime", 3000);
        return cycleTime;
    }

    /** Initialize cycleTime, repaintInterval, numFrames, animationIndex. */
    private void initIndeterminateDefaults() {
        initRepaintInterval(); // initialize repaint interval
        initCycleTime(); // initialize cycle length

        // Make sure repaintInterval is reasonable.
        if (repaintInterval <= 0) {
            repaintInterval = 100;
        }

        // Make sure cycleTime is reasonable.
        if (repaintInterval > cycleTime) {
            cycleTime = repaintInterval * 20;
        } else {
            // Force cycleTime to be a even multiple of repaintInterval.
            int factor = (int) Math.ceil(((double) cycleTime) / ((double) repaintInterval * 2));
            cycleTime = repaintInterval * factor * 2;
        }
    }

    /**
     * Invoked by PropertyChangeHandler.
     * 
     * NOTE: This might not be invoked until after the first paintIndeterminate
     * call.
     */
    private void initIndeterminateValues() {
        initIndeterminateDefaults();
        // assert cycleTime/repaintInterval is a whole multiple of 2.
        numFrames = cycleTime / repaintInterval;
        initAnimationIndex();

        boxRect = new Rectangle();
        nextPaintRect = new Rectangle();
        componentInnards = new Rectangle();
        oldComponentInnards = new Rectangle();

        // we only bother installing the HierarchyChangeListener if we
        // are indeterminate
        progressBar.addHierarchyListener(getSharedHandler());
    }

    /** Invoked by PropertyChangeHandler. */
    private void cleanUpIndeterminateValues() {
        progressBar.removeHierarchyListener(getSharedHandler());
    }

    // Called from initIndeterminateValues to initialize the animation index.
    // This assumes that numFrames is set to a correct value.
    private void initAnimationIndex() {
        if ((progressBar.getOrientation() == JProgressBar.HORIZONTAL)) {
            // If this is a left-to-right progress bar,
            // start at the first frame.
            setAnimationIndex(0);
        } else {
            // If we go right-to-left or vertically, start at the right/bottom.
            setAnimationIndex(numFrames / 2);
        }
    }

    //
    // Animation Thread
    //
    /**
     * Implements an animation thread that invokes repaint at a fixed rate. If
     * ADJUSTTIMER is true, this thread will continuously adjust the repaint
     * interval to try to make the actual time between repaints match the
     * requested rate.
     */
    private class Animator implements ActionListener {
        private Timer timer;
        private long previousDelay; // used to tune the repaint interval
        private int interval; // the fixed repaint interval
        private long lastCall; // the last time actionPerformed was called
        private int MINIMUM_DELAY = 5;

        /**
         * Creates a timer if one doesn't already exist, then starts the timer
         * thread.
         */
        private void start(int interval) {
            previousDelay = interval;
            lastCall = 0;

            if (timer == null) {
                timer = new Timer(interval, this);
            } else {
                timer.setDelay(interval);
            }

            if (ADJUSTTIMER) {
                timer.setRepeats(false);
                timer.setCoalesce(false);
            }

            timer.start();
        }

        /**
         * Stops the timer thread.
         */
        private void stop() {
            timer.stop();
        }

        /**
         * Reacts to the timer's action events.
         */
        public void actionPerformed(ActionEvent e) {
            if (ADJUSTTIMER) {
                long time = System.currentTimeMillis();

                if (lastCall > 0) { // adjust nextDelay
                    // XXX maybe should cache this after a while
                    // actual = time - lastCall
                    // difference = actual - interval
                    // nextDelay = previousDelay - difference
                    // = previousDelay - (time - lastCall - interval)
                    int nextDelay = (int) (previousDelay - time + lastCall + getRepaintInterval());
                    if (nextDelay < MINIMUM_DELAY) {
                        nextDelay = MINIMUM_DELAY;
                    }
                    timer.setInitialDelay(nextDelay);
                    previousDelay = nextDelay;
                }
                timer.start();
                lastCall = time;
            }

            incrementAnimationIndex(); // paint next frame
        }
    }

    /**
     * This inner class is marked &quot;public&quot; due to a compiler bug. This
     * class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of BasicProgressBarUI.
     */
    public class ChangeHandler implements ChangeListener {
        // NOTE: This class exists only for backward compatability. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.
        public void stateChanged(ChangeEvent e) {
            getSharedHandler().stateChanged(e);
        }
    }

    private class Handler implements ChangeListener, PropertyChangeListener, HierarchyListener {
        // ChangeListener
        public void stateChanged(ChangeEvent e) {
            BoundedRangeModel model = progressBar.getModel();
            int newRange = model.getMaximum() - model.getMinimum();
            int newPercent;
            int oldPercent = getCachedPercent();

            if (newRange > 0) {
                newPercent = (int) ((100 * (long) model.getValue()) / newRange);
            } else {
                newPercent = 0;
            }

            if (newPercent != oldPercent) {
                setCachedPercent(newPercent);
                repaintAllIndeterminate();
            }
        }

        // PropertyChangeListener
        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            if ("indeterminate" == prop) {
                if (progressBar.isIndeterminate()) {
                    initIndeterminateValues();
                } else {
                    // clean up
                    cleanUpIndeterminateValues();
                }
                repaintAllIndeterminate() ;
            }
        }

        // we don't want the animation to keep running if we're not displayable
        public void hierarchyChanged(HierarchyEvent he) {
            if ((he.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                if (progressBar.isIndeterminate()) {
                    if (progressBar.isDisplayable()) {
                        startAnimationTimer();
                    } else {
                        stopAnimationTimer();
                    }
                }
            }
        }
    }
}
