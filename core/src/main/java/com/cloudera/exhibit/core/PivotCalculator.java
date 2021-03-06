/*
 * Copyright (c) 2015, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.exhibit.core;

import com.cloudera.exhibit.core.simple.SimpleObs;
import com.cloudera.exhibit.core.simple.SimpleObsDescriptor;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PivotCalculator implements Calculator {

  public static class Key implements Serializable {
    String name;
    Set<String> levels;

    public Key(String name, Set<String> levels) {
      this.name = name;
      this.levels = levels;
    }
  }

  private Calculator fc;
  private List<String> ids;
  private Map<String, Set<String>> keys;
  private transient ObsDescriptor descriptor;

  public PivotCalculator(Calculator base, List<String> idColumns, List<Key> keys) {
    this.fc = base;
    this.ids = idColumns;
    this.keys = Maps.newLinkedHashMap();
    for (Key key : keys) {
      this.keys.put(key.name, key.levels);
    }
  }

  @Override
  public ObsDescriptor initialize(ExhibitDescriptor descriptor) {
    ObsDescriptor fd = fc.initialize(descriptor);
    List<ObsDescriptor.Field> idFields = Lists.newArrayList();
    List<ObsDescriptor.Field> valueFields = Lists.newArrayList();
    List<Set<String>> levelSet = Lists.newArrayList(keys.values());
    for (ObsDescriptor.Field f : fd) {
      if (ids.contains(f.name)) {
        idFields.add(new ObsDescriptor.Field(f.name, f.type));
      } else if (!ids.contains(f.name) && !keys.containsKey(f.name)) { // value fields
        for (List<String> suffix : Sets.cartesianProduct(levelSet)) {
          StringBuilder sb = new StringBuilder(f.name);
          sb.append('_');
          sb.append(Joiner.on('_').join(suffix));
          valueFields.add(new ObsDescriptor.Field(sb.toString(), f.type));
        }
      }
    }
    idFields.addAll(valueFields);
    return new SimpleObsDescriptor(idFields);
  }

  @Override
  public void cleanup() {
    fc.cleanup();
  }

  @Override
  public Iterable<Obs> apply(Exhibit exhibit) {
    Iterable<Obs> frame = fc.apply(exhibit);
    if (descriptor == null) {
      descriptor = initialize(exhibit == null ? null : exhibit.descriptor());
    }
    Map<List<Object>, List<Object>> valuesById = Maps.newHashMap();
    for (Obs obs : frame) {
      List<Object> idv = Lists.newArrayListWithExpectedSize(ids.size());
      for (String id : ids) {
        idv.add(obs.get(id));
      }
      List<Object> values = valuesById.get(idv);
      if (values == null) {
        values = Arrays.asList(new Object[descriptor.size() - ids.size()]);
        valuesById.put(idv, values);
      }
      List<String> keyValues = Lists.newArrayListWithExpectedSize(keys.size());
      for (String key : keys.keySet()) {
        Object v = obs.get(key);
        keyValues.add(v == null ? "null" : v.toString());
      }
      String lookupKey = Joiner.on('_').join(keyValues);
      for (ObsDescriptor.Field f : obs.descriptor()) {
        if (!ids.contains(f.name) && !keys.containsKey(f.name)) {
          String retField = new StringBuilder(f.name).append('_').append(lookupKey).toString();
          int index = descriptor.indexOf(retField);
          if (index >= ids.size()) {
            values.set(index - ids.size(), obs.get(f.name));
          }
        }
      }
    }

    return Iterables.transform(valuesById.entrySet(), new Function<Map.Entry<List<Object>, List<Object>>, Obs>() {
      @Override
      public Obs apply(Map.Entry<List<Object>, List<Object>> e) {
        List<Object> key = e.getKey();
        key.addAll(e.getValue());
        return new SimpleObs(descriptor, key);
      }
    });
  }
}
