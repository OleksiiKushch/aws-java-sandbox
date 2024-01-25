import boto3
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    start_time = event['start_time']
    end_time = event['end_time']
    client = boto3.client('cloudtrail')

    usernames_set = set()

    data = client.lookup_events(StartTime=start_time, EndTime=end_time)
    process_events(data['Events'], usernames_set)

    while 'NextToken' in data:
        data = client.lookup_events(StartTime=start_time, EndTime=end_time, NextToken=data['NextToken'])
        process_events(data['Events'], usernames_set)

    usernames_list = sorted(list(usernames_set))

    logger.info(usernames_list)

    return usernames_list

def process_events(events, usernames_set):
    logger.info(events)
    logger.info(len(events))
    for event in events:
        username = event.get('Username')
        if username is not None:
            usernames_set.add(username)
