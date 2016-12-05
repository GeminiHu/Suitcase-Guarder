/* BT_MultiSensor_TestData_LED_CSV */

#include <SoftwareSerial.h>
#include "pitches.h"

#define rxPin 10  // connected to Bluetooth module Rx pin
#define txPin 11  // connected to Bluetooth module Tx pin

#define ledPin 12 // connected to LED via R=330 Ohm

#define aref_voltage 3.3         // we tie 3.3V to ARef and measure it with a multimeter!


const int   lumpin      = A2, frcepin      = A1;  // slider is connected to analog pin A0
int         dVoltageVal_lum = 0, dVoltageVal_frc = 0;   // variable to hold digitized ADC voltage value
float       fVoltageVal_lum = 0.0, fVoltageVal_frc = 0.0; // floating value of voltage reading
static char sVoltageVal_lum[10], sVoltageVal_frc[6],stemperatureC[6],stemperatureF[6];   // formatted float string representation of voltage

int tempPin = A0;        //the analog pin the TMP36's Vout (sense) pin is connected to
                        //the resolution is 10 mV / degree centigrade with a
                        //500 mV offset to allow for negative temperatures
int tempReading;        // the analog reading from the sensor
int CommBegin=0;

// Set up a new (software-based) serial port
SoftwareSerial btSerial(rxPin, txPin);

byte data = '0'; // a byte received from Android

// notes in the melody:
int melody[] = {
  NOTE_C4, NOTE_G3, NOTE_G3, NOTE_A3, NOTE_G3, 0, NOTE_B3, NOTE_C4
};

// note durations: 4 = quarter note, 8 = eighth note, etc.:
int noteDurations[] = {
  4, 8, 8, 4, 4, 4, 4, 4
};

unsigned long time;
//int rcv = 0; //receive data from android, set rcv = 1

void setup(){


  // initialize digital pin ledPin for output.
  pinMode(ledPin, OUTPUT);
  pinMode(lumpin, INPUT);
  pinMode(frcepin, INPUT);

  // Set up PC/Arduino HardSerial port
  Serial.begin(9600);
  
  // Set up Android/Arduino SoftwareSerial port
  btSerial.begin(9600);
  
  analogReference(EXTERNAL);
  
}

void loop(){
  if ((millis()-time)>5000 && CommBegin == 1){
            for (int thisNote = 0; thisNote < 8; thisNote++) {

                      // to calculate the note duration, take one second
                      // divided by the note type.
                      //e.g. quarter note = 1000 / 4, eighth note = 1000/8, etc.
                      int noteDuration = 1000 / noteDurations[thisNote];
                      tone(8, melody[thisNote], noteDuration);

                      // to distinguish the notes, set a minimum time between them.
                      // the note's duration + 30% seems to work well:
                      int pauseBetweenNotes = noteDuration * 1.30;
                      delay(pauseBetweenNotes);
                      // stop the tone playing:
                      noTone(8);
            }
  }

  if(Serial){ // if serial comm established
    
 // if (rcv == 1){
    dVoltageVal_lum = analogRead(lumpin);
    fVoltageVal_lum = (float)dVoltageVal_lum*5/1023;
    
    dtostrf(fVoltageVal_lum, 1, 3, sVoltageVal_lum); // convert float to its string representation
    
    dVoltageVal_frc = analogRead(frcepin);
    fVoltageVal_frc = (float)dVoltageVal_frc*5/1023*8;// '*8' convert volt into kilogram
    dtostrf(fVoltageVal_frc, 1, 2, sVoltageVal_frc); // convert float to its string representation
    
    tempReading = analogRead(tempPin);  
    float voltage = (float)tempReading * aref_voltage;
    voltage /= 1024.0; 
    // now print out the temperature
    float temperatureC = (voltage - 0.5) * 100 ;  //converting from 10 mv per degree wit 500 mV offset
    float temperatureF = (temperatureC * 9.0 / 5.0) + 32.0;
    dtostrf(temperatureC, 1, 1, stemperatureC); 
    dtostrf(temperatureF, 1, 1, stemperatureF); 
     
    Serial.print(sVoltageVal_lum);
    Serial.print(',');
    btSerial.print(sVoltageVal_lum);
    btSerial.print(',');


    Serial.print(sVoltageVal_frc);
    Serial.print("kg");
    Serial.print(',');
    btSerial.print(sVoltageVal_frc);
    btSerial.print("kg");
    btSerial.print(',');

    
   
    Serial.print(stemperatureC);
    Serial.print("C");
    Serial.print(',');
    btSerial.print(stemperatureC);
    btSerial.print("C");
    btSerial.print(',');

    Serial.print(stemperatureF);
    Serial.print("F");
    btSerial.print(stemperatureF);
    btSerial.print("F");
    
    Serial.print("\r\n");
    btSerial.print("#");

   // rcv == 0;
  //}

    if (btSerial.available()) {   // check if SoftSerial chars are in the buffer

        CommBegin = 1;
        time = millis();
        data = btSerial.read();     // read received byte     
        if (data == '0'){
          digitalWrite(ledPin,LOW);  // turn LED OFF
        }
        else if(data == '1') {          
          digitalWrite(ledPin,HIGH); // turn LED ON
        }
        //else if(data == '2'){
          //rcv = 1;
        //}

        // Send to terminal info on which LED ON/OFF button was pressed
        Serial.print("Received LED ON/OF command: ");
        Serial.write(data);
        Serial.write("\r\n");
    }
    
    delay(1000);
  } //if Serial
} // loop

