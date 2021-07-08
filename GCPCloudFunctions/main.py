def firestoreToRaspi(event, context):

    import json
    from google.cloud import iot_v1

    resource_string = context.resource
    # print out the resource string that triggered the function
    print(f"Function triggered by change to: {resource_string}.")
    # now print out the entire event object
    print(str(event))

    """send command to device"""
    print("Sending command to device")
    client = iot_v1.DeviceManagerClient()
    device_path = client.device_path('morsecode-317712','asia-east1','morse_code_registry','flashlight')

    currentMessage= event['value']['fields']['message']['stringValue']
    messageState = event['value']['fields']['messageState']['integerValue']
    deviceId= event['value']['fields']['deviceId']['stringValue']
    
    print("hi0")

    formattedValues = '{"currentMessage":"%s","messageState":"%s","deviceId":"%s"}' % (str(currentMessage), str(messageState), str(deviceId))
    data = formattedValues.encode("utf-8")
    
    print(formattedValues)
    client.send_command_to_device(
        request={"name": device_path, "binary_data":data}
    )

    print("hi2")
    """send command to device"""
    
