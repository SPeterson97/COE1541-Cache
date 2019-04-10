public class L1Cache {
    //define final values
    private int WRITE_BACK = 1;
    private int WRITE_THROUGH = 2;
    private int WRITE_EVICT = 3;

    //store details about cache
    public double size;
    public int latency;
    public int associativity;
    public double blockSize;
    public int writePolicy;
    public int allocationPolicy;
    public int outstandingMisses;
    public int entriesPerSA;

    //store actual cache
    public CacheEntry[][] cacheEntries;
    public L2Cache L2;
    private int cols;

    public L1Cache(int size, int latency, int blockSize, int associativity, int writePolicy,
                   int allocationPolicy, int outstandingMisses){
        this.size = Math.pow(2, size);
        this.latency = latency;
        this.associativity = associativity;
        this.blockSize = Math.pow(2, blockSize);
        this.writePolicy = writePolicy;
        this.allocationPolicy = allocationPolicy;
        this.outstandingMisses = outstandingMisses;

        //check if associativity is too great for this component
        double totalEntries = size/blockSize;
        entriesPerSA = (int) (totalEntries/associativity);
        cols = size/entriesPerSA;
        cacheEntries = new CacheEntry[entriesPerSA][size/entriesPerSA];

        //initialize all entries
        for(int i = 0; i < entriesPerSA; i++) {
            for (int j = 0; j < size/entriesPerSA; j++) {
                cacheEntries[i][j] = new CacheEntry();
            }
        }

    }

    /**
     * Use this function to query for the data. Return time to fetch
     * @param instruction
     * @return
     */
    public int read(String instruction){
        //check the appropriate index in each SA block.
        for (int i = 0; i < cols; i++){
            if ( i % associativity == 0 && cacheEntries[getWordIndex(instruction)][i].getLRU() != -1 &&
                    cacheEntries[getWordIndex(instruction)][i].tagEquals(getTag(instruction))){
                //hit, the cache entry with matching tags was found.
                //latency of this level of cache is returned
                //update LRU
                updateLRU(cacheEntries[i][getWordIndex(instruction)].getLRU());
                return latency;
            }
        }

        //was not found in L1 cache, must query L2 cache


        return 0;
    }

    public int write(String instruction){
        if (writePolicy == WRITE_BACK){

        }
        else if (writePolicy == WRITE_THROUGH){

        }
        else if (writePolicy == WRITE_EVICT){

        }

        return 0;
    }

    private int writeBack(String instruction){
        return 0;
    }

    private int writeThrough(String instruction){
        return 0;
    }

    private int writeEvict(String instruction){
        return 0;
    }

    /**
     * insert value of LRU that is being modified
     * all values above that will be decremented
     * all values below will be kept the same
     * @param valueLRU
     */
    private void updateLRU(int valueLRU){
        for(CacheEntry[] ceArr : cacheEntries){
            for (CacheEntry ce : ceArr){
                if (ce.getLRU() == -1) {
                    //skip
                }
                else{
                    if (ce.getLRU() > valueLRU)
                        ce.updateLRU(ce.getLRU() - 1);
                }
            }
        }
    }

    /**
     * This function will find the lowest LRU and return the a and b cacheEntries[a][b]
     * index. The rest of the LRUs will be updated accordingly
     * @return
     */
    private int[] nextEvict(){
        //need to consider row that's empty
        int[] a = {0,2};
        return a;
    }

    /**
     * Use this function to set the L2 cache
     * @param L2
     */
    public void setL2(L2Cache L2){
        this.L2 = L2;
    }

    public int getTag(String instruction){
        //tag = (memory word address)/(cache size in words)
        //cache size in words = size(bytes)/2
        return (toDecimal(instruction)/2)/(int)size;
    }

    public int getWordIndex(String instruction){
        //tag = (memory word address)%(cache size in words)
        return (toDecimal(instruction)/2)%(int)size;
    }

    public int getBlockOffset(String instruction){
        //block index = (word index)%(block size)
        return getWordIndex(instruction)%(int)blockSize;
    }

    public int getBlockIndex(String instruction){
        //block index = (word index)/(block size)
        return getWordIndex(instruction)/(int)blockSize;
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

    /**
     * This function is used to convert base 10 numbers to binary.
     * @param num
     * @return String of binary
     */
    public String toBinary(int num){
        StringBuilder result = new StringBuilder();

        while(num > 0) {
            int r = num % 2;
            num /= 2;
            result.append(Integer.toString(r));
        }

        return result.reverse().toString();
    }
}
