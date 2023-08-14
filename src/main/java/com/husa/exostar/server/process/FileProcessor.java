package com.husa.exostar.server.process;

import com.husa.exostar.server.api.ExostarFileProcessResponse;
import com.husa.exostar.server.data.DataFileRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;

public class FileProcessor {




    private final Path rootLocation;

    public FileProcessor(@Value("${file.location.root}") String rootProperty) {
        this.rootLocation = Paths.get(rootProperty);
    }




    public ExostarFileProcessResponse store(MultipartFile file) throws Exception {

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

        ExostarFileProcessResponse response = new ExostarFileProcessResponse();

        try {
            ;
            try (CSVParser reader = new CSVParser(new InputStreamReader(file.getInputStream()), CSVFormat.DEFAULT)) {
                reader.stream().forEach(this::processCSVRecord);
            }
        }
        catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    private void processCSVRecord(CSVRecord in) throws ParseException {
        DataFileRecord out = new DataFileRecord();

        /**
         * Format of file: name, phone number, email
         */

        out.setName(in.get(0));

        EmailValidator emailValidator = new EmailValidator();
    }



}
