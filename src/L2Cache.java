public class L2Cache {

    //store info about the cache
    public double size;
    public int latency;
    public int associativity;
    public double blockSize;
    public int writePolicy;
    public int allocationPolicy;
    public int outstandingMisses;

    //store the actual data
    public CacheEntry[][] cacheEntries;

    public L2Cache(int size, int latency, int blockSize, int associativity, int writePolicy,
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
        int entriesPerBlock = (int) (totalEntries/associativity);
        cacheEntries = new CacheEntry[associativity][entriesPerBlock];
    }

    /**
     * Use this function to query for the data. Return time to fetch
     * @param type
     * @param instruction
     * @return
     */
    public int get(String type, String instruction){
        return 0;
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
