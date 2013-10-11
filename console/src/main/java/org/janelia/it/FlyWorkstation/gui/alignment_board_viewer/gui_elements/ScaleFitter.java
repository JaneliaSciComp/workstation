package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements;

public class ScaleFitter {
    public enum BestDiffDirection {
        HIGHSIDE, LOWSIDE, UNTRIED
    }

    private double tolerance;
    private int startOfRange;
    private int sizeOfRange;

    public ScaleFitter( double tolerance, int startOfRange, int sizeOfRange ) {
        this.tolerance = tolerance;
        this.startOfRange = startOfRange;
        this.sizeOfRange = sizeOfRange;
    }

    public static void main( String[] args ) throws Exception {
        ScaleFitter ta = new ScaleFitter( 0.05, 85, 215 );
        int previousBestInx = (215 - 85)/2;
        for ( int i = 0; i < 1000; i++ ) {
            Double micronsPerPixel = 0.7378 + ( i * 0.01 );
            double factor = 1.0 / micronsPerPixel;
            FitReportBean fitReport = ta.findClosestPixelWidth(factor, 1, previousBestInx);

            if ( fitReport.getMinInx() < (previousBestInx - 40 )) {
                System.out.println("Recalculating");
                fitReport = ta.findClosestPixelWidth(factor, -1, previousBestInx);
            }
            previousBestInx = fitReport.getMinInx();

            System.out.print(
                    "Testing factor of " + fitReport.getPixelsPerMicron() + " fit with tolerance of " +
                    fitReport.getTolerance()
            );
            System.out.println(
                    ".  Found minDiff of " + fitReport.getMinDiff() + " on the " + fitReport.getBestDiffDirection() +
                    " and value of " + ((fitReport.getPixelCount()) * fitReport.getPixelsPerMicron()) +
                    " at pixel count " + (fitReport.getPixelCount()) + "."
            );
            if ( fitReport.getMinDiff() > fitReport.getTolerance() ) {
                System.out.println("Warning: missed tolerance by " + (fitReport.getMinDiff() - fitReport.getTolerance()) );
            }

        }
    }

    public FitReportBean findClosestPixelWidth(
            double pixelsPerMicron, int increment, int previousBestInx
    ) throws Exception {

        // Special case: very near to a whole number.
        //  In this case, repeatedly multiplying by larger and larger spans will cause the difference to get
        //  wider and wider to the whole number.  However, the approach to the next whole number will be very
        //  slow.  Conversely, the difference between the pix/micron and some whole number is so small,
        //  that 1:1 can be assumed.  So break the loop early.
        double rawDiff = Math.abs(Math.round(pixelsPerMicron) - pixelsPerMicron);
        if ( rawDiff < 0.005 ) {
            FitReportBean rtnVal = new FitReportBean();
            rtnVal.setTolerance(tolerance);
            rtnVal.setMinInx(previousBestInx);
            rtnVal.setPixelsPerMicron(pixelsPerMicron);
            rtnVal.setBestDiffDirection( BestDiffDirection.HIGHSIDE );
            rtnVal.setPixelCount( startOfRange + previousBestInx );
            rtnVal.setMinDiff( rawDiff );
            return rtnVal;
        }

        double minDiff = Double.MAX_VALUE;
        int minInx = Integer.MAX_VALUE;
        
        BestDiffDirection bestDiffDirection = BestDiffDirection.UNTRIED;

        int i = previousBestInx;
        boolean endLoop = false;
        while ( ! endLoop ) {
            double fit = (startOfRange + i)*pixelsPerMicron;
            double fitFloor = fit - Math.floor( fit );
            double fitCeil = Math.ceil( fit ) - fit;
            if ( minDiff > fitCeil ) {
                minDiff = fitCeil;
                minInx = i;
                bestDiffDirection = BestDiffDirection.LOWSIDE;
                if ( minDiff < tolerance ) {
                    endLoop = true;
                }
            }
            if ( minDiff > fitFloor ) {
                minDiff = fitFloor;
                minInx = i;
                bestDiffDirection = BestDiffDirection.HIGHSIDE;
                if ( minDiff < tolerance ) {
                    endLoop = true;
                }
            }

            // Wrap over/wrap under
            i = ( i + increment ) % sizeOfRange;
            if ( i < 0 ) {
                i = sizeOfRange;
            }
 
            if ( i == previousBestInx ) {
                endLoop = true;
            }
        }

        FitReportBean rtnVal = new FitReportBean();
        rtnVal.setTolerance( tolerance );
        rtnVal.setBestDiffDirection(bestDiffDirection);
        rtnVal.setMinDiff(minDiff);
        rtnVal.setMinInx(minInx);
        rtnVal.setPixelsPerMicron( pixelsPerMicron );
        rtnVal.setPixelCount( startOfRange + minInx );

        return rtnVal;
    }

    public static class FitReportBean {
        private BestDiffDirection bestDiffDirection;
        private double pixelsPerMicron;
        private double minDiff;
        private double tolerance;
        private int minInx;
        private int pixelCount;

        public BestDiffDirection getBestDiffDirection() {
            return bestDiffDirection;
        }

        public void setBestDiffDirection(BestDiffDirection bestDiffDirection) {
            this.bestDiffDirection = bestDiffDirection;
        }

        public double getPixelsPerMicron() {
            return pixelsPerMicron;
        }

        public void setPixelsPerMicron(double pixelsPerMicron) {
            this.pixelsPerMicron = pixelsPerMicron;
        }

        public double getMinDiff() {
            return minDiff;
        }

        public void setMinDiff(double minDiff) {
            this.minDiff = minDiff;
        }

        public double getTolerance() {
            return tolerance;
        }

        public void setTolerance(double tolerance) {
            this.tolerance = tolerance;
        }

        public int getMinInx() {
            return minInx;
        }

        public void setMinInx(int minInx) {
            this.minInx = minInx;
        }

        public int getPixelCount() {
            return this.pixelCount;
        }

        public void setPixelCount( int pixelCount ) {
            this.pixelCount = pixelCount;
        }
    }
}
