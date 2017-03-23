package com.EventBus;

/**
 *
 * <p>EventBus interface defines the core  methods for event distribution  in a publish/subscribe system. An implementation of this interface should adhere to key characteristics of an EventBus as below:
 *
 * <p>An EventBus is a pub/sub feature  meant to facilitate communication between components and objects within a process space. An implementation of this EventBus provides a framework for very loosely coupled interaction between components. In that sense, publishers and subscribers don't have to know each other, and they can come and go  at any time. So an EventBus, as a hub, coordinates all the interactions between pub/sub components.
 *
 * <p>EventBus could receive large volume of events data in real time and push it to numerous  subscribers. To accommodate such a high throughput, the bus  could function like a pass through with minimal processing, and yet it must support key requirements of no missing or duping of events, as well as ordered  and  timely (concurrent) delivery of events to as  many subscribers. In addition, while minimal processing could help to reduce event handling errors, being a critical component of a large system, the bus needs to be as robust as possible to withstand publisher/subscriber errors as well.
 *
 * @author Mustaq Ali
 */

public interface EventBus {
    public void addSubscriber(Object subscriber);
    public void removeSubscriber(Object subscriber);
    public void publishEvent(Object event);
    //shutdown()
    //public void addSubscriberForFilteredEvents(); //Moved to Filter services Interface
}

