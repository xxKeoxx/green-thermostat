/**
 *  HVAC Auto Off
 *
 *  Author: jprosiak@sbcglobal.net
 *  Date: 2016-04-05
 */

definition(
    name: "Green Thermostat",
    namespace: "xxKeoxx",
    author: "jprosiak@sbcglobal.net",
    description: "Automatically turn off thermostat if a contact sensor is open. Turn it back on when everything is closed up.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    oauth: true
)

preferences {
	section("Control") {
		input("thermostat", "capability.thermostat", title: "Thermostat")
	}
    
    section("Open/Close") {
    	input("sensors", "capability.contactSensor", title: "Sensors", multiple: true)
        input("delay", "number", title: "Delay (minutes) before turning thermostat off")
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	turnOff()
	state.changed = false
    state.thermostatMode = thermostat.currentValue("thermostatMode")
    subscribe(thermostat, 'thermostatMode', "thermostatChange")
	subscribe(sensors, 'contact', "sensorChange")
    //log.debug "State Info: $state"
}

def contactOpenList() {
	def result = sensors.findAll() { it.currentValue('contact') == 'open'; }
    return result
    log.debug "Open results: $result"
}

def thermostatChange(evt){
	if (thermostat.currentValue("thermostatMode") != "off"){
		log.debug "termostat is " + thermostat.currentValue("thermostatMode")
        turnOff()
    }
}

def sensorChange(evt) {
	//log.debug "Desc: $evt.value , $state"
    def result = contactOpenList()
    log.debug "Desc: $evt.value , $result"
    if(evt.value == 'open' && (contactOpenList.size() < "1")) {    	
        log.debug "getting ready to turn off"
    	runIn(delay * 60, 'turnOff')
	} else if(evt.value == 'closed' && !result) {
        log.debug "getting ready to restore"
        unschedule()
    	restore()
    }
}


def turnOff() {
    def result = contactOpenList()
    if (result){
    	def sensorList = result.join(", ")
    	log.debug "Turning off thermostat.  The following contacts are open: $sensorList"
    	state.thermostatMode = thermostat.currentValue("thermostatMode")
    	thermostat.off()
    	state.changed = true
    	log.debug "State: $state"
		sendPush("I changed ${thermostat} to ${state.thermostatMode} because The following contacts are open: ${sensorList}")
	}
}

def restore() {
    log.debug "Setting thermostat to $state.thermostatMode"
    thermostat.setThermostatMode(state.thermostatMode)
    state.changed = false
    sendPush("I changed ${thermostat} back to ${state.thermostatMode} because everything is closed.")
}