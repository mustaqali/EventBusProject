package com.EventBus;

/**
 * Test the Nested / Recursive / Reentrant publishing for an AsyncEventBus. Reentrant publish occurs when a callback method (subscriber)  publishes another event within the callback method soon after it had received an event. (Such as an Execution event is published once an Order event received and executed by a trading system, which is a subscriber of Order Events). But it's very important that reentrant publishing doesn't loop, which could happen when reentrant publishing is doesn't have proper exit condition.
 *<p> In this tests, both types of reentrant are tested, a subscriber for a String event publishes both String event and Integer event within it's String callback method and the Integer callback also publishes a String event back, eventually they the chain stops due conditioned publication.
 */

import org.junit.After;
import org.junit.Test;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class AsyncBusCallbackReentrantTest {

   AsyncEventBus aBus;
   String stringEvent;
   Integer integerEvent;
   int totalEventCount;


   @Test
   public void testAsyncBusReentrantPublishing() throws Exception {
      aBus =  new AsyncEventBus();
      aBus.addSubscriber(this);

      aBus.publishEvent("Hello");
      Thread.sleep(10);

      //Different types of events (String event "World" callback triggers Integer 555 event which in turn triggers another String event "No More!" within Integer callback  and the chain stops. But all total events are counted)
      assertEquals("Recent event is Nested/Reentrant Published Event by callback, not the one Published above:",
              555, integerEvent.intValue());

      Thread.sleep(10);

//      assertEquals("Recent event is Nested/Reentrant Published Event by callback, not the one Published above:",
//              "No More!", stringEvent);
      assertEquals("Count includes reentrant as well:", 3, totalEventCount);
   }

   public static void  wait_a_bit(int time) {
      try {
         Thread.sleep(time);
      } catch (InterruptedException e){

      }
   }
//   public void reset() {
//      aBus.shutdownTheBus();
//      aBus = null;
//   }
   @Subscribe
   public void onEventString(String  event) {
      stringEvent = event;
      totalEventCount++;
      if (event.equals("Hello")) {
         aBus.publishEvent(new Integer(555));
      }
   }

   @Subscribe
   public void onEventInteger(Integer  event) {
      integerEvent = event;
      totalEventCount++;
      aBus.publishEvent("No More!");
   }
}