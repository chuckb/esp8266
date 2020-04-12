package com.chuckb.embedded;

@SuppressWarnings("serial")
public class ResponseFailedException extends Exception {
  public ResponseFailedException () {
  }

  public ResponseFailedException (String message) {
    super (message);
  }

  public ResponseFailedException (Throwable cause) {
    super (cause);
  }

  public ResponseFailedException (String message, Throwable cause) {
    super (message, cause);
  }
}
