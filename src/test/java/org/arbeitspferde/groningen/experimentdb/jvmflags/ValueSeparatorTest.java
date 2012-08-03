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

package org.arbeitspferde.groningen.experimentdb.jvmflags;


import junit.framework.TestCase;

import org.arbeitspferde.groningen.experimentdb.jvmflags.ValueSeparator;

/**
 * Tests for {@link ValueSeparator}.
 */
public class ValueSeparatorTest extends TestCase {
  public void test_getInfix_None() {
    assertEquals("", ValueSeparator.NONE.getInfix());
  }

  public void test_getInfix_Equal() {
    assertEquals("=", ValueSeparator.EQUAL.getInfix());
  }
}