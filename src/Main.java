public class Main {
    public static void main(String [] args){
        int i = 0;
        for (;i<5;i++){
            System.out.println(toBinary(i));
        }
    }

    public static String toBinary(int num){
        StringBuilder result = new StringBuilder();

        while(num > 0) {
            int r = num % 2;
            num /= 2;
            result.append(Integer.toString(r));
        }
        //System.out.println(result.reverse().toString());
        return result.reverse().toString();
    }
}
