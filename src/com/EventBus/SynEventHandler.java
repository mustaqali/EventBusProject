package com.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A SyncEventHandler is the main class that handles event delivery for a single threaded bus. Essentially the publisher executes all  calls starting from publishEvent() at the bus to invoking each subscribers callback methods. For a single threaded bus to be effective in performance, callback operations should be light and they shouldn't block (and error free!).
 *
 * <p>In a single threaded bus, though a publisher thread executes end-to-end event handling, multiple publishers  can  use this handler. Therefore SyncEventHandler is made thread safe, and the thread safety  is achieved  by each publisher's thread having its own ThreadLocal object (PublisherThreadState class).
 *
 * <p>SyncEventHandler handler is also reentrant safe. Reentrant occurs when a callback method itself  publishes another event as a result of processing an event it had received. (In a Trading system  an Order event subscriber could  publish a Trade event or Order Reject event etc.) Note: Reentrant could end up as a recursive call, so the reentrant
 * callback method should exit properly without looping.
 *
 *<p> In SyncEventHandler the publisher's callback execution is abstracted as an Executor  by implementing  a dummy/direct executor of PublisherThreadExecutor.
 *
 * <p>SyncEventHandler can be instantiated stand-alone without a bus.
 * @author Mustaq Ali
 */
public class SynEventHandler {
   SubscriberHandler subscriptionHandler;
   final Executor executor = new PublisherThreadExecutor();
   private final ThreadLocal<PublisherThreadState>
           currentPublisherThreadState = ThreadLocal.withInitial(PublisherThreadState::new);
   static final Logger logger = Logger.getLogger(SynEventHandler.class.getName());

   SynEventHandler(SubscriberHandler subscriptionHandler){
      this.subscriptionHandler = subscriptionHandler;
   }

   void queueAndPublish(Object event) throws Error {
      PublisherThreadState publisherState = currentPublisherThreadState.get();
      List<Object> eventQueue = publisherState.eventQueue;
      eventQueue.add(event);
      if (!publisherState.isPublishing) {
         publisherState.isPublishing = true;
         while (!eventQueue.isEmpty()) {
            try {
               Object thisEvent = eventQueue.remove(0);
               publishAnEvent(thisEvent);
            } finally {
               publisherState.isPublishing = false;
            }
         }
      }
   }

   boolean  publishAnEvent(Object event) throws Error {
      List<Subscription> subscriptions =  subscriptionHandler.getAllSubscriptions(event);
      if (subscriptions != null && !subscriptions.isEmpty()) {
         publishToSubscribers(event, subscriptions);
      }
      else {
         logger.warning( "No subscribers registered for event " + event.getClass().getName());
         return  false;
      }
      return  true;
   }

   /**
    * When publishing thread  invokes a subscriber's  callback(single threaded bus),
    queue the next event when the publisher is busy (ie. there is already a publication in progress).
    This is will avoid reentrant  blocking at the callback method when a subscriber's callback does a re-publishing
    of another event within the callback stack / - a recursive call.
    Also,  re-publishing  could end up infinite-loop  if not handled properly.
    * @param event
    * @param subscriptions
    * @throws Error
    */
    void  publishToSubscribers(Object event, List<Subscription> subscriptions) throws Error {
      for (Subscription subscription : subscriptions) {
         publishToSingleSubscriber(event, subscription);
      }
   }

    void publishToSingleSubscriber(Object event, Subscription subscription) {
      invokeSubscriber(event, subscription, executor);
   }

   /**
    * Last entry point for the bus before the call is forwarded to subscribers callback. All the subscribers
    * errors will fall on this. Thus a robust error handling is necessary to protect the bus.
    */
   void invokeSubscriber(Object event, Subscription subscription, Executor executor) {
      //System.out.println("Invoking Callback");
      executor.execute(
              new Runnable() {
                 @Override
                 public void run() {
                    try {
                       subscription.method.invoke(subscription.subscriber, event);
                    } catch(InvocationTargetException e) {
                       throw new EventBusException(
                               subscription.method.getName() + "/" + event.getClass().getName(),e);
                    } catch(IllegalAccessException e) {
                       throw new EventBusException("Unexpected Exception:" +
                               subscription.method.getName() + "/" +  event.getClass().getName(),e);
                    }
                 }
              });
   }

    void setSubscriptionHandler(SubscriberHandler subscriptionHandler) {
      this.subscriptionHandler = subscriptionHandler;
   }
   /**
    Publisher threads own states used in ThreadLocal
    */
   private static final  class PublisherThreadState {
      final List<Object> eventQueue = new ArrayList<Object>();
      boolean isPublishing;
   }
   //Dummy/Direct  Executor that runs on the same / publisher's  thread
   private static final class PublisherThreadExecutor implements Executor {
      public void execute(Runnable r) {
         r.run();
      }
   }
}
