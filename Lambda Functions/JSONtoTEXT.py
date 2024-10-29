import boto3
import json
import uuid


def lambda_handler(event, context):
    # Initialize S3 client
    s3_client = boto3.client('s3')
    dynamodb = boto3.resource('dynamodb')

    table = dynamodb.Table('audios')

    id = uuid.uuid4()
    user = 1


    # Get bucket name and object key from the event (if Lambda is triggered by S3 event)
    bucket_name = event['Records'][0]['s3']['bucket']['name']
    object_key = event['Records'][0]['s3']['object']['key']

    # Retrieve the object
    response = s3_client.get_object(Bucket=bucket_name, Key=object_key)

    # Read and parse the JSON content
    file_content = response['Body'].read().decode('utf-8')
    json_content = json.loads(file_content)  # Parse JSON content
    transcript = json_content["results"]["transcripts"][0]["transcript"]


    item = {
            'id':f'{id}',
            'user': f'{user}',
            'transcript': f'{transcript}',
        }

    try:
        # Insert the item into the DynamoDB table
        table.put_item(Item=item)
        return {
            'statusCode': 200,
            'body': json.dumps('audio created successfully')
        }

    except Exception as e:
        return {
            'statusCode': 500,
            'body': json.dumps('Error creating audio')
        }


    return {
        'statusCode': 200,
        'body': transcript
    }
