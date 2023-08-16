package com.husa.exostar.server.process;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.i18n.phonenumbers.NumberParseException;
import com.husa.exostar.server.api.ExostarFileProcessResponse;
import com.husa.exostar.server.data.DatabaseDAO;
import com.husa.exostar.server.data.record.Users;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileProcessorTest {

  private static MultipartFile testFile;

  @Mock DatabaseDAO mockDatabaseDAO;

  @InjectMocks FileProcessor fileProcessor;

  @BeforeAll
  static void setupAll() throws Exception {
    testFile =
        new MockMultipartFile("file", new ClassPathResource("testdata.csv").getInputStream());
  }

  /**
   * Standard text to data source using Guava
   * @param inputData test data
   * @return test source
   */
  ByteSource setupStoreTest(CharSequence inputData) {
    return CharSource.wrap(inputData).asByteSource(StandardCharsets.UTF_8);
  }

  @Test
  void testStore_happy() throws Exception {
    setupStoreTest("null");

    ExostarFileProcessResponse response = fileProcessor.store(testFile.getInputStream());

    assertEquals(3, response.getLinesParsed(), "Lines Parsed");
    assertEquals(3, response.getLinesInFile(), "Lines Read");
    assertTrue(response.getErrors().isEmpty(), "Error List not empty");
    verify(mockDatabaseDAO, times(3)).createOrUpdateUsers(any(Users.class));
  }

  @Test
  void testStore_blank() throws Exception {
    try (InputStream stream = setupStoreTest("").openStream()) {
      ExostarFileProcessResponse response = fileProcessor.store(stream);

      assertEquals(0, response.getLinesParsed(), "Lines Parsed");
      assertEquals(0, response.getLinesInFile(), "Lines Read");
      assertFalse(response.getErrors().isEmpty(), "Error List empty");
      assertEquals("No Records Parsed", response.getErrors().get(0));
      verify(mockDatabaseDAO, never()).createOrUpdateUsers(any(Users.class));
    }
  }

  @Test
  void testStore_errorCases() throws Exception {
    try (InputStream stream = setupStoreTest("DROP TABLE USERS;,000,null").openBufferedStream()) {
      ExostarFileProcessResponse response = fileProcessor.store(stream);

      assertEquals(0, response.getLinesParsed(), "Lines Parsed");
      assertEquals(1, response.getLinesInFile(), "Lines Read");
      assertFalse(response.getErrors().isEmpty(), "Error List empty");
      verify(mockDatabaseDAO, never()).createOrUpdateUsers(any(Users.class));

      assertEquals(3, response.getErrors().size(), "Error count");
      assertTrue(response.getErrors().get(0).startsWith("Invalid Name"), "Invalid Name");
      assertTrue(response.getErrors().get(1).startsWith("Invalid Phone Number"), "Invalid Name");
      assertTrue(response.getErrors().get(2).startsWith("Invalid Email"), "Invalid Name");
    }
  }

  @Test
  void testStore_mixed() throws Exception {
    try (InputStream stream =
        setupStoreTest("Eric Smith,3125915367,eric@movers.com\nJane Doe,4405699876,no")
            .openBufferedStream()) {
      ExostarFileProcessResponse response = fileProcessor.store(stream);

      assertEquals(1, response.getLinesParsed(), "Lines Parsed");
      assertEquals(2, response.getLinesInFile(), "Lines Read");
      assertFalse(response.getErrors().isEmpty(), "Error List empty");
      verify(mockDatabaseDAO, atMostOnce()).createOrUpdateUsers(any(Users.class));

      assertEquals(1, response.getErrors().size(), "Error count");
      assertTrue(response.getErrors().get(0).startsWith("Invalid Email"), "Invalid Email");
    }
  }

  @Test
  void testStandardizePhoneNumber() {
    assertEquals("+12121234567", fileProcessor.standardizePhoneNumber("2121234567"));
    assertEquals("+442098765432", fileProcessor.standardizePhoneNumber("442098765432"));
    assertEquals("+12121234567", fileProcessor.standardizePhoneNumber("+12121234567"));
  }

  @Test
  void testValidateName() {
    assertTrue(fileProcessor.validateName("John Smith"), "Failed validating Name");
    assertFalse(fileProcessor.validateName("DROP TABLE USERS;"), "Failed validating Name");
  }

  @Test
  void testValidatePhoneNumber() throws Exception {
    assertTrue(fileProcessor.validatePhoneNumber("+12125551212"), "Failed parsing NANP number");
    assertTrue(
        fileProcessor.validatePhoneNumber("+527441234567"), "Failed parsing International number");
    assertFalse(
        fileProcessor.validatePhoneNumber("+1212555121234"), "Failed checking invalid number");
    assertThrows(
        NumberParseException.class,
        () -> fileProcessor.validatePhoneNumber("2125551212"),
        "Failed parsing NANP number");
    assertThrows(NumberParseException.class, () -> fileProcessor.validatePhoneNumber("NaN"));
  }

  @Test
  void testValidateEmailAddress() {
    assertTrue(fileProcessor.validateEmailAddress("hello@java.com"), "Email Does not validate");
    assertFalse(fileProcessor.validateEmailAddress("this.is.not.an.email.com"), "Email validates");
  }
}
