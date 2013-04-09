package org.janelia.it.FlyWorkstation.gui.framework.progress_meter;

import org.janelia.it.FlyWorkstation.api.entity_model.access.TaskRequestStatusObserverAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.ActiveThreadModel;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.TaskRequestState;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.TaskRequestStatus;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ProgressMeter extends JDialog {
  private MyObserver observer = new MyObserver();
  private JPanel panel1 = new JPanel();
  private BoxLayout boxLayout;
  private static ProgressMeter progressMeter;
  private Hashtable meters=new Hashtable();
  private java.util.Timer timer=new java.util.Timer(true);
  private TimerTask updateTask;
  private Meter spacerMeter = new Meter("No Active Tasks  ", 100, 260);
  //Allow for 112.5 seconds of timer
  private static int TIMER_TICK=1500;
  private static int MAX_TIMER_PERCENT=75;
  private TaskRequestStatusObserverAdapter statusObserver =
     new MyTaskRequestStatusObserver();

  static {
      ProgressMeter.getProgressMeter();
  }

  private ProgressMeter() {
    this(null,"Progress Meter",false);
  }

  public static ProgressMeter getProgressMeter() {
     if (progressMeter==null) progressMeter=new ProgressMeter();
     return progressMeter;
  }


  private ProgressMeter(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try  {
      jbInit();
      ActiveThreadModel.getActiveThreadModel().addObserver(observer);
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public synchronized void addProgressBar(TaskRequestStatus statusObject) {
     String name = statusObject.getId();
     if (statusObject.getTaskRequest().isUnloadRequest()) {name = "Unloading "+name;}
     else {name = "Running "+name;}
     if (meters.containsKey(statusObject)) return;
     Meter meter=new Meter(name,100,this.getWidth());
     if (meters.size()==0)  {
        panel1.remove(spacerMeter);
        updateTask=new UpdateTask();
        timer.scheduleAtFixedRate(updateTask,0,TIMER_TICK);
     }
     meters.put(statusObject,meter);
     panel1.add(meter);
     panel1.validate();
     this.pack();
     repaint();
  }

  public synchronized void removeProgressBar(TaskRequestStatus statusObject) {
    if (!meters.containsKey(statusObject)) return;
    try {
     panel1.remove((Component)meters.get(statusObject));
    }
    catch (Exception ex) {
       System.out.println("*****ERROR meter "+
       statusObject.getId()+" not found******");
     }
     meters.remove(statusObject);
     if (meters.isEmpty()) {
        panel1.add(spacerMeter);
        updateTask.cancel();
     }
     panel1.validate();
     this.pack();
     repaint();
  }

  public synchronized void modifyProgress(TaskRequestStatus statusObject,int progress) {
     if (meters.containsKey(statusObject))
      ((Meter)meters.get(statusObject)).setValueFromLoad(progress);
  }

  void jbInit() throws Exception {
    this.setResizable(false);
    panel1.setLayout(boxLayout= new BoxLayout(panel1,BoxLayout.Y_AXIS));
    panel1.add(spacerMeter);
    getContentPane().add(panel1);
    this.pack();
  }

  class Meter extends JPanel {
     BoxLayout boxLayout=new BoxLayout(this,BoxLayout.X_AXIS);
     JProgressBar bar;
     JLabel label;
     private int timerStop;

     public Meter (String name,int max, int width) {
       label=new JLabel(" "+name+" ");
       bar=new JProgressBar(JProgressBar.HORIZONTAL,0,max);
       bar.setStringPainted(true);
       setLayout(boxLayout);
       bar.setMaximumSize(new Dimension(250,12));
       bar.setPreferredSize(new Dimension(250,12));
       bar.setMinimumSize(new Dimension(250,12));
       label.setFont(new Font("Dialog",0,10));
       this.setAlignmentX(this.LEFT_ALIGNMENT);
       this.add(label);
       this.add(Box.createHorizontalGlue());
       this.add(bar);
     }

     public void setValueFromLoad(int value) {
        if (timerStop==0) timerStop=getValue();
        bar.setValue((int)((100.0-timerStop)*(value/100.0))+timerStop);
     }

     public void setValueFromTimer(int value) {
        if (timerStop==0) bar.setValue(value);
     }

     public final int getValue() {
        return bar.getValue();
     }

  }

  private class MyTaskRequestStatusObserver extends TaskRequestStatusObserverAdapter {
    public void stateChanged(TaskRequestStatus taskRequestStatus, TaskRequestState newState){
      if (newState== TaskRequestStatus.LOADING || newState== TaskRequestStatus.UNLOADING) {
        addProgressBar(taskRequestStatus);
      }
      if (newState== TaskRequestStatus.COMPLETE ) {
           taskRequestStatus.removeTaskRequestStatusObserver(this);
           removeProgressBar(taskRequestStatus);
      }
    }

    public void notifiedPercentageChanged(TaskRequestStatus taskRequestStatus,
      int newPercent){
        modifyProgress(taskRequestStatus, newPercent);
    }
  }

  private class UpdateTask extends TimerTask {
      public void run(){
          Enumeration e=meters.elements();
          Meter meter;
          int oldValue;
          while (e.hasMoreElements()) {
             meter=((Meter)e.nextElement());
             oldValue=meter.getValue();
             if (oldValue<MAX_TIMER_PERCENT) meter.setValueFromTimer(oldValue+1);
          }
      }
  }

  public class MyObserver implements Observer {
    public void update(Observable o, Object arg) {
      if (o instanceof ActiveThreadModel && arg instanceof TaskRequestStatus) {  //adding AND deleting active threads
         TaskRequestStatus status=(TaskRequestStatus)arg;
         status.addTaskRequestStatusObserver(statusObserver, true);
      }
    }
  }
}


