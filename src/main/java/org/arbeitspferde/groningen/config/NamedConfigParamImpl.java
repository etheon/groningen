/* Copyright 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.arbeitspferde.groningen.config;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * Trivial {@link NamedConfigParam} implementation
 */
public class NamedConfigParamImpl implements NamedConfigParam, Serializable {
  private static final long serialVersionUID = 0;

  private final String value;

  public NamedConfigParamImpl(String value) {
    this.value = checkNotNull(value, "name");
  }

  @Override
  public String value() {
    return this.value;
  }

  @Override
  public int hashCode() {
    // This is specified in java.lang.Annotation.
    return (127 * "value".hashCode()) ^ value.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NamedConfigParam)) {
      return false;
    }

    NamedConfigParam other = (NamedConfigParam) o;
    return value.equals(other.value());
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(NamedConfigParamImpl.class)
        .add("value", value)
        .toString();
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return NamedConfigParam.class;
  }
}
