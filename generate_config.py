from jinja2 import Environment, FileSystemLoader
import json

conf = json.load(open("config.json"))
env = Environment(loader=FileSystemLoader('.'))

template = env.get_template('app/logstash/config/logstash.conf.template')
open("app/logstash/config/logstash.conf", "w").write(template.render(conf))

template = env.get_template('node/create_msg.py.template')
open("node/create_msg.py", "w").write(template.render(conf))

template = env.get_template('node/raspi/config.py.template')
open("node/raspi/config.py", "w").write(template.render(conf))
