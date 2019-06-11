#include <SoftwareSerial.h>   
SoftwareSerial Genotronex(0, 1); // TX, RX 
 
#define MAX_BUFFER 4 
const int stepPin = 3;  
const int dirPin = 4;    
int speed = 0; 
char data; 
char* buffer; 
boolean receiving = false; 
int pos; 
 
void setup()  { 
   Genotronex.begin(9600);   
   Genotronex.println("Bluetooth On");     
   pinMode(stepPin,OUTPUT);      
   pinMode(dirPin,OUTPUT);       
   buffer = new char[MAX_BUFFER];
   }  
 
void loop()  {  
      if (Genotronex.available()){ 
          data=Genotronex.read();                   
		  switch(data) {             
		  //3: End of transmission             
		  case 3:  receiving = false;                       
		  speed = buffer2int(buffer);                                                     
		  Genotronex.print("Received: ");                     
		  Genotronex.print(buffer);                     
		  Genotronex.print(", Speed: ");                     
		  Genotronex.println(speed);                                            
		  digitalWrite(dirPin,HIGH); // Enables the motor to move in a particular direction                   
		  // Makes 200 pulses for making one full cycle rotation                   
		  for(int x = 0; x <speed; x++) {
		  digitalWrite(stepPin,HIGH);                      
		  delayMicroseconds(500);                      
		  digitalWrite(stepPin,LOW);                      
		  delayMicroseconds(500);                    
		  }                   
		  delay(1000); // One second delay                           
		  break; //end message             
		  default: if (receiving == false) resetData();                    
		  buffer[pos] = data;                     
		  pos++;                      
		  receiving = true;                     
		  }            
		  }                   

 
} 
 
 void resetData(){
 for (int i=0; i<=pos; i++){
 buffer[i] = 0;     
 pos = 0; 
 }      
 int buffer2int(char* buffer){   
 int i;   
 sscanf(buffer, "%d", &i);   
 return i;   
 } 