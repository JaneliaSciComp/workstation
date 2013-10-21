package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 6/4/13
 * Time: 2:14 PM
 *
 * Captures frequencies of colors occurring in input, and writes them back as a dump for later analysis.
 */

import java.util.HashMap;
import java.util.Map;

public class ByteFrequencyDumper {
    private String name;
    private int byteCount;
    private int channelCount;
    private Map<FreqBean,Integer> colorFreq = new HashMap<FreqBean,Integer>();

    public ByteFrequencyDumper(String name, int byteCount, int channelCount) {
        this.name = name;
        this.byteCount = byteCount;
        this.channelCount = channelCount;
    }

    public void frequencyCapture(byte[] channelBytes) {
        if ( byteCount == 1   &&   channelCount >= 3 ) {
            try {
                FreqBean color = new FreqBean(channelBytes);
                Integer freq = colorFreq.get( color );
                if ( freq == null ) {
                    freq = 1;
                }
                else {
                    freq = freq + 1;
                }
                colorFreq.put( color, freq );
            } catch ( Exception ex ) {
                System.out.print("Failed to make bean of channel bytes: " );
                for ( int i = 0; i < channelBytes.length; i++ ) {
                    System.out.print( "::" + channelBytes[ i ] );
                }
                System.out.println();
            }
        }
    }

    public void close() throws Exception {
        if ( colorFreq.size() > 0 ) {
            StringBuffer outBuf = new StringBuffer();
            analyze( outBuf );

            //for ( FreqBean color: colorFreq.keySet() ) {
            //    System.out.println( name + "\t" + color + "\t" + colorFreq.get( color ) );
            //}

        }
    }

    private static class FreqBean {
        private String concatValues;
        public FreqBean( byte[] values ) {
            StringBuilder bldr = new StringBuilder();
            for ( int i = 0; i < values.length; i++ ) {
                bldr.append( values[i] ).append("//");
            }
            concatValues = bldr.toString();
        }
        @Override
        public String toString() { return concatValues; }
        @Override
        public boolean equals( Object o ) {
            if ( o==null || !(o instanceof FreqBean)) {
                return false;
            }
            else {
                return o.toString().equals( toString() );
            }
        }
        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }

    public void analyze( StringBuffer outBuf ) throws Exception {
        int[] maxRGB = new int[ 3 ];
        int[] minRGB = new int[] { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };

        double minStdev = Double.MAX_VALUE;
        double maxStdev = Double.MIN_VALUE;

        int counter = 0;
        double greenOverBlue = 0.0;
        double redOverGreen = 0.0;
        double redOverBlue = 0.0;

        for ( FreqBean color: colorFreq.keySet() ) {
            String colorStr = color.toString();
            String[] rgb = colorStr.split( "//" );
            int freq = colorFreq.get( color );
            int rgbInt[] = new int[ 3 ];
            for ( int i = 0; i < 3; i++ ) {
                int nextVal = Integer.parseInt( rgb[ i ] );
                if ( nextVal < 0 ) nextVal += 256;

                if ( maxRGB[ i ] < nextVal )  maxRGB[ i ] = nextVal;
                if ( minRGB[ i ] > nextVal )  minRGB[ i ] = nextVal;

                rgbInt[ i ] = nextVal;

            }

            double stdev = getStdDev( rgbInt );
            if ( stdev < minStdev ) minStdev = stdev;
            if ( stdev > maxStdev ) maxStdev = stdev;

            if ( rgbInt[ 2 ] != 0  &&  rgbInt[ 1 ] != 0 ) {
                greenOverBlue += freq * (rgbInt[ 1 ] / rgbInt[ 2 ]);
                redOverGreen += freq * (rgbInt[ 0 ] / rgbInt[ 1 ]);
                redOverBlue += freq * (rgbInt[ 0 ] / rgbInt[ 2 ]);
            }

            counter+=freq;
        }

        // Bail if nothing to say.
        if ( maxRGB[0] == 0 && maxRGB[1] == 0 && maxRGB[2] == 0 ) {
            return;
        }

        outBuf.append( name ).append("\n");
        outBuf.append("--------------------").append("\n");

        // Reporting.
        outBuf.append("Max standard deviation is ").append(maxStdev ).append("\n");
        outBuf.append("Min standard deviation is ").append(minStdev).append("\n");

        outBuf.append("Red range is ").append(minRGB[0]).append("..").append(maxRGB[0]).append("\n");
        outBuf.append("Green range is ").append(minRGB[1]).append("..").append(maxRGB[2]).append("\n");
        outBuf.append("Blue range is ").append(minRGB[1]).append("..").append(maxRGB[2]).append("\n");

        outBuf.append("Avg green:blue ratio is ").append(greenOverBlue / counter).append("\n");
        outBuf.append("Avg red:green ratio is ").append(redOverGreen / counter).append("\n");
        outBuf.append("Avg red:blue ratio is ").append(redOverBlue / counter).append("\n");

        outBuf.append("\n");
        outBuf.append("\n");

        System.out.print( outBuf );
    }

    private double getMean( int[] values ) {
        double rtnVal = 0.0;
        for ( int i = 0; i < values.length; i++ ) {
            rtnVal += values[ i ];
        }
        return rtnVal / values.length;
    }

    private double getStdDev( int[] values ) {
        double accum = 0.0;
        double mean = getMean( values );
        for ( int value: values ) {
            double diff = mean - value;
            accum += diff * diff;
        }
        if ( accum == 0.0 ) return accum;
        return Math.sqrt( accum  / values.length );
    }
}

