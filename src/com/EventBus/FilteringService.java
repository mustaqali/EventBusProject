package com.EventBus;

import java.util.function.Predicate;

/**
 *
 * FilteringService is an interface for various Filtering or Enhanced event services that can  be provided by the EventBus.
 * In addition to providing core Event services, an EventBus can provide these services to facilitate advanced event
 * handling during delivery. This interface supports Event Filtering and Event Caching services.
 *
 * @author Mustaq Ali
 */
public interface FilteringService {
   /**
    * {@literal Set the filtering on/off: (on:  Predicate<?> != null & off: Predicate<?>  == null)}
    * eg. {@literal Predicate<Trade>  bigListedTrade = t -> t.qty > 10000 && t.exchange.equals("NYSE")}
    *    {@literal setEventFilter (listedBookSubscriber, Trade.class, bigListedTrade) }
    */
   public void setEventFilter(Object subscriber, Class<?> eventType, Predicate<?> filter);


   /**
    *  Methods for dealing with Caching Latest event
    */
   public void setCacheLastEvent(Object subscriber, Class<?> eventType, boolean setCaching);
   public <T> T  pollCashedEvent(Class<T> eventType);
   public <T> T removeCachedEvent(Class<T> eventType);
   public void removeAllCacheEvent();
}
