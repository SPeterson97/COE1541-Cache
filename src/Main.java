/*
    This file was created on 4/5/19 by Sam Peterson and Justin Anderson.
    The purpose of this file is to simulate a 2 level cache.
    More specifics can be read in the assignment file.
    This take in a stream of inputs and process them according to predefined
    specifications.
 */


import javax.sound.midi.SysexMessage;
import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

public class Main {

    public static int sizeL1;
    private static int sizeL2;
    public static int latencyL1;
    public static int latencyL2;
    private static int blockSize;
    private static int associativity;
    private static int writePolicy;
    private static int allocationPolicy;
    private static int outstandingMisses;
    public static int instructionCounter = 0;
    public static int[] accesses = {1, 10, 20, 30, 40, 50, 60,70, 80, 90, 100, 106, 110, 112};


    public static void run(int mode, int running){
        //create scanner to read in data
        Scanner in = new Scanner(System.in);

        //get all the data from the user
        if (mode == 1){
            try {
                Scanner preset = new Scanner(new File("preset.txt"));
                if (running == 1)
                    preset = new Scanner(new File("case1config.txt"));
                if (running == 2)
                    preset = new Scanner(new File("case2config.txt"));

                dataCollection(preset,1);
            }
            catch (Exception e){
                System.out.println("Problem with preset values");
                System.out.println(e);
            }
        }
        else
            dataCollection(in);

        //Initialize L1 and L2 caches with information
        L1Cache L1 = new L1Cache(sizeL1, latencyL1, blockSize, associativity,
                writePolicy, allocationPolicy, outstandingMisses);
        L2Cache L2 = new L2Cache(sizeL2, latencyL2, blockSize, associativity,
                writePolicy, allocationPolicy, outstandingMisses);
        //pass the L2 cache to L1, and L1 to L2
        L1.setL2(L2);
        L2.setL1(L1);

        //get file to process
        String filename = "test1.txt";
        if (running == 1)
            filename = "case1.txt";
        if (running == 2)
            filename = "case2.txt";

        //try to open file
        File newFile = null;
        Scanner inFile = null;
        try{
            newFile = new File(filename);
            inFile = new Scanner(newFile);
        }
        catch(Exception e){
            System.out.println("Can't find the file. Might not be in same directory.\n");
            System.exit(0);
        }

        if( running == 0)
            sequential(inFile, L1, L2);
        else
            accessUnderMiss(inFile, L1, L2);

    }

    public static void sequential(Scanner inFile, L1Cache L1, L2Cache L2){
        //variable to store total cycles
        double totalCycles = 0;
        int run = 1;

        //run collection of read/writes
        while (inFile.hasNextLine()){
            //read in next line and get address (binary) and type(0 = load, 1 = write)
            String line = inFile.nextLine();
            int type = getType(line);
            String instruction = getInstruction(line);

            if (type == 0)
                totalCycles += L1.read(instruction);
            else
                totalCycles += L1.write(instruction);

            totalCycles++;

            System.out.println(run+". L1 Cache: ");
            System.out.println(L1.toString());
            System.out.println("L2 Cache: ");
            System.out.println(L2.toString());
            System.out.println("");
            run++;

        }
        System.out.println("Total Cycles: "+totalCycles);
    }

    public static void accessUnderMiss(Scanner in, L1Cache L1, L2Cache L2){
        //Initialize L1 and L2 caches with information
        L1Cache dummyL1 = new L1Cache(sizeL1, latencyL1, blockSize, associativity,
                writePolicy, allocationPolicy, outstandingMisses);
        L2Cache dummyL2 = new L2Cache(sizeL2, latencyL2, blockSize, associativity,
                writePolicy, allocationPolicy, outstandingMisses);
        //pass the L2 cache to L1, and L1 to L2
        dummyL1.setL2(dummyL2);
        dummyL2.setL1(dummyL1);

        //initialize the run
        int cycles = 0;
        CacheEntry[] readInstructions = new CacheEntry[14];
        int[] types = new int[14];
        boolean notDone = true;
        int backup = 0;

        //go cycle by cycle
        while (notDone){
            cycles++;
            String currentInstruction = "";
            int type;
            String temp;

            //check to see if we can service any requests
            if (backup == outstandingMisses){
                //can't add anything else to the back up, need to see if we can execute a command
                // or if not, then store it in the back up (if need to read another command).

                //check if an instruction needs to be processed
                int index = process(cycles, readInstructions);
                if (index != -1){
                    //execute the command
                    if (types[index] == 0)
                        L1.read(readInstructions[index].getInstruction());
                    else
                        L2.write(readInstructions[index].getInstruction());
                    //System.out.print(index+". Instruction: "+readInstructions[index].getInstruction());
                    //System.out.println(" , Cycle: "+cycles);
                    System.out.println("Instruction number "+index+" is finishing at cycle: "+cycles);
                    backup--;

                    boolean readNext = next(cycles);
                    if (readNext) {
                        //System.out.println("Reaches Here");
                        //0 = read, 1 = write
                        temp = in.nextLine();
                        currentInstruction = getInstruction(temp);
                        type = getType(temp);
                        types[instructionCounter-1] = type;

                        //get latency for the instruction
                        int latency;
                        if (type == 0)
                            latency = dummyL1.read(currentInstruction);
                        else
                            latency = dummyL1.write(currentInstruction);

                        latency++;
                        //System.out.println("Latency1: "+dummyL2.latency);
                        //System.out.println("Full: "+latency);

                        //put the instruction in the list of instructions read
                        CacheEntry temp2 = new CacheEntry(currentInstruction, 0);
                        temp2.tag = L1.getTag(currentInstruction);

                        //get the cycle which the instruction will finish at
                        int newLat = waiting(latency, cycles, readInstructions, temp2);
                        System.out.println("Instruction "+(instructionCounter-1)+" will finish at: "+newLat);
                        temp2.cycles = newLat;

                        readInstructions[instructionCounter-1] = temp2;
                        backup++;

                        //instruction is now in the buffer
                    }
                }
                else {
                    //nothing can be removed from the queue
                    //lets check if we get another cache access or not
                    //if we do, lets continue to push it off until we can remove one from our
                    //outstanding misses
                    if (instructionCounter < 14 && accesses[instructionCounter] == cycles) {
                        //oh no, we have to push it back because we would access it now
                        //System.out.println(accesses[instructionCounter]);
                        accesses[instructionCounter]++;
                    }
                    else{
                        //dont have to read another access here, it's all good
                    }
                }
            }
            else{
                //see if another instruction has to be read
                boolean readNext = next(cycles);

                //need to read the next instruction
                if (readNext){
                    //0 = read, 1 = write
                    temp = in.nextLine();
                    currentInstruction = getInstruction(temp);
                    type = getType(temp);
                    types[instructionCounter-1] = type;

                    //get latency for the instruction
                    int latency;
                    if (type == 0)
                        latency = dummyL1.read(currentInstruction);
                    else
                        latency = dummyL1.write(currentInstruction);
                    latency++;
                    //System.out.println("Latency1: "+dummyL2.latency);
                    //System.out.println(latency);

                    //put the instruction in the list of instructions read
                    CacheEntry temp2 = new CacheEntry(currentInstruction, 0);
                    temp2.tag = L1.getTag(currentInstruction);

                    //get the cycle which the instruction will finish at
                    int newLat = waiting(latency, cycles, readInstructions, temp2);
                    System.out.println("Instruction "+(instructionCounter-1)+" will finish at: "+newLat);
                    temp2.cycles = newLat;

                    readInstructions[instructionCounter-1] = temp2;
                    backup++;
                    //instruction is now in the buffer
                }
                //do not need to read in another instruction
                //check if an instruction needs to be processed
                int index = process(cycles, readInstructions);
                if (index != -1){
                    //execute the command
                    if (types[index] == 0)
                        L1.read(readInstructions[index].getInstruction());
                    else
                        L2.write(readInstructions[index].getInstruction());
                    //System.out.print(index+". Instruction: "+readInstructions[index].getInstruction());
                    //System.out.println(" , Cycle: "+cycles);
                    System.out.println("Instruction number "+index+" is finishing at cycle: "+cycles);
                    backup--;
                }
                //dont need to process a command
            }
            notDone = !(instructionCounter == 14 && backup == 0);
        }
        System.out.println("Final Number of Cycles: "+cycles);
    }

    public static int waiting(int latency, int cycles, CacheEntry[] arr, CacheEntry entry){
        int maxi = -1;
        for (int i = 0; i< arr.length; i++){
            if (arr[i] != null) {
                if (cycles < arr[i].cycles && arr[i].tagEquals(entry.tag)) {
                    //number of cycles currently on is less than when the cache will finish
                    //the tags match, so its a hit
                    if (i > maxi)
                        maxi = i;
                }
            }
        }
        if (maxi == -1) {
            //this means that there is nothing blocking the cache from proceeding
            return cycles + latency;
        }
        //got the last usage of that item (single ported)
        if (arr[maxi].cycles == accesses[instructionCounter-1])
            arr[maxi].cycles++;

        return arr[maxi].cycles+latency;
    }

    public static void accessUnderMiss2(Scanner in, L1Cache L1, L2Cache L2){
        int cycles = 1;

        while (in.hasNextLine()){
            //read in next line and get address (binary) and type(0 = load, 1 = write)
            String line = in.nextLine();
            int type = getType(line);
            String instruction = getInstruction(line);




        }
    }

    public static int process(int cycle, CacheEntry[] instructions){
        int index = -1;
        for (int i = 0; i< instructions.length; i++){
            if (instructions[i] != null && cycle == instructions[i].cycles){
                return i;
            }
        }
        return index;
    }

    public static boolean next(int cycles){
        if (instructionCounter < 14 && cycles >= accesses[instructionCounter]){
            //means that we need to read another instruction now
            instructionCounter++;
            return true;
        }
        return false;
    }

    /**
     * Takes in the read line and returns whether it's a load or write
     * @param line
     * @return
     */
    public static int getType(String line){
        String[] temp = line.split(" ");
        if (temp[0].equals("read"))
            return 0;
        else if (temp[0].equals("write"))
            return 1;
        else{
            System.out.println("Invalid load/write access");
            System.exit(0);
        }
        return -1;
    }

    /**
     * Takes in the read line and returns the binary line
     * @param line
     * @return
     */
    public static String getInstruction(String line){
        String[] temp = line.split(" ");
        return temp[1];
    }

    /**
     * This function is used to gather all necessary data
     * for the initialization and running of the cache simulation
     */
    public static void dataCollection(Scanner in){
        /*
         *Data to retrieve:
         * Size of L1 and L2 cache
         * Access latency of L1 (L2 = L1 + 100)
         * Block size in bytes
         * Set associativity
         * Write policy: Write Back, Write Through, Write Evict
         * Allocation Policy: Write Allocate, Non-Write Allocate
         * Max Number of Outstanding Misses
         */
        String error = "";
        try{
            //obtain number of bytes
            System.out.print("Please configure the size of the L1 cache (2^X Bytes): ");
            sizeL1 = in.nextInt();
            System.out.println("");
            System.out.print("Please configure the size of the L2 cache (2^X Bytes): ");
            sizeL2 = in.nextInt();
            System.out.println("");

            //add some rules
            if (sizeL2 < sizeL1){
                error = "\nSize of L2 can't be less than L1";
                throw new InternalError();
            }
            else if (sizeL1 < 0 || sizeL2 < 0){
                error = "\nSize can't be less than 0.";
                throw new InternalError();
            }

            //obtain access latency
            System.out.print("Please configure the access latency for L1 cache: ");
            latencyL1 = in.nextInt();
            System.out.println("");
            System.out.print("Please configure the access latency for L2 cache: ");
            latencyL2 = in.nextInt();
            System.out.println("");

            //add some rules
            if (latencyL1 <= 0 || latencyL2 <= 0){
                error = "\nCan't have negative, or 0, latency";
                throw new InternalError();
            }

            //obtain block size
            System.out.print("Please configure the block size of the cache (2^Bytes): ");
            blockSize = in.nextInt();
            System.out.println("");

            //add some rules
            if (blockSize < 0){
                error = "\nSize can't be less than 0.";
                throw new InternalError();
            }

            //obtain associativity
            System.out.print("Please configure the associativity of the cache (2^Sets): ");
            associativity = in.nextInt();
            System.out.println("");

            //add some rules
            if (0 > associativity){
                error = "\nAssociativity can't be less than 0";
                throw new InternalError();
            }
            else if (associativity == 0)
                associativity = 1;

            //write policy
            System.out.print("Please configure the write policy: \n1. Write Back\n" +
                    "2. Write Through \n3. Write Evict\nEnter 1, 2, or 3: ");
            writePolicy = in.nextInt();
            System.out.println("");

            //add some rules
            if ( !(writePolicy <= 3 || writePolicy >= 1)){
                error = "\nWrite policy has to be either 1, 2, or 3.";
                throw new InternalError();
            }

            //allocation policy
            System.out.print("Please configure the allocation policy: \n1. Write Allocate\n" +
                    "2. Non-Write Allocate\nEnter 1 or 2: ");
            allocationPolicy = in.nextInt();
            System.out.println("");

            //add some rules
            if ( !(allocationPolicy <= 2 || allocationPolicy >= 1)){
                error = "\nAllocation policy has to be either 1 or 2.";
                throw new InternalError();
            }

            //obtain max outstanding misses
            System.out.print("Please configure the max number of outstanding misses(0 indicates serial execution): ");
            outstandingMisses = in.nextInt();
            System.out.println("");

            //add some rules
            if (0 > outstandingMisses){
                error = "\nOutstanding misses can't be less than 0";
                throw new InternalError();
            }


        }
        catch (NumberFormatException e){
            System.out.println("The value you entered is not accepted. Please try again.");
        }
        catch (InternalError e){
            System.out.println("The configuration you entered is not acceptable. Try again. "+ error);
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    public static void dataCollection(Scanner in, int mode){
        /*
         *Data to retrieve:
         * Size of L1 and L2 cache
         * Access latency of L1 (L2 = L1 + 100)
         * Block size in bytes
         * Set associativity
         * Write policy: Write Back, Write Through, Write Evict
         * Allocation Policy: Write Allocate, Non-Write Allocate
         * Max Number of Outstanding Misses
         */
        int i = 0;
        while(in.hasNextLine()){
            String line = in.nextLine();
            if (i == 0){
                sizeL1 = Integer.parseInt(line.split(" ")[1]);
                System.out.println("Size L1: "+sizeL1);
            }
            else if (i == 1) {
                sizeL2 = Integer.parseInt(line.split(" ")[1]);
                System.out.println("Size L2: "+sizeL2);
            }
            else if (i == 2) {
                latencyL1 = Integer.parseInt(line.split(" ")[1]);
                System.out.println("Latency L1: "+latencyL1);
            }
            else if (i == 3) {
                latencyL2 = Integer.parseInt(line.split(" ")[1]);
                System.out.println("Latency L2: "+latencyL2);
            }
            else if (i == 4) {
                blockSize = Integer.parseInt(line.split(" ")[1]);
                System.out.println("Blocksize: "+blockSize);
            }
            else if (i == 5) {
                associativity = Integer.parseInt(line.split(" ")[1]);
                System.out.println("Associativity: "+associativity);
            }
            else if (i == 6) {
                writePolicy = Integer.parseInt(line.split(" ")[1]);
                System.out.println("Write Policy: "+writePolicy);
            }
            else if (i == 7) {
                allocationPolicy = Integer.parseInt(line.split(" ")[1]);
                System.out.println("Allocation: "+allocationPolicy);
            }
            else if (i == 8) {
                outstandingMisses = Integer.parseInt(line.split(" ")[1]);
                System.out.println("Misses: "+outstandingMisses);
            }
            i++;
        }

    }

    /**
     * This function is used to convert base 10 numbers to binary.
     * @param num
     * @return String of binary
     */
    public static String toBinary(int num){
        StringBuilder result = new StringBuilder();

        while(num > 0) {
            int r = num % 2;
            num /= 2;
            result.append(Integer.toString(r));
        }

        return result.reverse().toString();
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

    public static void main(String[] args){
        run(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }
}
