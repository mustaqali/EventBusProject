package com.EventBus;

/**
 * Created by Mustaq on 3/5/2017.
 */

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Test to make sure the thread pool load is distributed as expected using hash function
 * Created by Mustaq on 2/26/2017.
 */
public class AsyncBusThreadHashDistributionTest   {
   AsyncEventBus bus;
   SubscriberHandler sH;
   int asyncThreadCount = 9;
   List<IntegerSubscriber> iThreads = new ArrayList<>();
   List<Integer> publishedDataList = new ArrayList<>();

   @Before
   public void setup(){
      bus = new AsyncEventBus(asyncThreadCount);
      sH = bus.SubscriberHandler();
   }

   @After
   public void reset() {
      bus.shutdownTheBus();
      bus = null;
      sH = null;
   }


   //Test hash partitioned Thread load distribution that guarantees event ordering
   @Test
   public  void  testMultiThreadHashScheduledPartition() throws Exception {

      StringSubscriber stringSubscriber = new StringSubscriber("sSub", bus);
      stringSubscriber.start();
      bus.addSubscriber(stringSubscriber);

      IntegerSubscriber integerSubscriber = new IntegerSubscriber("iSub", bus);
      integerSubscriber.start();
      bus.addSubscriber(integerSubscriber);

      int stringThreadPartitionId = sH.findSubscription(stringSubscriber,String.class).getLocalHashCode() % asyncThreadCount;
      int integerThreadPartitionId = sH.findSubscription(integerSubscriber,Integer.class).getLocalHashCode() % asyncThreadCount;

      //In a hash based thread selection logic from the pool, a Subscriber's  particular EventType will always be bound to the same thread in the pool,
      // thus making sure event delivery  order is preserved, which is a paramount requirement in pub/sub framework.
      // The Subscription hash partitioning ensures a single threaded delivery for the same Subscriber/Event combo.
      // (The hashcode of the Subscription is a combo of Subscriber + EventType)
      // Also, different Event(s) of the same Subscriber (different hash) could go to  another Thread making sure that  the work loads are also distributed.
      //***** NOTE: This test is not guaranteed at times as there may my hash collision *****, but event ordering still will be preserved.
      // ****** Also note that a typical thread pool like CachedPool will not guarantee thread ordering due to same eventType could be delivered by any thread.

      assertTrue("Two Different Events(Subscriptions) are bound to two different Threads:", stringThreadPartitionId != integerThreadPartitionId);
   }

    class StringSubscriber extends Thread{
      String sname;
      String lastEvent;
      int eventCount;
      AsyncEventBus bus;

      public StringSubscriber(String name, AsyncEventBus bus) {
         sname = name;
         this.bus = bus;
      }

      @Override
      public void run() {
         while(true) {;}
      }

      @Subscribe
      public void onEvent(String event) {
         lastEvent = event;
         eventCount++;
         //  System.out.println("Thread: " + sname  + "/ Event: " + lastEvent + "/ Count: " + eventCount);
      }
   }

   class IntegerSubscriber extends Thread{
      String sname;
      Integer lastEvent;
      List<Integer> subscribedDataList = new ArrayList<>();

      int eventCount;
      AsyncEventBus bus;

      public IntegerSubscriber(String name, AsyncEventBus bus) {
         sname = name;
         this.bus = bus;
      }

      @Override
      public void run() {
         while(true) {;}
      }

      @Subscribe
      public void onEvent(Integer event) {
         lastEvent = event;
         subscribedDataList.add(lastEvent);
         eventCount++;
         // System.out.println("Thread: " + sname  + "/ Event: " + lastEvent.intValue() + "/ Count: " + eventCount);
      }

   }
}


