package com.husa.exostar.server.process;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileProcessorTest {

  private static MultipartFile testFile;

  @Autowired FileProcessor fileProcessor;

  @BeforeAll
  static void setup() throws Exception {
    testFile = new MockMultipartFile("file", new FileInputStream("testdata.csv"));
  }

  @Test
  void store() {}

  @Test
  void validateFileProperties() {}

  @Test
  void validateName() {}

  @Test
  void testValidatePhoneNumber() throws Exception {
    assertTrue(fileProcessor.validatePhoneNumber("+12125551212"), "Failed parsing NANP number");
    assertFalse(fileProcessor.validatePhoneNumber("2125551212"), "Failed parsing NANP number");
  }

  @Test
  void testValidateEmailAddress() {
    assertTrue(fileProcessor.validateEmailAddress("hello@java.com"), "Email Does not validate");
    assertFalse(fileProcessor.validateEmailAddress("this.is.not.an.email.com"), "Email validates");
  }
}
