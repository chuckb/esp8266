package com.chuckb.embedded;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

public class ESP8266 {
  private final InputStream inputStream;
  private final OutputStream outputStream;
  private final PrintStream printStream;
  private final InputStreamReader inputStreamReader;

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

  public ESP8266(InputStream inputStream, OutputStream outputStream) {
    this.inputStream = inputStream;
    this.outputStream = outputStream;
    printStream = new PrintStream(outputStream, true, Charset.forName("US_ASCII"));
    inputStreamReader = new InputStreamReader(inputStream, Charset.forName("US_ASCII"));
  }

  public String getFirmwareVersion () throws IOException, ProtocolException {
    String output = "";
    printStream.print("AT+GMR");
    BufferedReader reader = new BufferedReader(inputStreamReader);
    StringBuilder out = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      output = out.toString();
    }
    while ((line = reader.readLine()) != null) {
      if (out.toString() != "OK") {
        throw new ProtocolException();
      }
    }
    reader.close();
    return output;
  }
}