package com.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * An AsyncEventHandler provides  services to AsyncEventBus for  implementing  multi threaded event delivery, event filtering for a subscriber, and caching of latest event for an event type requested by subscribers.
 *
 *
 * <p><b>Multi Thread  event Delivery:</b> Event ordering is a critical requirement for pub/sub framework, without it a bus can't support  application like price quote etc.  To facilitate this key feature of a thread safe "ordered" event delivery, AsyncSynEventHandler implements a custom  thread pool called <i>"hash-partitioned-thread-pool"</i>. Java stock thread pools such as CachedThreadPool etc. can't  guarantee ordered sequence of event delivery due to the  possibilities of multiple threads   handling a specific event stream to a particular subscriber. In practice, strict event ordering for each subscriber necessitates event serialization at the delivery point (even with queueing).  One way to achieve this is to have a single thread assigned to deliver  all the events of a type to a subscriber (aka Subscription in this framework). To realize a proper sequence of event delivery, AsyncSynEventHandler implements a <i>hash-partitioned-thread-pool</i> by using an array of single threads (Java SingleThreadExecutor, but any single thread would do). Upon an event arrival and at the time of event distribution to subscribers (an event delivery loop), a delivery thread to a subscriber is chosen from the thread pool array  using the hashCode of the Subscription object. (which is made out of Subscriber reference + Event class). The logic for thread selection is:
 *<p>{@code  Thread_Id_Array_Index = (Subscription.hashCode) % (thread_count) }
 *
 * <p>Though a particular thread handles one specific event type for a subscriber at all times, however, handling of different event-type(s) for the same subscriber could fall into another thread (due to different hash codes). Thus the thread pool load is distributed even while servicing a single subscriber. (Evidently the same thread can also service another subscriber for the same event). In a  low  thread-count system,  threads could be overloaded due to chances of hash collision. But in a typical system with many Subscriptions thread  load will  be fairly  distributed.
 *
 *
 * <p><b>Filtering Events:</b> A subscriber can register a filtering function ( for a particular event type using Java Functional interface of {@literal Predicate<T>} with boolean Lambda  expression for the event). During the delivery of the event to the subscriber, the bus will evaluates the filter against the event and will allow or block the event. The usage is as follows:
 *
 * <p>{@code  Predicate<Trade> bigListedTrade = t -> t.quantity >= 100000 && t.exchange.equals("NYSE") }
 * <p>{@code bus.setEventFilter(tradeSubscriber, Trade.class, bigListedTrade); }
 *
 * <p><b>Warning:</b> As setEventFilter()  method accepts wildcard type  parameters,  an issues related to filtering exists when a subscriber uses the method {@literal setEventFilter (Object subscriber, Class<?> eventType, Predicate<?>  filter )}. For filtering to work properly, static Types of Event Class and Predicate Class should match during the call. Otherwise a runtime error of type mismatch will occur during filter evaluation and an EventBusException will be thrown. (The Predicate loses its type due to type erasure. An alternate implementation of filtering is possible via an annotation of @Filter method as described later)
 *
 * <p><b>Caching Last Events:</b> A busy  or real-time bound subscriber may decide not to receive continuous event stream  due to its business needs (as in, it's only interested in last price quote etc.). It  can do so by requesting the EventBus to Cache only the last / latest event, with an intention of processing it later. In this implementation, a subscriber can set/reset
  event-type-caching at any time, but  after caching is set no further event delivery will be done for that subscriber.
 However, the subscriber can  poll the the latest event whenever it requires it, or it can reset the caching
 to resume normal subscription. As an additional feature, at the end of cache reset call the bus will also selectively publish any latest cached event  only to this subscriber  to keep the subscriber up to date. The caching is done at  the event-type level, so all the subscribers in the bus will share the same cache for an event type. However, caching by a subscriber doesn't affect other normal subscribes event delivery. Any subscriber can remove a caching or clear all event caching ( no subscriber privilege is implemented here)
 *
 * <p>The caching is thread safe by using java.util.ConcurrentHashMap
 *
 *<p><b>Alternate Implementation Choice:</b> This EventBus framework relies on  method annotation (@Subscribe) for inferring callback methods. In addition to callback discovery, one can alternatively  use Annotations and its Parameter/Value  to implement features like caching and filtering as described above. Besides, filtering logic itself can be defined like callback method using another annotation  such as  @Filter to designate a filtering method. However, in this framework  caching and filtering is done via conventional method calls and parameters to the EventBus. This choice is made due to the fact that Caching and Filtering are dynamic in nature and the subscriber could change the  filtering as necessary based on dynamic business needs. Lambdas are also a natural fit for function passing like filtering. In fact, even a callback method itself could be a  parameter using java {@literal Consumer<T>} Functional interface and  can be sent to the bus as a method parameter. Annotations, besides being cumbersome,  is pretty much static in nature and may not be versatile for dynamic situations.
 *
 * @author Mustaq Ali
 */
public class AsyncSynEventHandler extends SynEventHandler {
   private final int workerCount;
   private final List<ExecutorService> threadList = new ArrayList<>();
   private final Map<Class<?>, Object> cachedEvents = new ConcurrentHashMap<>();
   SubscriberHandler subscriptionHandler;

   //Executor executor = Executors.newCachedThreadPool();

   AsyncSynEventHandler(SubscriberHandler subscriptionHandler, int threadCount) {
      super(subscriptionHandler);
      this.subscriptionHandler = subscriptionHandler;
      this.workerCount = threadCount;
      setExecutorPool();
      // this.logger.setLevel(Level.WARNING);
   }

   private void setExecutorPool() {
      for (int i = 0; i < workerCount; i++) {
         threadList.add(Executors.newSingleThreadExecutor());
      }
   }

    void shutDownExecutorPool(){
      threadList.forEach(thread -> thread.shutdown());
   }

   @Override
    void  publishToSubscribers(Object event, List<Subscription> subscriptions) throws Error {
      for (Subscription subscription : subscriptions) {
         publishToSingleSubscriber( event,subscription);
      }
   }

   @Override
    void publishToSingleSubscriber(Object event, Subscription subscription) {
      if (subscription.filter == null  || isEventAllowed(event, subscription) ) { //Allow
         if (subscription.holdLastEvent == true) {
            cachedEvents.put(subscription.eventType, event);
            logger.fine("Event Cached: " + event);
            return;
         }
         int threadId = Math.abs(subscription.hash) % workerCount;
         logger.fine("Channel/Hash/Event: " + threadId + " / " + subscription.hash + " / " + event);

         Executor currExecutor = threadList.get(threadId);
         //Executor currExecutor = super.executor;
         //Executor currExecutor = Executors.newCachedThreadPool();
         invokeSubscriber(event, subscription, currExecutor);
      }
      else {
         logger.fine("Event Filtered:" + event);
      }
   }

    void publishACachedEvent( Class<?> eventType, Object subscriber) {
      Subscription subscription = subscriptionHandler.findSubscription(subscriber, eventType);
      Object event = getCachedEvent(eventType);
      if (event != null) {
         publishToSingleSubscriber( event, subscription);
      }
   }

    void setSubscriptionHandler(SubscriberHandler subscriptionHandler) {
      this.subscriptionHandler = subscriptionHandler;
   }

    boolean isEventAllowed(Object event, Subscription subscription) {
      try {
         if (subscription.filter.test(event)) return true;
      } catch (ClassCastException e) {
         throw new EventBusException(e.getMessage());
      }
      return false;
   }

    Object getCachedEvent(Class<?> eventType) {
      return cachedEvents.get(eventType);
   }

   Object  removeCachedEvent(Class<?> eventType) {
      return cachedEvents.remove(eventType);
   }

   void clearAllCachedEvent() {
      cachedEvents.clear();
   }

}
