package com.EventBus;

import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**

 *
 * An AsyncEventBus is a multi threaded implementation of the EventBus interface. It delivers the events using separate thread pool to enhance concurrency and scale. The AsyncEventBus specifically  off-loads  publisher's thread from callback invocation as opposed to single threaded bus (SyncEventBus) which uses publisher's thread all the way for publishing to subscriber callback.
 *
 *<p>Additionally, this AsyncEventBus implements a FilteringService interface  to  provide enhanced event delivery. These additional services are:  Filtering  events based on Subscriber provided filtering function, and the Caching of Latest event so that subscriber can pause its (continuous) subscription when only interested in latest event.
 *
 * <p>The  AsyncEventBus relies  on AsyncEventHandler for event delivery and reuses SubscriberHandler and ExceptionHandler for other core functions.
 * Note: As an AsyncEventBus is just a forwarder class, so it can as well hold a SyncEventHandler instead to perform single threaded functions. (not tested)
 *
 * <p>Please refer to AsyncEventHandler for more details.
 * @author Mustaq
 */
public class AsyncEventBus   implements EventBus, FilteringService{
   private static final int SINGLE_THREAD_POOL_COUNT = 10;
   private final String busId;
   private final SubscriberHandler subscriptionHandler;
   private final AsyncSynEventHandler eventHandler; //For AsyncMode
   private final boolean reThrow;
   EventBusExceptionHandler exceptionHandler;
   static final Logger logger = Logger.getLogger(AsyncEventBus.class.getName());
   //private final SynEventHandler syncEventHandler;  //If needed, AsyncBus can be configured in SyncMode!

   public AsyncEventBus() {
      this("AsyncEventBus");
   }
   public AsyncEventBus(String id) {
      this(id,SINGLE_THREAD_POOL_COUNT,true);
   }
   public AsyncEventBus(int threadCount) {
      this("AsyncEventBus",threadCount,true);
   }
   public AsyncEventBus(boolean reThrow) {
      this("AsyncEventBus",SINGLE_THREAD_POOL_COUNT, reThrow);
   }
   public AsyncEventBus(String id, int threadCount) {
      this(id,threadCount,true);
   }
   public AsyncEventBus( int threadCount,boolean reThrow) {
      this("AsyncEventBus", threadCount,reThrow);
   }
   public AsyncEventBus(String id, int threadCount, boolean reThrow) {
      this.busId = id;
      this.subscriptionHandler = new SubscriberHandler();;
      this.eventHandler = new AsyncSynEventHandler(subscriptionHandler, threadCount);
      this.reThrow = reThrow;
      this.exceptionHandler =  new EventBusExceptionHandler(busId, logger, reThrow );
      //this.logger.setLevel(Level.WARNING);
      logger.fine("Event Bus Started");
   }

   @Override
   public void addSubscriber(Object subscriber) {
      try {
         subscriptionHandler.addSubscriber(subscriber);
      } catch (EventBusException e) {
         exceptionHandler.handleBusException("Error Add Subscriber:", e);
      }
   }

   @Override
   public void removeSubscriber(Object subscriber) {
      try {
         subscriptionHandler.removeSubscriber(subscriber);
      } catch (EventBusException e) {
         exceptionHandler.handleBusException("Error Remove Subscriber:", e);
      }
   }

   @Override
   public void publishEvent(Object event) {

      try {
         eventHandler.publishAnEvent(event);
      } catch (EventBusException e){
         exceptionHandler.handleBusException("Error  PublishEvent:", e);
      }
   }

   @Override
   public void setEventFilter(Object subscriber, Class<?> eventType, Predicate<?> filter){
      try {
         subscriptionHandler.setEventFilter(subscriber, eventType, filter);
      } catch (EventBusException e){
         exceptionHandler.handleBusException("Error in Set Filter:", e);
      }
   }

   @Override
   public void setCacheLastEvent(Object subscriber, Class<?> eventType, boolean setCaching){

      subscriptionHandler.setCacheLastEvent(subscriber,eventType, setCaching);

      // When a subscriber suspends caching (setCaching = false), normal publishing should eventually resume on next live event,
      // however, just send  the current cached event (if any available) ONLY  to this subscriber to keep them up-to-date
      // Otherwise this cached event needs to  be polled manually.
      if (setCaching == false) {
         eventHandler.publishACachedEvent( eventType, subscriber );
      }
   }

   @Override
   public <T> T  pollCashedEvent(Class<T> eventType){
      return  eventType.cast(eventHandler.getCachedEvent(eventType));
   }

   @Override
   public <T> T  removeCachedEvent(Class<T> eventType){
      return eventType.cast(eventHandler.removeCachedEvent(eventType));
   }

   @Override
   public void removeAllCacheEvent(){
      eventHandler.clearAllCachedEvent();
   }

   public SubscriberHandler SubscriberHandler(){
      return subscriptionHandler;
   }

   public AsyncSynEventHandler EventHandler(){
      return eventHandler;
   }

   public void shutdownTheBus() {
      eventHandler.shutDownExecutorPool();
   }


   /* Can use this logic to run the AsyncBus as a SyncBus by switching the eventHandler
   public AsyncEventBus(String id, boolean runInSyncMode) {
      this.busId = id;
      this.subscriptionHandler = new SubscriberHandler();
      if (runInSyncMode == true) {
         this.syncEventHandler = new SynEventHandler(subscriptionHandler);
         this.eventHandler = null;
      } else {
         this.eventHandler = new AsyncSynEventHandler(subscriptionHandler, SINGLE_THREAD_POOL_COUNT);
         this.syncEventHandler = null;
      }
   }*/
}



