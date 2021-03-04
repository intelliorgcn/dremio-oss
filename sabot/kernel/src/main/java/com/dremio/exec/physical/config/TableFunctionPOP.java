/*
 * Copyright (C) 2017-2019 Dremio Corporation
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
package com.dremio.exec.physical.config;

import java.util.Iterator;

import com.dremio.exec.physical.base.AbstractSingle;
import com.dremio.exec.physical.base.OpProps;
import com.dremio.exec.physical.base.PhysicalOperator;
import com.dremio.exec.physical.base.PhysicalVisitor;
import com.dremio.exec.proto.UserBitShared;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Iterators;

/**
 * POP for table function
 */
@JsonTypeName("table-function")
public class TableFunctionPOP extends AbstractSingle {

  private final TableFunctionConfig function;
  @JsonCreator
  public TableFunctionPOP(
    @JsonProperty("props") OpProps props,
    @JsonProperty("child") PhysicalOperator child,
    @JsonProperty("function") TableFunctionConfig config
  ) {
    super(props, child);
    this.function = config;
  }

  public TableFunctionConfig getFunction() {
    return function;
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new TableFunctionPOP(this.props, child, function);
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitTableFunction(this, value);
  }

  @Override
  public int getOperatorType() {
    return UserBitShared.CoreOperatorType.TABLE_FUNCTION_VALUE;
  }

  @Override
  public int getOperatorSubType() {
    return getFunction().getType().ordinal();
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return Iterators.singletonIterator(child);
  }
}
