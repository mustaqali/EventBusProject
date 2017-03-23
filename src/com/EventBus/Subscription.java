package com.EventBus;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A {@literal Subscription<T>} is primarily a data abstraction within the bus. A subscriber reference and a single  event type  constitutes a unique Subscription and its hashCode. However, in addition to these key fields a Subscription also holds subscriber's corresponding  event callback method and other optional service data for Filtering and Caching etc. There could be more than one Subscription object for a single subscriber based on number of distinct event types it has subscribed.
 *
 * <p>Subscription objects are created by SubscriberHandler during addSubscriber() / registration process, and all of its Subscriptions  are removed when the subscriber removes/unregister itself.
 *
 * <p>Additionally, after adding itself to the EventBus, at runtime a subscriber can activate/deactivate Filtering and LastEventCaching services for an event, and the Subscriptions holds these status.
 *@author Mustaq Ali
 */
public  class Subscription<T> {
    final Object subscriber;
    final Class<?> eventType;
    final Method method;
    volatile  boolean holdLastEvent;
    volatile Predicate<?>  filter;
    String subscriptionSignature;
    // final int priority; // Not used, but can support abstraction of prioritized publishing order to subscribers


    int hash; // Precomputed hash

    public Subscription(Object subscriber, Method method, Class<?> eventType) {
        this.subscriber = subscriber;
        this.method = method;
        this.eventType = eventType;
        this.filter = null;
        this.holdLastEvent = false;
        this.subscriptionSignature = makeSubscriptionId(subscriber, eventType);
        this.hash = this.hashCode();
        // System.out.println("Subscription Added: " + subscriptionSignature);
    }


    //- Subscriber.Class  + Event.Class names form unique subscription.
    //- Method name ignored, as multiple methods with same event parameter is not allowed
    //i.e. only one callback method per event type

    static public String makeSubscriptionId(Object subscriber, Class<?> eventType) {
        StringBuilder builder = new StringBuilder(64);
        builder.append(subscriber.getClass().getName());
        // builder.append('#').append(method.getName()); // no need: We allow only one method per eventType
        builder.append('-').append(eventType.getName());
        return builder.toString();
    }

    public String getSubscriptionId(){
        return this.subscriptionSignature;
    }

    boolean signatureMatch(Object Subscriber, Class<?> eventType){
        return getSubscriptionId().equals(makeSubscriptionId(Subscriber, eventType));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        else if (other instanceof Subscription) {
            Subscription otherSubscriber = (Subscription)other;
            return subscriptionSignature.equals(otherSubscriber.getSubscriptionId());
        } else {
            return false;
        }
    }

    public int getLocalHashCode(){
        return this.hash;
    }

    @Override
    public int hashCode() {
        //- Objects.hash could return negative and could lead array index problem
        // with hash partitioning. So need to make index +ve.
        //- Other robust hash calculations are possible like removing sign bit of the int etc.
        return Math.abs(Objects.hash(subscriber,eventType));
    }
}

