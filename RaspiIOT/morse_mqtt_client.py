
# [START iot_mqtt_includes]
import argparse
import datetime
import logging
import os
import random
import ssl
import time
import json

import jwt
import paho.mqtt.client as mqtt

# import RPi.GPIO as GPIO
import time
import RPi.GPIO as GPIO
import Adafruit_DHT
import firebase_admin 
from firebase_admin import credentials
from firebase_admin import firestore

from threading import Thread
import multiprocessing
# [END iot_mqtt_includes]

logging.getLogger('googleapiclient.discovery_cache').setLevel(logging.CRITICAL)

# The initial backoff time after a disconnection occurs, in seconds.
minimum_backoff_time = 1

led = 21

OFF=True
ON=False

GPIO.setmode(GPIO.BCM)
GPIO.setup(led,GPIO.OUT)


# The maximum backoff time before giving up, in seconds.
MAXIMUM_BACKOFF_TIME = 32

# Whether to wait with exponential backoff before publishing.
should_backoff = False

thread = None
cred = credentials.Certificate("/home/pi/Desktop/morseKeys/morsecode-317712-firebase-adminsdk-z5huq-44a6d3e7ae.json")
firebase_admin.initialize_app(cred)
deviceId = ""

RESET=-1
SENT=0
RECEIVED=1
DONE=2

db = firestore.client()


MORSE_CODE_DICT = { 'A':'.-', 'B':'-...',
                    'C':'-.-.', 'D':'-..', 'E':'.',
                    'F':'..-.', 'G':'--.', 'H':'....',
                    'I':'..', 'J':'.---', 'K':'-.-',
                    'L':'.-..', 'M':'--', 'N':'-.',
                    'O':'---', 'P':'.--.', 'Q':'--.-',
                    'R':'.-.', 'S':'...', 'T':'-',
                    'U':'..-', 'V':'...-', 'W':'.--',
                    'X':'-..-', 'Y':'-.--', 'Z':'--..',
                    '1':'.----', '2':'..---', '3':'...--',
                    '4':'....-', '5':'.....', '6':'-....',
                    '7':'--...', '8':'---..', '9':'----.',
                    '0':'-----', ', ':'--..--', '.':'.-.-.-',
                    '?':'..--..', '/':'-..-.', '-':'-....-',
                    '(':'-.--.', ')':'-.--.-',' ':'|'}

MORSE_INTERVALS_DICT = { '.' : 1,
                         '-' : 3,
                         ' ' : 3,
                         '|':6,
                         'code_space' : 1
                          }


def to_morse(text):
        """
        Translates text to morse code.
        Accepts:
            text (str): A string of text to translate
        Returns:
            str: A translated string of morse code
        """

        if text == "":
            return "You must provide a morse code string to translate"

        translation = ""

        words = text.split(" ")

        for word in words:
            w = list()
            for char in word:
                if char.upper() in MORSE_CODE_DICT:
                    w.append(MORSE_CODE_DICT[char.upper()])

            translation += " ".join(w)
            translation += "|"

        return translation.rstrip()

def from_morse(s):
    return ''.join(MORSE_CODE_DICT.get(i) for i in s.split())

def blink_message(morse):
    GPIO.output(led,OFF)
    ##get ready delay
    time.sleep(3)
    for index,code in enumerate(morse):
        if code == '.':
            GPIO.output(led,ON)
            time.sleep(MORSE_INTERVALS_DICT[code])
        elif code == '-':
            GPIO.output(led,ON)
            time.sleep(MORSE_INTERVALS_DICT[code])
        elif code == ' ':
            GPIO.output(led,OFF)
            time.sleep(MORSE_INTERVALS_DICT[code])
        elif code == '|':
            GPIO.output(led,OFF)
            time.sleep(MORSE_INTERVALS_DICT[code])
            if index == len(morse)-1:
                GPIO.output(led,ON)
                db.collection('users').document(deviceId).update({"messageState":2})
                print(morse)

    
        GPIO.output(led,OFF)
        if code != ' ':
            time.sleep(MORSE_INTERVALS_DICT['code_space'])

# [START iot_mqtt_jwt]
def create_jwt(project_id, private_key_file, algorithm):
    token = {
            # The time that the token was issued at
            'iat': datetime.datetime.utcnow(),
            # The time the token expires.
            'exp': datetime.datetime.utcnow() + datetime.timedelta(minutes=20),
            # The audience field should always be set to the GCP project id.
            'aud': project_id
    }
    # Read the private key file.
    with open(private_key_file, 'r') as f:
        private_key = f.read()

    print('Creating JWT using {} from private key file {}'.format(
            algorithm, private_key_file))

    return jwt.encode(token, private_key, algorithm=algorithm)
# [END iot_mqtt_jwt]


# [START iot_mqtt_config]
def error_str(rc):
    """Convert a Paho error to a human readable string."""
    return '{}: {}'.format(rc, mqtt.error_string(rc))


def on_connect(unused_client, unused_userdata, unused_flags, rc):
    """Callback for when a device connects."""
    print('on_connect', mqtt.connack_string(rc))

    # After a successful connect, reset backoff time and stop backing off.
    global should_backoff
    global minimum_backoff_time
    should_backoff = False
    minimum_backoff_time = 1


def on_disconnect(unused_client, unused_userdata, rc):
    """Paho callback for when a device disconnects."""
    print('on_disconnect', error_str(rc))

    # Since a disconnect occurred, the next loop iteration will wait with
    # exponential backoff.
    global should_backoff
    should_backoff = True


def on_publish(unused_client, unused_userdata, unused_mid):
    """Paho callback when a message is sent to the broker."""
    print('on_publish')

#modify
def on_message(unused_client, unused_userdata, message):
    """Callback when the device receives a message on a subscription."""
    #to remove double quotes for json formatting error purpose

    GPIO.output(led,OFF)
    
    payload = '{}'.format(str(message.payload.decode('utf-8')))
    print('Received message \'{}\' on topic \'{}\' with Qos {}'.format(payload, message.topic, str(message.qos)))

    try:
        #can cause exception
        jsondata = json.loads(payload)

        if int(jsondata["messageState"]) in [RESET,SENT]:
            globals()["deviceId"] =  jsondata["deviceId"]
        else:
            return

        if (str(jsondata["currentMessage"]) is not "reset") and (int(jsondata["messageState"]) not in [RESET])  :
            db.collection('users').document(deviceId).update({"messageState":RECEIVED})
            m  = to_morse(jsondata["currentMessage"])
        
        else:
            ##means it is reset so dont do anything
            print("return")
            globals()["thread"].kill()
            return
            
        print(m)

        if globals()["thread"] is not  None:
                globals()["thread"].kill()
                globals()["thread"]  = multiprocessing.Process(target=blink_message,args = (m,),name="morse")
                globals()["thread"].start()
        else:
            globals()["thread"]  = multiprocessing.Process(target=blink_message,args = (m,),name="morse")
            globals()["thread"].start()
        
                
    except:
             print('Message: {}  is not in json format'.format( str(payload)))
             return        
            
            
# GPIO.cleanup()   


def get_client(
        project_id, cloud_region, registry_id, device_id, private_key_file,
        algorithm, ca_certs, mqtt_bridge_hostname, mqtt_bridge_port):
    """Create our MQTT client. The client_id is a unique string that identifies
    this device. For Google Cloud IoT Core, it must be in the format below."""
    client_id = 'projects/{}/locations/{}/registries/{}/devices/{}'.format(
            project_id, cloud_region, registry_id, device_id)
    print('Device client_id is \'{}\''.format(client_id))

    client = mqtt.Client(client_id=client_id)

    # With Google Cloud IoT Core, the username field is ignored, and the
    # password field is used to transmit a JWT to authorize the device.
    client.username_pw_set(
            username='unused',
            password=create_jwt(
                    project_id, private_key_file, algorithm))

    # Enable SSL/TLS support.
    client.tls_set(ca_certs=ca_certs, tls_version=ssl.PROTOCOL_TLSv1_2)

    client.on_connect = on_connect
    client.on_publish = on_publish
    client.on_disconnect = on_disconnect
    client.on_message = on_message

    # Connect to the Google MQTT bridge.
    client.connect(mqtt_bridge_hostname, mqtt_bridge_port)

    # This is the topic that the device will receive configuration updates on.
    mqtt_config_topic = '/devices/{}/config'.format(device_id)

    # Subscribe to the config topic.
    client.subscribe(mqtt_config_topic, qos=1)

    # The topic that the device will receive commands on.
    mqtt_command_topic = '/devices/{}/commands/#'.format(device_id)

    # Subscribe to the commands topic, QoS 1 enables message acknowledgement.
    print('Subscribing to {}'.format(mqtt_command_topic))
    client.subscribe(mqtt_command_topic, qos=0)

    return client
# [END iot_mqtt_config]


def detach_device(client, device_id):
    """Detach the device from the gateway."""
    # [START iot_detach_device]
    detach_topic = '/devices/{}/detach'.format(device_id)
    print('Detaching: {}'.format(detach_topic))
    client.publish(detach_topic, '{}', qos=1)
    # [END iot_detach_device]


def attach_device(client, device_id, auth):
    """Attach the device to the gateway."""
    # [START iot_attach_device]
    attach_topic = '/devices/{}/attach'.format(device_id)
    attach_payload = '{{"authorization" : "{}"}}'.format(auth)
    client.publish(attach_topic, attach_payload, qos=1)
    # [END iot_attach_device]


def parse_command_line_args():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description=(
            'Example Google Cloud IoT Core MQTT device connection code.'))
    parser.add_argument(
            '--algorithm',
            choices=('RS256', 'ES256'),
            required=True,
            help='Which encryption algorithm to use to generate the JWT.')
    parser.add_argument(
            '--ca_certs',
            default='roots.pem',
            help='CA root from https://pki.google.com/roots.pem')
    parser.add_argument(
            '--cloud_region', default='us-central1', help='GCP cloud region')
    parser.add_argument(
            '--data',
            default='Hello there',
            help='The telemetry data sent on behalf of a device')
    parser.add_argument(
            '--device_id', required=True, help='Cloud IoT Core device id')
    parser.add_argument(
            '--gateway_id', required=False, help='Gateway identifier.')
    parser.add_argument(
            '--jwt_expires_minutes',
            default=20,
            type=int,
            help='Expiration time, in minutes, for JWT tokens.')
    parser.add_argument(
            '--listen_dur',
            default=60,
            type=int,
            help='Duration (seconds) to listen for configuration messages')
    parser.add_argument(
            '--message_type',
            choices=('event', 'state'),
            default='event',
            help=('Indicates whether the message to be published is a '
                  'telemetry event or a device state message.'))
    parser.add_argument(
            '--mqtt_bridge_hostname',
            default='mqtt.googleapis.com',
            help='MQTT bridge hostname.')
    parser.add_argument(
            '--mqtt_bridge_port',
            choices=(8883, 443),
            default=8883,
            type=int,
            help='MQTT bridge port.')
    parser.add_argument(
            '--num_messages',
            type=int,
            default=100,
            help='Number of messages to publish.')
    parser.add_argument(
            '--private_key_file',
            required=True,
            help='Path to private key file.')
    parser.add_argument(
            '--project_id',
            default="",
            help='GCP cloud project name')
    parser.add_argument(
            '--registry_id', required=True, help='Cloud IoT Core registry id')
    parser.add_argument(
            '--service_account_json',
            default=os.environ.get("GOOGLE_APPLICATION_CREDENTIALS"),
            help='Path to service account json file.')

    # Command subparser
    command = parser.add_subparsers(dest='command')

    command.add_parser(
        'device_demo',
        help=mqtt_device_demo.__doc__)


    return parser.parse_args()


    
def mqtt_device_demo(args):
    """Connects a device, sends data, and receives data."""
    # [START iot_mqtt_run]
    global minimum_backoff_time
    global MAXIMUM_BACKOFF_TIME
    ranOnce = True
    
    sub_topic = "events" if args.message_type == "event" else "state"

#     mqtt_topic = "/devices/{}/{}/subf2".format(args.device_id, sub_topic)
    mqtt_topic = "/devices/{}/{}".format(args.device_id, sub_topic)

    jwt_iat = datetime.datetime.utcnow()
    jwt_exp_mins = args.jwt_expires_minutes
    client = get_client(
        args.project_id,
        args.cloud_region,
        args.registry_id,
        args.device_id,
        args.private_key_file,
        args.algorithm,
        args.ca_certs,
        args.mqtt_bridge_hostname,
        args.mqtt_bridge_port,
    )

   
    for i in range(1, args.num_messages + 1):
        client.loop()

        if should_backoff:
            if minimum_backoff_time > MAXIMUM_BACKOFF_TIME:
                print("Exceeded maximum backoff time. Giving up.")
                break

         
            delay = minimum_backoff_time + random.randint(0, 1000) / 1000.0
            print("Waiting for {} before reconnecting.".format(delay))
            time.sleep(delay)
            minimum_backoff_time *= 2
            client.connect(args.mqtt_bridge_hostname, args.mqtt_bridge_port)


        
        seconds_since_issue = (datetime.datetime.utcnow() - jwt_iat).seconds
        if seconds_since_issue > 60 * jwt_exp_mins:
            print("Refreshing token after {}s".format(seconds_since_issue))
            jwt_iat = datetime.datetime.utcnow()
            client.loop()
            client.disconnect()
            client = get_client(
                args.project_id,
                args.cloud_region,
                args.registry_id,
                args.device_id,
                args.private_key_file,
                args.algorithm,
                args.ca_certs,
                args.mqtt_bridge_hostname,
                args.mqtt_bridge_port,
            )
     
               
        for i in range(0, 10):
            #delay = number to loop iterations * sleep time
            #delay = 10*1 = 10 secs
            time.sleep(1)
            
            print(i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i)
            client.loop()
    # [END iot_mqtt_run]


def main():
    args = parse_command_line_args()


    mqtt_device_demo(args)
    print("Finished.")


if __name__ == "__main__":
    main()