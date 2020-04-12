import java.io.IOException;
import java.util.Set;

import com.chuckb.embedded.AccessPoint;
import com.chuckb.embedded.ESP8266;
import com.chuckb.embedded.ProtocolException;
import com.chuckb.embedded.ResponseFailedException;
import com.chuckb.embedded.ResponseTimeoutException;
import com.fazecast.jSerialComm.*;

public class Main {
  public static void main(String[] args) throws IOException, ProtocolException, ResponseFailedException, ResponseTimeoutException {
    String port = "COM5";

    SerialPort comPort = SerialPort.getCommPort(port);
    comPort.setBaudRate(9600);
    comPort.openPort();
    comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 200, 200);
    ESP8266 esp8266 = new ESP8266(comPort.getInputStream(), comPort.getOutputStream());
    System.out.println(esp8266.isReady());
    System.out.println(esp8266.getFirmwareVersion());
    esp8266.setWifiMode(ESP8266.WifiModeEnum.STATION);
    System.out.println("Set to station mode.");
    esp8266.restart(3000);
    System.out.println("Restarted module.");
    Set<AccessPoint> accessPoints = esp8266.getAccessPoints(3000);
    accessPoints.forEach(accessPoint -> {
      System.out.print(accessPoint.encryption + " ");
      System.out.print(accessPoint.SSID + " ");
      System.out.print(accessPoint.RSSI + " ");
      System.out.print(accessPoint.macAddress + " ");
      System.out.print(accessPoint.channel + "\r\n");
    });
    System.out.println(esp8266.getWifiMode());
    esp8266.setWifiMode(ESP8266.WifiModeEnum.ACCESSPOINT);
    System.out.println("Set to access point mode.");
    esp8266.restart(3000);
    System.out.println("Restarted module.");
    System.out.println(esp8266.getWifiMode());
    System.out.println(esp8266.getIPAddress());
    esp8266.setMuxMode(true);
    System.out.println("Turned on mux mode.");
    comPort.closePort();
  }
}