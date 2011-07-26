package org.janelia.it.FlyWorkstation.gui.framework.external_listener;

import org.janelia.it.FlyWorkstation.api.stub.data.ControlledVocabulary;

import javax.swing.*;
import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class ExternalListener {
  private int port;
  private ServerSocket serverSocket;
  private String browserRedirectTop,browserRedirectBottom;
  private String errorString,instructionString;
  private String commandAcceptedTop,commandAcceptedBottom;
  private Listener listenerThread;
  private String lineSep=System.getProperty("line.separator");
  private DateFormat dateFormatter=new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z");
  private static final String FW_VERSION ="3.0.1";
  private static final char CR=13;
  private static final char LF=10;
  private static final String DYNAMIC_STRING="$INSERT_DYNAMIC_STRINGS_HERE$";

  public ExternalListener(int port) {
     this.port=port;
     setUpStrings();
     setUpListener();
  }


  private void setUpStrings() {
    browserRedirectTop=new String("<HTML><HEAD><META Http-equiv=\"refresh\" Content=\"0; Url=");
    browserRedirectBottom=new String("\"></HEAD><BODY></BODY></HTML>");
    String bodyString= "URL requests must contain an action varible, and must be  specified in"+
       " a GET request (URL encoded), not POST request, which is form encoded.  Valid formats are as follows: <BR><BR><B>Links:</B><BR> "+
       "&lt a href=http://localhost:30000?action=search&unknown_oid=17000001001565&redir=http://www.msn.com&gt Test &lt/a &gt"+
       "<BR><P><B>Forms:</B> (note method is Get, not Post)<BR> "+
       "&lt form method=\"Get\" action=\"http://localhost:30000\"&gt<BR>"+
       "&lt INPUT Type=hidden name=action value=search&gt<BR>"+
       "&lt INPUT Type=hidden name=redir value=http://www.msn.com&gt"+
       "&lt INPUT Type=hidden name=oid value=17000001001565&gt<BR>"+
       "&lt input type=\"submit\" value=Submit&gt<BR>"+
       "&lt /form&gt"+
       "<P><B>Valid actions:</B>"+
       "<P><B>search</B><BR>Required search parameters: "+
       DYNAMIC_STRING+
       "<BR><BR><P><B>redir</B><BR>redir is optional and can be left out for debugging<BR>"+
       "specifing redir=204 will send a 204 \"No Content\" header in return, which"+
       " will tell the web browser not to do anything.</BODY></HTML>";
    errorString=new String("<HTML><HEAD><TITLE>ERROR 404 Bad Request!!</TITLE></HEAD><BODY><B>Fly Workstation URL Interface</B>"+
       "<BR><BR>Error 400 Bad Request<BR><a href=http://localhost:30000>Click here for instructions </a></BODY></HTML>");
    instructionString=new String("<HTML><HEAD><TITLE>Fly Workstation URL Interface Instructions</TITLE></HEAD><BODY>"+
      "<B>Fly Workstation URL Interface Instructions</B><BR>The Fly Workstation can accept a URL based request for certain actions, allowing links to be "+
       " added to a web page that will tell the browser what to do.  "+bodyString);
    commandAcceptedTop=new String("<HTML><HEAD><TITLE>Fly Workstation URL Interface Command</TITLE></HEAD><BODY><B>Fly Workstation URL Interface </B><BR><BR>"+
      "The Fly Workstation has accepted your command<BR>");
    commandAcceptedBottom=new String("</BODY>");
  }


  private String formRedirString(String newLocation) {
     newLocation= stripTrailingBackslash(normalizeHexCodedString(newLocation));
     return browserRedirectTop+newLocation+browserRedirectBottom;
  }

  private String formCommandAcceptedString(Map commands) {
     String rtnString=commandAcceptedTop;
     Iterator keys=commands.keySet().iterator();
     Object tmpObj;
     for (;keys.hasNext();) {
       tmpObj=keys.next();
       rtnString=rtnString+"<BR>"+tmpObj+":"+commands.get(tmpObj);
     }
     rtnString=rtnString+commandAcceptedBottom;
     return rtnString;
  }

  private String stripTrailingBackslash(String orig){
     if (!orig.endsWith("/")) return orig;
     return orig.substring(0,orig.length()-1);
  }

  private String normalizeHexCodedString(String inString) {
     if (inString.indexOf("%")==-1) return inString;
     char[] newChars=new char[inString.length()];
     char[] oldChars=inString.toCharArray();
     int newCounter=0;
     for (int i=0;i<inString.length();i++) {
        if (oldChars[i]=='%') {
           newChars[newCounter++]=(char)(Integer.decode("0x"+oldChars[++i]+oldChars[++i]).intValue());
        }
        else newChars[newCounter++]=oldChars[i];
     }
     System.arraycopy(oldChars,0,newChars,0,inString.indexOf("%"));
     return new String(newChars).trim();
  }

  private boolean validateRequest(HashMap hash) {
   try{
     if (!hash.containsKey("action")) return false;
     String action=(String)hash.get("action");
     if (action.equals("search")) {
       Set searchTypes=getValidSearchTypes();
       for (Iterator it=searchTypes.iterator();it.hasNext();) {
         if (hash.containsKey(it.next())) return true;
       }
       return false;
     }
     else return false;
   }
   catch (Exception ex) {
     return false;
   }
  }


  private HashMap parseGetString(String inString){
     HashMap hash=new HashMap();
     // This try/catch block is only necessary for execution under jdk1.2.2
     try {
      inString = URLDecoder.decode(inString, "UTF-8");
     }
     catch (Exception ex) {
      normalizeHexCodedString(inString);
     }
     //Parse off the HTTP type
     int httpTypeIndex=inString.indexOf("HTTP/");
     if (httpTypeIndex>-1) inString=inString.substring(0,httpTypeIndex-1);

     int beginIndex=inString.indexOf("?");
     inString=inString.substring(beginIndex+1,inString.length());
     inString=stripTrailingBackslash(inString);


     //normalizeHexCodedString(inString);

     int ampIndex;
     String keyValue;
     if (inString.indexOf("=")==-1) return hash;
     while (true) {
       ampIndex=inString.indexOf("&");
       if (ampIndex==-1) {
         keyValue=inString.substring(0,inString.length());
      //   System.out.println("KeyValue: "+keyValue);
         hash.put(keyValue.substring(0,keyValue.indexOf("=")),
           keyValue.substring(keyValue.indexOf("=")+1));
         break;
       }
     //
       keyValue=inString.substring(0,inString.indexOf("&"));
     //  System.out.println("KeyValue: "+keyValue);
       hash.put(keyValue.substring(0,keyValue.indexOf("=")),keyValue.substring(keyValue.indexOf("=")+1));
     //  System.out.println("Key: "+keyValue.substring(0,keyValue.indexOf("="))+" Value: "+keyValue.substring(keyValue.indexOf("=")+1));
       inString=inString.substring(ampIndex+1,inString.length());
     }
     return hash;
  }

  private void setUpListener() {
     listenerThread = new Listener();
     listenerThread.start();
     //System.out.println("Started new External Listener on Port: "+port);
  }

  public void stop() {
      if (listenerThread!=null) {
         listenerThread.end();
         //System.out.println("Stopped the External Listener on Port: "+port);
         listenerThread = null;
      }
  }

  private String get200Header(int length) {
     return ("HTTP/1.0 200 OK "+lineSep+stdHeader(length));
  }

  private String get202Header(int length) {
     return ("HTTP/1.0 202 Accepted "+lineSep+stdHeader(length));
  }


  private String get204Header() {
     return ("HTTP/1.0 204 No Content "+lineSep+stdHeader());
  }

  private String get400Header(int length) {
     return ("HTTP/1.0 400 Bad Request "+lineSep+stdHeader(length));
  }

  private String stdHeader(int length){
     StringBuffer sb=new StringBuffer(200);
     Date date=new Date();
     String dateStr=dateFormatter.format(date);
     sb.append("Date: "+dateStr+" ");
     sb.append(lineSep);
     sb.append("Server: FlyWorkstation/"+ FW_VERSION +" " );
     sb.append(lineSep);
     sb.append("Last-modified: "+dateStr+" ");
     sb.append(lineSep);
     sb.append("Expires: "+dateStr+" ");
     sb.append(lineSep);
     sb.append("Content-type:  text/html ");
     sb.append(lineSep);
     sb.append("Content-Length: "+length+" ");
     sb.append(lineSep);
     sb.append(CR);
     sb.append(LF);
     return sb.toString();
  }

  private String stdHeader(){
     StringBuffer sb=new StringBuffer(200);
     Date date=new Date();
     String dateStr=dateFormatter.format(date);
     sb.append("Date: "+dateStr+" ");
     sb.append(lineSep);
     sb.append("Server: FlyWorkstation/"+ FW_VERSION +" " );
     sb.append(lineSep);
     sb.append("Last-modified: "+dateStr+" ");
     sb.append(lineSep);
     sb.append("Expires: "+dateStr+" ");
     sb.append(lineSep);
     sb.append("Content-type:  text/html ");
     sb.append(lineSep);
     sb.append(CR);
     sb.append(LF);
     return sb.toString();
  }

  private Set getValidSearchTypes() {
      // todo Fix this
      ControlledVocabulary cv=new ControlledVocabulary();//AutoNavigationMgr.getAutoNavigationMgr().getNavigationSearchTypes();
    String[] values=cv.getValues();
    return new HashSet(Arrays.asList(values));
  }

  private String getInstructionString() {
    StringBuffer sb=new StringBuffer(instructionString);
    int index=instructionString.indexOf(DYNAMIC_STRING);
    String bodyString="";
      // todo Fix this
      ControlledVocabulary cv=new ControlledVocabulary(); //AutoNavigationMgr.getAutoNavigationMgr().getNavigationSearchTypes();
    String[] values=cv.getValues();
    for (int i=0;i<values.length;i++) {
       bodyString+="<BR>"+values[i]+" = "+cv.lookup(values[i]);
    }
    sb.replace(index,index+DYNAMIC_STRING.length(),bodyString);
    return sb.toString();
  }

  private String getErrorString() {
    StringBuffer sb=new StringBuffer(errorString);
    int index=errorString.indexOf(DYNAMIC_STRING);
    if (index>-1) {
      String bodyString="";
        // Fix this
        ControlledVocabulary cv= new ControlledVocabulary();//AutoNavigationMgr.getAutoNavigationMgr().getNavigationSearchTypes();
      String[] values=cv.getValues();
      for (int i=0;i<values.length;i++) {
         bodyString+="<BR>"+values[i]+" = "+cv.lookup(values[i]);
      }
      sb.replace(index,index+DYNAMIC_STRING.length(),bodyString);
    }
    return sb.toString();
  }

  static public void main (String[] args) {
     new ExternalListener(30000);
  }

  class ProcessRequest implements Runnable {
     HashMap parameters;

     public ProcessRequest(HashMap parameters) {
        this.parameters=parameters;
     }

     public void run() {
        if(parameters.get("action").equals("search")) {
           //System.out.println("Processing search request");
           Set searchTypes=getValidSearchTypes();
           String nextType=null;
           for (Iterator it=searchTypes.iterator();it.hasNext();) {
             nextType=(String)it.next();
             if (parameters.get(nextType)!=null) {
                 // Fix this
                 ControlledVocabulary cv=new ControlledVocabulary(); //AutoNavigationMgr.getAutoNavigationMgr().getNavigationSearchTypes();
                String tmpSearch = cv.lookup(nextType);
                if (tmpSearch.equals(nextType)) tmpSearch = null;
                 // Fix this
//                SearchManager.getSearchManager().launchSearch(tmpSearch,(String)parameters.get(nextType));
                break;
             }
           }
        }
    }
  }

  class Listener extends Thread {

    public void end() {
     try{
      serverSocket.close();
     }
     catch (Exception ex) {}
    }

    public void run () {
     try {
      try {
       serverSocket=new ServerSocket(port);
      }
      catch (BindException bEx) {
         //System.out.println("Server socket "+port+" in use.");
         Thread.sleep(10000);
         setUpListener();
         return;
      }
      while (true) {
       Socket socket=null;
       try {
        socket=serverSocket.accept();
        while (!socket.getInetAddress().equals(socket.getLocalAddress())) {
           System.out.println("Potential security violation - non-localhost connection attempt");
           socket=serverSocket.accept();
        }
        BufferedReader in =new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String inString;
        HashMap parameters=null;
        while (true) {
           inString=in.readLine();
           if (inString!=null && !inString.equals("")) {
            // System.out.println(inString);
             if (inString.startsWith("GET")) {
                parameters=parseGetString (inString);
                //System.out.println("Received External request");
             }
             if (inString.startsWith("Referer: ")) {
                if (parameters!=null && !parameters.containsKey("redir"))
                   parameters.put("redir",inString.substring("Referer: ".length()));
             }
           }
           else break;
        }
        PrintWriter out= new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
        String reDirLocation=null;
        if (parameters!=null) reDirLocation = (String)parameters.get("redir");
        if (!validateRequest(parameters)) {
            if (parameters.size()==0 || (parameters.size()==1 && parameters.get("redir")!=null)) {
                //System.out.println("Sending: Instruction Msg");
                //System.out.println(get200Header(instructionString.length())+instructionString);
                String instructions=getInstructionString();
                out.println(get200Header(instructions.length())+instructions);
            }
            else {
                //System.out.println("Sending: Error Msg");
                String errors=getErrorString();
                out.println(get200Header(errors.length())+errors);
                //get400Header()+
            }
        }
        else {
          //  System.out.println("Sending: "+formRedirString(reDirLocation));
            if (reDirLocation==null || reDirLocation.equals("")){
               String body=formCommandAcceptedString(parameters);
               out.println(get202Header(body.length())+body);
            }
            else if (reDirLocation.startsWith("204"))
              out.println(get204Header());
            else {
              String body=formRedirString(reDirLocation);
              out.println(get202Header(body.length())+body);
            }
            SwingUtilities.invokeLater(new ProcessRequest(parameters));
        }
       }
       catch (IOException ioEx2) {
         ioEx2.printStackTrace();
       }
       finally {
        //System.out.println();
        if (socket!=null) socket.close();
       }
      }
     }
     catch (Exception ex) {
       ex.printStackTrace();
       try {
         serverSocket.close();
       }
       catch (Exception ex1) {}
       setUpListener();
     }
    }
  }

}