public class L1Cache {
    //define final values
    private int WRITE_BACK = 1;
    private int WRITE_THROUGH = 2;
    private int WRITE_EVICT = 3;
    private int READ_REPLACE = 0;
    private int ADDING_TO_CACHE = 1;
    private int REMOVING_FROM_CACHE = -1;

    //store details about cache
    public double size;
    public int latency;
    public int associativity;
    public double blockSize;
    public int writePolicy;
    public int allocationPolicy;
    public int outstandingMisses;
    public int filledCache;

    //store actual cache
    public CacheEntry[][] cacheEntries;
    public L2Cache L2;
    private int cols;
    private int indexes;

    public L1Cache(int size, int latency, int blockSize, int associativity, int writePolicy,
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
        indexes= (int) (totalEntries/associativity);
        cols = size/indexes;
        cacheEntries = new CacheEntry[indexes][cols];

        //initialize all entries
        for(int i = 0; i < indexes; i++) {
            for (int j = 0; j < cols; j++) {
                cacheEntries[i][j] = new CacheEntry(null);
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
        //if associativity = 1, look at col = 0
        //if associativity > 1, look at col % associativity == 0
        int index = getWordIndex(instruction);
        int instructionTag = getTag(instruction);

        if (associativity == 0){
            //compare when only col = 0;
            //make sure data is consistent across blocks
            if ( cacheEntries[index][0].tagEquals(instructionTag) ){
                //tags are equal, so cache hit
                //update LRU
                updateLRU(cacheEntries[index][0].getLRU(),READ_REPLACE);

                //set all blocks in that row to have LRU of 0
                for (int i = 0; i < blockSize; i++){
                    cacheEntries[index][i].updateLRU(READ_REPLACE);
                }
                //return time to complete read
                return latency;
            }
            else{
                //L1 miss, check L2
                //read needs to send back cache blocks and latency
                this.write(instruction);
                return latency + L2.read(instruction);
            }
        }
        else{
            //need to only read first in each way of associativity
            //need to check cols that satisfy col % associativity == 0
            //iterate through the cols
            for (int i = 0; i < cols; i++){
                if (i % associativity == 0){
                    //check tags
                    if( cacheEntries[index][i].getTag() == instructionTag ){
                        //match, L1 cache hit
                        //update LRU
                        updateLRU(cacheEntries[index][i].getLRU(),READ_REPLACE);

                        //set all blocks in that row to have LRU of 0
                        for (int j = 0; i < blockSize; i++){
                            cacheEntries[index][j].updateLRU(READ_REPLACE);
                        }
                        //return time to complete read
                        return latency;
                    }
                }
            }

            //no match was found
            //L1 miss, check L2
            //read needs to send back cache blocks and latency
            this.write(instruction);
            return latency + L2.read(instruction);
        }
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
        //index = (memory word address)%(cache size in words)
        return (toDecimal(instruction)/2)%(int)size;
    }

    public int getBlockOffset(String instruction){
        //block offset = (word index)%(block size)
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
        StringBuilder s = new StringBuilder();
        if (binary.charAt(0) == '1')
            s.append('0');
        s.append(binary);
        return Integer.parseInt(s.toString(), 2);
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
