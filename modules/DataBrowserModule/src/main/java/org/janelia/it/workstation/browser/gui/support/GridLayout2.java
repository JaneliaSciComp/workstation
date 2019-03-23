package org.janelia.it.workstation.browser.gui.support;

import java.awt.*;

/**
 * Grid Layout which allows components of differrent sizes
 * 
 * From https://www.javaworld.com/article/2077486/core-java/java-tip-121--flex-your-grid-layout.html#resources
 */
public class GridLayout2 extends GridLayout {
    
    private boolean equalWidthRows = false;
    private boolean equalWidthCols = false;
    
    public GridLayout2() {
        this(1, 0, 0, 0);
    }

    public GridLayout2(int rows, int cols, int hgap, int vgap) {
        super(rows, cols, hgap, vgap);
    }

    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int nrows = getRows();
            int ncols = getColumns();
            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            }
            else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }
            int[] w = new int[ncols];
            int[] h = new int[nrows];
            for (int i = 0; i < ncomponents; i++) {
                int r = i / ncols;
                int c = i % ncols;
                Component comp = parent.getComponent(i);
                Dimension d = comp.getPreferredSize();
                if (w[c] < d.width) {
                    w[c] = d.width;
                }
                if (h[r] < d.height) {
                    h[r] = d.height;
                }
            }
            
            int nw = calculateTotalSize(w, equalWidthRows);
            int nh = calculateTotalSize(h, equalWidthCols);
            return new Dimension(insets.left + insets.right + nw + (ncols - 1) * getHgap(),
                                 insets.top + insets.bottom + nh + (nrows - 1) * getVgap());
        }
    }

    public Dimension minimumLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int nrows = getRows();
            int ncols = getColumns();
            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            }
            else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }
            int[] w = new int[ncols];
            int[] h = new int[nrows];
            for (int i = 0; i < ncomponents; i++) {
                int r = i / ncols;
                int c = i % ncols;
                Component comp = parent.getComponent(i);
                Dimension d = comp.getMinimumSize();
                if (w[c] < d.width) {
                    w[c] = d.width;
                }
                if (h[r] < d.height) {
                    h[r] = d.height;
                }
            }
            int nw = calculateTotalSize(w, equalWidthRows);
            int nh = calculateTotalSize(h, equalWidthCols);
            return new Dimension(insets.left + insets.right + nw + (ncols - 1) * getHgap(),
                                 insets.top + insets.bottom + nh + (nrows - 1) * getVgap());
        }
    }

    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int nrows = getRows();
            int ncols = getColumns();
            if (ncomponents == 0) {
                return;
            }
            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            }
            else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }
            int hgap = getHgap();
            int vgap = getVgap();
            // scaling factors
            Dimension pd = preferredLayoutSize(parent);
            double sw = (1.0 * parent.getWidth()) / pd.width;
            double sh = (1.0 * parent.getHeight()) / pd.height;
            // scale
            int[] w = new int[ncols];
            int[] h = new int[nrows];
            for (int i = 0; i < ncomponents; i++) {
                int r = i / ncols;
                int c = i % ncols;
                Component comp = parent.getComponent(i);
                Dimension d = comp.getPreferredSize();
                d.width = (int) (sw * d.width);
                d.height = (int) (sh * d.height);
                if (w[c] < d.width) {
                    w[c] = d.width;
                }
                if (h[r] < d.height) {
                    h[r] = d.height;
                }
            }
            int wmax = calculateMax(w);
            int hmax = calculateMax(h);
            for (int c = 0, x = insets.left; c < ncols; c++) {
                for (int r = 0, y = insets.top; r < nrows; r++) {
                    int i = r * ncols + c;
                    if (i < ncomponents) {
                        int cw = equalWidthRows ? wmax : w[c];
                        int ch = equalWidthRows ? hmax : h[r];
                        parent.getComponent(i).setBounds(x, y, cw, ch);
                    }
                    y += h[r] + vgap;
                }
                x += w[c] + hgap;
            }
        }
    }
    
    private int calculateTotalSize(int[] sizes, boolean useMax) {
        if (useMax) {
            return calculateMax(sizes) * sizes.length;
        }
        else {
            int total = 0;
            for (int j = 0; j < sizes.length; j++) {
                total += sizes[j];
            }
            return total;
        }
    }

    private int calculateMax(int[] sizes) {
        int max = 0;
        for (int j = 0; j < sizes.length; j++) {
            if (sizes[j] > max) {
                max = sizes[j];
            }
        }
        return max;
    }
    
    public boolean isEqualWidthRows() {
        return equalWidthRows;
    }

    public void setEqualWidthRows(boolean equalWidthRows) {
        this.equalWidthRows = equalWidthRows;
    }

    public boolean isEqualWidthCols() {
        return equalWidthCols;
    }

    public void setEqualWidthCols(boolean equalWidthCols) {
        this.equalWidthCols = equalWidthCols;
    }

    public GridLayout2(int rows, int cols) {
        this(rows, cols, 0, 0);
    }
}
