# ESP8266 Java Library

## Overview
This is a Java class library that wraps communication functionalty of the ESP8266 AT command firmware. The ESP8266 class implements the capability by abstracting communication away from the serial port. The constructor takes a Java InputStream and OutputStream. The ./samples directory contains examples that demonstrate calling the library from a Windows/Mac/Linux operating system with the very good [jSerialComm](http://fazecast.github.io/jSerialComm/) serial port library. The ./boards directory demonstrate using this library on embedded boards, like the Raspberry Pi, with no operating system.

This library has been coded and tested against the [AI Thinker revision 018 of the AT command firmware](docs/Espressif AT instruction set AI Thinker version.html). This document was originally written in Chinese and is translated here in English for convenience. I wrote this library against this version because this was the revision installed on my ESP8266 boards (purchased some years ago). 

## Requirements
- Java JDK (my version was `openjdk 11.0.1-redhat 2018-10-16 LTS`, but earlier versions would probably work)
- A USB TTL serial cable like [this one from Adafruit](https://www.adafruit.com/product/954)
- An ESP-01 like [this one](https://solarbotics.com/product/29246/)
- A 3.3v power supply

## OS Usage Example
To run the operating system specific example (tested on Windows 10 but should work on OSX and Linux):
1. Clone the repo
2. Connect TTL cable to ESP-01 like this:
3. Change constants within `./samples/os/src/main/java/Main.java` according to comments to match your wifi environment.
4. Change directory to `./samples/os`
5. `gradlew run`

## Limitations
- Mux mode is not yet supported.
- TCP server mode is not supported.

## References
- [Syonyk/LiteESP8266Client](https://github.com/Syonyk/LiteESP8266Client)