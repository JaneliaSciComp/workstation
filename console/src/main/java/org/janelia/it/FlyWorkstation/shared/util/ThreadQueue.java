package org.janelia.it.FlyWorkstation.shared.util;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedList;

/**
* This class acts as a generic queue mechanism for passing execution between threads.
* This class creates a queue that accepts Runnables.  It then executes the Runnables
* in one of several threads from a thread group that it manages.  The number of
* threads executing can be set by the client, as well as the priorities of the
* threads. This class speeds execution in a multi-threaded app as it eliminates
* the longer overhead associated with creating and killing threads many times.  It
* also eliminates the potencial for many, many threads to be created without that intent.
*
* This class also will run runnables directly if numThreads is 0
*
* Defaults: Priority=normal
*           Execution on ThreadQueue threads (not System Event Queue)
*/
public class ThreadQueue {
  LinkedList<Runnable> queue=new LinkedList<Runnable>();
  int queueDepthMonitorCounter;
  ArrayList<Thread> threads = new ArrayList<Thread>();
  ArrayList<Runner> runners = new ArrayList<Runner>();
  boolean monitor;
  boolean threadQueueNotification=true;
  ThreadGroup threadGroup;
  boolean runDirectly;
  int totalThreads;

  public ThreadQueue(int numThreads, String groupName) {
    this(numThreads,groupName,Thread.NORM_PRIORITY,true);
  }

  public ThreadQueue(int numThreads, String groupName, int priority) {
    this(numThreads,groupName,priority,true);
  }

  public ThreadQueue(int numThreads, String groupName, int priority, boolean threadQueueNotification) {
    this.threadQueueNotification=threadQueueNotification;
    if (numThreads==0) runDirectly=true;
    else initThreads(numThreads,groupName,priority);
    totalThreads = numThreads;
  }

  public int getTotalThreads() {
    return totalThreads;
  }

  public void setPriority(int priority) {
    for (Thread thread: threads) {
       thread.setPriority(priority);
    }
  }

  public void monitor(boolean monitor) {
     this.monitor=monitor;
  }

  synchronized public void addQueue(Runnable runnable) {
    if (!runDirectly) {
        queue.add(runnable);
        Object obj;
        for (Runner runner : runners) {
            obj = runner;
            synchronized (obj) {
             obj.notify();
            }
        }
    }
    else runnable.run();
    }

  synchronized private Runnable deQueue() {
     if (queue.size()==0) return null;
     if (monitor) {
       queueDepthMonitorCounter++;
       if (queueDepthMonitorCounter==10) {
         queueDepthMonitorCounter=0;
         System.out.println("ThreadGroup: "+threadGroup.getName()+" Current Queue Depth: "+queue.size());
       }
     }
     return (Runnable) queue.removeFirst();
  }

  private void initThreads(int numberThreads,String groupName,int priority) {
    Thread thread;
    threadGroup=new ThreadGroup(groupName);
    threadGroup.setDaemon(true);
    Runner runner;
    for (int i=1;i<=numberThreads;i++) {
        runner=new Runner();
        thread=new Thread(threadGroup,runner,"Runner Thread "+i);
        threads.add(thread);
        runners.add(runner);
        thread.setPriority(priority);
        thread.start();
    }
  }


  class Runner implements Runnable {
      Runnable runnable;

      public void run () {
        while (true) {
          runnable=deQueue();
          if (runnable==null) {
            synchronized(this) {
              try {
                wait();
              }
              catch (Exception ie) {}
            }
          }
          if (runnable==null) runnable=deQueue();
          try {
             if (runnable!=null)
               if (threadQueueNotification) runnable.run();
               else SwingUtilities.invokeAndWait(runnable);
          }
          catch (Exception ite) {
             ite.printStackTrace();
          }
        }
     }
  }
}