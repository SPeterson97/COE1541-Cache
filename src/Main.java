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
import java.util.Scanner;

public class Main {

    public static int sizeL1;
    private static int sizeL2;
    public static int latency;
    private static int blockSizeL1;
    private static int blockSizeL2;
    private static int associativityL1;
    private static int associativityL2;
    private static int writePolicy;
    private static int allocationPolicy;
    private static int outstandingMisses;


    public static void run(){
        //create scanner to read in data
        Scanner in = new Scanner(System.in);

        //get all the data from the user
        dataCollection(in);

        //Initialize L1 and L2 caches with information
        L1Cache L1 = new L1Cache(sizeL1, latency, blockSizeL1, associativityL1,
                writePolicy, allocationPolicy, outstandingMisses);
        L2Cache L2 = new L2Cache(sizeL2, latency+100, blockSizeL2, associativityL2,
                writePolicy, allocationPolicy, outstandingMisses);
        //pass the L2 cache to L1, and L1 to L2
        L1.setL2(L2);
        L2.setL1(L1);

        //get file to process
        System.out.println("Please enter the filename of cache accesses: ");
        String filename = in.nextLine();
        System.out.println("");

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

        //variable to store total cycles
        double totalCycles = 0;

        //run collection of read/writes
        while (inFile.hasNextLine()){
            //read in next line and get address (binary) and type(0 = load, 1 = write)
            String line = inFile.nextLine();
            int type = getType(line);
            String instruction = getInstruction(line);


        }
    }

    /**
     * Takes in the read line and returns whether it's a load or write
     * @param line
     * @return
     */
    public static int getType(String line){
        String[] temp = line.split(" ");
        if (temp[0].equals("load"))
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
            System.out.print("Please configure the access latency for L1 cache (L2 cache = L1 + 100): ");
            latency = in.nextInt();
            System.out.println("");

            //add some rules
            if (latency <= 0){
                error = "\nCan't have negative, or 0, latency";
                throw new InternalError();
            }

            //obtain block size
            System.out.print("Please configure the block size of the L1 cache (2^Bytes): ");
            blockSizeL1 = in.nextInt();
            System.out.println("");
            System.out.print("Please configure the block size of the L2 cache (2^Bytes): ");
            blockSizeL2 = in.nextInt();
            System.out.println("");

            //add some rules
            if (blockSizeL2 < blockSizeL1){
                error = "\nBlock size of L2 can't be less than L1";
                throw new InternalError();
            }
            else if (blockSizeL1 < 0 || blockSizeL2 < 0){
                error = "\nSize can't be less than 0.";
                throw new InternalError();
            }

            //obtain associativity
            System.out.print("Please configure the associativity of L1 cache (2^Sets): ");
            associativityL1 = in.nextInt();
            System.out.println("");
            System.out.print("Please configure the associativity of L2 (2^Sets): ");
            associativityL2 = in.nextInt();
            System.out.println("");

            //add some rules
            if (0 > associativityL1){
                error = "\nAssociativity can't be less than 0";
                throw new InternalError();
            }
            else if (associativityL1 == 0)
                associativityL1 = 1;

            //add some rules
            if (0 > associativityL2){
                error = "\nAssociativity can't be less than 0";
                throw new InternalError();
            }
            else if (associativityL2 == 0)
                associativityL2 = 1;

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
        run();
    }
}
