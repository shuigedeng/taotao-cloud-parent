/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.taotao.cloud.bigdata.trino.udaf.collect_list;

import com.taotao.cloud.bigdata.trino.udaf.collect_list.CollectListAggregationFunctions;
import com.taotao.cloud.bigdata.trino.udaf.collect_list.CollectListStats;
import io.trino.spi.block.Block;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.function.AccumulatorStateSerializer;
import io.trino.spi.type.Type;

import static io.trino.spi.type.VarbinaryType.VARBINARY;


/**
 * @author dengtao
 * @since 2020/10/29 18:04
 * @version 1.0.0
 */
public class CollectListStatsSerializer implements AccumulatorStateSerializer<CollectListAggregationFunctions.CollectState> {
    @Override
    public Type getSerializedType() {
        return VARBINARY;
    }

    @Override
    public void serialize(CollectListAggregationFunctions.CollectState state, BlockBuilder out) {
        if (state.get() == null) {
            out.appendNull();
        } else {
            VARBINARY.writeSlice(out, state.get().serialize());
        }
    }

    @Override
    public void deserialize(Block block, int index, CollectListAggregationFunctions.CollectState state) {
        state.set(new CollectListStats(VARBINARY.getSlice(block, index)));
    }
}