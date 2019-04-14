public class L2Cache {

    //define final values
    private int WRITE_BACK = 1;
    private int WRITE_THROUGH = 2;
    private int WRITE_EVICT = 3;
    private int READ_REPLACE = 0;
    private int ADDING_TO_CACHE = 1;
    private int REMOVING_FROM_CACHE = -1;

    //store info about the cache
    public double size;
    public int latency;
    public int associativity;
    public double blockSize;
    public int writePolicy;
    public int allocationPolicy;
    public int outstandingMisses;
    public int filledCache;

    //store the actual data
    public CacheEntry[][] cacheEntries;
    private int cols;
    private int indexes;
    private L1Cache L1;

    public L2Cache(int size, int latency, int blockSize, int associativity, int writePolicy,
                   int allocationPolicy, int outstandingMisses){
        this.size = Math.pow(2, size);
        this.latency = latency;
        this.associativity = associativity;
        this.blockSize = Math.pow(2, blockSize);
        this.writePolicy = writePolicy;
        this.allocationPolicy = allocationPolicy;
        this.outstandingMisses = outstandingMisses;
        this.filledCache = 0;

        //check if associativity is too great for this component
        double totalEntries = size/blockSize;
        indexes = (int) (totalEntries/associativity);
        cols = size/indexes;
        cacheEntries = new CacheEntry[indexes][size/indexes];

        //initialize all entries
        for(int i = 0; i < indexes; i++) {
            for (int j = 0; j < cols; j++) {
                cacheEntries[i][j] = new CacheEntry(null, 0);
            }
        }
    }

    /**
     * Use this function to query for the data. Return time to fetch
     * @param instruction
     * @return
     */
    public int read(String instruction){
        //get useful info
        int index = getIndex(instruction);
        int instructionTag = getTag(instruction);

        //check if it is in L2
        if (snoop(instruction)){
            //it is in L2, will only need to update LRU and return latency
            for (int i = 0; i< cols; i++){
                if ( i % associativity == 0 && cacheEntries[index][i].getTag() == instructionTag){
                    //get lru value of block to replace
                    int lru = cacheEntries[index][i].getLRU();

                    //update lru
                    updateLRU(lru, 0);

                    return latency;
                }
            }
        }
        else {
            //not in L2, must go to memory
            //find the next block to replace
            int idx = L1.nextEvict(cacheEntries[index]);

            //replace the block at the found index
            int lru = cacheEntries[index][idx].getLRU();

            //need to update lru of L2
            int action = 0;
            if (lru == -1)
                action = 1;
            updateLRU(lru, action);

            //replace the blocks
            replace(cacheEntries[index], idx, instruction, false);

            return latency + 100;
        }
        //should ever reach here
        return -1;
    }

    /**
     * Use this function to forward into the correct write policy
     * this allows main to call one write regardless of policy
     * @param instruction
     * @return
     */
    public int write(String instruction){
        if (writePolicy == WRITE_BACK){
            latency = writeBack(instruction);
        }
        else if (writePolicy == WRITE_THROUGH){
            latency = writeThrough(instruction);
        }
        else if (writePolicy == WRITE_EVICT){
            latency = writeEvict(instruction);
        }

        return latency;
    }

    public int writeBack(String instruction){
        return 0;
    }

    public int writeThrough(String instruction){
        return  0;
    }

    public int writeEvict(String instruction){
        //get index of instruction to evict
        int idx = getIndex(instruction);
        int tag = getTag(instruction);

        if (snoop(instruction)) {
            //value is in this cache, possibly in L2 as well
            for (int i = 0; i < cols; i++) {
                if ( i % associativity == 0 && cacheEntries[idx][i].getTag() == tag){
                    //get current LRU value
                    int lru = cacheEntries[idx][i].getLRU();

                    //need to update the LRU
                    updateLRU(lru, -1);

                    //found the match, need to evict
                    replace(cacheEntries[idx], i, instruction, true);

                    //will need to go to memory too to write
                    return latency + 100;
                }
            }
        }
        else{
            //not in l2, just need to write to memory
            return latency + 100;
        }
        //shouldn't reach here
        return -1;
    }

    /**
     * insert value of LRU that is being modified
     * all values above that will be decremented
     * all values below will be kept the same
     * @param valueLRU
     */
    private void updateLRU(int valueLRU, int action){
        //action = 0--> update on a read, replacing existing block(LRU of block replacing
        //action = 1 --> adding to cache, not in cache
        //action = -1 --> removing from cache
        int max = -1;
        for(CacheEntry[] ceArr : cacheEntries){
            for (CacheEntry ce : ceArr){
                if (action == 0){
                    int currentLRU = ce.getLRU();
                    //keep track of max LRU
                    if (currentLRU > max)
                        max = ce.getLRU();

                    if (currentLRU == -1){
                        //do nothing
                    }
                    else if (currentLRU < valueLRU){
                        ce.updateLRU(currentLRU + 1);
                    }
                }
                else if (action == 1){
                    int currentLRU = ce.getLRU();
                    //keep track of max LRU
                    if (currentLRU > max)
                        max = ce.getLRU();
                    //increment all LRUs
                    if (currentLRU != -1){
                        ce.updateLRU(currentLRU + 1);
                    }
                }
                else if (action ==-1){
                    int currentLRU = ce.getLRU();
                    //keep track of max LRU
                    if (currentLRU > max)
                        max = ce.getLRU() - 1;

                    //decrease all LRUs that are greater than
                    if (currentLRU > valueLRU){
                        ce.updateLRU(currentLRU-1);
                    }
                }
                else{
                    System.out.println("error, this case should never occur.");
                }
            }
        }
        //compare to filled size of cache
        if (max != filledCache){
            System.out.println("Max value of LRU is different than the filled cache variable.");
        }
    }

    /**
     * Use this function to replace a block of cache with new cache entries
     * If evict is true, then we are just replacing the block with an empty cache entry
     * @param cacheRow
     * @param index
     * @param instruction
     * @param evict
     */
    private void replace(CacheEntry[] cacheRow, int index, String instruction, boolean evict){
        for(int i = index; i< index+blockSize; i++){
            if (evict){
                //replace the spot with empty space
                cacheRow[i] = new CacheEntry(null, 0);
            }
            else{
                cacheRow[i] = new CacheEntry(instruction, i - index);
                cacheRow[i].updateValidBit(1);
                cacheRow[i].updateIndex(index);
                cacheRow[i].updateTag(getTag(instruction));
                cacheRow[i].updateLRU(0);
            }
        }
    }

    public void setL1(L1Cache l1){
        this.L1 = l1;
    }

    /**
     * Use this method to snoop the cache and see if it is present in the cache
     * This method will be useful when doing accesses under writes
     * @param instruction
     * @return
     */
    public boolean snoop(String instruction){
        for (int i = 0; i< cols; i++){
            if (i % associativity == 0 && cacheEntries[indexes][i].getTag() == getTag(instruction)){
                return true;
            }
        }
        return false;
    }

    public String parseBits(String instruction, int type){
        //offset, bytes per block log(bytes per block)
        //index, log(rows)
        //tag = remaining
        int offsetBits = (int) Math.log((int) blockSize);
        int indexBits = (int) Math.log(indexes);
        int tag = instruction.length() - (offsetBits + indexBits);

        //type: 1-tag, 2-index, 3-offset
        if (type == 1)
            return instruction.substring(0,tag);
        else if (type == 2)
            return instruction.substring(tag,offsetBits);
        else
            return instruction.substring(tag+indexBits);
    }

    public int getTag(String instruction){
        //tag = (memory word address)/(cache size in words)
        //cache size in words = size(bytes)/2
        return toDecimal(parseBits(instruction,1));
    }

    public int getBlockOffset(String instruction){
        //block offset = (word index)%(block size)
        return toDecimal(parseBits(instruction,3));
    }

    public int getIndex(String instruction){
        //block index = (word index)/(block size)
        return toDecimal(parseBits(instruction, 2));
    }

    /**
     * This function is used to convert a binary number to decimal.
     * This function will return an unsigned int.
     * @param binary
     * @return
     */
    public int toDecimal(String binary){
        //will check to see if the number starts with a 1, will add 0 to get positive value
        if (binary.charAt(0) == '1')
            binary = '0'+binary;
        return Integer.getInteger(binary, 2);
    }
}
