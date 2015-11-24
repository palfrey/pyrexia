from jinja2 import Environment, FileSystemLoader
import json
import os

conf = json.load(open("config.json"))
env = Environment(loader=FileSystemLoader('.'))
conf["pwd"] = os.getcwd()

template = env.get_template('app/logstash/config/logstash.conf.template')
open("app/logstash/config/logstash.conf", "w").write(template.render(conf))

template = env.get_template('node/create_msg.py.template')
open("node/create_msg.py", "w").write(template.render(conf))

template = env.get_template('node/raspi/config.py.template')
open("node/raspi/config.py", "w").write(template.render(conf))

template = env.get_template('node/raspi/init-script.template')
open("node/raspi/init-script", "w").write(template.render(conf))

template = env.get_template('node/nodemcu/config.json.template')
open("node/nodemcu/config.json", "w").write(template.render(conf))
