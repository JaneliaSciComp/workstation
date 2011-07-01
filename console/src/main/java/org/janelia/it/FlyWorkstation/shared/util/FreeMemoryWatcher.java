package org.janelia.it.FlyWorkstation.shared.util;

import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:50 PM
 * Class to observe the memory used by the system
 */
public class FreeMemoryWatcher extends MTObservable {
    static private FreeMemoryWatcher freeMemoryWatcher;
    private int numSecondsToUpdate;
    private Thread updater;
    private Runtime runtime = Runtime.getRuntime();
    private long totalMemory, reservedMemory;
//    private  LoadRequestStatusObserverAdapter statusObserver =
//       new MyLoadRequestStatusObserver();


    private FreeMemoryWatcher(int numSecondsToUpdate, boolean startUpdate) {
        this.numSecondsToUpdate = numSecondsToUpdate;
        if (startUpdate) startUpdate();
    }

    static public FreeMemoryWatcher getFreeMemoryWatcher() {
        if (freeMemoryWatcher == null) freeMemoryWatcher = new FreeMemoryWatcher(ConsoleProperties.getInt("console.memory.updateSeconds"),
                true);
        return freeMemoryWatcher;
    }

    public void startUpdate() {
        updater = new Thread(new Updater(numSecondsToUpdate), "Free Memory Watcher");
        updater.setDaemon(true);
        updater.setPriority(Thread.MIN_PRIORITY);
        updater.start();
    }

    public void stopUpdate() {
        updater.interrupt();
        updater = null;
    }

    private void update() {
        setChanged();
        notifyObservers(new Integer((int) (((double) getFreeMemory() / (double) getTotalMemory()) * 100)));
        clearChanged();
//      System.out.println("Free Memory Percent: "+(int)((double)runtime.freeMemory()/(double)runtime.totalMemory()*100));
    }

    public long getTotalMemory() {
        if (totalMemory > 0) return totalMemory;
        String memoryStr = ConsoleProperties.getString("console.memory.TotalMemory");
        try {
            if (memoryStr.endsWith("mb") || memoryStr.endsWith("MB")) {
                totalMemory = 1024 * 1000 * Long.parseLong(memoryStr.substring(0, memoryStr.length() - 1));
                return totalMemory;
            }
            if (memoryStr.endsWith("kb") || memoryStr.endsWith("KB")) {
                totalMemory = 1024 * Long.parseLong(memoryStr.substring(0, memoryStr.length() - 1));
                return totalMemory;
            }
            totalMemory = Long.parseLong(memoryStr);
        } catch (Exception ex) {
            totalMemory = 262144000; // 256MB, if necessary.  The property file should be there.
        }
        return totalMemory;
    }


    public long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public long getFreeMemory() {
        return getTotalMemory() - getUsedMemory();
    }


//    public LoadRequestStatusObserverAdapter getLoadStatusObserver(){
//      return statusObserver;
//    }
//
//
//     private class MyLoadRequestStatusObserver extends LoadRequestStatusObserverAdapter {
//      public void stateChanged(LoadRequestStatus loadRequestStatus, LoadRequestState newState){
//
//        if (newState==LoadRequestStatus.COMPLETE ) {
//             loadRequestStatus.removeLoadRequestStatusObserver(this);
//             System.gc();
//        }
//      }
//  }


    class Updater implements Runnable {
        private int numSecondsToUpdate;

        Updater(int numSecondsToUpdate) {
            this.numSecondsToUpdate = numSecondsToUpdate;
        }

        public void run() {
            while (true) {
                update();
                try {
                    Thread.sleep(numSecondsToUpdate * 1000);
                } catch (InterruptedException ie) {
                    System.err.println("Memory Watcher Threads Sleep was interrupted");
                }
            }
        }
    }


}
