package mgo.echo.handler.social.dto;

import mgo.echo.util.Error;

/**
 * Represents an error for a message recipient.
 */
public class MessageRecipientError {
    private final String recipient;
    private final Error error;

    public MessageRecipientError(String recipient, Error error) {
        this.recipient = recipient;
        this.error = error;
    }

    public String getRecipient() {
        return recipient;
    }

    public Error getError() {
        return error;
    }
}
