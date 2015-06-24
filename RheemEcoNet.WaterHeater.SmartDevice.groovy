/**
 *  Rheem Econet Water Heater
 *
 *  Copyright 2015 Jason Mok
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Rheem Econet Water Heater", namespace: "copy-ninja", author: "Jason Mok") {
		capability "Actuator"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Thermostat Heating Setpoint"
		
		command "heatLevelUp"
		command "heatLevelDown"
	}

	simulator { }

	tiles {
		valueTile("heatingSetpoint", "device.heatingSetpoint", inactiveLabel: false, width: 2, height: 2) {
			state("heatingSetpoint", label:'${currentValue}°',
				backgroundColors:[
					[value: 90,  color: "#f49b88"],
					[value: 100, color: "#f28770"],
					[value: 110, color: "#f07358"],
					[value: 120, color: "#ee5f40"],
					[value: 130, color: "#ec4b28"],
					[value: 140, color: "#ea3811"]					
				]
			)
		}
		standardTile("heatLevelUp", "device.switch", canChangeIcon: false, inactiveLabel: true, decoration: "flat" ) {
			state("heatLevelUp",   action:"heatLevelUp",   icon:"st.thermostat.thermostat-up", backgroundColor:"#F7C4BA")
		}  
		standardTile("heatLevelDown", "device.switch", canChangeIcon: false, inactiveLabel: true, decoration: "flat") {
			state("heatLevelDown", action:"heatLevelDown", icon:"st.thermostat.thermostat-down", backgroundColor:"#F7C4BA")
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state("default", action:"refresh.refresh",        icon:"st.secondary.refresh")
		}
		main "heatingSetpoint"
		details(["heatingSetpoint", "heatLevelUp", "heatLevelDown", "refresh"])
	}
}

def parse(String description) { }

// handle commands
def poll() { updateDeviceData(parent.getDeviceData(this.device)) }

def refresh() { parent.refresh() }

def setHeatingSetpoint(Number heatingSetPoint) {
	// set maximum & minimum for heating setpoint
	def actualData = parent.getDeviceData(this.device).clone()
    def deviceData = convertTemperatureUnit(actualData.clone(), getTemperatureScale())
    
	heatingSetPoint = (heatingSetPoint < deviceData.minTemp)? deviceData.minTemp : heatingSetPoint
	heatingSetPoint = (heatingSetPoint > deviceData.maxTemp)? deviceData.maxTemp : heatingSetPoint
	deviceData.setPoint = heatingSetPoint
    
	updateDeviceData(deviceData)    
	
	def deviceSetData = convertTemperatureUnit(deviceData, actualData.temperatureUnit)
	parent.setDeviceSetPoint(this.device, deviceSetData)
}

def heatLevelUp() { 
	def actualData = parent.getDeviceData(this.device).clone()
	def deviceData = convertTemperatureUnit(actualData.clone(), getTemperatureScale())
	def heatingSetPoint = device.currentValue("heatingSetpoint")
	actualData.setPoint = (actualData.temperatureUnit != getTemperatureScale())?((actualData.temperatureUnit=="F")?(celsiusToFahrenheit(heatingSetPoint).toInteger()):(fahrenheitToCelsius(heatingSetPoint).toInteger())):heatingSetPoint
	actualData.setPoint = ((actualData.setPoint + 1) > actualData.maxTemp)? actualData.maxTemp : (actualData.setPoint + 1)
	heatingSetPoint = (actualData.temperatureUnit != getTemperatureScale())?(heatingSetPoint = (actualData.temperatureUnit == "F")?(fahrenheitToCelsius(actualData.setPoint).toInteger()):(celsiusToFahrenheit(actualData.setPoint).toInteger())):(actualData.setPoint)
	deviceData.setPoint = heatingSetPoint
	updateDeviceData(deviceData) 
	setHeatingSetpoint(heatingSetPoint) 
}	

def heatLevelDown() { 
	def actualData = parent.getDeviceData(this.device).clone()
	def deviceData = convertTemperatureUnit(actualData.clone(), getTemperatureScale())
	def heatingSetPoint = device.currentValue("heatingSetpoint")
	actualData.setPoint = (actualData.temperatureUnit != getTemperatureScale())?((actualData.temperatureUnit=="F")?(celsiusToFahrenheit(heatingSetPoint).toInteger()):(fahrenheitToCelsius(heatingSetPoint).toInteger())):heatingSetPoint
	actualData.setPoint = ((actualData.setPoint - 1) > actualData.maxTemp)? actualData.maxTemp : (actualData.setPoint - 1)
	heatingSetPoint = (actualData.temperatureUnit != getTemperatureScale())?(heatingSetPoint = (actualData.temperatureUnit == "F")?(fahrenheitToCelsius(actualData.setPoint).toInteger()):(celsiusToFahrenheit(actualData.setPoint).toInteger())):(actualData.setPoint)
	deviceData.setPoint = heatingSetPoint
	updateDeviceData(deviceData) 
	setHeatingSetpoint(heatingSetPoint) 
}


def updateDeviceData(actualData = []) {
	def deviceData = convertTemperatureUnit(actualData, getTemperatureScale())
	sendEvent(name: "heatingSetpoint", value: deviceData.setPoint, unit: getTemperatureScale())
}

def convertTemperatureUnit(actualData = [], temperatureUnit) {
	def deviceData = actualData.clone()
    if (deviceData.temperatureUnit != temperatureUnit) { 
		if (deviceData.temperatureUnit == "F") {
			deviceData.temperatureUnit = "C"
			deviceData.setPoint = fahrenheitToCelsius(deviceData.setPoint) as Integer
			deviceData.maxTemp = fahrenheitToCelsius(deviceData.maxTemp) as Integer
			deviceData.minTemp = fahrenheitToCelsius(deviceData.minTemp) as Integer
		} else {
			deviceData.temperatureUnit = "F"
			deviceData.setPoint = celsiusToFahrenheit(deviceData.setPoint) as Integer
			deviceData.maxTemp = celsiusToFahrenheit(deviceData.maxTemp) as Integer
			deviceData.minTemp = celsiusToFahrenheit(deviceData.minTemp) as Integer
		}
	}
    return deviceData
}
