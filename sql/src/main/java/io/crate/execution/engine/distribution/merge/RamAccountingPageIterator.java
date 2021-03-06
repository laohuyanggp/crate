/*
 * Licensed to Crate.IO GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.execution.engine.distribution.merge;

import com.google.common.annotations.VisibleForTesting;
import io.crate.breaker.RowAccounting;
import io.crate.data.Row;

/**
 * Wraps a PagingIterator and uses {@link io.crate.breaker.RamAccountingContext} to apply the circuit breaking logic
 */
class RamAccountingPageIterator implements PagingIterator<Integer, Row> {

    @VisibleForTesting
    final PagingIterator<Integer, Row> delegatePagingIterator;
    private final RowAccounting rowAccounting;

    RamAccountingPageIterator(PagingIterator<Integer, Row> delegatePagingIterator, RowAccounting rowAccounting) {
        this.delegatePagingIterator = delegatePagingIterator;
        this.rowAccounting = rowAccounting;
    }

    @Override
    public void merge(Iterable<? extends KeyIterable<Integer, Row>> keyIterables) {
        for (KeyIterable<Integer, Row> iterable : keyIterables) {
            for (Row row : iterable) {
                rowAccounting.accountForAndMaybeBreak(row);
            }
        }
        delegatePagingIterator.merge(keyIterables);
    }

    @Override
    public void finish() {
        rowAccounting.close();
        delegatePagingIterator.finish();
    }

    @Override
    public Integer exhaustedIterable() {
        return delegatePagingIterator.exhaustedIterable();
    }

    @Override
    public Iterable<Row> repeat() {
        return delegatePagingIterator.repeat();
    }

    @Override
    public boolean hasNext() {
        return delegatePagingIterator.hasNext();
    }

    @Override
    public Row next() {
        return delegatePagingIterator.next();
    }
}
