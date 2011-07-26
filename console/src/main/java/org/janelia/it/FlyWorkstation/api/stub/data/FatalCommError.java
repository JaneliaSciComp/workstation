package org.janelia.it.FlyWorkstation.api.stub.data;

public class FatalCommError extends SystemError {

  String machineName;

  public FatalCommError() {}

  public FatalCommError(String msg) {
    super (msg);
  }

  public FatalCommError(String machineName, String msg) {
    super (msg);
    this.machineName=machineName;
  }

  public String getMachineName() {
     return machineName;
  }

}
