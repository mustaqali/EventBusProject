package com.EventBus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the Nested / Recursive / Reentrant publishing for  single thread buses. Reentrant publish occurs when a callback method (subscriber)  publishes another event within the callback method soon after it had received an event. (Such as an Execution event is published once an Order event received and executed by a trading system, which is a subscriber of Order Events). But it's very important that reentrant publishing doesn't loop, which could happen when reentrant publishing is doesn't have proper exit condition.
 *<p> In this tests, both types of reentrant is tested, a subscriber for a String event publishes both String event and Integer event within it's String callback method and the Integer callback also publishes a String event back, eventually they the chain stops due conditioned publication.
 */
public class SyncEventBusReentrantTest {

// SyncEventBus syncBbus;
// AsyncEventBus asyncBus;

   SyncEventBus bus;
   String subEvent;
   Integer intEvent;
   int eCount;

   @After
   public void reset() {
      bus = null;
   }


   @Test
   public void testSyncBusReentrantPublishing() throws Exception {

      bus = new SyncEventBus();
      bus.addSubscriber(this);

      bus.publishEvent("Hello");
      Thread.sleep(5);
      //Same types of events (String event "Hello" to callback triggers only String "Reentrant Call" event )
      assertEquals("Recent event is Nested/Reentrant Published Event by callback, not the one Published above:","Reentrant Call", subEvent);
      assertEquals("Count:", 2, eCount);

      bus.publishEvent("World");
      Thread.sleep(5);
      //Different types of events (String callback triggers integer event)
      assertEquals("Recent event is Nested/Reentrant Published Event by callback, not the one Published above", 555, intEvent.intValue());
      Thread.sleep(5);
      assertEquals("Recent event is Nested/Reentrant Published Event by callback, not the one Published above:", "No More!", subEvent);
      assertEquals("Count:", 5, eCount);

     // assertEquals("Count:", 4, eCount);
   }

   @Subscribe
   public void onEvent(String  event) {
      subEvent = event;
      eCount++;
      if (eCount < 2) {
         bus.publishEvent("Reentrant Call");
      }
      if (event.equals("World")) {
         bus.publishEvent(new Integer(555));

      }
   }

   @Subscribe
   public void onEvent(Integer  event) {
      intEvent = event;
      eCount++;
    bus.publishEvent("No More!");
   }
}
