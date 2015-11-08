from twisted.internet.protocol import DatagramProtocol
from twisted.internet import reactor

from time import strftime, gmtime
from datetime import datetime
from elasticsearch import Elasticsearch
from json import loads

es = Elasticsearch(['elasticsearch'])

class NodeServer(DatagramProtocol):
	def datagramReceived(self, data, (host, port)):
		print "received %r from %s:%d" % (data, host, port)
		index = strftime("temperature-%Y.%m.%d", gmtime())
		doc = loads(data)
		doc["@timestamp"] = datetime.now()
		res = es.index(index=index, doc_type='temp', body=doc)
		if res['created'] == True:
			print "Sent to Elasticsearch OK"
		else:
			print "ERROR! ", res

reactor.listenUDP(3000, NodeServer())
reactor.run()
