package com.EventBus;

import java.util.logging.Logger;

/**
 * A SyncEventBus is an implementation of EventBus interface meant to operate in a single threaded mode. In a single thread mode a publisher thread itself  performs the tasks of publishing and invoking subscriber's callback.
 * <p>SyncEventBus is implemented as an aggregation of different handler services, and as such it simply delegates all the client calls to the SyncEventHandler object for event dispatching. It also relies on a SubscriberHandler class for managing  subscribers and event subscriptions. SyncEventBus handles all the  Exceptions for the bus at a single point with the help of an EventBusExceptionHandler class.
 * <p>Please refer to respective handlers for details.
 *@author Mustaq Ali
 */

public class SyncEventBus implements EventBus {
     private final String busId;
     private final SubscriberHandler subscriptionHandler;
     private final SynEventHandler synEventHandler;
     static final Logger logger = Logger.getLogger(EventBus.class.getName());
     EventBusExceptionHandler exceptionHandler;


    public SyncEventBus() {
        this("SyncEventBus", true);
    }

   public SyncEventBus(String id) {
      this("SyncEventBus", true);
   }

   public SyncEventBus(boolean reThrow) {
      this("SyncEventBus", reThrow);
   }

    public SyncEventBus(String id, boolean reThrow) {
        this.busId = id;
        this.subscriptionHandler = new SubscriberHandler();
        this.synEventHandler = new SynEventHandler(subscriptionHandler);
        this.exceptionHandler =  new EventBusExceptionHandler(busId, logger, reThrow );
    }

    public void addSubscriber(Object subscriber) {
        try {
            subscriptionHandler.addSubscriber(subscriber);
        }catch(EventBusException e) {
            exceptionHandler.handleBusException("Error Remove  Subscriber:", e);
        }
    }

   public void removeSubscriber(Object subscriber) {
      try {
         subscriptionHandler.removeSubscriber(subscriber);
      } catch (EventBusException e) {
         exceptionHandler.handleBusException("Error Remove Subscriber:", e);
      }
   }

   public void publishEvent(Object event) {
        try {
            synEventHandler.queueAndPublish(event);
        } catch (EventBusException e){
            exceptionHandler.handleBusException("Error inPublisEvent:", e);
        }
    }
    public SubscriberHandler SubscriberHandler(){
        return subscriptionHandler;
    }
}

