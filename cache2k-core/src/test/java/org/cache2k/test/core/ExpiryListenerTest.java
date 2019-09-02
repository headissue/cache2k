package org.cache2k.test.core;

/*
 * #%L
 * cache2k implementation
 * %%
 * Copyright (C) 2000 - 2019 headissue GmbH, Munich
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.cache2k.Cache2kBuilder;
import org.cache2k.expiry.ExpiryPolicy;
import org.cache2k.integration.AdvancedCacheLoader;
import org.cache2k.test.util.TestingBase;
import org.cache2k.Cache;
import org.cache2k.CacheEntry;
import org.cache2k.test.core.TestingParameters;
import org.cache2k.test.util.Condition;
import org.cache2k.event.CacheEntryExpiredListener;
import org.cache2k.core.HeapCache;
import org.cache2k.core.util.TunableFactory;
import org.cache2k.integration.CacheLoader;
import org.cache2k.testing.category.SlowTests;
import static org.hamcrest.Matchers.*;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * More thorough expiry events.
 *
 * @author Jens Wilke
 */
@Category(SlowTests.class)
public class ExpiryListenerTest extends TestingBase {

  @Test
  public void simpleAsyncExpiredListenerCalled() {
    testListenerCalled(false, false);
  }

  @Test
  public void asyncExpiredListenerCalled() {
    testListenerCalled(false, true);
  }

  @Test
  public void expireBeforePut() {
    final AtomicInteger _callCount = new AtomicInteger();
    HeapCache.Tunable t = TunableFactory.get(HeapCache.Tunable.class);
    final long EXPIRY_MILLIS = 100;
    assertThat(t.sharpExpirySafetyGapMillis, greaterThan(EXPIRY_MILLIS));
    final Cache<Integer, Integer> c = builder(Integer.class, Integer.class)
      .addListener(new CacheEntryExpiredListener<Integer, Integer>() {
        @Override
        public void onEntryExpired(final Cache<Integer, Integer> c, final CacheEntry<Integer, Integer> e) {
          _callCount.incrementAndGet();
        }
      })
      .expiryPolicy(new ExpiryPolicy<Integer, Integer>() {
        @Override
        public long calculateExpiryTime(final Integer key, final Integer value, final long loadTime, final CacheEntry<Integer, Integer> oldEntry) {
          return loadTime + EXPIRY_MILLIS;
        }
      })
      .sharpExpiry(true)
      .build();
    final int ANY_KEY = 1;
    c.put(ANY_KEY, 4711);
    for (int i = 1; i <= 10; i++) {
      sleep(EXPIRY_MILLIS);
      assertFalse("expired based on wall clock", c.containsKey(ANY_KEY));
      c.put(ANY_KEY, 1802);
      assertThat("expiry event before put", _callCount.get(), greaterThanOrEqualTo(i));
    }
  }

  @Test
  public void expireAfterRefreshProbationEnded() {
    final AtomicInteger loaderCount = new AtomicInteger();
    final AtomicInteger eventCount = new AtomicInteger();
    final long EXPIRY_MILLIS = TestingParameters.MINIMAL_TICK_MILLIS;
    Cache2kBuilder<Integer, Integer> builder =
      builder(Integer.class, Integer.class)
        .loader(new AdvancedCacheLoader<Integer, Integer>() {
          @Override
          public Integer load(final Integer key, final long currentTime, final CacheEntry<Integer, Integer> currentEntry) {
            loaderCount.getAndIncrement();
            assertEquals(0, eventCount.get());
            return key;
          }
        })
        .expireAfterWrite(EXPIRY_MILLIS, TimeUnit.MILLISECONDS)
        .refreshAhead(true)
        .addListener(new CacheEntryExpiredListener<Integer, Integer>() {
          @Override
          public void onEntryExpired(final Cache<Integer, Integer> c, final CacheEntry<Integer, Integer> e) {
            eventCount.incrementAndGet();
          }
        });

    final Cache<Integer, Integer> c = builder.build();
    final int ANY_KEY = 1;
    c.get(ANY_KEY);
    await(new Condition() {
      @Override
      public boolean check() throws Exception {
        return eventCount.get() == 1;
      }
    });
    assertEquals(2, loaderCount.get());
  }

  @Test
  public void expireBeforeLoadSharp() {
    expireBeforeLoad(true);
  }

  @Test
  public void expireBeforeLoad() {
    expireBeforeLoad(false);
  }

  private void expireBeforeLoad(boolean sharpExpiry) {
    final AtomicInteger callCount = new AtomicInteger();
    final long EXPIRY_MILLIS = 432;
    Cache2kBuilder<Integer, Integer> builder =
      builder(Integer.class, Integer.class)
      .loader(new AdvancedCacheLoader<Integer, Integer>() {
        @Override
        public Integer load(final Integer key, final long currentTime, final CacheEntry<Integer, Integer> currentEntry) {
          if (currentEntry != null) {
            assertEquals(1, callCount.get());
            return 0;
          }
          return 1;
        }
      })
      .keepDataAfterExpired(true)
      .addListener(new CacheEntryExpiredListener<Integer, Integer>() {
        @Override
        public void onEntryExpired(final Cache<Integer, Integer> c, final CacheEntry<Integer, Integer> e) {
          callCount.incrementAndGet();
        }
      });
    if (sharpExpiry) {
      builder.expiryPolicy(new ExpiryPolicy<Integer, Integer>() {
        @Override
        public long calculateExpiryTime(final Integer key, final Integer value, final long loadTime, final CacheEntry<Integer, Integer> oldEntry) {
          return loadTime + EXPIRY_MILLIS;
          }
        })
        .sharpExpiry(true);
    } else {
      builder.expireAfterWrite(EXPIRY_MILLIS, TimeUnit.MILLISECONDS);
    }
    final Cache<Integer, Integer> c = builder.build();
    final int ANY_KEY = 1;
    await(new Condition() {
      @Override
      public boolean check() throws Exception {
        return c.get(ANY_KEY) == 0;
      }
    });
  }

  @Test
  public void simpleAsyncExpiredListenerCalledSharpExpiry() {
    testListenerCalled(true, false);
  }

  @Test
  public void asyncExpiredListenerCalledSharpExpiry() {
    testListenerCalled(true, true);
  }

  private void testListenerCalled(boolean sharp, boolean beyondSafetyGap) {
    final AtomicInteger _callCount = new AtomicInteger();
    HeapCache.Tunable t = TunableFactory.get(HeapCache.Tunable.class);
    final long _EXPIRY_MILLIS =
      ( beyondSafetyGap ? t.sharpExpirySafetyGapMillis : 0 )
        + TestingParameters.MINIMAL_TICK_MILLIS ;
    final Cache<Integer, Integer> c = builder(Integer.class, Integer.class)
      .addListener(new CacheEntryExpiredListener<Integer, Integer>() {
        @Override
        public void onEntryExpired(final Cache<Integer, Integer> c, final CacheEntry<Integer, Integer> e) {
          _callCount.incrementAndGet();
        }
      })
      .expireAfterWrite(_EXPIRY_MILLIS, TimeUnit.MILLISECONDS)
      .sharpExpiry(sharp)
      .build();
    final int ANY_KEY = 1;
    within(_EXPIRY_MILLIS)
      .work(new Runnable() {
        @Override
        public void run() {
          c.put(ANY_KEY, 4711);
        }
      })
      .check(new Runnable() {
        @Override
        public void run() {
          assertEquals(0, _callCount.get());
          assertTrue(c.containsKey(ANY_KEY));
        }
      });
    await(new Condition() {
      @Override
      public boolean check() throws Exception {
        return _callCount.get() == 1;
      }
    });
  }

  @Test
  public void asyncExpiredListenerAfterRefreshCalled() {
    final AtomicInteger _callCount = new AtomicInteger();
    final AtomicInteger _loaderCallCount = new AtomicInteger();
    HeapCache.Tunable t = TunableFactory.get(HeapCache.Tunable.class);
    final long _EXTRA_GAP = TestingParameters.MINIMAL_TICK_MILLIS;
    final long _EXPIRY_MILLIS = t.sharpExpirySafetyGapMillis + _EXTRA_GAP ;
    final Cache<Integer, Integer> c = builder(Integer.class, Integer.class)
      .addListener(new CacheEntryExpiredListener<Integer, Integer>() {
        @Override
        public void onEntryExpired(final Cache<Integer, Integer> c, final CacheEntry<Integer, Integer> e) {
          _callCount.incrementAndGet();
        }
      })
      .expireAfterWrite(_EXPIRY_MILLIS, TimeUnit.MILLISECONDS)
      .refreshAhead(true)
      .loader(new CacheLoader<Integer, Integer>() {
        @Override
        public Integer load(final Integer key) throws Exception {
          _loaderCallCount.incrementAndGet();
          return 123;
        }
      })
      .build();
    final int ANY_KEY = 1;
    within(_EXPIRY_MILLIS * 2)
      .work(new Runnable() {
        @Override
        public void run() {
          c.get(ANY_KEY);
        }
      })
      .check(new Runnable() {
        @Override
        public void run() {
          assertEquals(0, _callCount.get());
          assertTrue(c.containsKey(ANY_KEY));
        }
      });
    await(new Condition() {
      @Override
      public boolean check() throws Exception {
        return _callCount.get() == 1;
      }
    });
    assertEquals(2, _loaderCallCount.get());
  }

}
