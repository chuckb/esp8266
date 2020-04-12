package com.chuckb.embedded;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Wrapper class for communicating with ESP8266 via streams.
 * Tested with AI Thinker version 0018000902-AI03. Defined as:
 * <ul>
 *  <li>AT Version 0018</li>
 *  <li>esp_iot_sdk version 0902-AI03</li>
 * </ul>
 * See command set at http://www.pridopia.co.uk/pi-doc/ESP8266ATCommandsSet.pdf
 * <p>Heavily inspired by https://github.com/Syonyk/LiteESP8266Client</p>
 */
public class ESP8266 {
  private final OutputStream outputStream;
  private final InputStream inputStream;
  private final Charset defaulCharset;
  private final long DEFAULT_MS_TIMEOUT = 200;
  private long defaultMsTimeout = DEFAULT_MS_TIMEOUT;
  private final String DEFAULT_CHARSET_NAME = "US-ASCII";
  private final String LINE_END = "\r\n";
  private final String AT = "AT";
  private final String GETMODULERELEASE = "GMR";
  private final String RESPONSE_OK = "OK\r\n";
  private final String RESPONSE_ERROR = "ERROR\r\n";
  private final String RESPONSE_OK_TRIMMED = "OK";
  private final String RESPONSE_ERROR_TRIMMED = "ERROR";
  private final String LISTACCESSPOINTS = "CWLAP";
  private final String GETWIFIMODE = "CWMODE?";
  private final String SETWIFIMODE = "CWMODE=";
  private final String NOCHANGE = "no change";
  private final String RESTART = "RST";
  private final String READY = "ready\r\n";
  private final String GETIP = "CIFSR";
  private final String SETMUXMODE = "CIPMUX=";
  private final String IPSTART = "CIPSTART=";
  private final String SETIPSERVER = "CIPSERVER=";
  private final String DISABLE_ECHO = "ATE0";
  private final String ENABLE_ECHO = "ATE1";
  private final String REPLY_WIFI_MODE = "+CWMODE:";

  public enum WifiModeEnum 
  { 
    STATION("1"), ACCESSPOINT("2"), BOTH("3");
  
    private String code;     
    private WifiModeEnum(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public static WifiModeEnum getEnum(String code) throws ProtocolException {
    
      switch (code) {
        case "1":
          return STATION;
        case "2":
          return ACCESSPOINT;
        case "3":
          return BOTH;
        default:
          throw new ProtocolException("Unexpected Wifi mode code.");
      }
    }    
  } 

  public enum UDPPeerModeEnum 
  { 
    USEDEFINEDREMOTE("0"), CHANGEREMOTEONCE("1"), ESTABLISHPEER("2");
  
    private String code;     
    private UDPPeerModeEnum(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public static UDPPeerModeEnum getEnum(String code) throws ProtocolException {
    
      switch (code) {
        case "0":
          return USEDEFINEDREMOTE;
        case "1":
          return CHANGEREMOTEONCE;
        case "2":
          return ESTABLISHPEER;
        default:
          throw new ProtocolException("Unexpected Wifi mode code.");
      }
    }    
  } 

  /**
   * Constructor receiving open streams for communicating with ESP8266 device.
   * This class contains no platform specific code.
   * @param inputStream   An open {@link InputStream} for getting data from the device.
   * @param outputStream  An open {@link OutputStream} for sending data to the device.
   */
  public ESP8266(InputStream inputStream, OutputStream outputStream) throws IOException, ResponseFailedException, ResponseTimeoutException {
    // Establish for convenience
    this.defaulCharset = Charset.forName(DEFAULT_CHARSET_NAME);
    // Save off streams for later use.
    this.inputStream = inputStream;
    this.outputStream = outputStream;
    // Rig up streams so we can read/write to them easily.
    if (isReady()) {
      disableEcho();
    }
  }

  /**
   * Helper method to convert from default character set to byte stream
   * for writing to the ESP8266.
   * @param output
   * @throws IOException
   */
  private void print(String output) throws IOException {
    outputStream.write(output.getBytes(defaulCharset));
  }

  /**
   * Reads from the ESP8266 until an end of line is reached and returns the string,
   * subject to the timeout and the maximum return buffer allowed.
   * @param maxBuffer     The maximum buffer to read into.
   * @param msTimeout     The time in milliseconds to read before a timeout occurs.
   * @return              A string containing the line read.
   * @throws IOException
   * @throws ResponseTimeoutException
   */
  private String readLine(int maxBuffer, long msTimeout) throws IOException, ResponseTimeoutException {
    // Read up to the new line
    ByteBuffer buffer = readIntoByteBuffer('\n', maxBuffer, msTimeout);
    // Ignore the CRLF
    byte[] truncatedBuffer = new byte[buffer.position()-2];
    buffer.flip();
    buffer.get(truncatedBuffer);
    // Convert byte array to string version
    return new String(truncatedBuffer, defaulCharset);
  }

  /**
   * Simple delegation to flushing the outputStream.
   */
  private void flush() throws IOException {
    outputStream.flush();
  }

  /**
   * Helper method to sniff if data are available in the inputStream.
   * 
   * @return              True if data is available to be read.
   * @throws IOException
   */
  private boolean canRead() throws IOException {
    return (inputStream.available() > 0);
  }

  /**
   * Send a command to the module. This function will prepend the AT start sequence
   * and flush the contents to the module automatically. If a blank command is sent,
   * the AT sequence by itself will be sent.
   * 
   * @param command The ESP8266 command to be sent.
   */
  private void sendCommand(String command) throws IOException {
    if (command == "") {
      print(AT);
    } else {
      print(AT + "+" + command);
    }
    print(LINE_END);
    flush();
  }

  /**
   * Send a raw command to the device. What you send is exactly what is sent.
   * @param command
   */
  private void sendRawCommand(String command) throws IOException {
    print(command);
    flush();
  }

  /**
   * Read the input from the ESP8266 and look for a pass or fail condition defined by strings
   * passed in. A {@link ResponseFailedException} is thrown if the fail contition is met. The function
   * simply returns if pass condition is met.
   * @param pass                        The string looked for defining a pass condition.
   * @param fail                        The string looked for defining a fail condition.
   * @param msTimeout                   Timeout in millseconds waiting for find a reply condition.
   * @throws IOException                Thrown by serial hardware if communication is interrupted.
   * @throws ResponseFailedException    Thrown if fail condition is met.
   * @throws ResponseTimeoutException   Thrown if timeout expires without a response condition being found.
   */
  private void readForResponses(String pass, String fail, long msTimeout) throws IOException, ResponseFailedException, ResponseTimeoutException {
    int passedMatchedChars = 0;
    int failedMatchedChars = 0;
    byte[] passBytes = pass.getBytes(defaulCharset);
    byte[] failBytes = fail.getBytes(defaulCharset);

    // Start timeout timer
    long startTime = System.currentTimeMillis();

    // Start timeout loop
    while(System.currentTimeMillis() < (startTime + msTimeout)) {
      if (canRead()) {
        int nextCharacter = inputStream.read();

        // Check and update the pass case
        if (nextCharacter == passBytes[passedMatchedChars]) {
          passedMatchedChars++;
          if (passedMatchedChars == passBytes.length) {
            return;
          }
        } else {
          passedMatchedChars = 0;
        }

        // Check and update the fail case
        if (nextCharacter == failBytes[failedMatchedChars]) {
          failedMatchedChars++;
          if (failedMatchedChars == failBytes.length) {
            throw new ResponseFailedException();
          } else {
            failedMatchedChars = 0;
          }
        }
      }
    }
    
    // Timeout expired
    throw new ResponseTimeoutException();
  }

  private void readForResponse(String response, long msTimeout) throws IOException, ResponseTimeoutException {
    int matchedChars = 0;
    byte[] responseBytes = response.getBytes(defaulCharset);

    // Start timeout timer
    long startTime = System.currentTimeMillis();

    // Start timeout loop
    while(System.currentTimeMillis() < (startTime + msTimeout)) {
      if (canRead()) {
        int nextCharacter = inputStream.read();

        // Check and update matched chars
        if (nextCharacter == responseBytes[matchedChars]) {
          matchedChars++;
          if (matchedChars == responseBytes.length) {
            return;
          }
        } else {
          // If no match, start over
          matchedChars = 0;
        }
      }
    }
    
    // Timeout expired
    throw new ResponseTimeoutException();
  }

  /**
   * Read data from the ESP8266 until a matched character is found.
   * 
   * @param matchedCharacter            The matched character to find.
   * @param msTimeout                   Timeout in milliseconds to find the matched character.
   * @throws IOException                Thrown by serial hardware if communication is interrupted.
   * @throws ResponseTimeoutException   Thrown if timeout expires without a match being found.
   */
  private void readUntil(char matchedCharacter, long msTimeout) throws IOException, ResponseTimeoutException {
    int matchedCharacterDecoded = String.valueOf(matchedCharacter).getBytes(defaulCharset)[0];

    // Start timeout timer
    long startTime = System.currentTimeMillis();

    // Start timeout loop
    while(System.currentTimeMillis() < (startTime + msTimeout)) {
      if (canRead()) {
        int nextCharacter = inputStream.read();
        if (nextCharacter == matchedCharacterDecoded) {
          return;
        }
      }
    }

    throw new ResponseTimeoutException();
  }

  /**
   * Read from the ESP8266 until either a character is matched, the max number of characters have been read,
   * or a timeout has elapsed.
   * 
   * @param matchedCharacter            The character to match.
   * @param maxBytes                    The max number of bytes to read.
   * @param msTimeout                   The timeout in milliseconds.
   * @return                            A {@link ByteBuffer} containing the bytes read.
   * @throws IOException                Thrown by serial hardware if communication is interrupted.
   * @throws ResponseTimeoutException   Thrown if timeout expires without a match being found.
   */
  private ByteBuffer readIntoByteBuffer(char matchedCharacter, int maxBytes, long msTimeout) throws IOException, ResponseTimeoutException {
    int matchedCharacterDecoded = String.valueOf(matchedCharacter).getBytes(defaulCharset)[0];
    int bytesRead = 0;
    ByteBuffer buffer = ByteBuffer.allocate(maxBytes);

    // Start timeout timer
    long startTime = System.currentTimeMillis();

    // Start timeout loop
    while(System.currentTimeMillis() < (startTime + msTimeout)) {
      if (canRead()) {
        int nextCharacter = inputStream.read();
        buffer.put((byte)nextCharacter);

        // If character matches, return
        if (nextCharacter == matchedCharacterDecoded) {
          return buffer;
        }

        bytesRead++;

        if (bytesRead >= maxBytes) {
          return buffer;
        }
      }
    }

    throw new ResponseTimeoutException();
  }

  /**
   * Disables ESP8266 echoing commands.
   * 
   * @param msTimeout                   Timeout waiting for command to reply.
   * @throws IOException                Thrown if serial hardware communication is interrupted.
   * @throws ResponseFailedException    Thrown if the command reply is not as expected.
   * @throws ResponseTimeoutException   Thrown if the timeout waiting for a reply failed.
   */
  public void disableEcho(long msTimeout) throws IOException, ResponseFailedException, ResponseTimeoutException {
    sendRawCommand(DISABLE_ECHO);
    sendRawCommand(LINE_END);
    readForResponses(RESPONSE_OK, RESPONSE_ERROR, msTimeout);
  }

  /**
   * Disables ESP8266 echoing commands.
   * 
   * @throws IOException                Thrown if serial hardware communication is interrupted.
   * @throws ResponseFailedException    Thrown if the command reply is not as expected.
   * @throws ResponseTimeoutException   Thrown if the default timeout waiting for a reply failed.
   */
  public void disableEcho() throws IOException, ResponseFailedException, ResponseTimeoutException {
    disableEcho(defaultMsTimeout);
  }

  /**
   * Enables ESP8266 echoing commands.
   * 
   * @param msTimeout                   Timeout waiting for command to reply.
   * @throws IOException                Thrown if serial hardware communication is interrupted.
   * @throws ResponseFailedException    Thrown if the command reply is not as expected.
   * @throws ResponseTimeoutException   Thrown if the timeout waiting for a reply failed.
   */
  public void enableEcho(long msTimeout) throws IOException, ResponseFailedException, ResponseTimeoutException {
    sendRawCommand(ENABLE_ECHO);
    sendRawCommand(LINE_END);
    readForResponses(RESPONSE_OK, RESPONSE_ERROR, msTimeout);
  }

  /**
   * Enables ESP8266 echoing commands.
   * 
   * @throws IOException                Thrown if serial hardware communication is interrupted.
   * @throws ResponseFailedException    Thrown if the command reply is not as expected.
   * @throws ResponseTimeoutException   Thrown if the default timeout waiting for a reply failed.
   */
  public void enableEcho() throws IOException, ResponseFailedException, ResponseTimeoutException {
    enableEcho(defaultMsTimeout);
  }

  /**
   * Gets firmware version as reported by AT+GMR command.
   * @return  The firmware version reported.
   * @param msTimeout                   Timeout waiting for command to reply.
   * @throws IOException        If unable to read from the serial port as expected.
   * @throws ResponseFailedException    Thrown if the command reply is not as expected.
   * @throws ResponseTimeoutException   Thrown if the timeout waiting for a reply failed.
   */
  public String getFirmwareVersion(long msTimeout) throws IOException, ResponseFailedException, ResponseTimeoutException {
    sendCommand(GETMODULERELEASE);
    // Convert byte array to string version
    String version = readLine(30, msTimeout);
    // Get the OK response
    readForResponses(RESPONSE_OK, RESPONSE_ERROR, msTimeout);
    return version;
  }

  /**
   * Gets firmware version as reported by AT+GMR command.
   * @return  The firmware version reported.
   * @throws IOException        If unable to read from the serial port as expected.
   * @throws ResponseFailedException    Thrown if the command reply is not as expected.
   * @throws ResponseTimeoutException   Thrown if the default timeout waiting for a reply failed.
   */
  public String getFirmwareVersion() throws IOException, ResponseFailedException, ResponseTimeoutException {
    return getFirmwareVersion(defaultMsTimeout);
  }

  /**
   * Sends a simple AT command to see if the device responds back
   * with OK predictably.
   * @param msTimeout                   Timeout waiting for command to reply.
   * @return                            true if ESP8266 replies with OK; false otherwise
   */
  public boolean isReady(long msTimeout) {
    try {
      sendCommand("");
      readForResponses(RESPONSE_OK, RESPONSE_ERROR, msTimeout);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Sends a simple AT command to see if the device responds back
   * with OK predictably. Uses default timeout.
   * @return  true if ESP8266 replies with OK; false otherwise
   */
  public boolean isReady() {
    return isReady(defaultMsTimeout);
  }

  /**
   * Get all access points within range of the module.
   *
   * @param msTimeout           Timeout waiting for command to reply.
   * @return  A {@link Set} of {@link AccessPoint} objects.
   * @throws IOException        If the serial port has a problem.
   * @throws ProtocolException  If protocol interaction with module does not function as expected.
   * @throws ResponseFailedException    Thrown if the command reply is not as expected.
   * @throws ResponseTimeoutException   Thrown if the timeout waiting for a reply failed.
   */
  public Set<AccessPoint> getAccessPoints(long msTimeout) throws IOException, ProtocolException, ResponseFailedException, ResponseTimeoutException {
    Set<AccessPoint> accessPoints = new HashSet<AccessPoint>();
    sendCommand(LISTACCESSPOINTS);
    String line = readLine(100, msTimeout);

    while(true) {
      switch (line) {
        case "":
          line = readLine(100, msTimeout);
          break;
        case RESPONSE_OK_TRIMMED:
          return accessPoints;
        case RESPONSE_ERROR_TRIMMED:
          throw new ResponseFailedException("Device not in station or dual mode.");
        default:
          AccessPoint accessPoint = new AccessPoint(line);
          accessPoints.add(accessPoint);
          line = readLine(100, msTimeout);
      }
    }
  }

  /**
   * Get all access points within range of the module.
   * @return  A {@link Set} of {@link AccessPoint} objects.
   * @throws IOException        If the serial port has a problem.
   * @throws ProtocolException  If protocol interaction with module does not function as expected.
   * @throws ResponseFailedException    Thrown if the command reply is not as expected.
   * @throws ResponseTimeoutException   Thrown if the default timeout waiting for a reply failed.
   */
  public Set<AccessPoint> getAccessPoints() throws IOException, ProtocolException, ResponseFailedException, ResponseTimeoutException {
    return getAccessPoints(defaultMsTimeout);
  }
  
  /**
   * Get the current Wifi mode of the module.
   * @return                    Mode of type {@link WifiModeEnum}
   * @param msTimeout           Timeout waiting for command to reply.
   * @throws IOException        If the serial port has a problem.
   * @throws ResponseTimeoutException   Thrown if the timeout waiting for a reply failed.
   * @throws ProtocolException  If mode returned does not match expected specification.
   */
  public WifiModeEnum getWifiMode(long msTimeout) throws IOException, ResponseTimeoutException, ProtocolException {
    sendCommand(GETWIFIMODE);
    readForResponse(REPLY_WIFI_MODE, msTimeout);
    String mode = new String(readIntoByteBuffer('\r', 1, msTimeout).array(), defaulCharset);
    readForResponse(RESPONSE_OK, msTimeout);
    return WifiModeEnum.getEnum(mode);
  }

  /**
   * Get the current Wifi mode of the module.
   * @return                    Mode of type {@link WifiModeEnum}
   * @throws IOException        If the serial port has a problem.
   * @throws ResponseTimeoutException   Thrown if the default timeout waiting for a reply failed.
   * @throws ProtocolException  If mode returned does not match expected specification.
   */
  public WifiModeEnum getWifiMode() throws IOException, ResponseTimeoutException, ProtocolException {
    return getWifiMode(defaultMsTimeout);
  }

  /**
   * Set the Wifi station mode.
   * @param mode                Mode of type {@link WifiModeEnum}
   * @param msTimeout           Timeout waiting for command to reply.
   * @throws IOException        If the serial port has a problem.
   * @throws ResponseTimeoutException   Thrown if the timeout waiting for a reply failed.
   */
  public void setWifiMode(WifiModeEnum mode, long msTimeout) throws IOException, ResponseTimeoutException {
    sendCommand(SETWIFIMODE + mode.getCode());
    String response = readLine(20, msTimeout);
    if (response.equals(NOCHANGE)) {
      return;
    }
    readForResponse(RESPONSE_OK, msTimeout);
  }

  /**
   * Set the Wifi station mode.
   * @param mode                Mode of type {@link WifiModeEnum}
   * @throws IOException        If the serial port has a problem.
   * @throws ResponseTimeoutException   Thrown if the timeout waiting for a reply failed.
   */
  public void setWifiMode(WifiModeEnum mode) throws IOException, ResponseTimeoutException {
    setWifiMode(mode, defaultMsTimeout);
  }

  /**
   * Restart the module. This need to be done when changing Wifi mode.
   *
   * @param msTimeout           Timeout waiting for command to reply.
   * @throws IOException        If the serial port has a problem.
   * @throws ResponseTimeoutException   Thrown if the timeout waiting for a reply expired.
   * @throws ResponseFailedException    Thrown if the command reply is not as expected.
   */
  public void restart(long msTimeout) throws IOException, ResponseTimeoutException, ResponseFailedException {
    sendCommand(RESTART);
    readForResponse(READY, msTimeout);
    disableEcho();
  }

  /**
   * Restart the module. This need to be done when changing Wifi mode.
   *
   * @throws IOException        If the serial port has a problem.
   * @throws ResponseTimeoutException   Thrown if the default timeout waiting for a reply expired.
   * @throws ResponseFailedException    Thrown if the command reply is not as expected.
   */
  public void restart() throws IOException, ResponseTimeoutException, ResponseFailedException {
    restart(defaultMsTimeout);
  }

  /**
   * Fetch the IP address of the device. Device must be in access point mode
   * or station mode and have an ip address assigned. Can return 0.0.0.0.
   * @return                    The IP address
   * @param msTimeout           Timeout waiting for command to reply.
   * @throws IOException        If the serial port has a problem.
   * @throws ResponseTimeoutException   Thrown if the msTimeout waiting for a reply expired.
   */
  public String getIPAddress(long msTimeout) throws IOException, ResponseTimeoutException {
    sendCommand(GETIP);
    String ip = readLine(20, msTimeout);
    readForResponse(RESPONSE_OK, msTimeout);
    return ip;
  }


  /**
   * Fetch the IP address of the device. Device must be in access point mode
   * or station mode and have an ip address assigned. Can return 0.0.0.0.
   * @return                    The IP address
   * @throws IOException        If the serial port has a problem.
   * @throws ResponseTimeoutException   Thrown if the default timeout waiting for a reply expired.
   */
  public String getIPAddress() throws IOException, ResponseTimeoutException {
    return getIPAddress(defaultMsTimeout);
  }

  /**
   * Set the multiplex mode for allowing up to 5 (0-4) simultaneous connections.
   * @param mux                 true means to allow mux mode; false to disallow
   * @param msTimeout           Timeout waiting for command to reply.
   * @throws IOException        If the serial port has a problem.
   * @throws ResponseTimeoutException   Thrown if the msTimeout waiting for a reply expired.
   */
  public void setMuxMode(boolean mux, long msTimeout) throws IOException, ResponseTimeoutException {
    if (mux) {
      sendCommand(SETMUXMODE + "1");
    } else {
      sendCommand(SETMUXMODE + "0");
    }
    readForResponse(RESPONSE_OK, msTimeout);
  }

  /**
   * Set the multiplex mode for allowing up to 5 (0-4) simultaneous connections.
   * @param mux                 true means to allow mux mode; false to disallow
   * @throws IOException        If the serial port has a problem.
   * @throws ResponseTimeoutException   Thrown if the default timeout waiting for a reply expired.
   */
  public void setMuxMode(boolean mux) throws IOException, ResponseTimeoutException {
    setMuxMode(mux, defaultMsTimeout);
  }

  public void startTCPServer(int port, long msTimeout) throws IOException, ResponseTimeoutException {
    if (port > 0) {
      sendCommand(SETIPSERVER + "1," + port);
    } else {
      sendCommand(SETIPSERVER + "1");
    }
    readForResponse(RESPONSE_OK, msTimeout);
  }

//  public void replyReady()
/*
  public void startUDPCommunication(int connectionId, 
      String remoteIPAddress, 
      int remotePort, 
      int localPort, 
      UDPPeerModeEnum udpPeerMode) {
    sendCommand(IPSTART + connectionId + ",UDP," + "\"" + remoteIPAddress + "\"," + remotePort + "," + localPort + "," + udpPeerMode.getCode());

  }
*/
}
