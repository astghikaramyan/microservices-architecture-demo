#!/bin/bash
# This script runs automatically when LocalStack is ready

awslocal s3 mb s3://permanent-resource-files
awslocal s3 mb s3://staging-resource-files
echo "S3 bucket 'permanent-resource-files' and 'staging-resource-files' created."