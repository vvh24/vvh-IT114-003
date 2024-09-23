package Module2; // Important: the package corresponds to the folder it resides in
import java.util.Arrays;

// usage
// compile: javac Module2/Problem1.java
// run: java Module2.Problem1

public class Problem1 {
    public static void main(String[] args) {
        //Don't edit anything here
        int[] a1 = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] a2 = new int[]{0, 1, 3, 5, 7, 9, 2, 4, 6, 8, 10};
        int[] a3 = new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        int[] a4 = new int[]{0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10};
        
        processArray(a1);
        processArray(a2);
        processArray(a3);
        processArray(a4);
    }
    static void processArray(int[] arr){
        System.out.println("Processing Array:" + Arrays.toString(arr));
        System.out.println("Odds output:");
        //hint: use the arr variable; don't diretly use the a1-a4 variables
        // Goal: output only add *values* of each passed array
        //TODO add/edit code here
        for (int num : arr){ //vvh-09/22/24 - In this line, I used for loop bc everytime the loop runs, it will pick one number from the list 
        //and temporarily store it in a variable called num.
            if (num % 2 != 0){ //vvh-09/22/24 - then I proceed with an if statement to check the condition that checks whether the number num is 
            //odd or even by dividing num by 2 and look for the remainder. if the remainder is 0, the number is even and if the remainder is not 0
            //the number is odd. so, if num % 2 != 0, it means the number is odd. 
                System.out.println("The odd number value is:" + num); //vvh-09/22/24 - so the loop goes over the arrays and check if the number is odd. 
                //if the number is odd, it prints the number. if the number is not odd, nothing happens. 
            }
        }
        //end add/edit section
        System.out.println();
        System.out.println("End process");
    }
    
}