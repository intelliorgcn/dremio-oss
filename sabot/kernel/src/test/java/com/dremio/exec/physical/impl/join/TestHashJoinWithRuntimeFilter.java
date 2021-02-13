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
package com.dremio.exec.physical.impl.join;

import org.junit.Before;
import org.junit.Test;

import com.dremio.PlanTestBase;

public class TestHashJoinWithRuntimeFilter extends PlanTestBase {

  private static final String NATION ="dfs.\"${WORKING_PATH}/src/test/resources/tpchmulti/nation\"";
  private static final String REGION ="dfs.\"${WORKING_PATH}/src/test/resources/tpchmulti/region\"";

  @Test
  public void testHashJoin() throws Exception {
    String sql = String.format("SELECT nations.N_NAME, count(*) FROM\n"
      + "%s nations \n"
      + "JOIN\n"
      + "%s regions \n"
      + "  on nations.N_REGIONKEY = regions.R_REGIONKEY \n"
      + "group by nations.N_NAME", NATION, REGION);
    String excludedColNames1 =  "runtimeFilter";
    testPlanMatchingPatterns(sql, new String[]{excludedColNames1});
  }

  @Test
  public void testLeftHashJoin() throws Exception {
    String sql = String.format("SELECT nations.N_NAME, count(*) FROM\n"
      + "%s nations \n"
      + "LEFT JOIN\n"
      + "%s regions \n"
      + "  on nations.N_REGIONKEY = regions.R_REGIONKEY \n"
      + "group by nations.N_NAME", NATION, REGION);
    String excludedColNames1 =  "runtimeFilterInfo";
    String excludedColNames2 =  "runtimeFilter";
    testPlanMatchingPatterns(sql, null, excludedColNames1, excludedColNames2);
  }


  @Test
  public void testHashJoinWithFuncJoinCondition() throws Exception {
    String sql = String.format("SELECT nations.N_NAME, count(*) FROM\n"
      + "%s nations \n"
      + "JOIN\n"
      + "%s regions \n"
      + "  on (nations.N_REGIONKEY +1) = regions.R_REGIONKEY \n"
      + "group by nations.N_NAME", NATION, REGION);
    String excludedColNames1 =  "runtimeFilterInfo";
    String excludedColNames2 =  "runtimeFilter";
    testPlanMatchingPatterns(sql, null, excludedColNames1, excludedColNames2);
  }

  @Test
  public void testHashJoinWithCast() throws Exception {
    String sql = String.format("SELECT nations.N_NAME, count(*) FROM\n"
      + "%s nations \n"
      + "JOIN\n"
      + "%s regions \n"
      + "  on CAST (nations.N_REGIONKEY as INT) = regions.R_REGIONKEY\n"
      + "group by nations.N_NAME", NATION, REGION);
    String excludedColNames1 =  "runtimeFilterInfo";
    String excludedColNames2 =  "runtimeFilter";
    testPlanMatchingPatterns(sql, null, excludedColNames1, excludedColNames2);
  }

  @Test
  public void testWithAlias() throws Exception {
    String sql = String.format("select * from\n" +
      "  (select n_regionkey as key, convert_from(n_comment, 'utf8') as comment from %s)\n" +
      "where key = (select max(r_regionkey) from %s)", NATION, REGION);
    testPlanMatchingPatterns(sql, new String[] {"runtimeFilter.*N_REGIONKEY"});
  }

  @Before
  public void setup() throws Exception{
    testNoResult("alter session set \"planner.slice_target\" = 1");
    testNoResult("alter session set \"planner.enable_broadcast_join\" = true");
    testNoResult("alter session set \"planner.filter.runtime_filter\" = true");
  }
}
