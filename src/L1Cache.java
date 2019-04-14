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
	public int LRUmax = 0;

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
        //check the appropriate index in each SA block.
        //if associativity = 1, look at col = 0
        //if associativity > 1, look at col % associativity == 0
        int index = getIndex(instruction);
        int instructionTag = getTag(instruction);

        if (snoop(instruction)){
            //if already in cache, return latency, update LRU and move on
            for (int i = 0; i< cols; i++){
                if ( i % associativity == 0 && cacheEntries[getIndex(instruction)][i].getTag() == getTag(instruction)){
                    //get lru
                    int lru = cacheEntries[getIndex(instruction)][i].getLRU();

                    //update lru
                    updateLRU(lru, 0);

                    return latency;
                }
            }
        }
        else {
            //see if the instruction is in L2Cache
            int latencyL2 = L2.read(instruction);
            int id = getIndex(instruction);

            //find the next block to replace
            int idx = nextEvict(cacheEntries[id]);

            //replace the block at the found index
            int lru = cacheEntries[id][idx].getLRU();

            //replace the blocks
            replace(cacheEntries[id], idx, instruction, false);

            //need to update lru of L1
            int action = 0;
            if (lru == -1)
                action = 1;
            updateLRU(lru, action);

            return latency + latencyL2;
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

    private int writeBack(String instruction){
		
        int idx = getIndex(instruction);
        int tag = getTag(instruction);
		
		//check for miss then hit, then write based on allocationPolicy
		if(!snoop(instruction)) {
			if(this.allocationPolicy == 1) {
				//write allocate miss - update main mem and bring block to cache
				
				//go to "mem"
				
				//value needs to be written back to L1
				for (int i = 0; i < cols; i++) {
					if ( i % associativity == 0 ){
						//get current LRU value
						int lru = cacheEntries[idx][i].getLRU();

						//found the match, need to evict
						replace(cacheEntries[idx], i, instruction, true);

						//need to update the LRU
						updateLRU(lru, -1);

						//no write to L2, on different pages
						return latency;
					}
				}
			}
		}
		else {
			//write allocate hit - writes to cache setting dirty bit, main mem not updated
			//non write allocate hit - writes to cache setting dirty bit, main mem not updated
			//only update L2 if evicting data
			
			for (int i = 0; i < cols; i++) {
				if ( i % associativity == 0 && cacheEntries[idx][i].getTag() == tag){
					//get current LRU value
					int lru = cacheEntries[idx][i].getLRU();

					//found the match, need to write through to L2
					replace(cacheEntries[idx], i, instruction, false);

					//need to update the LRU
					updateLRU(lru, 0);

					//need write back to L2 cache as well
					//existing data - tell L2 were evicting
					return latency + L2.write(instruction);
				}
			}
		}
    }

    private int writeThrough(String instruction){
		
		int idx = getIndex(instruction);
        int tag = getTag(instruction);
		
		//check for miss then hit, then write based on allocationPolicy
		if(!snoop(instruction)) {
			if(this.allocationPolicy == 1) {
				//write allocate miss - updates block in mem and brings back to cache
			}
			else if(this.allocationPolicy == 2) {
				//non write allocate miss - update main mem not bringing back to cache
			}
			
			return latency;
		}
		else {
			//write allocate hit - write to cache and main mem
			//non write allocate hit - write to cache and main mem
			
			for (int i = 0; i < cols; i++) {
				if ( i % associativity == 0 && cacheEntries[idx][i].getTag() == tag){
					//get current LRU value
					int lru = cacheEntries[idx][i].getLRU();

					//found the match, need to write through to L2
					replace(cacheEntries[idx], i, instruction, false);

					//need to update the LRU
					updateLRU(lru, 0);

					//need write through L2 cache as well
					//write through to memory as well
					return latency + L2.write(instruction);
				}
			}

		}
    }

    private int writeEvict(String instruction){
        int idx = getIndex(instruction);
        int tag = getTag(instruction);

        if (snoop(instruction)) {
            //value is in this cache, possibly in L2 as well
            for (int i = 0; i < cols; i++) {
                if ( i % associativity == 0 && cacheEntries[idx][i].getTag() == tag){
                    //get current LRU value
                    int lru = cacheEntries[idx][i].getLRU();

                    //found the match, need to evict
                    replace(cacheEntries[idx], i, instruction, true);

                    //need to update the LRU
                    updateLRU(lru, -1);

                    //need write evict from L2 cache as well
                    return latency + L2.write(instruction);
                }
            }
        }
        else{
            //need to check L2 as well. If not there, just write to mem
            return latency + L2.write(instruction);
        }
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

                    //don't update a non-used block
                    if (currentLRU < valueLRU){
                        ce.updateLRU(currentLRU + 1);
                    }
                }
                else if (action == 1){
                    //adding a block not already there
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
     * This function will find the lowest LRU cache block index
     * index.
     * @return
     */
    private int nextEvict(CacheEntry[] cacheRow){
        int maxLRU = -1;
        int index = -1;
        for (int i = 0; i < cacheRow.length; i++){
            if (i % associativity == 0 && cacheRow[i].getLRU() == -1){
                return i;
            }
            else if(i % associativity == 0 && cacheRow[i].getLRU() > maxLRU){
                maxLRU = cacheRow[i].getLRU();
                index = i;
            }
        }
        return index;
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

    /**
     * Use this function to set the L2 cache
     * @param L2
     */
    public void setL2(L2Cache L2){
        this.L2 = L2;
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
