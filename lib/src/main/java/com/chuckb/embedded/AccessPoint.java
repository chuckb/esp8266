package com.chuckb.embedded;

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
}