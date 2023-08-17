package com.husa.exostar.server.api;

import com.husa.exostar.server.process.FileProcessor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@CrossOrigin(origins = {"http://[::1]:4200"})
public class ExostarController {

  @Autowired FileProcessor fileProcessor;

  // Simple health check endpoint on root
  @GetMapping("/")
  public String index() {
    return "Health Check YES!";
  }

  // POST a new file with information
  @PostMapping(
      path = "/upload/raw",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  public ExostarFileProcessResponse uploadFile(@RequestPart MultipartFile file) {
    try {
      return fileProcessor.store(file.getInputStream());
    } catch (IOException ioException) {
      log.warn("Parse Error on " + file.getName(), ioException);
      return new ExostarFileProcessResponse("Failed to parse file: " + ioException);
    }
  }

  // POST a file name to upload
  @PostMapping(path = "/upload/name")
  public ExostarFileProcessResponse presentFile(@RequestBody String fileName) {
    // Assume coming from local file system
    // TODO: Support files on external servers

    String decoded = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

    // clean up any trailing bad characters
    if (!CharUtils.isAsciiAlphanumeric(decoded.charAt(decoded.length() - 1))) {
      decoded = StringUtils.chop(decoded);
    }

    try {
      try (FileInputStream fis = new FileInputStream(decoded)) {
        return fileProcessor.store(fis);
      }
    } catch (FileNotFoundException fnf) {
      log.warn("File Not Found " + decoded, fnf);
      return new ExostarFileProcessResponse("File Not Found: " + fnf);
    } catch (IOException ioException) {
      log.warn("Parse Error on " + decoded, ioException);
      return new ExostarFileProcessResponse("Failed to parse file: " + ioException);
    }
  }
}
