public class L1Cache {

    public double size;
    public int latency;
    public int associativity;

    public L1Cache(int size, int latency, int associativity){
        this.size = Math.pow(2, size);
        this.latency = latency;
        this.associativity = associativity;
    }
}
