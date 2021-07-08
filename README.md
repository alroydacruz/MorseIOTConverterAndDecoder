This Project allows you to trasmit a message via an android App to a raspberry pi which will blink the message in morse code using a bulb with a relay. The app also can decode any blinking morse code into its corresponding message using the light sensor, if your phone has one.

The app using kotlin, mvvm , dagger-hilt, firestore-sdk, etc.

The db of choice was Firestore because it integrates well with the entire google architecture

For the cloud service google iot-core was used (GCP)

and the iot device of choice was Raspberry Pi using python via mqtt bridge.
