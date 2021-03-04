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
package com.dremio.exec.store.deltalake;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.junit.Before;
import org.junit.Test;

import com.dremio.BaseTestQuery;
import com.dremio.connector.metadata.DatasetSplit;
import com.dremio.exec.hadoop.HadoopFileSystem;
import com.dremio.exec.server.SabotContext;
import com.dremio.io.file.FileSystem;
import com.dremio.io.file.Path;

public class TestDeltaLogCheckpointParquetReader extends BaseTestQuery {

  String path;
  File f;
  FileSystem fs;
  SabotContext sabotContext;

  @Before
  public void setup() throws Exception {
    fs = HadoopFileSystem.getLocal(new Configuration());
    sabotContext = getSabotContext();
  }

  @Test
  public void testCheckpointParquet() throws IOException {
    path = "src/test/resources/deltalake/checkpointParquet/00000000000000000020.checkpoint.parquet";
    f = new File(path);
    DeltaLogCheckpointParquetReader reader = new DeltaLogCheckpointParquetReader();
    DeltaLogSnapshot snapshot = reader.parseMetadata(null, sabotContext, fs, fs.getFileAttributes(Path.of(f.toURI())));
    assertTrue(snapshot.containsCheckpoint());
    assertEquals(snapshot.getSchema(), "{\"type\":\"struct\",\"fields\":[{\"name\":\"Year\",\"type\":\"integer\",\"nullable\":true,\"metadata\":{}},{\"name\":\"Month\",\"type\":\"integer\",\"nullable\":true,\"metadata\":{}},{\"name\":\"DayofMonth\",\"type\":\"integer\",\"nullable\":true,\"metadata\":{}},{\"name\":\"DayOfWeek\",\"type\":\"integer\",\"nullable\":true,\"metadata\":{}},{\"name\":\"DepTime\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"CRSDepTime\",\"type\":\"integer\",\"nullable\":true,\"metadata\":{}},{\"name\":\"ArrTime\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"CRSArrTime\",\"type\":\"integer\",\"nullable\":true,\"metadata\":{}},{\"name\":\"UniqueCarrier\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"FlightNum\",\"type\":\"integer\",\"nullable\":true,\"metadata\":{}},{\"name\":\"TailNum\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"ActualElapsedTime\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"CRSElapsedTime\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"AirTime\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"ArrDelay\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"DepDelay\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"Origin\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"Dest\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"Distance\",\"type\":\"integer\",\"nullable\":true,\"metadata\":{}},{\"name\":\"TaxiIn\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"TaxiOut\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"Cancelled\",\"type\":\"integer\",\"nullable\":true,\"metadata\":{}},{\"name\":\"CancellationCode\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"Diverted\",\"type\":\"integer\",\"nullable\":true,\"metadata\":{}},{\"name\":\"CarrierDelay\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"WeatherDelay\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"NASDelay\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"SecurityDelay\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"LateAircraftDelay\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}}]}");
    assertEquals(snapshot.getNetFilesAdded(), 135);
    assertEquals(snapshot.getNetBytesAdded(), 6139101);
    assertEquals(snapshot.getPartitionColumns(), Arrays.asList("DayOfWeek"));
  }

  @Test
  public void testLargeCheckpointParquet() throws IOException {
    path = "src/test/resources/deltalake/checkpointParquet/00000000000000000210.checkpoint.parquet";
    f = new File(path);
    DeltaLogCheckpointParquetReader reader = new DeltaLogCheckpointParquetReader();
    DeltaLogSnapshot snapshot = reader.parseMetadata(null, sabotContext, fs, fs.getFileAttributes(Path.of(f.toURI())));
    assertTrue(snapshot.containsCheckpoint());
    assertEquals(snapshot.getSchema(), "{\"type\":\"struct\",\"fields\":[{\"name\":\"iso_code\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"continent\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"location\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"date\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"total_cases\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_cases\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_cases_smoothed\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"total_deaths\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_deaths\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_deaths_smoothed\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"total_cases_per_million\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_cases_per_million\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_cases_smoothed_per_million\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"total_deaths_per_million\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_deaths_per_million\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_deaths_smoothed_per_million\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"reproduction_rate\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"icu_patients\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"icu_patients_per_million\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"hosp_patients\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"hosp_patients_per_million\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"weekly_icu_admissions\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"weekly_icu_admissions_per_million\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"weekly_hosp_admissions\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"weekly_hosp_admissions_per_million\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_tests\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"total_tests\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"total_tests_per_thousand\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_tests_per_thousand\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_tests_smoothed\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_tests_smoothed_per_thousand\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"positive_rate\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"tests_per_case\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"tests_units\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"stringency_index\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"population\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"population_density\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"median_age\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"aged_65_older\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"aged_70_older\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"gdp_per_capita\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"extreme_poverty\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"cardiovasc_death_rate\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"diabetes_prevalence\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"female_smokers\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"male_smokers\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"handwashing_facilities\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"hospital_beds_per_thousand\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"life_expectancy\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"human_development_index\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"id\",\"type\":\"long\",\"nullable\":true,\"metadata\":{}}]}");
    assertEquals(snapshot.getNetFilesAdded(), 211);
    assertEquals(snapshot.getNetBytesAdded(), 2737079);
    assertEquals(snapshot.getPartitionColumns(), Collections.emptyList());
  }

  @Test
  public void testMultipleRowGroupsCheckpointParquet() throws IOException {
    path = "src/test/resources/deltalake/checkpointParquet/00000000000000000290.checkpoint.parquet";
    f = new File(path);
    DeltaLogCheckpointParquetReader reader = new DeltaLogCheckpointParquetReader();
    DeltaLogSnapshot snapshot = reader.parseMetadata(null, sabotContext, fs, fs.getFileAttributes(Path.of(f.toURI())));
    assertTrue(snapshot.containsCheckpoint());
    assertEquals(snapshot.getSchema(), "{\"type\":\"struct\",\"fields\":[{\"name\":\"iso_code\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"continent\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"location\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"date\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"total_cases\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"new_cases\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"life_expectancy\",\"type\":\"string\",\"nullable\":true,\"metadata\":{}},{\"name\":\"id\",\"type\":\"long\",\"nullable\":true,\"metadata\":{}}]}");
    assertEquals(snapshot.getNetFilesAdded(), 1164);
    assertEquals(snapshot.getNetBytesAdded(), 202295052);
    assertEquals(snapshot.getPartitionColumns(), Collections.emptyList());

    List<DatasetSplit> splits = snapshot.getSplits();
    assertEquals(splits.get(0).getSizeInBytes(), 57977);
    assertEquals(splits.get(0).getRecordCount(), 751);
    assertEquals(splits.get(1).getSizeInBytes(), 36494);
    assertEquals(splits.get(1).getRecordCount(), 415);
  }

  @Test
  public void testCheckpointParquetRemovedFiles() throws IOException {
    path = "src/test/resources/deltalake/28554.checkpoint.parquet";
    f = new File(path);
    Path checkpointFilePath = Path.of(f.toURI());
    DeltaLogCheckpointParquetReader reader = new DeltaLogCheckpointParquetReader();
    DeltaLogSnapshot snapshot = reader.parseMetadata(null, sabotContext, fs, fs.getFileAttributes(checkpointFilePath));
    assertTrue(snapshot.containsCheckpoint());
    assertEquals(12, snapshot.getNetFilesAdded());
    assertEquals(12, snapshot.getNetOutputRows());
    assertEquals(6093, snapshot.getNetBytesAdded());
  }

  @Test
  public void testCheckpointParquetNoStats() throws IOException {
    path = "src/test/resources/deltalake/noStats/00000000000000000010.checkpoint.parquet";
    f = new File(path);
    Path checkpointFilePath = Path.of(f.toURI());
    Path rootDir = checkpointFilePath.getParent();
    DeltaLogCheckpointParquetReader reader = new DeltaLogCheckpointParquetReader();
    DeltaLogSnapshot snapshot = reader.parseMetadata(rootDir, sabotContext, fs, fs.getFileAttributes(checkpointFilePath));
    assertTrue(snapshot.containsCheckpoint());
    assertEquals(10, snapshot.getNetFilesAdded());
    assertEquals(10, snapshot.getNetOutputRows());
    assertEquals(6311, snapshot.getNetBytesAdded());
  }
}
