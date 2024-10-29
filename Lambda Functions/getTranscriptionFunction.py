import json
import boto3

# Initialize DynamoDB resource
dynamodb = boto3.resource('dynamodb')

def lambda_handler(event, context):
    # Retrieve transcription ID from event
    transcription_id = event.get('transcription_id')
    if not transcription_id:
        return {
            'statusCode': 400,
            'body': json.dumps('transcription_id is required')
        }

    # Connect to the 'audios' DynamoDB table
    audios = dynamodb.Table('audios')

    # Retrieve the single item based on the partition key
    response = audios.get_item(
        Key={'id': transcription_id}
    )

    # Check if the item exists and return it, or handle the case where it doesn’t exist
    item = response.get('Item')
    if item:
        return {
            'statusCode': 200,
            'body': json.dumps(item)
        }
    else:
        return {
            'statusCode': 404,
            'body': json.dumps('Item not found')
        }
