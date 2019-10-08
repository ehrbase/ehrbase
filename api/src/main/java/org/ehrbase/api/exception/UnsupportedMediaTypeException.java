package org.ehrbase.api.exception;

/**
 * Runtime Exception to handle a not supported media type which a client
 * has specified as the response type. I.e. if the header "accept" exists with
 * value "text/html" but there is no implementation for this target mime-type
 * this exception can be thrown at each step during processing and finally
 * handled at the controller to create the corresponding HTTP response.
 *
 * @see RuntimeException
 */
public class UnsupportedMediaTypeException extends RuntimeException {

    public UnsupportedMediaTypeException(String message) {
        super(message);
    }
    public UnsupportedMediaTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
