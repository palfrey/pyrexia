from jinja2 import Environment, FileSystemLoader
import json

conf = json.load(open("config.json"))
env = Environment(loader=FileSystemLoader('.'))

template = env.get_template('logstash/config/logstash.conf.template')
open("logstash/config/logstash.conf", "w").write(template.render(conf))

template = env.get_template('create_msg.py.template')
open("create_msg.py", "w").write(template.render(conf))
