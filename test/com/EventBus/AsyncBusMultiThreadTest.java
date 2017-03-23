package com.EventBus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Simple volume and Event ordering test for multithreaded bus with multiple subscribers
 * Created by Mustaq on 2/26/2017.
 */
public class AsyncBusMultiThreadTest   {
   AsyncEventBus bus;
   SubscriberHandler sH;
   int asyncThreadCount = 5;
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

   //Test Multi thread missing/dupe data and order of event pub/sub received
   //Create multiple threaded subscribers and publish 1000 random data  to be subscribed by each thread.
   //Compare each element of the  published data with each Subscriber's data for content and event order
   //This test thread count may vary based on machine tested
   @Test
   public  void  testMultiThreading() throws Exception {
      for (int i = 0; i < 4; i++) {
         IntegerSubscriber iSub = new IntegerSubscriber("iSub" + i, bus);
         iThreads.add(iSub);
         iSub.start();
         bus.addSubscriber(iSub);
      }

      Random rand = new Random();
      Integer data;
      for (int i = 0; i < 1000; i++) {
         data = new Integer(rand.nextInt());
         publishedDataList.add(data);
         bus.publishEvent(data);
         wait_a_bit(1);
      }

      wait_a_bit(100);
      assertTrue("All Subscribers got all the published messages, and in the order it was published:", comparePubSubData());
   }

   boolean comparePubSubData() {
      for (IntegerSubscriber thread : iThreads) {
         //Test No published message lost / All Subscribers received all published messages
         if (publishedDataList.size() != thread.subscribedDataList.size()) {
            System.out.println(publishedDataList.size());
            return false;
         }

         //Test Each Subscriber received messages  in the inorder published,  and the content is same
         for (int i = 0; i < publishedDataList.size(); i++) {
            if (publishedDataList.get(i).intValue() != thread.subscribedDataList.get(i).intValue()) {
               System.out.println(publishedDataList.get(i).intValue() + ":" + thread.subscribedDataList.get(i).intValue());
               return false;
            }
         }
      }
      return true; // All good
   }

   public static void  wait_a_bit(int time) {
      try {
         Thread.sleep(time);
      } catch (InterruptedException e){

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

