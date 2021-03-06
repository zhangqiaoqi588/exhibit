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
package com.cloudera.exhibit.etl.config;

import com.google.common.collect.Lists;
import org.apache.avro.Schema;
import org.apache.crunch.Target;
import org.apache.crunch.types.PType;
import org.apache.crunch.types.avro.Avros;

import java.util.List;

public class BuildConfig {
  public static enum KeyType {
    STRING {
      @Override
      public PType<?> getPType() {
        return Avros.strings();
      }

      @Override
      public Schema getSchema() {
        return Schema.create(Schema.Type.STRING);
      }

      @Override
      public Object parse(String stringKey) {
        return stringKey;
      }
    },

    INT {
      @Override
      public PType<?> getPType() {
        return Avros.ints();
      }

      @Override
      public Schema getSchema() {
        return Schema.create(Schema.Type.INT);
      }

      @Override
      public Object parse(String stringKey) {
        return Integer.valueOf(stringKey);
      }
    },

    LONG {
      @Override
      public PType<?> getPType() {
        return Avros.longs();
      }

      @Override
      public Schema getSchema() {
        return Schema.create(Schema.Type.LONG);
      }

      @Override
      public Object parse(String stringKey) {
        return Long.valueOf(stringKey);
      }
    };

    public abstract PType<?> getPType();

    public abstract Schema getSchema();

    public abstract Object parse(String stringKey);
  }

  public String uri;

  public String format = "avro";

  public String compress = "uncompressed";

  public String name;

  public String keyField;

  public KeyType keyType;

  public Target.WriteMode writeMode = Target.WriteMode.OVERWRITE;

  public int parallelism = -1;

  public List<SourceConfig> sources = Lists.newArrayList();

  public ComputeConfig compute = null;
}
