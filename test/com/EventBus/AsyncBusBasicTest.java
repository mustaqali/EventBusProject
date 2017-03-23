package com.EventBus;

/**
 * Created by Mustaq on 2/23/2017.
 Test cases  for  basic testing of functions and interface calls  to an Async / multithreaded bus
 */

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**

 */
public class AsyncBusBasicTest {

   AsyncEventBus bus;


   String subEvent;
   Integer intEvent;
   Double doubleEvent;
   int eCount;
   int asyncWaitTime = 100;

   @Before
   public void setup(){
      bus = new AsyncEventBus(true);
   }

   @After
   public void reset(){
      bus.shutdownTheBus();
      bus = null;
   }

   @Test
   public void testBasicPublishSubscribe() throws Exception {
      bus.addSubscriber(this);
      bus.publishEvent("Hello");

      Thread.sleep(asyncWaitTime);
      assertEquals("Event:","Hello", subEvent);
      assertEquals("Count:", 1, eCount);
   }

   @Test
   public void testValidSubscription() throws Exception {
      //Check Subscriber without Registering
      assertFalse(bus.SubscriberHandler().getSubscriberEvents().containsKey(this));

      //Then Register,   Test Registration
      bus.addSubscriber(this);
      //Method 1: Find Subscriber Using function
      assertTrue(bus.SubscriberHandler().subscriberExists(this, String.class));

      //Method2: Examine Subscription database (DB is Map<subscriber, Set<Event Keys>>
      //Test Registered Subscriber
      assertTrue(bus.SubscriberHandler().getSubscriberEvents().containsKey(this));
      //Test Matching Event Class
      assertTrue("We Should Have String Subscription", bus.SubscriberHandler().getSubscriberEvents().get(this).contains(String.class));
      assertTrue("We Should Also Have Integer Subscription", bus.SubscriberHandler().getSubscriberEvents().get(this).contains(Integer.class));
      assertFalse("We Should NOT Have Double Subscription", bus.SubscriberHandler().getSubscriberEvents().get(this).contains(Double.class));
   }

   @Test
   public void testPublishWithoutAnySubscriber() throws Exception {
      bus.publishEvent("Hello");

      Thread.sleep(asyncWaitTime);
      assertEquals("Event:",null, subEvent);
      assertEquals("Count:", 0, eCount);
   }

   @Test(expected = EventBusException.class)
   public void testAddingSubscriberTwice() throws Exception {
      bus.addSubscriber(this);
      bus.addSubscriber(this);
   }

   @Test(expected = EventBusException.class)
   public void testRemoveSubscriberWithoutAdding() throws Exception {
      bus.removeSubscriber(this);
   }

   @Test
   public void testPublishRemoveRePublish() throws Exception {
      bus.addSubscriber(this);
      bus.publishEvent("Hello");
      Thread.sleep(asyncWaitTime);
      assertEquals("Event Should be:","Hello", subEvent);
      assertEquals("Count Should be:", 1, eCount);

      bus.removeSubscriber(this);

      bus.publishEvent("World");

      assertEquals("Count is same as before:", 1, eCount);
      assertFalse("New Event Not Subscribed:",subEvent.equals("World"));
      assertTrue("Event (is stale)  same as before:",subEvent.equals("Hello"));
   }

   @Test
   public void testUnSubscribedEventType() throws Exception {
      bus.addSubscriber(this);
      bus.publishEvent("Hello");
      bus.publishEvent(new Integer(10));
      bus.publishEvent(new Double(1939.50));

      Thread.sleep(asyncWaitTime);
      assertTrue("Got the String Event:",subEvent.equals("Hello"));
      assertTrue("Got  Int Event:",intEvent.equals(new Integer(10)));
      assertTrue("Nope, shouldn't get Double Event/Invalid method:",doubleEvent==null);
   }

   //Sample Callbacks 2 Valid, One Invalid
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

}

