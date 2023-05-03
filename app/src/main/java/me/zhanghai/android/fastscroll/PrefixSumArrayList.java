/*
 * Copyright 2023 Green Mushroom
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

package me.zhanghai.android.fastscroll;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Lazy prefix sum {@link #ArrayList} implementation.
 *
 * @author Green Mushroom
 */
public class PrefixSumArrayList extends ArrayList<Integer> {
    private final ArrayList<Integer> prefixSum = new ArrayList<>();

    private Integer getPrefixSumBefore(final int index) {
        if (index == 0) {
            return 0;
        }
        return prefixSum.get(index - 1);
    }

    private void ensurePrefixSum(final int left, final int right) {
        if (getPrefixSumBefore(left) == null) {
            ensurePrefixSum(0, left);
        }
        if (getPrefixSumBefore(right) == null) {
            for (int i = left; i < right; i++) {
                prefixSum.set(i, getPrefixSumBefore(i) + super.get(i));
            }
        }
    }

    /**
     * Query range sum.
     *
     * @param left  The left endpoint of the range.
     * @param right The right endpoint of the range.
     * @return the sum of elements in the range
     */
    public int query(final int left, final int right) {
        ensurePrefixSum(left, right);
        return getPrefixSumBefore(right) - getPrefixSumBefore(left);
    }

    /**
     * @return the sum of all elements in this list
     */
    public int sum() {
        return query(0, super.size());
    }

    private void invalidatePrefixSum(final int index) {
        final int newSize = super.size();
        final Integer[] elementData = new Integer[newSize];
        Arrays.fill(elementData, null);
        for (int i = 0; i < index; i++) {
            if (i >= prefixSum.size()) {
                break;
            }
            final Integer val = prefixSum.get(i);
            if (val == null) {
                break;
            }
            elementData[i] = val;
        }

        prefixSum.clear();
        Collections.addAll(prefixSum, elementData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trimToSize() {
        super.trimToSize();
        prefixSum.trimToSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ensureCapacity(final int minCapacity) {
        super.ensureCapacity(minCapacity);
        prefixSum.ensureCapacity(minCapacity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer set(final int index, final Integer element) {
        try {
            return super.set(index, element);
        } finally {
            invalidatePrefixSum(index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(final Integer integer) {
        final int index = super.size();
        try {
            return super.add(integer);
        } finally {
            invalidatePrefixSum(index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final int index, final Integer element) {
        super.add(index, element);
        invalidatePrefixSum(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer remove(final int index) {
        try {
            return super.remove(index);
        } finally {
            invalidatePrefixSum(index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(@Nullable final Object o) {
        final int index = super.indexOf(o);
        try {
            return index != -1;
        } finally {
            remove(index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        super.clear();
        prefixSum.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(@NonNull final Collection<? extends Integer> c) {
        final int index = super.size();
        try {
            return super.addAll(c);
        } finally {
            invalidatePrefixSum(index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(final int index, @NonNull final Collection<? extends Integer> c) {
        try {
            return super.addAll(index, c);
        } finally {
            invalidatePrefixSum(index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void removeRange(final int fromIndex, final int toIndex) {
        super.removeRange(fromIndex, toIndex);
        invalidatePrefixSum(fromIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll(@NonNull final Collection<?> c) {
        try {
            return super.removeAll(c);
        } finally {
            invalidatePrefixSum(0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean retainAll(@NonNull final Collection<?> c) {
        try {
            return super.retainAll(c);
        } finally {
            invalidatePrefixSum(0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeIf(@NonNull final Predicate<? super Integer> filter) {
        try {
            return super.removeIf(filter);
        } finally {
            invalidatePrefixSum(0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void replaceAll(@NonNull final UnaryOperator<Integer> operator) {
        try {
            super.replaceAll(operator);
        } finally {
            invalidatePrefixSum(0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sort(@Nullable final Comparator<? super Integer> c) {
        try {
            super.sort(c);
        } finally {
            invalidatePrefixSum(0);
        }
    }
}
