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
package com.dremio.exec.store.iceberg;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.AbstractRelNode;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;

import com.dremio.common.expression.SchemaPath;
import com.dremio.exec.physical.base.PhysicalOperator;
import com.dremio.exec.planner.fragment.DistributionAffinity;
import com.dremio.exec.planner.physical.LeafPrel;
import com.dremio.exec.planner.physical.PhysicalPlanCreator;
import com.dremio.exec.planner.physical.Prel;
import com.dremio.exec.planner.physical.visitor.PrelVisitor;
import com.dremio.exec.record.BatchSchema;
import com.dremio.exec.store.TableMetadata;
import com.dremio.options.Options;
import com.dremio.options.TypeValidators;

/**
 * Iceberg manifest list reader prel
 */
@Options
public class IcebergManifestListPrel extends AbstractRelNode  implements LeafPrel {

    public static final TypeValidators.LongValidator RESERVE = new TypeValidators.PositiveLongValidator("planner.op.scan.iceberg.reserve_bytes", Long.MAX_VALUE, DEFAULT_RESERVE);
    public static final TypeValidators.LongValidator LIMIT = new TypeValidators.PositiveLongValidator("planner.op.scan.iceberg.limit_bytes", Long.MAX_VALUE, DEFAULT_LIMIT);

    protected final TableMetadata tableMetadata;
    private final BatchSchema schema;
    private final List<SchemaPath> projectedColumns;
    private final RelDataType relDataType;

    public IcebergManifestListPrel(RelOptCluster cluster, RelTraitSet traitSet, TableMetadata tableMetadata,
                                   BatchSchema schema,
                                   List<SchemaPath> projectedColumns,
                                   RelDataType relDataType) {
        super(cluster, traitSet);
        this.tableMetadata = tableMetadata;
        this.schema = schema;
        this.projectedColumns = projectedColumns;
        this.relDataType = relDataType;
    }

    @Override
    public double estimateRowCount(RelMetadataQuery mq) {
        // TODO: We should use the size of the manifest list file to estimate the number of
        // manifest files that would be produced by this scan
        return 1;
    }

    @Override
    public Iterator<Prel> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
        return new IcebergGroupScan(
                creator.props(this, tableMetadata.getUser(), schema, RESERVE, LIMIT),
                tableMetadata,
                projectedColumns);
    }

    @Override
    public Prel copy(RelTraitSet traitSet, List<RelNode> inputs) {
        return new IcebergManifestListPrel(getCluster(), getTraitSet(), tableMetadata, schema, projectedColumns, relDataType);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) throws E {
        return logicalVisitor.visitLeaf(this, value);
    }

    @Override
    public BatchSchema.SelectionVectorMode[] getSupportedEncodings() {
        return BatchSchema.SelectionVectorMode.DEFAULT;
    }

    @Override
    public BatchSchema.SelectionVectorMode getEncoding() {
        return BatchSchema.SelectionVectorMode.NONE;
    }

    @Override
    public boolean needsFinalColumnReordering() {
        return false;
    }

    @Override
    public int getMaxParallelizationWidth() {
        return 1;
    }

    @Override
    public int getMinParallelizationWidth() {
        return 1;
    }

    @Override
    public DistributionAffinity getDistributionAffinity() {
        return DistributionAffinity.NONE;
    }

    @Override
    protected RelDataType deriveRowType() {
        return relDataType;
    }
}
