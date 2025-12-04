package org.fressian.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * A thread-local pool of ArrayList instances for reuse during Fressian deserialization.
 * 
 * This pool uses a stack-based approach to handle recursive calls safely:
 * - Each call to acquire() pops an ArrayList from the pool (or creates a new one)
 * - Each call to release() pushes the ArrayList back to the pool
 * - Nested/recursive calls get separate ArrayList instances
 * 
 * After warmup, this achieves near-zero allocation for list building,
 * only allocating the final Object[] array in toArray().
 * 
 * Memory leak prevention:
 * - Pool size is limited to MAX_POOL_SIZE (8 by default, enough for deep recursion)
 * - ArrayLists larger than MAX_CACHED_CAPACITY are not returned to pool
 * - This prevents a single large list from permanently consuming memory
 */
public class ListBufferPool {
    
    // Maximum number of ArrayLists to keep in the pool per thread
    private static final int MAX_POOL_SIZE = 8;
    
    // Don't cache ArrayLists that grew beyond this size (to prevent memory leaks)
    private static final int MAX_CACHED_CAPACITY = 64 * 1024;  // 64K elements
    
    // Initial capacity for new ArrayLists
    private static final int INITIAL_CAPACITY = 64;
    
    private static final ThreadLocal<ArrayDeque<ArrayList<Object>>> pool = 
        ThreadLocal.withInitial(ArrayDeque::new);
    
    /**
     * Acquires an ArrayList from the pool, or creates a new one if pool is empty.
     * The returned ArrayList is guaranteed to be empty.
     */
    public static ArrayList<Object> acquire() {
        ArrayDeque<ArrayList<Object>> localPool = pool.get();
        ArrayList<Object> list = localPool.pollFirst();
        if (list == null) {
            list = new ArrayList<>(INITIAL_CAPACITY);
        }
        return list;
    }
    
    /**
     * Releases an ArrayList back to the pool for reuse.
     * The ArrayList is cleared before being returned to the pool.
     * 
     * Large ArrayLists (size > MAX_CACHED_CAPACITY before clear) are not cached
     * to prevent memory leaks from occasional large lists.
     */
    public static void release(ArrayList<Object> list) {
        // Check size BEFORE clearing - this is our proxy for capacity
        // If the list held > 64K elements, it has capacity >= 64K
        // Don't cache it to prevent memory leak
        int sizeBeforeClear = list.size();
        list.clear();
        
        if (sizeBeforeClear > MAX_CACHED_CAPACITY) {
            // Let GC collect this large ArrayList
            return;
        }
        
        ArrayDeque<ArrayList<Object>> localPool = pool.get();
        if (localPool.size() < MAX_POOL_SIZE) {
            localPool.addFirst(list);
        }
        // else: let GC collect it
    }
    
    /**
     * Clears the pool for the current thread.
     * Useful for testing or when you want to release memory.
     */
    public static void clear() {
        pool.get().clear();
    }
}

