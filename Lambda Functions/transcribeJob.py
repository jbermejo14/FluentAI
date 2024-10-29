import json
import boto3
import uuid

id = uuid.uuid1().int

def lambda_handler(event, context):
    transcribe_client = boto3.client('transcribe')

    bucket_name = event['Records'][0]['s3']['bucket']['name']
    file_name = event['Records'][0]['s3']['object']['key']
    object_url = "s3://" + bucket_name + "/" + file_name

    response = transcribe_client.start_transcription_job(
        TranscriptionJobName= "job" + str(id),
        Media={'MediaFileUri': object_url},
        MediaFormat='mp3',
        LanguageCode='es-ES',
        OutputBucketName='fluentai-outputs',
        OutputKey='job-output' + str(id) + ".json"
    )

    return {
        'statusCode': 200,
        'body': json.dumps({'message': 'Transcription started'})
    }
