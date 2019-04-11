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
        int index = getIndex(instruction);
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
		
        int blockIndex = getIndex(instruction);
		int blockOffset = getBlockOffset(instruction);
		int tag = getTag(instruction);
		
		CacheEntry newData = new CacheEntry(instruction);
		CacheEntry currData = cacheEntries[blockIndex][blockOffset];
		
		//check for miss then hit, then write based on allocationPolicy
		if(tag != currData.tag || currData.validBit == 0) {
			if(this.allocationPolicy == 1) {
				//write allocate miss - update main mem and bring block to cache
				
				if(currData.validBit == 0) {
					//no data, adding to cache - not in cache
					updateLRU(0, 1);
					newData.updateLRU(0);
				}
				else {
					//data, replacing existing block
					updateLRU(currData.getLRU(), 0);
					newData.updateLRU(0);
				}
	
				newData.write();
				newData.dirtyBit = 1;
				//write to L2 - call read
				cacheEntries[blockIndex][blockOffset] = newData;
			}
			else if(this.allocationPolicy == 2) {
				//non write allocate miss - update main mem, NOT bring block to cache
				//update L2 ?
			}
		}
		else if(tag == currData.tag && currData.validBit == 1) {
			//write allocate hit - writes to cache setting dirty bit, main mem not updated
			//non write allocate hit - writes to cache setting dirty bit, main mem not updated
			
			if(currData.validBit == 0) {
				//no data, adding to cache - not in cache
				updateLRU(0, 1);
				newData.updateLRU(0);
			}
			else {
				//data, replacing existing block
				updateLRU(currData.getLRU(), 0);
				newData.updateLRU(0);
			}
			
			newData.write();
			newData.dirtyBit = 1;

			//call update LRU
			cacheEntries[blockIndex][blockOffset] = newData;
		}
		
        return latency;
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
     * This function will find the lowest LRU cache block index
     * index.
     * @return
     */
    private int nextEvict(CacheEntry[] cacheRow){
        int maxLRU = -1;
        int index = -1;
        for (int i = 0; i < cacheRow.length/associativity; i++){
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
