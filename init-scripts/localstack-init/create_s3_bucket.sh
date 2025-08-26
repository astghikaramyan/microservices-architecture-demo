#!/bin/bash
# This script runs automatically when LocalStack is ready

awslocal s3 mb s3://resource-files
echo "S3 bucket 'resource-files' created."