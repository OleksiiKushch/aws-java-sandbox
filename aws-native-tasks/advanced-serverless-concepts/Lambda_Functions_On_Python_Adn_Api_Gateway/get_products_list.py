import json
import boto3
import logging
from boto3.dynamodb.conditions import Key

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
    response_product = product_table.query(
        KeyConditionExpression=Key('id').eq(uuid)
    )

    stock_table = dynamodb.Table(stock_table_name)
    response_stock = stock_table.query(
        KeyConditionExpression=Key('product_id').eq(uuid)
    )

    product_item = response_product['Items'][0]
    logger.info(product_item)
    stock_item = response_stock['Items'][0]
    logger.info(stock_item)

    product_with_stock_item = {**product_item, **stock_item}
    logger.info(product_with_stock_item)

    return {
        'body': json.dumps(product_with_stock_item)
    }