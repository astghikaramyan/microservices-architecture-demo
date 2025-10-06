package com.example.resourceservice.constants;

public class Constants {
  public static final String BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE = "Invalid value \'%s\' for ID. Must be a positive integer";
  public static final String BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE = "Invalid ID format: \'%s\' for ID. Only positive integers are allowed";
  public static final String BAD_REQUEST_CSV_TOO_LONG_ERROR_MESSAGE = "CSV string is too long: received %s characters, maximum allowed is 200";
  public static final String BAD_REQUEST_RESPONSE_CODE = "400";
  public static final String NOT_FOUND_REQUEST_RESPONSE_CODE = "404";
  public static final String STORAGE_ERROR_MESSAGE = "Failed to upload file to S3";
  public static final String INTERNAL_SERVER_ERROR_RESPONSE_CODE = "500";
  public static final String SERVICE_UNAVAILABLE_RESPONSE_CODE = "503";
  public static final String DATABASE_ERROR_MESSAGE = "Resource operation could not be completed";
  public static final String NOT_FOUNT_RESOURCE_ERROR_MESSAGE = "Resource with ID=%s not found";
}
