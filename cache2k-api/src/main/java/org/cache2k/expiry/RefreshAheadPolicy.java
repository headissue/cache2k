package org.cache2k.expiry;

/*-
 * #%L
 * cache2k API
 * %%
 * Copyright (C) 2000 - 2022 headissue GmbH, Munich
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

import org.cache2k.DataAware;

/**
 * @author Jens Wilke
 */
public interface RefreshAheadPolicy<K, V, T> extends DataAware<K, V> {

  @SuppressWarnings("Convert2Diamond")
  RefreshAheadPolicy<?, ?, ?> LEGACY_DEFAULT = new RefreshAheadPolicy<Object, Object, Object>() {
    @Override
    public long refreshAheadTime(Context<Object> ctx) {
      return ctx.getExpiryTime();
    }

    @Override
    public int requiredHits(Context<Object> ctx) {
      return ctx.isRefreshAhead() ? 1 : 0;
    }

  };

  @SuppressWarnings("Convert2Diamond")
  RefreshAheadPolicy<?, ?, ?> DEFAULT = new RefreshAheadPolicy<Object, Object, Object>() {
    @Override
    public long refreshAheadTime(Context<Object> ctx) {
      return ctx.getExpiryTime();
    }

    @Override
    public int requiredHits(Context<Object> ctx) {
      return 0;
    }

  };

  /**
   * Called after a load or refresh and after the expiry or resilience policy was run
   * to determine the refresh ahead time. For a refresh to happen, the result needs to be lower or
   * equal to expiry time.
   *
   * <p>
c   */
  long refreshAheadTime(Context<T> ctx);

  /**
   * Number of required j
   * @param ctx
   * @return
   */
  int requiredHits(Context<T> ctx);

  interface Context<T> {

    boolean isLoadException();
    boolean isExceptionSuppressed();

    /**
     * Start time of operation.
     */
    long getStartTime();

    /**
     * Stop time of operation.
     */
    long getStopTime();

    /**
     * The current time, identical to {@link #getStopTime()}
     */
    long getCurrentTime();

    /**
     * The expiry time as calculated by the expiry policy or resilience policy.
     * Always a positive value.
     */
    long getExpiryTime();

    /**
     * {@code True}, if the value was accessed since the last refresh or initial load.
     * The operation that triggered the initial load does not count as access.
     *
     */
    boolean isAccessed();

    /**
     * {@code true}, if load request, includes refresh
     */
    boolean isLoad();

    /**
     * {@code True}, if the load was not triggered by a user request
     */
    boolean isRefreshAhead();

    /**
     * Returns data set by {@link #setUserData(Object)} in an earlier call to the policy
     */
    T getUserData();

    /**
     * Sets arbitrary data used by the policy.
     */
    void setUserData(T data);

  }

}
