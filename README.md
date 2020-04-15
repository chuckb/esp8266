# ESP8266 Java Library

## Overview
This is a Java class library that wraps communication functionalty of the ESP8266 AT command firmware. The ESP8266 class implements the capability by abstracting communication away from the serial port. The constructor takes a Java InputStream and OutputStream. The ./samples directory contains examples that demonstrate calling the library from a Windows/Mac/Linux operating system with the very good [jSerialComm](http://fazecast.github.io/jSerialComm/) serial port library. The ./boards directory demonstrate using this library on embedded boards, like the Raspberry Pi, with no operating system.

This library has been coded and tested against the [AI Thinker revision 018 of the AT command firmware](https://htmlpreview.github.io/?https://github.com/chuckb/esp8266/blob/master/docs/Espressif%20AT%20instruction%20set%20AI%20Thinker%20version.html). This document was originally written in Chinese and is translated here in English for convenience. I wrote this library against said version because this was the revision installed on my ESP-01 boards (purchased several years ago). I did not want to have to flash upgrades, etc.

## Requirements
- Java JDK (my version was `openjdk 11.0.1-redhat 2018-10-16 LTS`, but earlier versions would probably work)
- A USB TTL serial cable like [this one from Adafruit](https://www.adafruit.com/product/954)
- An ESP-01 like [this one](https://solarbotics.com/product/29246/)
- A 3.3v power supply or alternative to get 5v down to somewhere between 3.3 and 4v @ 250ma
  - In my example, I use the voltage drop of a standard germanium diode (0.7v) to get voltage in range, and connected it to the supply from the USB cable (which is capable of 500 ma). Note: the specs say ESP-01 voltage requirement is 3.3v - I take no responsibility if this unregulated overvolt technique burns out your board. It works for mine.

## OS Usage Demo Example
To run the operating system specific example (tested on Windows 10 but should work on OSX and Linux):
1. Clone the repo
2. Connect TTL cable to ESP-01 like this:
```
                               +------------+  +------------------------------+
                               |            |  |                              |
   +-----------+   +---->|-----+----)|----+ |  | +-------------+              |
   |   CP2012  |   |  1N4148      100 mfd | |  | |    ESP-01   |              |
   |           |   |                      | |  +-+ TX      GND +---|'         |
   |   +5v Red +----                      | |    |             |              |
   |           |                          | +----+ CH_PD GPIO2 +              |
   | Gnd Black +------------------+-------+ |    |             |              |   ESP-01 top view
   |           |                  |         |    + RST   GPIO0 +              |   Antenna on this side
   |  Tx Green +------------+     __        |    |             |              |
   |           |            |      '        +----+ VCC      RX +----+         |
   |  Rx White +----+       |                    |             |    |         |
   |           |    |       |                    +-------------+    |         |
   +-----------+    |       |                                       |         |
                    |       +---------------------------------------+         |
                    |                                                         |
                    +---------------------------------------------------------+
                  
```
3. Change constants within `./samples/os/src/main/java/Main.java` according to comments to match your wifi environment.
4. Change directory to `./samples/os`
5. `gradlew run`

## Limitations
- Mux mode is not yet supported.
- TCP server mode is not supported.

## References
- [Syonyk/LiteESP8266Client](https://github.com/Syonyk/LiteESP8266Client)