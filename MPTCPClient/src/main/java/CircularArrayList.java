/**
 * Created by rkanchib on 4/18/17.
 */
import java.util.*;

/**
 * If you use this code, please consider notifying isak at du-preez dot com
 *  with a brief description of your application.
 *
 * This is free and unencumbered software released into the public domain.
 *  Anyone is free to copy, modify, publish, use, compile, sell, or
 *  distribute this software, either in source code form or as a compiled
 *  binary, for any purpose, commercial or non-commercial, and by any
 *  means.
 */

public class CircularArrayList<E> {

    private final int n; // buffer length
    private final List<Integer> buf; // a List implementing RandomAccess
    private int head = 0;
    private int tail = 0;

    public CircularArrayList(int capacity) {
        n = capacity + 1;
        buf = new ArrayList<>(capacity);
    }

    public void add(int currentSize, E value){
        if(currentSize > this.size()){
            head = 0;
        }
        buf.add(head);
        head++;
    }

    public int get(int index){
        return buf.get(index);
    }

    public int size(){
        return buf.size();
    }
}