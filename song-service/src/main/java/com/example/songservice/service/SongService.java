package com.example.songservice.service;

import com.example.songservice.dto.SongDTO;
import com.example.songservice.entity.SongEntity;
import com.example.songservice.exception.InvalidDataException;
import com.example.songservice.exception.NotFoundException;
import com.example.songservice.model.ErrorResponse;
import com.example.songservice.model.ValidationErrorResponse;
import com.example.songservice.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SongService {
    public static final String BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE = "Invalid value \'%s\' for ID. Must be a positive integer";
    public static final String BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE = "Invalid ID format: \'%s\' for ID. Only positive integers are allowed";
    public static final String BAD_REQUEST_CSV_TOO_LONG_ERROR_MESSAGE = "CSV string is too long: received %s characters, maximum allowed is 200";
    public static final String BAD_REQUEST_RESPONSE_CODE = "400";
    public static final String NOT_FOUND_REQUEST_RESPONSE_CODE = "404";
    @Autowired
    private SongRepository songRepository;

    public SongEntity addSong(SongEntity songEntity) {
        return this.songRepository.save(songEntity);
    }

    public Optional<SongEntity> getSong(final Integer id) {
        if (!isNumeric(String.valueOf(id))) {
            throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, id), BAD_REQUEST_RESPONSE_CODE));
        }
        if (!isValidNumeric(String.valueOf(id))) {
            throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE, id), BAD_REQUEST_RESPONSE_CODE));
        }
        if (!this.existById(Integer.valueOf(id))) {
            throw new NotFoundException(prepareErrorResponse(String.format("Song metadata for ID=%s not found", id), NOT_FOUND_REQUEST_RESPONSE_CODE));
        }
        return this.songRepository.findById(id);
    }

    public Optional<SongEntity> getSongByResourceId(final Integer resourceId) {
        return this.songRepository.findAll().stream().filter(songEntity -> songEntity.getResourceId().equals(resourceId)).findFirst();
    }


    public void deleteSong(final Integer id) {
        this.songRepository.deleteById(id);
    }

    public Map<String, List<Integer>> deleteSongByIds(final String id) {
        if (Objects.nonNull(id) && id.length() > 200) {
            throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_CSV_TOO_LONG_ERROR_MESSAGE, id.length()), BAD_REQUEST_RESPONSE_CODE));
        }
        String[] ids = Optional.ofNullable(id).map(param -> param.split(",")).orElse(new String[]{});
        final Map<String, List<Integer>> responseObject = new HashMap<>();
        List<Integer> removedIds = new LinkedList<>();
        Arrays.stream(ids).forEach(param -> {
            if (!isNumeric(param)) {
                throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, param), BAD_REQUEST_RESPONSE_CODE));
            }
            if (!isValidNumeric(param)) {
                throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE, param), BAD_REQUEST_RESPONSE_CODE));
            }
        });
        Arrays.stream(ids).forEach(param -> {
            Integer songIdentifier = Integer.valueOf(param);
            if (this.existById(songIdentifier)) {
                this.deleteSong(songIdentifier);
                removedIds.add(songIdentifier);
            }
        });
        responseObject.put("ids", removedIds);
        return responseObject;
    }

    public boolean existById(final Integer id) {
        return this.songRepository.existsById(id);
    }

    public ValidationErrorResponse checkValidity(SongDTO songDTO) {
        final ValidationErrorResponse validationErrorResponse = new ValidationErrorResponse();
        final Map<String, String> errorDetails = new HashMap<>();
        if (!isValidYear(songDTO.getYear())) {
            validationErrorResponse.setErrorCode("400");
            validationErrorResponse.setErrorMessage("Validation error");
        }
        if (!isValidDuration(songDTO.getDuration())) {
            validationErrorResponse.setErrorCode("400");
            validationErrorResponse.setErrorMessage("Validation error");
        }
        errorDetails.put("duration", "Duration must be in mm:ss format");
        errorDetails.put("year", "year must be between 1900 and 2099");
        validationErrorResponse.setErrorDetails(errorDetails);
        return validationErrorResponse;
    }

    public ErrorResponse checkMissingFields(SongDTO songDTO) {
        final ErrorResponse errorResponse = new ErrorResponse();
        if (Objects.isNull(songDTO.getName()) || songDTO.getName().isEmpty()) {
            errorResponse.setErrorCode("400");
            errorResponse.setErrorMessage("Song name is required");
        }
        return errorResponse;
    }

    public static boolean isValidDuration(String duration) {
        if (Objects.nonNull(duration) && duration.contains(":")) {
            String[] durationParts = duration.split(":");
            return durationParts.length == 2 && durationParts[0].length() == 2 && durationParts[1].length() == 2;
        }
        return false;
    }

    public static boolean isValidYear(String year) {
        final boolean isCorrectYear = Optional.ofNullable(year)
                                              .filter(v -> v.length() == 4)
                                              .map(s -> s.chars().allMatch(Character::isDigit))
                                              .orElse(false);
        return isCorrectYear && (Integer.parseInt(year) > 1900 && Integer.valueOf(year) < 2099);
    }

    private boolean isValidNumeric(String id) {
        final boolean isWholeNumber = Optional.ofNullable(id)
                                              .map(s -> s.chars().allMatch(Character::isDigit))
                                              .orElse(false);
        return isWholeNumber && Integer.parseInt(id) > 0;
    }

    private boolean isNumeric(final String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public ErrorResponse prepareErrorResponse(final String message, final String code) {
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage(message);
        errorResponse.setErrorCode(code);
        return errorResponse;
    }

}
