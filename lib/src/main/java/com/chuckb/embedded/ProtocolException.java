package com.chuckb.embedded;

@SuppressWarnings("serial")
public class ProtocolException extends Exception {
  public ProtocolException () {
  }

  public ProtocolException (String message) {
    super (message);
  }

  public ProtocolException (Throwable cause) {
    super (cause);
  }

  public ProtocolException (String message, Throwable cause) {
    super (message, cause);
  }
}
