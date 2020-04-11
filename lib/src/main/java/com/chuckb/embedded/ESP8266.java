package com.chuckb.embedded;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
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
 */
public class ESP8266 {
  private final PrintStream printStream;
  private final BufferedReader bufferedASCIIReader;
  private final String LINE_END = "\r\n";
  private final String AT = "AT";
  private final String GETMODULERELEASE = "GMR";
  private final String OK = "OK";
  private final String LISTACCESSPOINTS = "CWLAP";
  private final String GETWIFIMODE = "CWMODE?";
  private final String SETWIFIMODE = "CWMODE=";
  private final String NOCHANGE = "no change";
  private final String RESTART = "RST";
  private final String READY = "ready";
  private final String GETIP = "CIFSR";
  private final String SETMUXMODE = "CIPMUX=";
  private final String IPSTART = "CIPSTART=";
  private final String SETIPSERVER = "CIPSERVER=";

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
   * @param inputStream   An open {@link InputStream} for getting data from the device.
   * @param outputStream  An open {@link OutputStream} for sending data to the device.
   */
  public ESP8266(InputStream inputStream, OutputStream outputStream) {
    // Rig up streams so we can read/write to them easily.
    printStream = new PrintStream(outputStream, true, Charset.forName("US-ASCII"));
    bufferedASCIIReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("US-ASCII")));
  }

  /**
   * Send a command to the module. This function will prepend the AT start sequence
   * and flush the contents to the module automatically.
   * @param command
   */
  private void sendCommand(String command) {
    if (command == "") {
      printStream.print(AT);
    } else {
      printStream.print(AT + "+" + command);
    }
    printStream.print(LINE_END);
    printStream.flush();
  }

  /**
   * Gets firmware version as reported by AT+GMR command.
   * @return  The firmware version reported.
   * @throws IOException        If unable to read from the serial port as expected.
   * @throws ProtocolException  If protocol interaction with module does not function as expected.
   */
  public String getFirmwareVersion() throws IOException, ProtocolException {
    sendCommand(GETMODULERELEASE);
    String line = bufferedASCIIReader.readLine();      // Echo what was just sent
    line = bufferedASCIIReader.readLine();             // Blank line
    line = bufferedASCIIReader.readLine();             // Should be the version
    String response = bufferedASCIIReader.readLine();  // Blank line
    response = bufferedASCIIReader.readLine();         // Should be the OK response
    if (!response.equals(OK)) {
      throw new ProtocolException();
    }
    return line;
  }

  /**
   * Sends a simple AT command to see if the device responds back
   * with OK predictably.
   */
  public boolean isReady() {
    try {
      sendCommand("");
      String response = bufferedASCIIReader.readLine();      // Echo what was just sent
      response = bufferedASCIIReader.readLine();             // Blank line
      response = bufferedASCIIReader.readLine();             // Blank line
      response = bufferedASCIIReader.readLine();             // Should be OK response
      if (response.equals(OK)) {
        return true;
      }
        return false;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Get all access points within range of the module.
   * @return  A {@link Set} of {@link AccessPoint} objects.
   * @throws IOException        If the serial port has a problem.
   * @throws ProtocolException  If protocol interaction with module does not function as expected.
   */
  public Set<AccessPoint> getAccessPoints() throws IOException, ProtocolException {
    Set<AccessPoint> accessPoints = new HashSet<AccessPoint>();
    sendCommand(LISTACCESSPOINTS);
    String line = bufferedASCIIReader.readLine();      // Echo what was just sent
    line = bufferedASCIIReader.readLine();             // Prime pump by reading next line
    while(true) {
      switch (line) {
        case "":
          line = bufferedASCIIReader.readLine();
          break;
        case "OK":
          return accessPoints;
        case "ERROR":
          throw new ProtocolException("Device not in station or dual mode.");
        default:
          if (isAccessPoint(line)) {
            AccessPoint accessPoint = getAccessPoint(line);
            accessPoints.add(accessPoint);
            line = bufferedASCIIReader.readLine();
          } else {
            throw new ProtocolException("Access point [" + line + "] is not valid.");
          }
      }
    }
  }

  /**
   * Get the current Wifi mode of the module.
   * @return                    Mode of type {@link WifiModeEnum}
   * @throws IOException        If the serial port has a problem.
   * @throws ProtocolException  If protocol interaction with module does not function as expected.
   */
  public WifiModeEnum getWifiMode() throws IOException, ProtocolException {
    sendCommand(GETWIFIMODE);
    bufferedASCIIReader.readLine();             // Echo what was just sent
    bufferedASCIIReader.readLine();             // Blank line
    try (Scanner scanner = new Scanner(bufferedASCIIReader.readLine())) {
      scanner.useDelimiter("\\+CWMODE:");
      bufferedASCIIReader.readLine();           // Blank line
      if (bufferedASCIIReader.readLine().equals(OK)) {
        return WifiModeEnum.getEnum(scanner.next());
      } else {
        throw new ProtocolException();
      }
    }
  }

  /**
   * Set the Wifi station mode.
   * @param mode                Mode of type {@link WifiModeEnum}
   * @throws IOException        If the serial port has a problem.
   * @throws ProtocolException  If protocol interaction with module does not function as expected.
   */
  public void setWifiMode(WifiModeEnum mode) throws IOException, ProtocolException {
    sendCommand(SETWIFIMODE + mode.getCode());
    String line = bufferedASCIIReader.readLine();        // Echo what was just sent
    line = bufferedASCIIReader.readLine();               // Blank line
    line = bufferedASCIIReader.readLine();               // Sometimes "no change" will be emitted if there is no change
    if (line.equals(NOCHANGE)) {
      return;
    }
    line = bufferedASCIIReader.readLine();                // Should be "OK"
    if (line.equals(OK)) {
      return;
    } else {
      throw new ProtocolException();
    }
  }

  /**
   * Restart the module. This need to be done when changing Wifi mode.
   * @throws IOException        If the serial port has a problem.
   * @throws ProtocolException  If protocol interaction with module does not function as expected.
   */
  public void restart() throws IOException, ProtocolException {
    sendCommand(RESTART);
    bufferedASCIIReader.readLine();                 // Echo command
    bufferedASCIIReader.readLine();                 // Blank line
    bufferedASCIIReader.readLine();                 // Blank line
    String line = bufferedASCIIReader.readLine();   // Response code
    if (line.equals(OK)) {
      bufferedASCIIReader.readLine();               // Binary junk
      bufferedASCIIReader.readLine();               // Vendor line
      bufferedASCIIReader.readLine();               // Blank line
      if (bufferedASCIIReader.readLine().equals(READY)) {                // Ready line
        return;
      } else {
        throw new ProtocolException();
      }
    } else {
      throw new ProtocolException();
    }
  }

  /**
   * Fetch the IP address of the device. Device must be in access point mode
   * or station mode and have an ip address assigned. Can return 0.0.0.0.
   * @return                    The IP address
   * @throws IOException        If the serial port has a problem.
   * @throws ProtocolException  If protocol interaction with module does not function as expected.
   */
  public String getIPAddress() throws IOException, ProtocolException {
    sendCommand(GETIP);
    bufferedASCIIReader.readLine();                 // Echo command
    bufferedASCIIReader.readLine();                 // Blank line
    String ip = bufferedASCIIReader.readLine();     // IP
    bufferedASCIIReader.readLine();                 // Blank line
    if (bufferedASCIIReader.readLine().equals(OK)) {
      return ip;
    } else {
      throw new ProtocolException();
    }
  }

  /**
   * Set the multiplex mode for allowing up to 5 (0-4) simultaneous connections.
   * @param mux                 true means to allow mux mode; false to disallow
   * @throws IOException        If the serial port has a problem.
   * @throws ProtocolException  If protocol interaction with module does not function as expected.
   */
  public void setMuxMode(boolean mux) throws IOException, ProtocolException {
    if (mux) {
      sendCommand(SETMUXMODE + "1");
    } else {
      sendCommand(SETMUXMODE + "0");
    }
    bufferedASCIIReader.readLine();                 // Echo command
    bufferedASCIIReader.readLine();                 // Blank line
    bufferedASCIIReader.readLine();                 // Blank line
    if (bufferedASCIIReader.readLine().equals(OK)) {
      return;
    } else {
      throw new ProtocolException();
    }
  }

  public void startTCPServer(int port) throws IOException, ProtocolException {
    if (port > 0) {
      sendCommand(SETIPSERVER + "1," + port);
    } else {
      sendCommand(SETIPSERVER + "1");
    }
    bufferedASCIIReader.readLine();                 // Echo command
    bufferedASCIIReader.readLine();                 // Blank line
    bufferedASCIIReader.readLine();                 // Blank line
    if (bufferedASCIIReader.readLine().equals(OK)) {
      return;
    } else {
      throw new ProtocolException();
    }
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

  /**
   * Determines whether a line from the string returned from AT+CWLAP looks like an access point string.
   * @param stringToTest
   * @return              True is it looks like an access point.
   */
  private boolean isAccessPoint(String stringToTest) {
    if (stringToTest.substring(0, 8).equals("+CWLAP:(")) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Take the access point string from the module and parse it out into an {@link AccessPoint} object.
   * @param accessPointString   One line of a string returned from AT+CWLAP
   * @return                    {@link AccessPoint}
   * @throws ProtocolException  If protocol interaction with module does not function as expected.
   */
  private AccessPoint getAccessPoint(String accessPointString) throws ProtocolException {
    AccessPoint accessPoint = new AccessPoint();
    try (Scanner scanner = new Scanner(extractAccessPointParameters(accessPointString))) {
      scanner.useDelimiter(",");
      accessPoint.encryption = AccessPoint.EncryptionEnum.getEnum(scanner.next());
      accessPoint.SSID = stripQuotes(scanner.next());
      accessPoint.RSSI = scanner.nextInt();
      accessPoint.macAddress = stripQuotes(scanner.next());
      accessPoint.channel = scanner.nextInt();
      return accessPoint;  
    }
  }

  /**
   * Given a line from the string returned from AT+CWLAP, cut out just the parameters for parsing.
   * @param accessPointString
   * @return                    Comma delimited access point parameters.
   */
  private String extractAccessPointParameters(String accessPointString) {
    String parms = accessPointString.substring(8, accessPointString.length() - 1);
    return parms;
  }

  /**
   * If an access point parameter is delimited by double quotes, strip them.
   * @param quotedString
   * @return              The string without double quotes.
   */
  private String stripQuotes(String quotedString) {
    if (quotedString.startsWith("\"") && quotedString.endsWith("\"")) {
      return quotedString.substring(1, quotedString.length() - 1);
    } else {
      return quotedString;
    }
  }
}
