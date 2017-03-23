package com.EventBus;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.Assert.*;

/** A volume test for Single threaded EventBus
 * Created by Mustaq on 2/23/2017.
 */
public class SuperBulkTest {
   AsyncEventBus bus;
   SubscriberHandler sH;

   @Before
   public void setup(){
      bus = new AsyncEventBus(2);
      sH = bus.SubscriberHandler();
   }

   @After
   public void reset(){
      bus =null;
      sH = null;
   }

   @Test
   public void testMultiplePubSub() throws Exception {
      //--Multiple Subscriber / Event Subscription Tests-----
      StringSubscriber  sSub1 = new StringSubscriber();
      StringSubscriber  sSub2 = new StringSubscriber();
      IntegerSubscriber iSub = new IntegerSubscriber();
      ObjectSubscriber  oSub = new ObjectSubscriber();
      SuperSubscriber  superSub = new SuperSubscriber(); // Five Callbacks in One Subscriber

      bus.addSubscriber(sSub1);
      bus.addSubscriber(sSub2);
      bus.addSubscriber(iSub);
      bus.addSubscriber(oSub);
      bus.addSubscriber(superSub); // 5 Callbacks / Subscriptions for the same Subscriber

      assertEquals("TotalSubscribers:",5,sH.getSubscriberCount());

      //Obj, String, Integer, Quote & ExchangeQuote classes
      assertEquals("Total Distinct Event Subscriptions for Super Subscriber:",5,sH.getSubscriberEventCount(superSub));


      //--Multiple Publications to Multiple Subscribers & a Subscriber with Multiple Event Subscriptions Tests-----
      bus.publishEvent("World");
      bus.publishEvent(new Integer(10));
      bus.publishEvent(new Object());

      //Test Two different Events, one for Base and another Sub class
      //Publish some quotes & Specific Exchange Quotes
      bus.publishEvent(new Quote("MSFT", 42.40));
      bus.publishEvent(new ExchangeQuote("IBM UN", 90.40, "NYSE"));

      Thread.sleep(1000);
      assertEquals("Last Update String Event for sSub1:","World", sSub1.sEvent);
      assertEquals( "Total Event Count for sSub1:", 1, sSub1.sCount);

      assertEquals("Last Update String Event for sSub2:","World", sSub2.sEvent);
      assertEquals( "Total Event Count for sSub2:", 1, sSub2.sCount);

      assertEquals("Last Integer Event for iSub:",  new Integer(10), iSub.iEvent);
      assertEquals( "Total Event Count for iSub:", 1, iSub.iCount);

      assertEquals( "Total Event Count for oSub:", 1, oSub.oCount);

      assertEquals( "Total String Event Count for superSub:", 1, superSub.sCount);
      assertEquals( "Total Integer Event Count for superSub:", 1, superSub.iCount);
      assertEquals( "Total Object Event Count for superSub:", 1, superSub.oCount);
      assertEquals( "Total Quote Event Count for superSub:", 1, superSub.qCount);
      assertEquals( "Total Exchange Quote Event Count for superSub:", 1, superSub.eqCount);

      assertTrue("Last/Updated  Ticker  Received for Quote event", superSub.qEvent.ticker.equals("MSFT"));
      assertTrue("Last/Updated  Ticker  Received for Exchange Quote event", superSub.eqEvent.ticker.equals("IBM UN"));

      assertEquals( "Grand Total All Events Count for superSub:", 5, superSub.totalAllEvents);
   }
   @Test
   public void testLargeVolumeMultipleTest() throws Exception{
      SuperSubscriber  superSub = new SuperSubscriber(); // Five Callbacks in One Subscriber
      //Do Clean Bulk test
    //  bus.removeSubscriber(superSub);
      assertEquals("Total Event for Super Subscriber:",0,sH.getSubscriberEventCount(superSub));

      bus.addSubscriber(superSub);
      superSub.clearAllCounters();

      int j = 0;
      for (int i = 0;  i < 100; i++) {
         bus.publishEvent("Hello");
         bus.publishEvent("World");
         bus.publishEvent(new Object());
         bus.publishEvent(new Integer(j++));  //This event stream is recorded in Array w/ index = value
      }
      Thread.sleep(1000); // Depends on machine load
      //2 different  String Event Publication  * 100
      assertEquals( "Total String  Events Count for superSub:", 200, superSub.sCount);

      assertEquals( "Total Integer  Events Count for superSub:", 100, superSub.iCount);
      assertEquals( "Total Object  Events Count for superSub:", 100, superSub.iCount);

      assertEquals( "Total All Events Count for superSub:", 400, superSub.totalAllEvents);

      assertEquals( "Simple Event Order Check Via Array: Index/Value:", new Integer(76), superSub.iEventList.get(76));
   }

   class StringSubscriber {
      String sEvent;
      int sCount;

      @Subscribe
      public void onEvent(String  event) {
         sEvent = event;
         sCount++;
      }
   }

   class IntegerSubscriber {
      Integer iEvent;
      int iCount;

      @Subscribe
      public void onEvent(Integer  event) {
         iEvent = event;
         iCount++;
      }
   }

   class ObjectSubscriber {
      Object oEvent;
      int oCount;

      @Subscribe
      public void onEvent(Object event) {
         oEvent = event;
         oCount++;
      }
   }

   class QuoteSubscriber {
      Quote qEvent;
      int qCount;

      @Subscribe
      public void onEvent(Quote event) {
         qEvent = event;
         qCount++;
      }
   }

   class ExchangeQuoteSubscriber {
      ExchangeQuote eqEvent;
      int eqCount;

      @Subscribe
      public void onEvent(ExchangeQuote event) {
         eqEvent = event;
         eqCount++;
      }
   }

   class  SuperSubscriber {
      String sEvent;
      Integer iEvent;
      Object oEvent;
      Quote qEvent;
      ExchangeQuote eqEvent;

      int sCount;
      int iCount;
      int oCount;
      int qCount;
      int eqCount;
      int totalAllEvents;

      List<Integer> iEventList = new ArrayList<>();

      @Subscribe
      public void onEvent(String  event) {
         sEvent = event;
         sCount++;
         totalAllEvents++;
      }

      @Subscribe
      public void onEvent(Integer  event) {
         iEvent = event;
         iCount++;
         totalAllEvents++;
         iEventList.add(event);
      }

      @Subscribe
      public void onEvent(Object event) {
         oEvent = event;
         oCount++;
         totalAllEvents++;
      }

      @Subscribe
      public void onEvent(Quote event) {
         qEvent = event;
         qCount++;
         totalAllEvents++;
      }

      @Subscribe
      public void onEvent(ExchangeQuote event) {
         eqEvent = event;
         eqCount++;
         totalAllEvents++;
      }

      public void clearAllCounters() {
         sCount = 0;
         iCount = 0;
         oCount = 0;
         totalAllEvents = 0;
         iEventList.clear();
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

}
