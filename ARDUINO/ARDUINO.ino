#include <WiFiGeneric.h>
#include <WiFiType.h>
#include <ETH.h>
#include <WiFiClient.h>
#include <WiFiServer.h>
#include <WiFiAP.h>
#include <WiFiUdp.h>
#include <WiFiSTA.h>
#include <WiFiScan.h>
#include <WiFiMulti.h>
#include <WiFi.h>
#include <HTTP_Method.h>
#include <WebServer.h>
#include <MQTT.h>

volatile int interruptCounter;
int totalInterruptCounter;
long jam=3600, hari=3600*24, minggu=3600*24*7;
const char* ssid     = "Setiawan Hu";
const char* password = "thepasswordisnotthepassword";
const int buttonPin = 4;
int buttonState = 0;
bool statusPenuh = false;
hw_timer_t * timer = NULL;
portMUX_TYPE timerMux = portMUX_INITIALIZER_UNLOCKED;
bool indeksKosong[10] = {true,true,true,true,true,true,true,true,true,true};

// Set web server port number to 80
WiFiServer server(80);
WiFiClient net;

// Variable to store the HTTP request
String header;

// Auxiliar variables to store the current output state
String output26State = "off";
String output27State = "off";
bool statusLogin = false;

// Assign output variables to GPIO pins
const int output26 = 26;
const int output27 = 27;
MQTTClient client;

//struct untuk pin dan durasi pin
struct Pin {
  String pin;
  unsigned long waktu;
};

//size / jumlah pin saat ini
int sizePin = 0;

//array untuk menyimpan pin (maksimal 10)
Pin pins[10];

void decTime() {
  for(int i=0; i<10; i++) {
    if(!indeksKosong[i]) {
      pins[i].waktu -= 1;
    }
  }
}

void IRAM_ATTR onTimer() {
  portENTER_CRITICAL_ISR(&timerMux);
  decTime();
  portEXIT_CRITICAL_ISR(&timerMux);
}

void connect() {
   while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  // Print local IP address and start web server
  Serial.println("");
  Serial.println("WiFi connected.");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
  
  while (!client.connect("setiawanhu/secure-door", "securedoor", "thisisthesecret")) { //client id, ,password
    Serial.print(".");
    delay(1000);
  }
  client.subscribe("/door");
  client.subscribe("/lock");//dilempar ke messageRecived
  client.subscribe("/pin");
}

void messageReceived(String &topic, String &payload) {
  Serial.println("hai");
  Serial.println("incoming: " + topic + " - " + payload);
  if(topic == "/pin"){
      long tempWaktu=1;
      String tempPin = "";

      //memasukkan 6 karakter pertama dari payload ke variabel tampungan sebagai pin
      for(int i=0; i<6; i++) {
        tempPin += payload[i];
      }

      //memasukkan 6 karakter pertama dari payload ke variabel tampungan sebagai waktu
      String kodeWaktu = "";
      kodeWaktu += payload[7];

      //set waktu berlakunya pin menurut kode yang diberikan
      if(kodeWaktu == "1") {
        //set untuk satu jam
        tempWaktu = tempWaktu * 3600;
      } else if (kodeWaktu == "2") {
        //set untuk satu hari
        tempWaktu = tempWaktu * 3600 * 24;
      } else {
        //set untuk satu minggu
        tempWaktu = tempWaktu * 3600 * 24 * 7;
      }

      if (sizePin > 9) {
        client.publish("/pin","failed");
        client.unsubscribe("/pin");
      } else {
        //memasukkan variabel tampungan ke dalam struct Pin
        int tmpIndeks;
        for(int i=0; i<10; i++) {
          if(indeksKosong[i]) {
            tmpIndeks = i;
            indeksKosong[i] = false;
            break;
          } else {
            tmpIndeks = 99;
          }
        }
        
        pins[tmpIndeks].pin = tempPin;
        pins[tmpIndeks].waktu = tempWaktu;
        Serial.println(pins[tmpIndeks].pin);
        Serial.println(pins[tmpIndeks].waktu);

        client.publish("/info", "ok");
      }
      
      //menaikkan ukuran array
      sizePin += 1;
  
  }
}

void setup() {
  Serial.begin(115200);
  // Initialize the output variables as outputs
  pinMode(output26, OUTPUT);
  pinMode(output27, OUTPUT);
  // Set outputs to LOW
  digitalWrite(output26, LOW);
  digitalWrite(output27, LOW);
  pinMode(buttonPin, INPUT);

  // Connect to Wi-Fi network with SSID and password
  Serial.print("Connecting to ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  
  client.begin("broker.shiftr.io", net);
  client.onMessage(messageReceived);

  timer = timerBegin(0, 80, true);
  timerAttachInterrupt(timer, &onTimer, true);
  timerAlarmWrite(timer, 1000000, true);
  timerAlarmEnable(timer);

  connect();
 
  server.begin();

}

void loop(){
  client.loop();
  delay(10);
  
  if (!client.connected()) {
    connect();
  }

  if(sizePin != 0) {
    for(int i=0; i<10; i++) {
      if(!indeksKosong[i]) {
        if(pins[i].waktu == 0) {
          pins[i].pin = "";
          pins[i].waktu = 0;
          indeksKosong[i] = true;
          sizePin -= 1;
          client.subscribe("/pin");
        }
      }
    }
  }
  
  WiFiClient clientWifi = server.available();   // Listen for incoming clientWifis
  buttonState = digitalRead(buttonPin);
  client.onMessage(messageReceived);
  if (buttonState == HIGH) {
    //client.publish("/pin","request");
  }
  if (clientWifi) {                             // If a new clientWifi connects,
    Serial.println("New clientWifi.");          // print a message out in the serial port
    String currentLine = "";                // make a String to hold incoming data from the clientWifi
    while (clientWifi.connected()) {            // loop while the clientWifi's connected
      if (clientWifi.available()) {             // if there's bytes to read from the clientWifi,
        char c = clientWifi.read();             // read a byte, then
        Serial.write(c);                    // print it out the serial monitor
        header += c;
        if (c == '\n') {                    // if the byte is a newline character
          // if the current line is blank, you got two newline characters in a row.
          // that's the end of the clientWifi HTTP request, so send a response:
          if (currentLine.length() == 0) {
            // HTTP headers always start with a response code (e.g. HTTP/1.1 200 OK)
            // and a content-type so the clientWifi knows what's coming, then a blank line:
            clientWifi.println("HTTP/1.1 200 OK");
            clientWifi.println("Content-type:text/html");
            clientWifi.println("Connection: close");
            clientWifi.println();
            
            for(int i=0; i<10; i++) {
              if(!indeksKosong[i]) {
                if (header.indexOf("GET /verifikasi?pass="+pins[i].pin) >= 0){
                  statusLogin = true;
                  break;
                }
              }
            }
            
            // Display the HTML web page
            clientWifi.println("<!DOCTYPE html><html>");
            clientWifi.println("<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
            clientWifi.println("<link rel=\"icon\" href=\"data:,\">");
            // CSS to style the on/off buttons 
            // Feel free to change the background-color and font-size attributes to fit your preferences
            clientWifi.println("<style>html { font-family: Helvetica; display: inline-block; margin: 0px auto; text-align: center;}");
            clientWifi.println(".button { background-color: #4CAF50; border: none; color: white; padding: 16px 40px;");
            clientWifi.println("text-decoration: none; font-size: 30px; margin: 2px; cursor: pointer;}");
            clientWifi.println(".button2 {background-color: #555555;}</style></head>");
            
            // Web Page Heading
            clientWifi.println("<body><h1>ESP32 Web Server</h1>");
            if(statusLogin) {
              clientWifi.println("<p>Berhasil login sayang :* muach love you</p>");
              clientWifi.println("</body></html>");
              // The HTTP response ends with another blank line
              clientWifi.println();
              // Break out of the while loop
              break;
            } else {
              clientWifi.println("<form  method='get' action=\"/verifikasi\"><input type='text' name='pass'><input type='submit'></form>");
              
              clientWifi.println("</body></html>");
              
              // The HTTP response ends with another blank line
              clientWifi.println();
              // Break out of the while loop
              break;
            }
          } else { // if you got a newline, then clear currentLine
            currentLine = "";
          }
        } else if (c != '\r') {  // if you got anything else but a carriage return character,
          currentLine += c;      // add it to the end of the currentLine
        }
      }
    }
    // Clear the header variable
    header = "";
    // Close the connection
    clientWifi.stop();
    Serial.println("clientWifi disconnected.");
    Serial.println("");
  }
}
