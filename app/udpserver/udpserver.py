from twisted.internet.protocol import DatagramProtocol
from twisted.internet import reactor

class NodeServer(DatagramProtocol):
    def datagramReceived(self, data, (host, port)):
        print "received %r from %s:%d" % (data, host, port)

reactor.listenUDP(3000, NodeServer())
reactor.run()
