NodeMCU setup
-------------

1. &lt;copy config.json&gt;
2. `python generate_config.py`
3. `cd node/nodemcu`
4. Goto [NodeMCU custom builds](http://nodemcu-build.com/) and get a build from the dev branch with the following modules:
  * node, file, GPIO, WiFi, net, timer, UART, bit, MQTT, cJSON, DHT
5. Use esptool to flash the image
  * `./esptool/esptool.py --baud 230400 --port <tty port for your system> write_flash 0x0000 <image name>` (port is probably /dev/ttyUSB* for Linux, /dev/tty.wchusbserial* for OS X)
6. Upload the source and config files
  * `./uploader/-nodemcu-uploader.py --port <tty port for your system> upload init.lua config.json -r`
7. Plug the connections from the AM2302 into the these pins (see [here](http://www.14core.com/wp-content/uploads/2015/06/Node-MCU-Pin-Out-Diagram1.png) for a layout guide to the Nodemcu)

  1. 3.3V
  2. GPIO 4
  3. [not connected]
  4. Ground

FAQ
===
  * I can't reprogram the init.lua because it's already dumping output to serial
    * Delete the old init.lua first (try "file format" with the uploader) and reboot the node, as that's quite fast, then re-upload.
  * I can't connect to to my NodeMCU on OS X
    * If you're running OS X >= 10.11, the drivers might not be allowed any more. Try https://www.reddit.com/r/OSXElCapitan/comments/3d3uey/did_el_capitan_block_kext_signing_to_create/ct1w6zl if you're feeling adventurous, otherwise try another OS (Linux and Windows both have usable drivers)
  * I can't seem to flash my NodeMCU for some reason
    * Try https://github.com/nodemcu/nodemcu-flasher on Windows as I've had more luck with that
