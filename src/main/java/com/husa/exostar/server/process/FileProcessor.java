package com.husa.exostar.server.process;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.husa.exostar.server.api.ExostarFileProcessResponse;
import com.husa.exostar.server.data.DatabaseDAO;
import com.husa.exostar.server.data.record.Users;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileProcessor {

  private final EmailValidator emailValidator = EmailValidator.getInstance();

  private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

  @Autowired DatabaseDAO databaseDAO;

  private final CSVFormat csvFormat;

  public FileProcessor(@Value("${file.location.root}") String rootProperty) {
    // Set CSV formatter to clear up surrounding whitespace
    csvFormat =
        CSVFormat.Builder.create(CSVFormat.DEFAULT).setIgnoreSurroundingSpaces(true).build();
  }

  /***
   * Store the values inside a file into the database
   * @param file File to parse
   * @return results of store operation
   */
  public ExostarFileProcessResponse store(MultipartFile file) {

    int count = 0;
    int parsed = 0;
    List<String> errors = new ArrayList<>();

    try {
      // fail out if file empty
      if (file.isEmpty()) {
        errors.add("Failed to store empty file.");
      } else {
        try (CSVParser reader =
            new CSVParser(new InputStreamReader(file.getInputStream()), csvFormat)) {
          for (CSVRecord record : reader) {
            List<String> recordErrors = processCSVRecord(record);

            count++;

            if (!recordErrors.isEmpty()) {
              errors.addAll(recordErrors);
            } else {
              parsed++;
            }
          }
        }
      }
    } catch (Exception e) {
      errors.add("Exception parsing file: " + e);
    }

    return new ExostarFileProcessResponse(count, parsed, errors);
  }

  /***
   * Process a line of data coming from the file
   * Format of file: name, phone number, email
   * This assumes that the data coming in has already been whitespace cleared
   * @param in
   */
  private List<String> processCSVRecord(CSVRecord in) {
    Users out = new Users();

    List<String> errors = new ArrayList<>();

    String name = in.get(0);
    String phoneNumber = in.get(1);
    String email = in.get(2);

    // Parse Name
    if (validateName(name)) {
      out.setName(name);
    } else {
      errors.add("Invalid Name: " + name);
    }

    String standardNumber = standardizePhoneNumber(phoneNumber);

    try {
      if (validatePhoneNumber(standardNumber)) {
        out.setTelephone(standardNumber);
      } else {
        errors.add("Invalid Phone Number: " + standardNumber);
      }
    } catch (NumberParseException numberParseException) {
      errors.add("Invalid Phone Number: " + standardNumber + " " + numberParseException);
    }

    if (validateEmailAddress(email)) {
      out.setEmail(email);
    } else {
      errors.add("Invalid Email: " + email);
    }

    if (errors.isEmpty()) {
      databaseDAO.createOrUpdateUsers(out);
    }

    return errors;
  }

  /**
   * Standardize the input phone number
   *
   * @param inNumber
   * @return standardized phone number
   */
  String standardizePhoneNumber(String inNumber) {
    String outNumber;

    // assume that any 10-digit phone number without country code otherwise follows NANP
    if (inNumber.length() == 10 && !inNumber.startsWith("1")) {
      outNumber = "+1" + inNumber;
    } else if (!StringUtils.startsWith(inNumber, "+")) {
      outNumber = "+" + inNumber;
    } else {
      outNumber = inNumber;
    }

    return outNumber;
  }

  /**
   * Verify that the Name input is valid
   *
   * @param name Name to validate
   * @return is valid name
   */
  boolean validateName(String name) {
    // This should use something smarter to check.  For now do basics
    return StringUtils.isAlphaSpace(name);
  }

  /**
   * Verify Phone Number
   *
   * @param phoneNumberString number to validate
   * @return is valid number
   * @throws NumberParseException
   */
  boolean validatePhoneNumber(String phoneNumberString) throws NumberParseException {
    Phonenumber.PhoneNumber phoneNumber =
        phoneNumberUtil.parse(
            phoneNumberString, Phonenumber.PhoneNumber.CountryCodeSource.UNSPECIFIED.name());
    return phoneNumberUtil.isValidNumber(phoneNumber);
  }

  /**
   * Verify that the email address is valid
   *
   * @param emailAddress Incoming email address
   * @return is email valid
   */
  boolean validateEmailAddress(String emailAddress) {
    return emailValidator.isValid(emailAddress);
  }
}
