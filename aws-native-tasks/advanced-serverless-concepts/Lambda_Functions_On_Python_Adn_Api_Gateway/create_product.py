import json
import boto3
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    uuid = '${uuid}'
    product_table_name = '${products-table-name}'
    stock_table_name = '${stocks-table-name}'

    if 'headers' in event and 'random-uuid' in event["headers"]:
        uuid += f'-{event["headers"]["random-uuid"]}'

    dynamodb = boto3.resource('dynamodb')
    product_table = dynamodb.Table(product_table_name)
    stock_table = dynamodb.Table(stock_table_name)

    product_table.put_item(
        Item={
            'id': uuid,
            'title': event.get('title'),
            'description': event.get('description'),
            'price': event.get('price')
        }
    )

    stock_table.put_item(
        Item={
            'product_id': uuid,
            'count': event.get('count')
        }
    )

    return {
        'statusCode': 200,
        'body': json.dumps(f'Product with ID {uuid} added.')
    }
