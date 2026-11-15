package com.ngo.donation.exception;

import com.ngo.donation.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test-endpoint");
    }

    @Test
    @DisplayName("ErrorResponse field construction should correctly populate all fields")
    void testErrorResponseFieldConstruction() {
        LocalDateTime now = LocalDateTime.now();
        ErrorResponse response = new ErrorResponse(400, "Bad Request", "Invalid input", now, "/api/test");

        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getError());
        assertEquals("Invalid input", response.getMessage());
        assertEquals(now, response.getTimestamp());
        assertEquals("/api/test", response.getPath());

        // Test setters
        response.setStatus(404);
        response.setError("Not Found");
        response.setMessage("Missing");
        response.setPath("/api/other");
        LocalDateTime nextTime = now.plusMinutes(1);
        response.setTimestamp(nextTime);

        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getError());
        assertEquals("Missing", response.getMessage());
        assertEquals("/api/other", response.getPath());
        assertEquals(nextTime, response.getTimestamp());
    }

    @Test
    @DisplayName("BadRequestException should map to HTTP 400 Bad Request")
    void testHandleBadRequestException() {
        BadRequestException ex = new BadRequestException("Invalid date range provided");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequest(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Invalid date range provided", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("UnauthorizedException should map to HTTP 401 Unauthorized")
    void testHandleUnauthorizedException() {
        UnauthorizedException ex = new UnauthorizedException("Invalid email or password");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorized(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
        assertEquals("Invalid email or password", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    @DisplayName("ForbiddenException should map to HTTP 403 Forbidden")
    void testHandleForbiddenException() {
        ForbiddenException ex = new ForbiddenException("Donors can only access their own donation details.");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleForbidden(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
        assertEquals("Donors can only access their own donation details.", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    @DisplayName("AccessDeniedException should map to HTTP 403 Forbidden with standard message")
    void testHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
        assertEquals("Access denied: Insufficient permissions.", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    @DisplayName("ResourceNotFoundException should map to HTTP 404 Not Found")
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Campaign not found with ID: 99");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Campaign not found with ID: 99", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    @DisplayName("NoResourceFoundException should map to HTTP 404 Not Found with endpoint path")
    void testHandleNoResourceFoundException() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/api/unknown-endpoint", "/api/unknown-endpoint");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNoResourceFound(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("The requested endpoint does not exist: /api/unknown-endpoint", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    @DisplayName("ConflictException should map to HTTP 409 Conflict")
    void testHandleConflictException() {
        ConflictException ex = new ConflictException("Email is already registered: test@ngo.org");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConflict(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
        assertEquals("Email is already registered: test@ngo.org", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    @DisplayName("InsufficientFundsException should map to HTTP 422 Unprocessable Entity")
    void testHandleInsufficientFundsException() {
        InsufficientFundsException ex = new InsufficientFundsException("Expense amount 50000 exceeds available campaign funds 20000");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInsufficientFunds(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(422, response.getBody().getStatus());
        assertEquals("Unprocessable Entity", response.getBody().getError());
        assertEquals("Expense amount 50000 exceeds available campaign funds 20000", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    @DisplayName("InvalidOperationException should map to HTTP 422 Unprocessable Entity")
    void testHandleInvalidOperationException() {
        InvalidOperationException ex = new InvalidOperationException("Donations can only be made to ACTIVE campaigns.");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidOperation(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(422, response.getBody().getStatus());
        assertEquals("Unprocessable Entity", response.getBody().getError());
        assertEquals("Donations can only be made to ACTIVE campaigns.", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException should return HTTP 400 with field validation details")
    void testHandleMethodArgumentNotValidException() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "campaignRequest");
        bindingResult.addError(new FieldError("campaignRequest", "goalAmount", "Goal amount must be greater than 0"));
        bindingResult.addError(new FieldError("campaignRequest", "name", "Campaign name is required"));

        MethodParameter parameter = new MethodParameter(
                this.getClass().getDeclaredMethod("setUp"), -1
        );
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentNotValid(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("goalAmount: Goal amount must be greater than 0"));
        assertTrue(response.getBody().getMessage().contains("name: Campaign name is required"));
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    @DisplayName("HttpMessageNotReadableException should return HTTP 400 with safe malformed JSON message")
    void testHandleHttpMessageNotReadableException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Unexpected character",
                new MockHttpInputMessage("bad json".getBytes())
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadable(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Malformed JSON request body or invalid data type.", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException should return HTTP 400 with parameter name")
    void testHandleMethodArgumentTypeMismatchException() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "campaignId", null, new IllegalArgumentException("For input string: \"abc\"")
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Invalid parameter value for: campaignId", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    @DisplayName("Unexpected generic exception should return HTTP 500 without leaking internal details")
    void testHandleGenericExceptionDoesNotExposeInternalDetails() {
        RuntimeException ex = new RuntimeException("CRITICAL: Database connection failed at jdbc:mysql://internal-db:3306 with password secret123");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());

        // MUST NOT contain internal SQL / credential / stack trace information
        assertFalse(response.getBody().getMessage().contains("Database connection failed"));
        assertFalse(response.getBody().getMessage().contains("secret123"));
        assertFalse(response.getBody().getMessage().contains("internal-db"));
        assertEquals("An unexpected error occurred. Please try again later.", response.getBody().getMessage());
        assertEquals("/api/test-endpoint", response.getBody().getPath());
    }

    @Test
    @DisplayName("Unexpected generic exception should not log sensitive exception message or stack trace")
    void testHandleGenericExceptionDoesNotLogSensitiveDetails() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            String sensitiveMessage = "CRITICAL: password=supersecret jdbc:mysql://internal-db:3306 connection failed";
            RuntimeException ex = new RuntimeException(sensitiveMessage);

            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

            assertNotNull(response);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("An unexpected error occurred. Please try again later.", response.getBody().getMessage());

            assertFalse(listAppender.list.isEmpty(), "Expected at least one log event to be captured");

            for (ILoggingEvent event : listAppender.list) {
                String formattedMessage = event.getFormattedMessage();

                // Verify captured log messages do not contain sensitive markers or original message
                assertFalse(formattedMessage.contains("supersecret"), "Log must not contain 'supersecret'");
                assertFalse(formattedMessage.contains("internal-db"), "Log must not contain 'internal-db'");
                assertFalse(formattedMessage.contains(sensitiveMessage), "Log must not contain original exception message");

                // Verify no throwable proxy or stack trace is attached
                assertNull(event.getThrowableProxy(), "Log event must not contain a throwable proxy / stack trace");

                // Verify only the class name and fixed message are logged
                assertTrue(formattedMessage.contains(RuntimeException.class.getName()), "Log should contain exception class name");
            }
        } finally {
            logger.detachAppender(listAppender);
            listAppender.stop();
        }
    }
}
