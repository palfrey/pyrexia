from uuid import getnode as get_mac

URL = "amqp://qyiyktsa:bNk2OIFo-i32AxFyVEHMXQzQV_Y8yjKY@bunny.cloudamqp.com/qyiyktsa"

# Grabbed from http://stackoverflow.com/questions/159137/getting-mac-address#comment42261244_159195
NODE_ID = "temp-" + ':'.join(("%012x" % get_mac())[i:i+2] for i in range(0, 12, 2))
PIN = 4
