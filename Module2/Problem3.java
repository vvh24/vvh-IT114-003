package Module2; // Important: the package corresponds to the folder it resides in
import java.util.Arrays;

// usage
// compile: javac Module2/Problem3.java
// run: java Module2.Problem3

public class Problem3 {
    public static void main(String[] args) {
        //Don't edit anything here
        Integer[] a1 = new Integer[]{-1, -2, -3, -4, -5, -6, -7, -8, -9, -10};
        Integer[] a2 = new Integer[]{-1, 1, -2, 2, 3, -3, -4, 5};
        Double[] a3 = new Double[]{-0.01, -0.0001, -.15};
        String[] a4 = new String[]{"-1", "2", "-3", "4", "-5", "5", "-6", "6", "-7", "7"};
        
        bePositive(a1);
        bePositive(a2);
        bePositive(a3);
        bePositive(a4);
    }
    // <T> turns this into a generic so it can take in any datatype, it'll be passed as an Object so casting is required
    static <T> void bePositive(T[] arr){
        System.out.println("Processing Array:" + Arrays.toString(arr));
        //your code should set the indexes of this array
        Object[] output = new Object[arr.length];
        //hint: use the arr variable; don't diretly use the a1-a4 variables
        //TODO convert each value to positive
        //set the result to the proper index of the output array and maintain the original data type
        //hint: don't forget to handle the data types properly, the result datatype should be the same as the original datatype
        for (int i = 0; i< arr.length; i++){ //vvh-09/23/24 - for loop goes through each element in the array until it reaches the last 
        //element and it increases i by 1 each time
            T element = arr[i]; //vvh-09/23/24 - this line picks out the current element from the array at position i and stores it in a 
            //variable called element 
            if (element instanceof Integer){ //vvh-09/23/24 - checks if the element is an int
                output[i] = Math.abs((Integer) element); //vvh-09/23/24 - if the element is an int, this makes it positive using Math.abs() and stores
                //it in the output array at the same position 
            }else if (element instanceof Double){ //vvh-09/23/24 - checks if element is a double
                output[i] = Math.abs((Double) element); //vvh-09/23/24 - if the element is a double, this makes it positive and stores it in the 
                //output array at the same index
            }else if (element instanceof String){ //vvh-09/23/24 - checks if element is a String
                int num = Integer.parseInt((String) element); //vvh-09/23/24 - this line converts the String into an Integer, so we can work with it as a number
                output[i] = String.valueOf(Math.abs(num));//vvh-09/23/24 - makes the num positive and converts it back into a string, and stores the output 
                //at the same index
            }else { //vvh-09/23/24 - if the element is not an int, double, or string
                output[i] = element; //vvh-09/23/24 - copy the element to the output array
            }
        }
        //end edit section

        StringBuilder sb = new StringBuilder();
        for(Object i : output){
            if(sb.length() > 0){
                sb.append(",");
            }
            sb.append(String.format("%s (%s)", i, i.getClass().getSimpleName().substring(0,1)));
        }
        System.out.println("Result: " + sb.toString());
    }
}