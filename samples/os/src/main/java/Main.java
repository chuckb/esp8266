import java.io.IOException;

import com.chuckb.embedded.ESP8266;
import com.chuckb.embedded.ESP8266.ProtocolException;
import com.fazecast.jSerialComm.*;

public class Main {
  public static void main() throws ProtocolException, IOException {
    String port = "";

    SerialPort comPort = SerialPort.getCommPort(port);
    comPort.setBaudRate(115200);
    comPort.openPort();
    ESP8266 esp8266 = new ESP8266(comPort.getInputStream(), comPort.getOutputStream());
    System.out.println(esp8266.getFirmwareVersion());
    comPort.closePort();
  }
}