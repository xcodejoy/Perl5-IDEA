/*
 * Copyright 2015-2018 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perl5.lang.perl.idea.codeInsight.typeInferrence.value;

import com.intellij.psi.stubs.StubOutputStream;
import com.perl5.lang.perl.lexer.PerlBaseLexer;
import org.jetbrains.annotations.NotNull;

public final class PerlUndefValue extends PerlValue {
  public static final PerlUndefValue UNDEF_VALUE = new PerlUndefValue();

  private PerlUndefValue() {
  }

  @Override
  protected void serializeData(@NotNull StubOutputStream dataStream) {
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(Object o) {
    return o == UNDEF_VALUE;
  }

  @Override
  protected int computeHashCode() {
    return getClass().hashCode();
  }

  @Override
  protected int getSerializationId() {
    return PerlValuesManager.UNDEF_ID;
  }

  @NotNull
  @Override
  PerlUndefValue createBlessedCopy(@NotNull PerlValue bless) {
    return this;
  }

  @NotNull
  @Override
  protected String getPresentableValueText() {
    return PerlBaseLexer.STRING_UNDEF;
  }

  @Override
  public String toString() {
    return "UNDEF_VALUE";
  }
}