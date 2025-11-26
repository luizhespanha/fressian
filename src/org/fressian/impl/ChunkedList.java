package org.fressian.impl;

/**
 * A list implementation that grows in fixed-size chunks, avoiding the
 * copy-on-grow behavior of ArrayList.
 * 
 * For a list of N elements:
 * - ArrayList allocates ~3.2N slots total (due to repeated copying during growth)
 * - ChunkedList allocates ~N slots total (just the chunks, no copying)
 * 
 * This results in ~70% reduction in allocation overhead for large lists.
 */
public class ChunkedList {
    // Use power of 2 for fast division/modulo via bit operations
    private static final int CHUNK_BITS = 10;  // 2^10 = 1024
    private static final int CHUNK_SIZE = 1 << CHUNK_BITS;  // 1024
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;   // 0x3FF
    
    private Object[][] chunks;
    private int size;
    
    public ChunkedList() {
        // Start with space for 16 chunks (16K elements)
        // The chunks array itself is small (16 * 8 bytes = 128 bytes)
        this.chunks = new Object[16][];
        this.size = 0;
    }
    
    /**
     * Add an element to the list. O(1) amortized.
     * Unlike ArrayList, this never copies existing elements.
     */
    public void add(Object element) {
        int chunkIndex = size >> CHUNK_BITS;      // size / CHUNK_SIZE
        int indexInChunk = size & CHUNK_MASK;     // size % CHUNK_SIZE
        
        // Need a new chunk?
        if (indexInChunk == 0) {
            ensureChunkCapacity(chunkIndex);
            chunks[chunkIndex] = new Object[CHUNK_SIZE];
        }
        
        chunks[chunkIndex][indexInChunk] = element;
        size++;
    }
    
    /**
     * Ensure we have space for the chunk at the given index.
     * Doubles the chunks array if needed (rare operation).
     */
    private void ensureChunkCapacity(int chunkIndex) {
        if (chunkIndex >= chunks.length) {
            Object[][] newChunks = new Object[chunks.length * 2][];
            System.arraycopy(chunks, 0, newChunks, 0, chunks.length);
            chunks = newChunks;
        }
    }
    
    /**
     * Convert to Object[]. This is the only copy operation.
     * Uses System.arraycopy for efficiency.
     */
    public Object[] toArray() {
        Object[] result = new Object[size];
        
        int fullChunks = size >> CHUNK_BITS;  // number of full chunks
        int remainder = size & CHUNK_MASK;     // elements in last partial chunk
        
        // Copy full chunks
        int destPos = 0;
        for (int i = 0; i < fullChunks; i++) {
            System.arraycopy(chunks[i], 0, result, destPos, CHUNK_SIZE);
            destPos += CHUNK_SIZE;
        }
        
        // Copy last partial chunk (if any)
        if (remainder > 0) {
            System.arraycopy(chunks[fullChunks], 0, result, destPos, remainder);
        }
        
        return result;
    }
    
    /**
     * Get element at index. O(1).
     */
    public Object get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return chunks[index >> CHUNK_BITS][index & CHUNK_MASK];
    }
    
    public int size() {
        return size;
    }
}

