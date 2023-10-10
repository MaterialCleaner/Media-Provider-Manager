// https://gist.github.com/seven332/eadc44f1b35f756e46c410a8487fcc1d

package me.gm.cleaner.plugin.ktx;

import java.util.Comparator;

public class NaturalSorter implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        int index1 = 0;
        int index2 = 0;
        while (true) {
            String data1 = nextSlice(o1, index1);
            String data2 = nextSlice(o2, index2);

            if (data1 == null && data2 == null) {
                return 0;
            }
            if (data1 == null) {
                return -1;
            }
            if (data2 == null) {
                return 1;
            }

            index1 += data1.length();
            index2 += data2.length();

            int result;
            if (isDigit(data1) && isDigit(data2)) {
                result = Long.compare(Long.valueOf(data1), Long.valueOf(data2));
                if (result == 0) {
                    result = -Integer.compare(data1.length(), data2.length());
                }
            } else {
                result = data1.compareToIgnoreCase(data2);
            }

            if (result != 0) {
                return result;
            }
        }
    }

    private static boolean isDigit(String str) {
        // Just check the first char
        char ch = str.charAt(0);
        return ch >= '0' && ch <= '9';
    }

    static String nextSlice(String str, int index) {
        int length = str.length();
        if (index == length) {
            return null;
        }

        char ch = str.charAt(index);
        if (ch == '.' || ch == ' ') {
            return str.substring(index, index + 1);
        } else if (ch >= '0' && ch <= '9') {
            return str.substring(index, nextNumberBound(str, index + 1));
        } else {
            return str.substring(index, nextOtherBound(str, index + 1));
        }
    }

    private static int nextNumberBound(String str, int index) {
        for (int length = str.length(); index < length; index++) {
            char ch = str.charAt(index);
            if (ch < '0' || ch > '9') {
                break;
            }
        }
        return index;
    }

    private static int nextOtherBound(String str, int index) {
        for (int length = str.length(); index < length; index++) {
            char ch = str.charAt(index);
            if (ch == '.' || ch == ' ' || (ch >= '0' && ch <= '9')) {
                break;
            }
        }
        return index;
    }
}
