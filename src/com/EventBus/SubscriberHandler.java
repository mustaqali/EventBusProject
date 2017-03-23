package com.EventBus;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * A SubscriberHandler is a class that performs extraction of a subscriber's callback methods when that subscriber is added; it holds the all information related to event subscriptions for many subscribers; and provides subscriptions list to event handlers during event publication. A SubscriberHandler helps the EventBus for outside client call and the EventHandler for internal calls. It can be instantiated stand-alone and  it provides query methods  for subscribers/event existence status during testing.
 *
 * <p>All relevant data structures of the SubscriberHandler are thread safe and are derived from java.util.concurrent package
 *
 *<p><b>Callback Method Scanning:</b> During subscriber addition,  SubscriberHandler scans subscriber class for any qualified callback method for events. A subscriber can have any number of callback methods annotated by @Subscribe interface. However, in this implementation callback methods are restricted to a public method having a single Object  parameter (any object type). Also, there can  be no more than one method with same event type parameter. (If dupe methods were found while scanning, the  SubscriberHandler will pick an arbitrary one). Besides, only declared methods are considered for callback (i.e. no super class @Subscribe method is scanned). Nonetheless,  a derived class is treated as a separate event type.
 *
 * <p>This implementation doesn't process annotation parameters, though any new feature or existing features like caching can  be implemented via annotation as well.
 *
 * <p>Some Examples:
 *
 <p>{@literal @Subscribe }
 <p>public void onEvent(String event){ process(event)}

 <p>{@literal @Subscribe }
 <p>public void onEvent(Integer event){ process(event)}

 <p>{@literal @Subscribe }
 <p>public void onEvent(Number event){ process(event)}

 <p>{@literal @Subscribe }
 <p>public void onEvent(PriceQuote event){ process(event)}
 * @author Mustaq Ali
 */

public class  SubscriberHandler {
   private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> eventSubscriptions;
   private final Map<Object, CopyOnWriteArraySet<Class<?>>> subscriberEvents;
   static final Logger logger = Logger.getLogger(AsyncSynEventHandler.class.getName());

   SubscriberHandler() {
      eventSubscriptions = new ConcurrentHashMap<>();
      subscriberEvents = new ConcurrentHashMap<>();
   }

   CopyOnWriteArrayList<Subscription> getSubscriptions(Class<?> eventType) {
      //synchronized (this) {
      {
         return eventSubscriptions.get(eventType);
      }
   }

   List<Subscription> getAllSubscriptions(Object event) {
      Class<?> eventClass = event.getClass();
      //Underlying is CopyOnArray
      return getSubscriptions(eventClass);
   }

/** Find all (@Subscribe) Annotated public  Methods with single input parameter of EventType (any class)
 * from the passed subscriber object. Every @Subscribe annotated method is a callback,
 * provided  they have a distinct (already unprocessed) EventType parameter i.e An Overloaded methods will form
 * different subscriptions, but they should  all  have  @Subscribe annotation
 * Note: We don't allow event-inheritance publication, by which a subEvent publication  can trigger a callback
 * for  base EventType subscribers.
 * However, if necessary this module can be expanded to incorporate event-inheritance based subscription.
  */
    void addSubscriber(Object subscriber) {
      Class<?> subscriberClass = subscriber.getClass();
      List<Method> annotatedMethods = getAnnotatedMethods(subscriberClass, Subscribe.class);
      //Now the annotations are valid & verified
      for(Method method : annotatedMethods) {
         Class<?> eventType = method.getParameterTypes()[0];
         synchronized (this) {
            CopyOnWriteArraySet<Class<?>> eventsSubscribed = subscriberEvents.get(subscriber);
            if (eventsSubscribed == null) {
               eventsSubscribed = new CopyOnWriteArraySet<>();
               subscriberEvents.put(subscriber, eventsSubscribed);
            } else if (eventsSubscribed.contains(eventType)) {
               throw new EventBusException("Subscriber " + subscriber.getClass() +
                       " already registered to event " + eventType);
            }
            eventsSubscribed.add(eventType);

            Subscription newSubscriptionForAnEvent =
                    new Subscription(subscriber, method, eventType);
            CopyOnWriteArrayList<Subscription> subscriptions = eventSubscriptions.get(eventType);
            if (subscriptions == null) {
               subscriptions = new CopyOnWriteArrayList<>();
               eventSubscriptions.put(eventType, subscriptions);
            }
            subscriptions.add(newSubscriptionForAnEvent);

//            logger.log( Level.INFO,"Subscriber Added: " +
//                    newSubscribtionForAnEvent.method.getName()
//                    + " / " + newSubscribtionForAnEvent.subscriptionSignature + " / " +
//                    newSubscribtionForAnEvent.eventType.getClass().getName()
//            );
         }
      }
   } // addSubscriber

   public List<Method>  getAnnotatedMethods(Class<?> subscriberClass, Class annotation) {
      Method[] methods = subscriberClass.getDeclaredMethods();
      List<Method>  validMethods = new ArrayList<>();
      for (Method method : methods) {
         if (method.isAnnotationPresent(annotation)) {
            int modifiers = method.getModifiers();
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (((modifiers & Modifier.PUBLIC) != 0) && (parameterTypes.length == 1)) {
               Class<?> eventType = parameterTypes[0];
               validMethods.add(method);
            }
         }
      }
      return validMethods;
   }

    synchronized void  removeSubscriber(Object subscriber) {
      Set<Class<?>> subscribedEvents = subscriberEvents.get(subscriber);
      if (subscribedEvents != null) {
         for (Class<?> eventType : subscribedEvents) {
            CopyOnWriteArrayList<Subscription> subscriptionsForAnEvent = eventSubscriptions.get(eventType);
            if (subscriptionsForAnEvent != null) {
               subscriptionsForAnEvent.removeIf(s -> s.subscriber.equals(subscriber));
            }
         }
         subscriberEvents.remove(subscriber);
         logger.fine("Subscriber Removed: " + subscriber.getClass().getName());
      } else {
         // Log.w(TAG, "Subscriber to unregister was not registered before: " + subscriber.getClass());
         throw new EventBusException("No Subscriber found to unregister: " + subscriber.getClass());
      }
   }

   //Toggles Event Filter Setting a valid Predicate<T> reference is filter on / null is filter off
    void setEventFilter(Object subscriber, Class<?> eventType, Predicate<?> filter){
      Subscription subscription = findSubscription(subscriber, eventType);
      if(subscription != null) {
         subscription.filter = filter;
         logger.fine("Set Filter:" + (filter != null ? "ON" :"OFF"));
      }
      else {
         throw new EventBusException("This Subscriber or Event Not found:" + eventType.getName());
      }
   }

   //Toggles Event Caching  (true = Cache Event  / false = Publish as usual)
    void setCacheLastEvent(Object subscriber, Class<?> eventType, boolean setCaching){
      Subscription subscription = findSubscription(subscriber, eventType);
      if(subscription != null) {
         subscription.holdLastEvent = setCaching;
         logger.fine("Set Caching:" + (setCaching == true ? "ON" :"OFF") );
      }
      else {
         throw new EventBusException("This Subscriber or Event Not found:" + eventType.getName());
      }
   }

   Subscription findSubscription(Object subscriber, Class<?> eventType ) {
      CopyOnWriteArraySet<Class<?>> eventsSubscribed = subscriberEvents.get(subscriber);
      Subscription subscription = null;
      if (eventsSubscribed != null && eventsSubscribed.contains(eventType)) {
         CopyOnWriteArrayList<Subscription> subscriptions = eventSubscriptions.get(eventType);
         subscription = subscriptions.stream().filter(s -> s.signatureMatch(subscriber,eventType)).findFirst().orElse(null);
      }
      return subscription;
   }

   public boolean subscriberExists(Object subscriber, Class<?> eventType) {
      return (findSubscription(subscriber, eventType) != null) ? true : false;
   }
   public Predicate findFilter(Object subscriber, Class<?> eventType) {
      Subscription s = findSubscription(subscriber, eventType);
      return   s != null ? s.filter : null;
   }
   public boolean isCachingSet(Object subscriber, Class<?> eventType) {
      Subscription s = findSubscription(subscriber, eventType);
      return   s != null ? s.holdLastEvent : false;
   }
   public int getSubscriberCount() {
      return   subscriberEvents.size();
   }

   public int getSubscriberEventCount(Object subscriber) {
      return   (subscriberEvents.get(subscriber) != null)  ? subscriberEvents.get(subscriber).size() : 0;
   }

   Map<Object, CopyOnWriteArraySet<Class<?>>>  getSubscriberEvents() {
      return subscriberEvents;}
}


