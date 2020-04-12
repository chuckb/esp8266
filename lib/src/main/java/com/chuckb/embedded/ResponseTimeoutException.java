package com.chuckb.embedded;

@SuppressWarnings("serial")
public class ResponseTimeoutException extends Exception {
  public ResponseTimeoutException () {
  }

  public ResponseTimeoutException (String message) {
    super (message);
  }

  public ResponseTimeoutException (Throwable cause) {
    super (cause);
  }

  public ResponseTimeoutException (String message, Throwable cause) {
    super (message, cause);
  }
}
