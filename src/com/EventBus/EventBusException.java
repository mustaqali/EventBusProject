package com.EventBus;

/**
 *
 * <p>Runtime exceptions thrown by handlers and subscribers code  are abstracted as EventBusException.
 * EventBus  and its EventBusExceptionHandler could decide to propagate the exception to client or swallow it.
 * @author Mustaq Ali
 */

public class EventBusException extends RuntimeException {

    public EventBusException(String detailMessage) {
        super(detailMessage);
    }

    public EventBusException(Throwable throwable) {
        super(throwable);
    }

    public EventBusException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

}
