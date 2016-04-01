from SCons.Script import DefaultEnvironment
import json

env = DefaultEnvironment()
config = json.loads(open("../config.json").read())
for k in config.keys():
	if type(config[k]) == unicode:
		formatted = "\\\"%s\\\"" % config[k]
	else:
		formatted = config[k]
	env["CPPDEFINES"].append("%s=%s" % (k, formatted))
