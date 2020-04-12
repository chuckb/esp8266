package com.chuckb.embedded;

import java.util.Scanner;

public class AccessPoint {
  public enum EncryptionEnum 
  { 
    OPEN("0"), WEP("1"), WPA_PSK("2"), WPA2_PSK("3"), WPA_WPA2_PSK("4");
  
    private String code;     
    private EncryptionEnum(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public static EncryptionEnum getEnum(String code) throws ProtocolException {
    
      switch (code) {
        case "0":
          return OPEN;
        case "1":
          return WEP;
        case "2":
          return WPA_PSK;
        case "3":
          return WPA2_PSK;
        case "4":
          return WPA_WPA2_PSK;
        default:
          throw new ProtocolException("Unexpected encryption code.");
      }
    }    
  } 
  
  public EncryptionEnum encryption;
  public String SSID;
  public int RSSI;
  public String macAddress;
  public int channel;

  /**
   * Construct an access point given a CWLAP string from the ESP8266.
   * 
   * @param accessPointString     The string to parse.
   * @throws ProtocolException
   */
  public AccessPoint(String accessPointString) throws ProtocolException {
    if (isAccessPoint(accessPointString)) {
      parseAccessPoint(accessPointString);
    } else {
      throw new ProtocolException("Access point [" + accessPointString + "] is not valid.");
    }
  }

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
   * @throws ProtocolException  If protocol interaction with module does not function as expected.
   */
  private void parseAccessPoint(String accessPointString) throws ProtocolException {
    try (Scanner scanner = new Scanner(extractAccessPointParameters(accessPointString))) {
      scanner.useDelimiter(",");
      encryption = AccessPoint.EncryptionEnum.getEnum(scanner.next());
      SSID = stripQuotes(scanner.next());
      RSSI = scanner.nextInt();
      macAddress = stripQuotes(scanner.next());
      channel = scanner.nextInt();
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