package com.EventBus;

/**
 * Tests  for Event filtering functions
 * Created by Mustaq on 2/23/2017.
 */

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

import static org.junit.Assert.*;
public class AsyncBusFilterEventTest {

   AsyncEventBus bus;
   SubscriberHandler sH;

   String pubEvent;
   String subEvent;
   Integer intEvent;
   Double doubleEvent;
   int eCount;
   int catchupTime = 15; // wait_a_bit: msec

   @Before
   public void setup(){
      bus = new AsyncEventBus(2,true);
      sH = bus.SubscriberHandler();
   }
   @After
   public void reset() {
      bus.shutdownTheBus();
      bus = null;
      sH = null;
   }
   @Test
   public void testAddUpdateRemoveFilter () throws Exception{
      bus.addSubscriber(this);

      assertTrue("Nothing set yet, No Filter should exist:", sH.findFilter(this, String.class) == null);

      Predicate<String>  stringFilter = s -> s.equals("Hello");
      bus.setEventFilter(this, String.class, stringFilter);
      assertTrue("A String Event Filter should exist:",stringFilter == sH.findFilter(this, String.class));

      Predicate<String>  newFilter = s ->  s.equals("Hello"); // Logically same filter , but different ref. newFilter != stringFilter
      assertTrue("This is a different but  look-alike Event Filter, but shouldn't  Exist:",newFilter != sH.findFilter(this, String.class));

      bus.setEventFilter(this, String.class, newFilter); //Now Replace stringFilter  with newFilter
      assertTrue("The old  Event Filter now replaced with new :",newFilter == sH.findFilter(this, String.class));

      Predicate<Integer>  newIntFilter = i -> i.intValue() > 10;
      bus.setEventFilter(this, Integer.class, newIntFilter); //Replace Filter with new matching types  (Integer vs Integer)
      assertTrue("The old  Event Filter Now replaced with new :",newIntFilter == sH.findFilter(this, Integer.class));
   }

   @Test (expected = EventBusException.class)
   public void testSettingFilterForNonExistentEventType () {
      Predicate<Double>  newDoubleFilter = i ->  i.doubleValue() > 100.21;  //Event Double not Subscribed for this Subscriber
      bus.setEventFilter(this, Double.class, newDoubleFilter); //Replace Filter with new non existent event Type
     // assertFalse("Try to set a Filter for non Existent  Event Type:",newDoubleFilter == sH.findFilter(this, Integer.class));
   }

   //This is the main issue with Generic Predicate Filter as its  Type is erased at the bus level,
   // therefore, any Filter Type can be wrongly configured  with a mismatching Event Type. Essentially a client's error.
   // However,  this error will show up during runtime when the wrong filter Type is applied to right Event during publication.
   // As a result a bus exception will be thrown. To avoid this client could  setup a  pre-check  method to sync the types before setting filter.
   @Test (expected = EventBusException.class)
   public void test_Event_vs_Filter_TypeSafetyError() throws Exception{
      bus.addSubscriber(this);
      Predicate<String>  stringFilter = s -> s.equals("Hello");
      bus.setEventFilter(this, String.class, stringFilter);

      Predicate<Integer>  newIntFilter = i -> i.intValue() > 10;
      bus.setEventFilter(this, String.class, newIntFilter); //****Wrong / Mismatch type Event=String / Filter=Integer )
      bus.publishEvent("World");
      wait_a_bit();
   }

   @Test
   public void testOnOffFilter () throws Exception {
      bus.addSubscriber(this);

      Predicate<Integer>  newIntFilter = t ->  t.intValue() > 55;  // True = publish, False = don't send event
      bus.setEventFilter(this, Integer.class, newIntFilter);
      bus.publishEvent(new Integer(60));
      wait_a_bit();
      assertTrue("Filter True/Success, Got wanted Event:",intEvent.equals(new Integer(60)));

      intEvent = null; // Clear Subscriber Stale Event
      bus.publishEvent(new Integer(40));
      wait_a_bit();
      assertTrue("Filtered out, Did Not Get Int Event:",intEvent == null); // event < 55

      intEvent = null; // Clear Stale ..
      bus.setEventFilter(this, Integer.class, null); // Switch off
      bus.publishEvent(new Integer(40));
      wait_a_bit();
      assertTrue("Filter Off, So Got the  Event:",intEvent.equals(new Integer(40)));


      Predicate<String>  stringFilter2 = s -> s.contains("World");
      bus.setEventFilter(this, String.class, stringFilter2); // Filter active again
      subEvent = null; // Clear Stale ..
      bus.publishEvent("Hello World");
      wait_a_bit();
      assertTrue("Filtered True/Success, Got Event:",subEvent.equals("Hello World"));
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

   public void wait_a_bit() throws InterruptedException{
         Thread.sleep(catchupTime);
   }

}

class Quote {
   String ticker;
   double price;
   LocalDateTime timeStamp = LocalDateTime.now();
   Quote(String ticker, double price) {
      this.ticker = ticker;
      this.price = price;
   }
}

class ExchangeQuote extends Quote {
   String exchange;
   ExchangeQuote(String ticker, double price, String exchange ) {
      super(ticker, price);
      this.exchange = exchange;
   }
}

class Trade {
   String ticker;
   double price;
   int side; //0=B, 1=S, 2=SS
   int qty;
   LocalDateTime tradeTimeStamp = LocalDateTime.now();
}