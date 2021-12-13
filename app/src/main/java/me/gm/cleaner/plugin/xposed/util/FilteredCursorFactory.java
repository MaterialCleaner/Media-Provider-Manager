/*
 * Copyright (C) 2014 Clover Network, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.plugin.xposed.util;

import android.database.Cursor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Common techniques for creating FilteredCursor objects.
 */
public class FilteredCursorFactory {

  private FilteredCursorFactory() {
  }

  /**
   * An interface to select rows to appear in a new FilteredCursor.
   */
  public interface Selector {
    /**
     * Return true to include the current position of the source Cursor into a FilteredCursor.
     */
    boolean select(Cursor cursor);
  }

  /**
   * Create a FilteredCursor that contains only rows selected by the given Selector. The order of the rows in the
   * FilteredCursor matches the order of the source Cursor.
   */
  public static FilteredCursor createUsingSelector(Cursor cursor, Selector selector) {
    if (cursor == null) {
      return null;
    }

    ArrayList<Integer> filterList = new ArrayList<Integer>();

    if (cursor.moveToFirst()) {
      do {
        if (selector.select(cursor)) {
          filterList.add(cursor.getPosition());
        }
      } while (cursor.moveToNext());
    }

    return FilteredCursor.createUsingFilter(cursor, toIntArray(filterList));
  }


  /**
   * Create a group of FilteredCursors. Each FilteredCursor will contain only rows with the same value for the given
   * column. The order of the rows in each FilteredCursor matches the order of the source Cursor. NULL is not treated
   * as unique so if there are any rows with the value NULL they will be grouped together.
   */
  public static Map<String, FilteredCursor> createGroups(Cursor cursor, String columnName) {
    if (cursor == null) {
      return null;
    }
    final int columnIndex = cursor.getColumnIndexOrThrow(columnName);
    return createGroups(cursor, columnIndex);
  }

  /**
   * Just like {@link FilteredCursorFactory#createGroups(Cursor, String)}, except takes a column index instead of column name.
   */
  public static Map<String, FilteredCursor> createGroups(Cursor cursor, int columnIndex) {
    if (cursor == null) {
      return null;
    }

    Map<String, List<Integer>> filters = new HashMap<String, List<Integer>>();

    if (cursor.moveToFirst()) {
      do {
        String key = cursor.getString(columnIndex);
        List<Integer> filterList = filters.get(key);
        if (filterList == null) {
          filterList = new ArrayList<Integer>();
          filters.put(key, filterList);
        }
        filterList.add(cursor.getPosition());
      } while (cursor.moveToNext());
    }

    Map<String, FilteredCursor> groups = new HashMap<String, FilteredCursor>();

    for (Map.Entry<String, List<Integer>> entry : filters.entrySet()) {
      groups.put(entry.getKey(), FilteredCursor.createUsingFilter(cursor, toIntArray(entry.getValue())));
    }

    return groups;
  }

  /**
   * When a source Cursor is joined with a list of values the JoinType specifies how rows are filtered. The JoinTypes
   * operates similarly to standard SQL joins.
   */
  public enum JoinType {
    /**
     * Specifies a left outer join, for rows where there is no match in the cursor {@link FilteredCursor#isEmpty()}
     * returns true.
     */
    LEFT_OUTER_JOIN,
    /**
     * Just like {@link JoinType#LEFT_OUTER_JOIN} except every row must match, if not {@link IllegalArgumentException}
     * is thrown.
     */
    STRICT_LEFT_OUTER_JOIN,
    /**
     * Specifies an inner join.
     */
    INNER_JOIN,
  }

  /**
   * Create a new FilteredCursor using a {@link JoinType#STRICT_LEFT_OUTER_JOIN}. The joinList acts as the left table
   * with a single column of values and source Cursor acts as the right table. The join is performed on the values of
   * the given columnName. Returns null if the provided cursor
   * is null.
   *
   * @see JoinType
   */
  public static FilteredCursor createUsingJoinList(Cursor cursor, String columnName, List<String> joinList) {
    return createUsingJoinList(cursor, columnName, joinList, JoinType.STRICT_LEFT_OUTER_JOIN);
  }

  /**
   * Create a new FilteredCursor using a {@link JoinType#STRICT_LEFT_OUTER_JOIN}. The joinList acts as the left table
   * with a single column of values and source Cursor acts as the right table. The join is performed on the values of
   * the given columnName. Returns null if the provided cursor
   * is null.
   *
   * @see JoinType
   */
  public static FilteredCursor createUsingJoinList(Cursor cursor, int columnIndex, List<String> joinList) {
    return createUsingJoinList(cursor, columnIndex, joinList, JoinType.STRICT_LEFT_OUTER_JOIN);
  }

  /**
   * Create a new FilteredCursor by joining the source Cursor with the given list using a {@link JoinType}. The
   * joinList acts as the left table with a single column of values and source Cursor acts as the right table. The join
   * is performed on the values of the given columnName. Returns null if the provided cursor is null.
   *
   * @see JoinType
   */
  public static FilteredCursor createUsingJoinList(Cursor cursor, String columnName, List<String> joinList, JoinType joinType) {
    if (cursor == null) {
      return null;
    }
    final int columnIndex = cursor.getColumnIndexOrThrow(columnName);
    return createUsingJoinList(cursor, columnIndex, joinList, joinType);
  }

  /**
   * Just like {@link FilteredCursorFactory#createUsingJoinList(Cursor, String, List<String>, JoinType)}, except it takes a
   * column index instead of column name.
   *
   * @see JoinType
   */
  public static FilteredCursor createUsingJoinList(Cursor cursor, int columnIndex, List<String> joinList, JoinType joinType) {
    if (cursor == null) {
      return null;
    }

    if (joinList == null || joinList.size() == 0) {
      return FilteredCursor.createUsingFilter(cursor, new int[0]);
    }

    int[] filterMap;

    final int filterListSize = joinList.size();
    filterMap = new int[filterListSize];
    // -1 is a magic value indicating this position has not been mapped, which is illegal
    Arrays.fill(filterMap, -1);

    Map<String, Deque<Integer>> filterValueMap = new HashMap<String, Deque<Integer>>(filterListSize);

    for (int i = 0; i < filterListSize; i++) {
      String value = joinList.get(i);

      Deque<Integer> filterIndexList = filterValueMap.get(value);
      if (filterIndexList == null) {
        filterIndexList = new ArrayDeque<Integer>();
        filterValueMap.put(value, filterIndexList);
      }
      filterIndexList.add(i);
    }

    if (cursor.moveToFirst()) {
      do {
        String value = cursor.getString(columnIndex);
        Deque<Integer> filterIndexList = filterValueMap.get(value);

        if (filterIndexList != null) {
          int cursorPosition = cursor.getPosition();

          for (Integer filterIndex : filterIndexList) {
            filterMap[filterIndex] = cursorPosition;
          }

          // If this cursor value comes up again point remaining filter indexes to it
          if (filterIndexList.size() > 1) {
            filterIndexList.removeFirst();
          } else {
            filterValueMap.remove(value);
          }
        }
      } while (cursor.moveToNext());
    }

    switch (joinType) {
      case STRICT_LEFT_OUTER_JOIN: {
        failOnEmptyPositions(cursor, filterMap, columnIndex, filterValueMap);
        break;
      }
      case INNER_JOIN: {
        filterMap = cullEmptyPositions(filterMap);
        break;
      }
      case LEFT_OUTER_JOIN: {
        // no need to do anything
        break;
      }
    }

    return FilteredCursor.createUsingFilter(cursor, filterMap);
  }

  private static void failOnEmptyPositions(Cursor cursor, int[] filterMap, int columnIndex, Map<String, Deque<Integer>> filterValueMap) {
    for (Map.Entry<String, Deque<Integer>> filterMapEntry : filterValueMap.entrySet()) {
      int filterIndex = filterMapEntry.getValue().getFirst();
      if (filterMap[filterIndex] == -1) {
        throw new IllegalArgumentException("Source cursor is missing entries for the column \""
                + cursor.getColumnName(columnIndex) + "\" with values " + filterMapEntry.getKey());
      }
    }
  }

  private static int[] cullEmptyPositions(int[] filterMap) {
    int culledSize = 0;
    for (int value : filterMap) {
      if (value != -1) {
        culledSize++;
      }
    }
    int[] culledFilterMap = new int[culledSize];
    int pos = 0;
    for (int value : filterMap) {
      if (value != -1) {
        culledFilterMap[pos++] = value;
      }
    }
    return culledFilterMap;
  }

  private static int[] toIntArray(List<Integer> list) {
    int[] ret = new int[list.size()];
    int i = 0;
    for (Integer e : list)
      ret[i++] = e;
    return ret;
  }

}
