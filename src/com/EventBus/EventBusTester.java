package com.EventBus;
import java.util.function.Predicate;
/**
 * Simple  stub for EventBus testing
 * Created by Mustaq on 2/24/2017.
 */
public class EventBusTester {

   int intCount;
   int strCount;
   int objCount;
   String lastStringEvent;

   EventBusTester() {}
   public static void main(String args[]) throws Exception{

      EventBusTester subscriber = new EventBusTester();
      String pubEvent = "Hello";

//Test single threaded bus
      System.out.println("---Sync/Single Threaded EventBus Test:");
      SyncEventBus syncBus = new SyncEventBus("SyncMyBus" );
      syncBus.addSubscriber(subscriber);
      syncBus.publishEvent(pubEvent);
      System.out.println("String Event Received: " + subscriber.lastStringEvent);
      syncBus.publishEvent(new Integer(55));
      syncBus.publishEvent(new Object());

//Test multithreaded bus
      System.out.println("---Async/Multi Threaded EventBus Test:");
      AsyncEventBus asyncBus = new AsyncEventBus("AsyncMyBus",  4);
      pubEvent = "World";
      asyncBus.addSubscriber(subscriber);
      asyncBus.publishEvent(pubEvent);
      Thread.sleep(10);
      System.out.println("String Event Received: " + subscriber.lastStringEvent);
      asyncBus.publishEvent(new Integer(155));

      Thread.sleep(10);
      asyncBus.removeSubscriber(subscriber);

//Test Filtering. True predicate will allow events to be published
      System.out.println("---Async/Multi Threaded EventBus Filter EventBus Test:");
      Predicate<Integer> bigTradeFilter = t -> {
         return t > 10000;
      };

      asyncBus.addSubscriber(subscriber);
      asyncBus.setEventFilter(subscriber, Integer.class,bigTradeFilter);
      asyncBus.publishEvent(new Integer(500)); // Filtered Out
      asyncBus.publishEvent(new Integer(20000)); // Allowed
   }

   //Callback methods
   @Subscribe
   public void onEvent(Object  event) {
      System.out.println("Callback Obj: " + event + " / Object Event Count= " + ++objCount);
   }

   @Subscribe
   public void onEvent(String  event) {
      lastStringEvent = event;
      System.out.println("Callback String: " + event + " / String Event Count= " + ++strCount);
   }

   @Subscribe
   public void onEvent(Integer event) {
      System.out.println("Callback Integer: " + event.intValue() + " / Integer Event Count= " + ++intCount);
   }
}
