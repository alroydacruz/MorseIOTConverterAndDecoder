The Project allows you to trasmit a message via and android App to the raspberry pi which will blink that message in morse code. The app also can decode any blinking morse code message into its corresponding message using the light sensors if your phone has one.

The app using kotlin, mvvm , dagger-hilt, firestore-sdk, etc.

The db of choice was Firestore because it integrates well with the entire google architecture
For the cloud service google iot-core was used (GCP)

and the iot device of choice was Raspberry Pi using python via mqtt bridge.
