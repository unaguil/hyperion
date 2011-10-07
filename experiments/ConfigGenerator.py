from xml.dom.minidom import *
from InterpolatedEntry import *

class ConfigGenerator:
	def __init__(self, expConfigFile):
		expConfig = parse(expConfigFile)
		experiment = expConfig.documentElement
		self.__tag = experiment.getAttribute('tag')
		self.__type = experiment.getAttribute('type') 
		
		staticConfig = expConfig.getElementsByTagName("staticConfig")		
		self.staticEntries = staticConfig[0].getElementsByTagName("entry")
		
		#Obtain the number of repetitions for each configuration
		repeatEntry = expConfig.getElementsByTagName("repeat")
		if len(repeatEntry) == 0:
			self.repeat = 3
		else: 
			self.repeat = int(repeatEntry[0].getAttribute("number"))
	
		self.interpolatedEntries = []

		dynamicEntries = expConfig.getElementsByTagName("dynamicEntry")
		if not dynamicEntries:
			self.interpolatedEntries.append(InterpolatedEntry('NONE', 'LinearInterpolator', 1, 1, 1))
		for dynamicEntry in dynamicEntries:
			key = dynamicEntry.getAttribute("key")
			interpolator = dynamicEntry.getAttribute("interpolator")
			start = int(dynamicEntry.getAttribute("start"))
			end = int(dynamicEntry.getAttribute("end"))
			step = int(dynamicEntry.getAttribute("step"))
			self.interpolatedEntries.append(InterpolatedEntry(key, interpolator, start, end, step))

	def hasNext(self):
		return self.interpolatedEntries[0].hasNext()		

	def next(self):
		start = """<?xml version="1.0" encoding="UTF-8"?>\n""" + """<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">\n""" + """<properties>"""
		
		end = """</properties>"""

		output = start

		for staticEntry in self.staticEntries:
			output = output + staticEntry.toxml() + "\n"

		interpolatedEntry = self.interpolatedEntries[0]

		value = interpolatedEntry.next()

		dynamicEntry = "<entry key=\"" + interpolatedEntry.getKey() + "\">" + str(value) + "</entry>"

		output = output + dynamicEntry + "\n"
		
		output = output + "<entry key=\"taxonomyFile\">taxonomy.xml</entry>"

		output = output + end

		return output
	
	def getTag(self):
		return self.__tag
	
	def getType(self):
		return self.__type
	
	def getRepeat(self):
		return self.repeat
	
def main():
	configGenerator = ConfigGenerator("ExpConfig.xml")

	while configGenerator.hasNext():
		config = configGenerator.next()
		print config

if __name__ == '__main__':
	 main()

