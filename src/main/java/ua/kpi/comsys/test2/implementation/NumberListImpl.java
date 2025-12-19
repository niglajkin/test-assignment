/*
 * Copyright (c) 2014, NTUU KPI, Computer systems department and/or its affiliates. All rights reserved.
 * NTUU KPI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 */

package ua.kpi.comsys.test2.implementation;

import java.io.*;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.*;

import ua.kpi.comsys.test2.NumberList;

/**
 * Custom implementation of INumberList interface.
 * Has to be implemented by each student independently.
 *
 * @author Alexander Podrubailo
 *
 */
public class NumberListImpl implements NumberList {

    /* try run workflows */

    private static final int RECORD_BOOK_NUMBER = 9085;

    private static final int C3 = RECORD_BOOK_NUMBER % 3;
    private static final int C5 = RECORD_BOOK_NUMBER % 5;
    private static final int C7 = RECORD_BOOK_NUMBER % 7;

    private static final int BASE_CURRENT = baseFromC5(C5);
    private static final int BASE_EXTRA = baseFromC5((C5 + 1) % 5);

    private static final class Node {
        byte v;
        Node next;
        Node(byte v) { this.v = v; }
    }

    private Node tail;
    private int size;
    private final int base;


    /** Default constructor. Returns empty NumberListImpl */
    public NumberListImpl() {
        this.base = BASE_CURRENT;
        this.tail = null;
        this.size = 0;
    }

    /** Private constructor for internal base switching / arithmetic results */
    private NumberListImpl(int base) {
        this.base = base;
        this.tail = null;
        this.size = 0;
    }

    /**
     * Constructs new NumberListImpl by decimal number from file.
     * If file missing/empty/corrupt -> empty list.
     */
    public NumberListImpl(File file) {
        this.base = BASE_CURRENT;
        this.tail = null;
        this.size = 0;

        if (file == null || !file.exists() || !file.isFile()) return;

        String line = null;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            line = br.readLine();
        } catch (IOException ignored) {
        }

        if (line == null) return;
        line = line.trim();
        if (isNotValidDecimal(line)) return;

        BigInteger val = new BigInteger(line, 10);
        initFromBigInteger(val);
    }

    /**
     * Constructs new NumberListImpl by decimal number in string notation.
     * Invalid input -> empty list.
     */
    public NumberListImpl(String value) {
        this.base = BASE_CURRENT;
        this.tail = null;
        this.size = 0;

        if (value == null) return;
        String s = value.trim();
        if (isNotValidDecimal(s)) return;

        BigInteger val = new BigInteger(s, 10);
        initFromBigInteger(val);
    }

    /**
     * Saves the number into specified file in decimal scale.
     */
    public void saveList(File file) {
        if (file == null) return;

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            bw.write(toDecimalString());
        } catch (IOException ignored) {
        }
    }

    /**
     * Returns student's record book number (4 digits).
     */
    public static int getRecordBookNumber() {
        return RECORD_BOOK_NUMBER;
    }

    /**
     * Returns new NumberListImpl representing same number in extra base.
     * Original list is not modified.
     */
    public NumberListImpl changeScale() {
        BigInteger val = toBigIntegerOrZero();
        NumberListImpl res = new NumberListImpl(BASE_EXTRA);
        res.initFromBigInteger(val);
        return res;
    }

    /**
     * Additional operation for variant: OR (C7 = 6).
     * Does not modify operands.
     */
    public NumberListImpl additionalOperation(NumberList arg) {
        BigInteger a = toBigIntegerOrZero();
        BigInteger b = bigIntegerFromAnyNumberList(arg, this.base);

        BigInteger r = a.or(b);

        if (r.signum() < 0) r = BigInteger.ZERO;

        NumberListImpl res = new NumberListImpl(this.base);
        res.initFromBigInteger(r);
        return res;
    }

    /**
     * Decimal string representation of the number.
     */
    public String toDecimalString() {
        if (isEmpty()) return "";
        return toBigIntegerOrZero().toString(10);
    }

    @Override
    public String toString() {
        if (isEmpty()) return "";

        StringBuilder sb = new StringBuilder(size);
        Node cur = head();
        for (int i = 0; i < size; i++) {
            assert cur != null;
            int d = cur.v & 0xFF;
            sb.append(digitToChar(d, base));
            cur = cur.next;
        }

        if (base == 16) {
            return sb.toString().toUpperCase(Locale.ROOT);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumberListImpl other)) return false;

        if (this.base != other.base) return false;
        if (this.size != other.size) return false;

        Node a = this.head();
        Node b = other.head();
        for (int i = 0; i < this.size; i++) {
            assert a != null;
            assert b != null;
            if (a.v != b.v) return false;
            a = a.next;
            b = b.next;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 31 * base + size;
        Node cur = head();
        for (int i = 0; i < size; i++) {
            assert cur != null;
            h = 31 * h + cur.v;
            cur = cur.next;
        }
        return h;
    }

    @Override
    public int size() { return size; }

    @Override
    public boolean isEmpty() { return size == 0; }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Byte)) return false;
        byte x = (Byte) o;
        Node cur = head();
        for (int i = 0; i < size; i++) {
            assert cur != null;
            if (cur.v == x) return true;
            cur = cur.next;
        }
        return false;
    }

    @Override
    public Iterator<Byte> iterator() {
        return new Iterator<>() {
            private int idx = 0;
            private int lastRet = -1;

            @Override
            public boolean hasNext() { return idx < size; }

            @Override
            public Byte next() {
                if (!hasNext()) throw new NoSuchElementException();
                Byte v = get(idx);
                lastRet = idx;
                idx++;
                return v;
            }

            @Override
            public void remove() {
                if (lastRet < 0) throw new IllegalStateException();
                NumberListImpl.this.remove(lastRet);
                idx = lastRet;
                lastRet = -1;
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size];
        Node cur = head();
        for (int i = 0; i < size; i++) {
            assert cur != null;
            arr[i] = cur.v;
            cur = cur.next;
        }
        return arr;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a == null) throw new NullPointerException();
        int n = size;

        T[] out = a.length >= n ? a : (T[]) Array.newInstance(a.getClass().getComponentType(), n);

        Node cur = head();
        for (int i = 0; i < n; i++) {
            assert cur != null;
            out[i] = (T) Byte.valueOf(cur.v);
            cur = cur.next;
        }
        if (out.length > n) out[n] = null;
        return out;
    }

    @Override
    public boolean add(Byte e) {
        if (isNotValidDigit(e, base)) return false;

        Node n = new Node(e);
        if (tail == null) {
            n.next = n;
        } else {
            n.next = tail.next;
            tail.next = n;
        }
        tail = n;
        size++;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Byte) || tail == null) return false;
        byte x = (Byte) o;

        Node prev = tail;
        Node cur = tail.next;
        for (int i = 0; i < size; i++) {
            if (cur.v == x) {
                unlinkAfter(prev);
                return true;
            }
            prev = cur;
            cur = cur.next;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        for (Object x : c) if (!contains(x)) return false;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Byte> c) {
        if (c == null) throw new NullPointerException();
        boolean changed = false;
        for (Byte b : c) changed |= add(b);
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Byte> c) {
        if (c == null) throw new NullPointerException();
        rangeCheckForAdd(index);

        boolean changed = false;
        int i = index;
        for (Byte b : c) {
            add(i, b);
            i++;
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        boolean changed = false;
        for (Object x : c) {
            while (remove(x)) changed = true;
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        boolean changed = false;

        int i = 0;
        while (i < size) {
            Byte v = get(i);
            if (!c.contains(v)) {
                remove(i);
                changed = true;
            } else {
                i++;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        tail = null;
        size = 0;
    }

    @Override
    public Byte get(int index) {
        rangeCheck(index);
        return nodeAt(index).v;
    }

    @Override
    public Byte set(int index, Byte element) {
        if (element == null) throw new NullPointerException();
        if (isNotValidDigit(element, base)) throw new IllegalArgumentException("Digit out of base range");

        rangeCheck(index);
        Node n = nodeAt(index);
        byte old = n.v;
        n.v = element;
        return old;
    }

    @Override
    public void add(int index, Byte element) {
        if (element == null) throw new NullPointerException();
        if (isNotValidDigit(element, base)) throw new IllegalArgumentException("Digit out of base range");
        rangeCheckForAdd(index);

        if (index == size) {
            add(element);
            return;
        }

        if (tail == null) {
            Node n = new Node(element);
            n.next = n;
            tail = n;
            size = 1;
            return;
        }

        if (index == 0) {
            Node n = new Node(element);
            n.next = tail.next;
            tail.next = n;
            size++;
            return;
        }

        Node prev = nodeAt(index - 1);
        Node n = new Node(element);
        n.next = prev.next;
        prev.next = n;
        size++;
    }

    @Override
    public Byte remove(int index) {
        rangeCheck(index);
        if (tail == null) throw new IndexOutOfBoundsException();

        if (size == 1) {
            byte v = tail.v;
            tail = null;
            size = 0;
            return v;
        }

        if (index == 0) {
            Node head = tail.next;
            byte v = head.v;
            tail.next = head.next;
            size--;
            return v;
        }

        Node prev = nodeAt(index - 1);
        Node cur = prev.next;
        byte v = cur.v;
        prev.next = cur.next;
        if (cur == tail) tail = prev;
        size--;
        return v;
    }

    @Override
    public int indexOf(Object o) {
        if (!(o instanceof Byte)) return -1;
        byte x = (Byte) o;

        Node cur = head();
        for (int i = 0; i < size; i++) {
            assert cur != null;
            if (cur.v == x) return i;
            cur = cur.next;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!(o instanceof Byte)) return -1;
        byte x = (Byte) o;

        int last = -1;
        Node cur = head();
        for (int i = 0; i < size; i++) {
            assert cur != null;
            if (cur.v == x) last = i;
            cur = cur.next;
        }
        return last;
    }

    @Override
    public ListIterator<Byte> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<Byte> listIterator(int index) {
        rangeCheckForAdd(index);

        return new ListIterator<>() {
            private int cursor = index;
            private int lastRet = -1;

            @Override
            public boolean hasNext() { return cursor < size; }

            @Override
            public Byte next() {
                if (!hasNext()) throw new NoSuchElementException();
                Byte v = get(cursor);
                lastRet = cursor;
                cursor++;
                return v;
            }

            @Override
            public boolean hasPrevious() { return cursor > 0; }

            @Override
            public Byte previous() {
                if (!hasPrevious()) throw new NoSuchElementException();
                cursor--;
                lastRet = cursor;
                return get(cursor);
            }

            @Override
            public int nextIndex() { return cursor; }

            @Override
            public int previousIndex() { return cursor - 1; }

            @Override
            public void remove() {
                if (lastRet < 0) throw new IllegalStateException();
                NumberListImpl.this.remove(lastRet);
                if (lastRet < cursor) cursor--;
                lastRet = -1;
            }

            @Override
            public void set(Byte e) {
                if (lastRet < 0) throw new IllegalStateException();
                NumberListImpl.this.set(lastRet, e);
            }

            @Override
            public void add(Byte e) {
                NumberListImpl.this.add(cursor, e);
                cursor++;
                lastRet = -1;
            }
        };
    }

    @Override
    public List<Byte> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
        }
        NumberListImpl res = new NumberListImpl(this.base);
        for (int i = fromIndex; i < toIndex; i++) {
            res.add(get(i));
        }
        return res;
    }

    @Override
    public boolean swap(int index1, int index2) {
        if (index1 < 0 || index1 >= size || index2 < 0 || index2 >= size) return false;
        if (index1 == index2) return true;

        Node a = nodeAt(index1);
        Node b = nodeAt(index2);
        byte t = a.v;
        a.v = b.v;
        b.v = t;
        return true;
    }

    @Override
    public void sortAscending() {
        for (int i = 0; i < size - 1; i++) {
            int minIdx = i;
            byte minVal = get(i);
            for (int j = i + 1; j < size; j++) {
                byte v = get(j);
                if ((v & 0xFF) < (minVal & 0xFF)) {
                    minVal = v;
                    minIdx = j;
                }
            }
            if (minIdx != i) swap(i, minIdx);
        }
    }

    @Override
    public void sortDescending() {
        for (int i = 0; i < size - 1; i++) {
            int maxIdx = i;
            byte maxVal = get(i);
            for (int j = i + 1; j < size; j++) {
                byte v = get(j);
                if ((v & 0xFF) > (maxVal & 0xFF)) {
                    maxVal = v;
                    maxIdx = j;
                }
            }
            if (maxIdx != i) swap(i, maxIdx);
        }
    }

    @Override
    public void shiftLeft() {
        if (size <= 1 || tail == null) return;
        tail = tail.next;
    }

    @Override
    public void shiftRight() {
        if (size <= 1 || tail == null) return;
        Node prev = tail.next;
        while (prev.next != tail) prev = prev.next;
        tail = prev;
    }

    private Node head() {
        return tail == null ? null : tail.next;
    }

    private Node nodeAt(int index) {
        Node cur = head();
        for (int i = 0; i < index; i++) {
            assert cur != null;
            cur = cur.next;
        }
        return cur;
    }

    private void unlinkAfter(Node prev) {
        if (size == 0 || tail == null) return;

        Node target = prev.next;

        if (size == 1) {
            tail = null;
            size = 0;
            return;
        }

        prev.next = target.next;

        if (target == tail) {
            tail = prev;
        }
        size--;
    }

    private void rangeCheck(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
    }

    private void rangeCheckForAdd(int index) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException();
    }

    private static boolean isNotValidDecimal(String s) {
        if (s == null || s.isEmpty()) return true;
        if (s.charAt(0) == '-') return true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return true;
        }
        return false;
    }

    private static int baseFromC5(int c5) {
        return switch (c5) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 8;
            case 4 -> 16;
            default -> 10;
        };
    }

    private static boolean isNotValidDigit(Byte b, int base) {
        int v = b & 0xFF;
        return v >= base;
    }

    private static char digitToChar(int d, int base) {
        if (d < 0 || d >= base) return '?';
        if (d < 10) return (char) ('0' + d);
        return (char) ('A' + (d - 10));
    }

    private void initFromBigInteger(BigInteger val) {
        clear();

        if (val == null) return;
        if (val.signum() < 0) val = BigInteger.ZERO;

        String repr = val.toString(base);

        if (repr.isEmpty()) repr = "0";

        for (int i = 0; i < repr.length(); i++) {
            char c = repr.charAt(i);
            int digit = Character.digit(c, base);
            if (digit < 0) {
                clear();
                return;
            }

            add((byte) digit);
        }
    }

    private BigInteger toBigIntegerOrZero() {
        if (isEmpty()) return BigInteger.ZERO;

        BigInteger acc = BigInteger.ZERO;
        Node cur = head();
        for (int i = 0; i < size; i++) {
            assert cur != null;
            int d = cur.v & 0xFF;
            acc = acc.multiply(BigInteger.valueOf(base)).add(BigInteger.valueOf(d));
            cur = cur.next;
        }
        return acc;
    }

    private static BigInteger bigIntegerFromAnyNumberList(NumberList list, int assumeBase) {
        if (list == null) return BigInteger.ZERO;

        if (list instanceof NumberListImpl nli) {
            return nli.toBigIntegerOrZero();
        }

        BigInteger acc = BigInteger.ZERO;
        for (Byte b : list) {
            if (b == null) continue;
            int d = b & 0xFF;
            if (d >= assumeBase) continue;
            acc = acc.multiply(BigInteger.valueOf(assumeBase)).add(BigInteger.valueOf(d));
        }
        return acc;
    }
}
