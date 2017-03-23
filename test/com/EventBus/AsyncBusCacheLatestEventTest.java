package com.EventBus;

/**
 * Test suite for Cach Latest event tests
 * Created by Mustaq on 2/25/2017.
 */

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class AsyncBusCacheLatestEventTest {

   AsyncEventBus bus;
   SubscriberHandler sH;

   String pubEvent;
   String subEvent;
   Integer intEvent;
   Double doubleEvent;
   int eCount;
   int catchupTime = 15; // wait_a_bit: msec


   @Before
   public void setup() {
      bus = new AsyncEventBus(2);
      sH = bus.SubscriberHandler();
   }

   @After
   public void reset() {
      bus.shutdownTheBus();
      bus = null;
      sH = null;
   }

   @Test
   public void testCacheStatusAfterSubscription() throws Exception {
      bus.addSubscriber(this); // This Subscribes  String & Integer events
      bus.setCacheLastEvent(this, String.class, true);
      assertTrue(":", true ==  sH.isCachingSet(this, String.class));
   }

   @Test (expected = EventBusException.class)
   public void testTrySetCachingWithoutSubscription() throws Exception {
      bus.setCacheLastEvent(this, String.class, true);
   }

   @Test (expected = EventBusException.class)
   public void testTrySetCachingForUnSubscribedEvent() throws Exception {
      bus.addSubscriber(this); // Subscribe for  String & Integer events here
      bus.setCacheLastEvent(this, Double.class, true);
   }

   @Test
   public void testEventPublicationBeforeCachingAfterCaching() throws Exception {

      //Caching OFF
      bus.addSubscriber(this); // Subscribe for  String & Integer events here
      bus.publishEvent("Hello");
      wait_a_bit();
      assertTrue("Normal Subscription, Got Event:",subEvent.equals("Hello"));

      //Caching ON
      bus.setCacheLastEvent(this, String.class, true);
      subEvent = null; // Clear Stale events
      bus.publishEvent("CacheMe");
      wait_a_bit();
      assertTrue("Caching On and didn't receive any Event:",null == subEvent);
      //  System.out.println(subEvent);
   }

   @Test
   public void testPollCachedEventAfterCaching() throws Exception {
      bus.addSubscriber(this); // Subscribe for  String & Integer events here

      //Caching ON
      bus.setCacheLastEvent(this, String.class, true);
      bus.publishEvent("CacheMe2");
      wait_a_bit();
      assertTrue("Caching On didn't receive any Event:",null == subEvent);

      //Manually receive event
      String polledEvent = bus.pollCashedEvent(String.class);
      assertTrue("Pulled Cached Event:",polledEvent.equals("CacheMe2"));
   }

   @Test
   public void testAutoDeliveryAndNormalDeliveryAfterCachingOff() throws Exception {
      bus.addSubscriber(this); // Subscribe for  String & Integer events here

      //Caching ON
      bus.setCacheLastEvent(this, String.class, true);  // Cache On
      bus.publishEvent("CacheMe3"); // Bus Should have Cached this
      wait_a_bit();
      assertTrue("Didn't receive any Event after Caching set:",null == subEvent);

      // Switch OFF caching & Receive Latest/Last Cached event automatically just after caching off
      bus.setCacheLastEvent(this, String.class, false);
      wait_a_bit();
      assertTrue("Received Cached Event Automatically:",subEvent.equals("CacheMe3"));

      bus.publishEvent("NormalEvent");
      wait_a_bit();
      assertTrue("Received normal Event as usual:",subEvent.equals("NormalEvent"));

   }

   @Test
   public void testSubscriptionOfDifferentEventAfterCaching() throws Exception {
      bus.addSubscriber(this); // Subscribe for  String & Integer events here

      //Caching ON for String Event
      bus.setCacheLastEvent(this, String.class, true);
      bus.publishEvent("CacheMe4");
      wait_a_bit();
      assertTrue("Caching On didn't receive any Event:",null == subEvent);

      //While Caching ON only for Strin Event receive a Integer event as usual
      bus.publishEvent(new Integer(45));
      wait_a_bit();
      assertTrue("Caching ON but can receive non Cached Event Type",45 == intEvent.intValue());
   }

   @Test
   public void testRemoveCachedEvent() throws Exception {
      bus.addSubscriber(this); // Subscribe for  String & Integer events here

      //Caching ON for String Event
      bus.setCacheLastEvent(this, String.class, true);
      bus.publishEvent("CacheMe4");
      wait_a_bit();
      assertTrue("Caching On didn't receive any Event:",null == subEvent);

      //Manually receive event
      String polledEvent  = bus.pollCashedEvent(String.class);
      assertTrue("Pulled Cached Event:",polledEvent.equals("CacheMe4"));

      //Remove Cached for this Type
      String removedEvent = bus.removeCachedEvent(String.class);
      assertTrue("Removed this Cached Event:",removedEvent.equals("CacheMe4"));

      //Try to pull again
       polledEvent  = bus.pollCashedEvent(String.class);
       assertTrue("No more Cached Event exists:",null == polledEvent);
   }

   @Test
   public void testRemoveAllCachedEvent() throws Exception {
      bus.addSubscriber(this); // Subscribe for  String & Integer events here

      //Caching ON for String Event
      bus.setCacheLastEvent(this, String.class, true);
      bus.publishEvent("CacheMe5");
      wait_a_bit();
      String polledString  = bus.pollCashedEvent(String.class);
      assertTrue("Pulled Cached Event:",polledString.equals("CacheMe5"));

      //Caching ON for Integer Event
      bus.setCacheLastEvent(this, Integer.class, true);
      bus.publishEvent(new Integer("777"));
      wait_a_bit();
      Integer polledInt  = bus.pollCashedEvent(Integer.class);
      assertTrue("Pulled Cached Event:", 777 == polledInt.intValue());

      bus.removeAllCacheEvent(); // All Cache gone
      polledString  = bus.pollCashedEvent(String.class);
      assertTrue("No more Cached Event exists:",null == polledString);
      polledInt  = bus.pollCashedEvent(Integer.class);
      assertTrue("No more Cached Event exists:",null == polledInt);


   }

   @Subscribe
   public void onEvent(String  event) {
      subEvent = event;
      eCount++;
   }
   @Subscribe
   public void onEvent(Integer  event) {
      intEvent = event;
      eCount++;
   }

   void wait_a_bit() throws InterruptedException{
      Thread.sleep(catchupTime);
   }

}
