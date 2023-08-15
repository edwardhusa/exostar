package com.husa.exostar.server.process;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.husa.exostar.server.api.ExostarFileProcessResponse;
import com.husa.exostar.server.data.DataFileRecord;
import com.husa.exostar.server.data.DatabaseWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class FileProcessor {

    @Autowired
    EmailValidator emailValidator;

    @Autowired
    PhoneNumberUtil phoneNumberUtil;

    @Autowired
    DatabaseWriter databaseWriter;

    private final Path rootLocation;

    public FileProcessor(@Value("${file.location.root}") String rootProperty) {
        this.rootLocation = Paths.get(rootProperty);
    }


    /***
     * Store the values inside a file into the database
     * @param file
     * @return
     */
    public ExostarFileProcessResponse store(MultipartFile file) {

        int count = 0;
        int parsed = 0;
        List<String> errors = new ArrayList<>();


        try {
            validateFileProperties(file);

            ;
            try (CSVParser reader = new CSVParser(new InputStreamReader(file.getInputStream()), CSVFormat.DEFAULT)) {
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
        catch (Exception e) {
            errors.add("Exception parsing file: " + e);
        }

        return new ExostarFileProcessResponse(count, parsed, errors);
    }

    /***
     * Verify that the input file properties are valid
     * @param file
     * @throws Exception
     */
    private void validateFileProperties(MultipartFile file) throws Exception {
        //return error if file is empty
        if (file.isEmpty()) {
            throw new EOFException("Failed to store empty file.");
        }
        Path destinationFile = this.rootLocation.resolve(
                        Paths.get(file.getOriginalFilename()))
                .normalize().toAbsolutePath();

        //Make sure we only allow files stored in our root directory
        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            throw new AccessDeniedException(destinationFile.toAbsolutePath().toString(),
                    rootLocation.toAbsolutePath().toString(), "File outside allowed location");
        }
    }

    /***
     * Process a line of data coming from the file
     * @param in
     * @throws ParseException
     */
    private List<String> processCSVRecord(CSVRecord in) throws ParseException {
        DataFileRecord out = new DataFileRecord();

        List<String> errors = new ArrayList<>();

        /**
         * Format of file: name, phone number, email
         */

        String name = in.get(0);
        String phoneNumber = in.get(1);
        String email = in.get(2);

        if (validateName(name)) {
            out.setName(name);
        } else {
            errors.add("Invalid Name: " + name);
        }

        try {
            if (validatePhoneNumber(phoneNumber)) {
                out.setTelephone(phoneNumber);
            } else {
                errors.add("Invalid Phone Number: " + phoneNumber);
            }
        } catch (NumberParseException numberParseException) {
            errors.add("Invalid Phone Number: " + numberParseException);
        }

        if (validateEmailAddress(email)) {
            out.setEmail(email);
        } else {
            errors.add("Invalid Email: " + email);
        }

        if (errors.isEmpty()) {
            databaseWriter.write(out);
        }

        return errors;

    }

    /**
     * Verify that the Name input is valid
     * @param name Name to validate
     * @return is valid name
     */
    private boolean validateName(String name) {
        //This should use something smarter to check.  For now do basics
        return StringUtils.isAlphaSpace(name);

    }

    /**
     * Verify that the Phone Number is valid
     * @param phoneNumberString Incoming phone number
     * @return is number valid
     * @throws NumberParseException
     */
    private boolean validatePhoneNumber(String phoneNumberString) throws NumberParseException {
        Phonenumber.PhoneNumber phonenumber = phoneNumberUtil.parse(phoneNumberString, Phonenumber.PhoneNumber.CountryCodeSource.UNSPECIFIED.name());
        return phoneNumberUtil.isValidNumber(phonenumber);
    }

    /**
     * Verify that the email address is valid
     * @param emailAddress Incoming email address
     * @return is email valid
     */
    private boolean validateEmailAddress(String emailAddress) {
        return emailValidator.isValid(emailAddress);
    }


}
