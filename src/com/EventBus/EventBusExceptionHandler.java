package com.EventBus;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EventBusExceptionHandler is the single place to handle all EventBus exceptions. In this framework all  exceptions (mostly runtime) are propagated to the EventBus implementer  and then handled  by EventBusExceptionHandler.
 * <p>An EventBus is a critical service facility and hence should be  protected from possible Exceptions and errors. A simple mechanism  employed here is to catch all runtime exceptions by the bus and forward them to EventBusExceptionHandler  to process them from  rethrowing to the callers. Optionally, for what ever reason, bus can also set to swallow exception intended to be thrown to the callers. A reThrow parameter facilitates this. If Exceptions are swallowed (i.e. not reThrown), EventBusExceptionHandler will log the error. But make sure that during exception testing, reThrow is set to true (default)
 * @author  Mustaq Ali
 */
public class EventBusExceptionHandler {
   private final Logger logger;
   private final String busId;
   private final boolean reThrow;

   EventBusExceptionHandler(String busId, Logger logger, boolean reThrow) {
      this.logger = logger;
      this.busId = busId + ":";
      this.reThrow = reThrow;
   }

    void handleBusException(String message, Throwable throwable) {
      String logMsg = busId + message + ":" + throwable.toString() + ":" + throwable.getMessage();
      if(reThrow){
         EventBusException busException = new EventBusException(busId + message, throwable);
         throw busException;
      } else {
         logger.log(Level.WARNING, logMsg);
      }
   }
}
