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
package com.dremio.exec.store.parquet;

import static com.dremio.exec.ExecConstants.NUM_SPLITS_TO_PREFETCH;
import static com.dremio.exec.ExecConstants.PARQUET_CACHED_ENTITY_SET_FILE_SIZE;
import static com.dremio.exec.ExecConstants.PREFETCH_READER;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyByte;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.hadoop.metadata.FileMetaData;
import org.apache.parquet.schema.MessageType;
import org.junit.Assert;
import org.junit.Test;

import com.dremio.common.AutoCloseables;
import com.dremio.common.config.SabotConfig;
import com.dremio.common.expression.SchemaPath;
import com.dremio.exec.ExecConstants;
import com.dremio.exec.catalog.StoragePluginId;
import com.dremio.exec.physical.base.OpProps;
import com.dremio.exec.store.RecordReader;
import com.dremio.exec.store.SplitAndPartitionInfo;
import com.dremio.exec.store.dfs.FileSystemPlugin;
import com.dremio.exec.store.dfs.SplitReaderCreator;
import com.dremio.exec.store.dfs.implicit.CompositeReaderConfig;
import com.dremio.io.file.FileSystem;
import com.dremio.options.OptionManager;
import com.dremio.sabot.exec.context.OperatorContext;
import com.dremio.sabot.exec.fragment.FragmentExecutionContext;
import com.dremio.sabot.exec.store.parquet.proto.ParquetProtobuf;
import com.dremio.service.namespace.dataset.proto.PartitionProtobuf;
import com.dremio.service.namespace.file.proto.FileConfig;
import com.google.protobuf.ByteString;

public class TestParquetSplitReaderCreatorIterator {

  private void testSplitReaderCreatorIteratorWithNoPrefetch(int mode) throws Exception {
    InputStreamProvider inputStreamProvider = mock(InputStreamProvider.class);
    MutableParquetMetadata footer = mock(MutableParquetMetadata.class);
    ParquetSplitReaderCreatorIterator splitReaderCreatorIterator = createSplitReaderCreator(false, 1, 10, inputStreamProvider, footer, mode);
    InputStreamProvider lastInputStreamProvider = null;
    MutableParquetMetadata lastFooter = null;
    int iteratorIdx = 0;
    while (splitReaderCreatorIterator.hasNext()) {
      SplitReaderCreator creator = splitReaderCreatorIterator.next();
      Assert.assertNull(creator.getNext());
      creator.createInputStreamProvider(lastInputStreamProvider, lastFooter);
      lastInputStreamProvider = creator.getInputStreamProvider();
      lastFooter = creator.getFooter();
      Assert.assertEquals(lastFooter, footer);
      Assert.assertEquals(lastInputStreamProvider, inputStreamProvider);
      RecordReader recordReader = creator.createRecordReader(lastFooter);
      Assert.assertNotNull(recordReader);
      iteratorIdx++;
    }
    Assert.assertEquals(10, iteratorIdx);
  }

  @Test
  public void testSplitReaderCreatorIteratorWithNoPrefetch() throws Exception {
    // Mode 0: all splits in diff files
    // Mode 1: all splits in same file. each split one rg
    // Mode 2: one split contains all rowgroups
    for (int i = 0; i < 3; ++i) {
      testSplitReaderCreatorIteratorWithNoPrefetch(i);
    }
  }

  private void testSplitReaderCreatorIteratorWithPrefetch(int mode) throws Exception {
    InputStreamProvider inputStreamProvider = mock(InputStreamProvider.class);
    MutableParquetMetadata footer = mock(MutableParquetMetadata.class);
    ParquetSplitReaderCreatorIterator splitReaderCreatorIterator = createSplitReaderCreator(true, 1, 10, inputStreamProvider, footer, mode);
    InputStreamProvider lastInputStreamProvider = null;
    MutableParquetMetadata lastFooter = null;
    int iteratorIdx = 0;
    while (splitReaderCreatorIterator.hasNext()) {
      SplitReaderCreator creator = splitReaderCreatorIterator.next();

      creator.createInputStreamProvider(lastInputStreamProvider, lastFooter);
      lastInputStreamProvider = creator.getInputStreamProvider();
      lastFooter = creator.getFooter();
      Assert.assertEquals(lastFooter, footer);
      Assert.assertEquals(lastInputStreamProvider, inputStreamProvider);
      RecordReader recordReader = creator.createRecordReader(lastFooter);
      Assert.assertNotNull(recordReader);
      if (iteratorIdx < 9) {
        Assert.assertNotNull(creator.getNext());
        Assert.assertNull(creator.getNext().getNext());
        // due to prefetch for one forward split inputstream will not be null
        Assert.assertNotNull(creator.getNext().getInputStreamProvider());
      } else {
        Assert.assertNull(creator.getNext());
      }
      iteratorIdx++;
    }
    Assert.assertEquals(10, iteratorIdx);
  }

  @Test
  public void testSplitReaderCreatorIteratorWithPrefetch() throws Exception {
    for (int i = 0; i < 3; i++) {
      testSplitReaderCreatorIteratorWithPrefetch(i);
    }
  }

  private void testSplitReaderCreatorIteratorWithAllPrefetch(int mode) throws Exception {
    InputStreamProvider inputStreamProvider = mock(InputStreamProvider.class);
    MutableParquetMetadata footer = mock(MutableParquetMetadata.class);
    ParquetSplitReaderCreatorIterator splitReaderCreatorIterator = createSplitReaderCreator(true, 10, 10, inputStreamProvider, footer, mode);
    InputStreamProvider lastInputStreamProvider = null;
    MutableParquetMetadata lastFooter = null;
    int iteratorIdx = 0;
    while (splitReaderCreatorIterator.hasNext()) {
      SplitReaderCreator creator = splitReaderCreatorIterator.next();
      if (iteratorIdx < 9) {
        Assert.assertNotNull(creator.getNext());
      } else {
        Assert.assertNull(creator.getNext());
      }
      creator.createInputStreamProvider(lastInputStreamProvider, lastFooter);
      lastInputStreamProvider = creator.getInputStreamProvider();
      lastFooter = creator.getFooter();
      Assert.assertEquals(lastFooter, footer);
      Assert.assertEquals(lastInputStreamProvider, inputStreamProvider);
      RecordReader recordReader = creator.createRecordReader(lastFooter);
      Assert.assertNotNull(recordReader);
      while (creator.getNext() != null) {
        creator = creator.getNext();
        Assert.assertNotNull(creator.getInputStreamProvider());
      }
      iteratorIdx++;
    }
    Assert.assertEquals(10, iteratorIdx);
  }

  @Test
  public void testSplitReaderCreatorIteratorWithAllPrefetch() throws Exception {
    for (int i = 0; i < 3; ++i) {
      testSplitReaderCreatorIteratorWithAllPrefetch(i);
    }
  }

  @Test
  public void testSplitReaderCreatorIteratorWithMultipleSplitsButOnlyOneRowGroup() throws Exception {
    InputStreamProvider inputStreamProvider = mock(InputStreamProvider.class);
    MutableParquetMetadata footer = mock(MutableParquetMetadata.class);
    ParquetSplitReaderCreatorIterator splitReaderCreatorIterator = createSplitReaderCreator(true, 10, 10, inputStreamProvider, footer, 3);

    Assert.assertTrue(splitReaderCreatorIterator.hasNext());
    SplitReaderCreator creator = splitReaderCreatorIterator.next();

    InputStreamProvider lastInputStreamProvider = null;
    MutableParquetMetadata lastFooter = null;
    creator.createInputStreamProvider(lastInputStreamProvider, lastFooter);
    lastInputStreamProvider = creator.getInputStreamProvider();
    lastFooter = creator.getFooter();
    Assert.assertEquals(lastFooter, footer);
    Assert.assertEquals(lastInputStreamProvider, inputStreamProvider);
    RecordReader recordReader = creator.createRecordReader(lastFooter);
    Assert.assertNotNull(recordReader);

    Assert.assertNull(creator.getNext());
    Assert.assertFalse(splitReaderCreatorIterator.hasNext());
  }

  @Test
  public void testWithZeroSplits() throws Exception {
    for (int i = 0; i < 3; ++i) {
      InputStreamProvider inputStreamProvider = mock(InputStreamProvider.class);
      MutableParquetMetadata footer = mock(MutableParquetMetadata.class);
      ParquetSplitReaderCreatorIterator splitReaderCreatorIterator = createSplitReaderCreator(true, 1, 0, inputStreamProvider, footer, i);
      Assert.assertFalse(splitReaderCreatorIterator.hasNext());
    }
  }

  private void testCreatorIteratorPrematurelyClosed(int mode) throws Exception {
    InputStreamProvider inputStreamProvider = mock(InputStreamProvider.class);
    MutableParquetMetadata footer = mock(MutableParquetMetadata.class);
    ParquetSplitReaderCreatorIterator splitReaderCreatorIterator = createSplitReaderCreator(true, 2, 10, inputStreamProvider, footer, mode);

    SplitReaderCreator creator = null;
    InputStreamProvider lastInputStreamProvider = null;
    MutableParquetMetadata lastFooter = null;
    for (int i = 0; i < 5; ++i) {
      Assert.assertTrue(splitReaderCreatorIterator.hasNext());
      creator = splitReaderCreatorIterator.next();
      creator.createInputStreamProvider(lastInputStreamProvider, lastFooter);
      lastInputStreamProvider = creator.getInputStreamProvider();
      lastFooter = creator.getFooter();
      creator.createRecordReader(lastFooter);
    }

    Assert.assertNotNull(creator.getNext());
    Assert.assertNotNull(creator.getNext().getNext());
    Assert.assertNull(creator.getNext().getNext().getNext());


    Assert.assertNotNull(creator.getNext().getInputStreamProvider());
    Assert.assertNotNull(creator.getNext().getNext().getInputStreamProvider());

    AutoCloseables.close(splitReaderCreatorIterator);

    // inputstreamproviders are closed; accessing them will have NPE
    Exception ex = null;
    try {
      creator.getNext().getInputStreamProvider();
    } catch (Exception e) {
      ex = e;
    }
    Assert.assertTrue(ex instanceof NullPointerException);

    ex = null;
    try {
      creator.getNext().getNext().getInputStreamProvider();
    } catch (Exception e) {
      ex = e;
    }
    Assert.assertTrue(ex instanceof NullPointerException);

    verify(inputStreamProvider, times(2)).close();
  }

  @Test
  public void testCreatorIteratorPrematurelyClosed() throws Exception {
    for (int i = 0; i < 3; ++i) {
      testCreatorIteratorPrematurelyClosed(i);
    }
  }

  private ParquetSplitReaderCreatorIterator createSplitReaderCreator(boolean prefetch, long numPrefetch, int numSplits, InputStreamProvider inputStreamProvider, MutableParquetMetadata footer, int mode) throws Exception {
    FragmentExecutionContext fragmentExecutionContext = mock(FragmentExecutionContext.class);
    OperatorContext context = mock(OperatorContext.class);
    ParquetSubScan config = mock(ParquetSubScan.class);

    SabotConfig sabotConfig = mock(SabotConfig.class);
    InputStreamProviderFactory inputStreamProviderFactory = mock(InputStreamProviderFactory.class);
    OptionManager optionManager = mock(OptionManager.class);
    FileSystemPlugin fileSystemPlugin = mock(FileSystemPlugin.class);
    FileSystem fs = mock(FileSystem.class);
    StoragePluginId storagePluginId = mock(StoragePluginId.class);
    OpProps opProps = mock(OpProps.class);
    CompositeReaderConfig readerConfig = mock(CompositeReaderConfig.class);

    when(sabotConfig.getInstance(InputStreamProviderFactory.KEY, InputStreamProviderFactory.class, InputStreamProviderFactory.DEFAULT)).thenReturn(inputStreamProviderFactory);
    when(sabotConfig.getInstance("dremio.plugins.parquet.factory", ParquetReaderFactory.class, ParquetReaderFactory.NONE)).thenReturn(ParquetReaderFactory.NONE);
    when(context.getConfig()).thenReturn(sabotConfig);
    when(context.getOptions()).thenReturn(optionManager);
    when(optionManager.getOption(PREFETCH_READER)).thenReturn(prefetch);
    when(optionManager.getOption(NUM_SPLITS_TO_PREFETCH)).thenReturn(numPrefetch);
    when(optionManager.getOption(PARQUET_CACHED_ENTITY_SET_FILE_SIZE)).thenReturn(true);
    when(fragmentExecutionContext.getStoragePlugin(any())).thenReturn(fileSystemPlugin);
    when(fileSystemPlugin.createFS(anyString(), any())).thenReturn(fs);
    when(fs.supportsPath(any())).thenReturn(true);
    when(fs.supportsAsync()).thenReturn(true);
    when(config.getPluginId()).thenReturn(storagePluginId);
    when(storagePluginId.getName()).thenReturn("");
    when(config.getProps()).thenReturn(opProps);
    when(opProps.getUserName()).thenReturn("");
    when(config.getColumns()).thenReturn(Collections.singletonList(SchemaPath.getSimplePath("*")));
    when(config.getFormatSettings()).thenReturn(FileConfig.getDefaultInstance());
    when(optionManager.getOption(ExecConstants.FILESYSTEM_PARTITION_COLUMN_LABEL_VALIDATOR)).thenReturn("dir");
    when(inputStreamProviderFactory.create(any(),any(),any(),anyByte(),anyByte(),any(),any(),any(),any(),anyBoolean(),any(),anyByte(),anyBoolean(),anyBoolean())).thenReturn(inputStreamProvider);

    BlockMetaData blockMetaData = mock(BlockMetaData.class);
    when(footer.getBlocks()).thenReturn(Collections.singletonList(blockMetaData));
    ColumnChunkMetaData chunkMetaData = mock(ColumnChunkMetaData.class);
    when(blockMetaData.getColumns()).thenReturn(Collections.singletonList(chunkMetaData));
    when(chunkMetaData.getFirstDataPageOffset()).thenReturn(0L);
    when(inputStreamProvider.getFooter()).thenReturn(footer);
    when(footer.getFileMetaData()).thenReturn(new FileMetaData(new MessageType("", new ArrayList<>()), new HashMap<>(), ""));
    when(readerConfig.getPartitionNVPairs(any(), any())).thenReturn(new ArrayList<>());
    when(readerConfig.wrapIfNecessary(any(), any(), any())).then(i -> i.getArgumentAt(1, RecordReader.class));

    List<SplitAndPartitionInfo> splits = new ArrayList<>();

    if (mode == 0) {
      // all splits in diff files
      IntStream.range(0, numSplits).forEach(i -> splits.add(createBlockBasedSplit(0, 10, i)));
    } else if (mode == 1) {
      // all splits in same file. each split one rg
      // giving start length same for all for simplicity since they are not used in mocked objects.
      IntStream.range(0, numSplits).forEach(i -> splits.add(createBlockBasedSplit(0, 10, 0)));
    }  else if (mode == 2){
      // one split contains all rowgroups
      List<BlockMetaData> blocks = new ArrayList<>();
      for (int i = 0; i < numSplits; ++i) {
        BlockMetaData block = mock(BlockMetaData.class);
        ColumnChunkMetaData columnChunkMetaData = mock(ColumnChunkMetaData.class);
        when(columnChunkMetaData.getFirstDataPageOffset()).thenReturn((long) i);
        when(block.getColumns()).thenReturn(Collections.singletonList(columnChunkMetaData));
        blocks.add(block);
      }
      when(footer.getBlocks()).thenReturn(blocks);
      splits.add(createBlockBasedSplit(0, 10 * numSplits, 0));
    } else if (mode == 3) {
      // only one split contains row group
      IntStream.range(0, numSplits).forEach(i -> splits.add(createBlockBasedSplit(i * 10, 10, 0)));
    }


    when(config.getSplits()).thenReturn(splits);

    return new ParquetSplitReaderCreatorIterator(fragmentExecutionContext, context, config, false);
  }

  private SplitAndPartitionInfo createBlockBasedSplit(int start, int length, int p) {
    ParquetProtobuf.ParquetBlockBasedSplitXAttr splitXAttr = ParquetProtobuf.ParquetBlockBasedSplitXAttr.newBuilder()
      .setStart(start)
      .setLength(length)
      .setPath("/tmp/path/" + p)
      .setFileLength(100)
      .setLastModificationTime(0)
      .build();

    ByteString serializedSplitXAttr = splitXAttr.toByteString();

    PartitionProtobuf.NormalizedPartitionInfo partitionInfo = PartitionProtobuf.NormalizedPartitionInfo.newBuilder()
      .setId("0")
      .build();

    PartitionProtobuf.NormalizedDatasetSplitInfo datasetSplitInfo = PartitionProtobuf.NormalizedDatasetSplitInfo.newBuilder()
      .setPartitionId("0")
      .setExtendedProperty(serializedSplitXAttr)
      .build();

    return new SplitAndPartitionInfo(partitionInfo, datasetSplitInfo);
  }

}
