package com.husa.exostar.server.api;

import com.husa.exostar.server.process.FileProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin(origins = {"http://localhost:3000"})
public class ExostarController {

  @Autowired FileProcessor fileProcessor;

  // Simple health check endpoint on root
  @GetMapping("/")
  public String index() {
    return "Health Check YES!";
  }

  // POST a new file with information
  @PostMapping("/upload")
  public ExostarFileProcessResponse uploadFile(@RequestParam("file") MultipartFile file) {
    try {
      return fileProcessor.store(file.getInputStream());
    } catch (IOException ioException) {
      return new ExostarFileProcessResponse("Failed to parse file: " + ioException);
    }
  }
}
