/*
    This file was created on 4/5/19 by Sam Peterson and Justin Anderson.
    The purpose of this file is to simulate a 2 level cache.
    More specifics can be read in the assignment file.
    This take in a stream of inputs and process them according to predefined
    specifications.
 */

import com.sun.jdi.InternalException;

import java.util.Scanner;

public class Main {

    public int sizeL1;
    private int sizeL2;
    public int latency;
    private int blockSizeL1;
    private int blockSizeL2;
    private int associativityL1;
    private String error;
    private int writePolicy;


    public void run(){
        //get all the data from the user
        dataCollection();

        //Initialize L1 and L2 caches with information


    }

    /**
     * This function is used to gather all necessary data
     * for the initialization and running of the cache simulation
     */
    public void dataCollection(){
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
        error = "";
        try{
            Scanner in = new Scanner(System.in);

            //obtain number of bytes
            System.out.print("Please configure the size of the L1 cache (2^Bytes): ");
            sizeL1 = in.nextInt();
            System.out.println("");
            System.out.print("Please configure the size of the L2 cache (2^Bytes): ");
            sizeL2 = in.nextInt();
            System.out.println("");

            //add some rules
            if (sizeL2 < sizeL1){
                error = "\nSize of L2 can't be less than L1";
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

            //obtain associativity
            System.out.print("Please configure the associativity of L1 and L2 cache (2^Sets): ");
            associativityL1 = in.nextInt();
            System.out.println("");

            //add some rules
            if (0 > associativityL1){
                error = "\nAssociativity can't be less than 0";
                throw new InternalError();
            }

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

        }
        catch (NumberFormatException e){
            System.out.println("The value you entered is not accepted. Please try again.");
        }
        catch (InternalError e){
            System.out.println("The configuration you entered is not acceptable. Try again. "+error);
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

    public void main(String[] args){
        run();
    }
}
