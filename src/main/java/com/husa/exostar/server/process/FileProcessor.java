package com.husa.exostar.server.process;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.husa.exostar.server.api.ExostarFileProcessResponse;
import com.husa.exostar.server.data.DatabaseDAO;
import com.husa.exostar.server.data.record.Users;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileProcessor {

  private final EmailValidator emailValidator = EmailValidator.getInstance();

  private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

  @Autowired DatabaseDAO databaseDAO;

  private final CSVFormat csvFormat;

  public FileProcessor() {
    // Set CSV formatter to clear up surrounding whitespace
    csvFormat =
        CSVFormat.Builder.create(CSVFormat.DEFAULT).setIgnoreSurroundingSpaces(true).build();
  }

  /***
   * Store the values from the stream into the database
   * @param inputStream Data stream
   * @return results of store operation
   */
  public ExostarFileProcessResponse store(InputStream inputStream) {

    int count = 0;
    int parsed = 0;
    List<String> errors = new ArrayList<>();

    try {
      try (CSVParser reader = new CSVParser(new InputStreamReader(inputStream), csvFormat)) {
        for (CSVRecord record : reader) {
          count++;

          List<String> recordErrors = processCSVRecord(record);

          if (!recordErrors.isEmpty()) {
            errors.addAll(recordErrors);
          } else {
            parsed++;
          }
        }
      }
    } catch (Exception e) {
      errors.add("Exception parsing file: " + e);
    }

    // if no records parsed, throw error
    if (count == 0) {
      errors.add("No Records Parsed");
    }

    return new ExostarFileProcessResponse(count, parsed, errors);
  }

  /***
   * Process a line of data coming from the file
   * Format of file: name, phone number, email
   * This assumes that the data coming in has already been whitespace cleared
   * @param in  Parsed Data Record
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
   * @param inNumber Raw Parsed Number
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
   * @throws NumberParseException Thrown if there is a critical validation error in the library
   */
  boolean validatePhoneNumber(String phoneNumberString) throws NumberParseException {
    Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(phoneNumberString, null);
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
