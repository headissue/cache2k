package org.cache2k.ee.impl;

/*
 * #%L
 * cache2k ee
 * %%
 * Copyright (C) 2000 - 2016 headissue GmbH, Munich
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

import org.cache2k.Cache;
import org.cache2k.core.CacheManagerImpl;
import org.cache2k.core.InternalCache;
import org.cache2k.core.InternalCacheInfo;
import org.cache2k.jmx.CacheManagerMXBean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* @author Jens Wilke; created: 2014-10-09
*/
public class ManagerMXBeanImpl implements CacheManagerMXBean {

  CacheManagerImpl manager;

  public ManagerMXBeanImpl(CacheManagerImpl manager) {
    this.manager = manager;
  }

  @Override
  public String getHealthStatus() {
    List<InternalCacheInfo.Health> li = new ArrayList<InternalCacheInfo.Health>();
    int v = 0;
    for (Cache c : manager) {
      InternalCache ic = (InternalCache) c;
      li.addAll(ic.getInfo().getHealth());
    }
    return constructHealthString(li);
  }

  private String constructHealthString(final List<InternalCacheInfo.Health> _li) {
    List<InternalCacheInfo.Health> _sortedList = new ArrayList<InternalCacheInfo.Health>();
    for (InternalCacheInfo.Health hi : _li) {
      if (InternalCacheInfo.Health.FAILURE.equals(hi.getLevel())) {
        _sortedList.add(0, hi);
      } else {
        _sortedList.add(hi);
      }
    }
    if (_sortedList.isEmpty()) {
      return "OK";
    }
    boolean _comma = false;
    StringBuilder sb = new StringBuilder();
    for (InternalCacheInfo.Health hi : _sortedList) {
      if (_comma) {
        sb.append(", ");
      }
      sb.append(hi.getLevel()).append(": ");
      sb.append(hi.getMessage())
        .append(" (").append(hi.getCache().getCacheManager().getName()).append(':').append(hi.getCache().getName()).append(')');
      _comma = true;
    }
    return sb.toString();
  }

  @Override
  public void clear() {
    manager.clear();
  }

  @Override
  public String getVersion() { return manager.getVersion(); }

  @Override
  public String getBuildNumber() { return manager.getBuildNumber(); }

}
