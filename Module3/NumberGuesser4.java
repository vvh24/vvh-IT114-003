package Module3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class NumberGuesser4 {
    private int maxLevel = 1;
    private int level = 1;
    private int strikes = 0;
    private int maxStrikes = 5;
    private int number = -1;
    private boolean pickNewRandom = true;
    private Random random = new Random();
    private String fileName = "ng4.txt";
    private String[] fileHeaders = { "Level", "Strikes", "Number", "MaxLevel" };// used for demo readability

    private void saveState() {
        String[] data = { level + "", strikes + "", number + "", maxLevel + "" };
        String output = String.join(",", data);
        // Note: we don't need a file reference as FileWriter creates the file if it
        // doesn't exist
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(String.join(",", fileHeaders));
            fw.write("\n");// new line
            fw.write(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadState() {
        File file = new File(fileName);
        if (!file.exists()) {
            // Not providing output here as it's expected for a fresh start
            return;
        }
        try (Scanner reader = new Scanner(file)) {
            int lineNumber = 0;
            while (reader.hasNextLine()) {
                String text = reader.nextLine();
                // System.out.println("Text: " + text);
                if (lineNumber == 1) {
                    String[] data = text.split(",");
                    String level = data[0];
                    String strikes = data[1];
                    String number = data[2];
                    String maxLevel = data[3];
                    int temp = strToNum(level);
                    if (temp > -1) {
                        this.level = temp;
                    }
                    temp = strToNum(strikes);
                    if (temp > -1) {
                        this.strikes = temp;
                    }
                    temp = strToNum(number);
                    if (temp > -1) {
                        this.number = temp;
                        pickNewRandom = false;
                    }
                    temp = strToNum(maxLevel);
                    if (temp > -1) {
                        this.maxLevel = temp;
                    }
                }
                lineNumber++;
            }
        } catch (FileNotFoundException e) {// specific exception
            e.printStackTrace();
        } catch (Exception e2) {// any other unhandled exception
            e2.printStackTrace();
        }
        System.out.println("Loaded state");
        int range = 10 + ((level - 1) * 5);
        System.out.println("Welcome to level " + level);
        System.out.println(
                "I picked a random number between 1-" + (range) + ", let's see if you can guess.");
    }

    /***
     * Gets a random number between 1 and level.
     * 
     * @param level (level to use as upper bounds)
     * @return number between bounds
     */
    private void generateNewNumber(int level) {
        int range = 10 + ((level - 1) * 5);
        System.out.println("Welcome to level " + level);
        System.out.println(
                "I picked a random number between 1-" + (range) + ", let's see if you can guess.");
        number = random.nextInt(range) + 1;
    }

    private void win() {
        System.out.println("That's right!");
        level++;// level up!
        strikes = 0;
    }

    private boolean processCommands(String message) {
        boolean processed = false;
        if (message.equalsIgnoreCase("quit")) {
            System.out.println("Tired of playing? No problem, see you next time.");
            processed = true;
        }
        // TODO add other conditions here
        return processed;
    }

    private void lose() {
        System.out.println("Uh oh, looks like you need to get some more practice.");
        System.out.println("The correct number was " + number);
        strikes = 0;
        level--;
        if (level < 1) {
            level = 1;
        }
    }

    private void processGuess(int guess) {
        if (guess < 0) {
            return;
        }
        System.out.println("You guessed " + guess);
        if (guess == number) {
            win();
            pickNewRandom = true;
        } else {
            System.out.println("That's wrong");
            
        if (strikes < maxStrikes - 1){//vvh-09/30/24 - checks if number of wrong guesses is less than one less than the max allowed strikes 
            if (guess > number){//vvh-09/30/24 - checks if guess is higher than the correct number
                System.out.println("Try to guess a lower number, your guess it too high");//vvh-09/30/24 - if entered number was high, displays message as a hint to lower the number
            }else{//vvh-09/30/24 - if the first condition is not true, handles the case where the guess is less than or equal to the correct number
                System.out.println("Try to guess a higher number, your guess it too low");//vvh-09/30/24 - if entered number was low, displays message as a hint to higher the number
            }
        } 
            strikes++;
            if (strikes >= maxStrikes) {
                lose();
                pickNewRandom = true;
            }
        }
        saveState();
    }

    private int strToNum(String message) {
        int guess = -1;
        try {
            guess = Integer.parseInt(message.trim());
        } catch (NumberFormatException e) {
            System.out.println("You didn't enter a number, please try again");
        } catch (Exception e2) {
            System.out.println("Null message received");
        }
        return guess;
    }

    private void selectDifficulty() {//vvh-09/30/24 - Defines the method 
        Scanner input = new Scanner(System.in);//vvh-09/30/24 - Scanner implemented to take input
        boolean validInput = false;//vvh-09/30/24 - set to false since there's not input value yet 
    while(!validInput){ //vvh-09/30/24 - loop that will run until valid input is provided 
        System.out.println("Select level of difficulty: easy, medium, or hard"); //vvh-09/30/24 - Displays message asking to choose difficulty level
        String difficulty = input.nextLine().toLowerCase();//vvh-09/30/24 - Takes input and store it in the difficulty variable

        switch(difficulty){//vvh-09/30/24 - Checks the value of difficulty and based on the input, it will run a specific case
            case "easy"://vvh-09/30/24 - Checks if the input was easy
                maxStrikes = 10;//vvh-09/30/24 - set maxStrikes to 10 
                System.out.println("Easy mode is on. You have 10 strikes. Have fun!");//vvh-09/30/24 - displays message confirming that easy mode was selected
                validInput = true; //vvh-09/30/24 - input is valid, loop stops
                break;//vvh-09/30/24 - if the input was easy, the program will skip the other cases and exit the switch statement 

            case "medium"://vvh-09/30/24 - checks if the input was medium
                maxStrikes = 5;//vvh-09/30/24 - set maxStrikes to 5
                System.out.println("Medium mode is on. You have 5 strikes. Enjoy!");//vvh-09/30/24 - displays message confirming that medium mode was selected
                validInput = true; //vvh-09/30/24 - input is valid, loop stops
                break;//vvh-09/30/24 - if the input was medium, the program will skip the other cases and exit the switch statement 

            case "hard"://vvh-09/30/24 - checks if the input was hard 
                maxStrikes = 3;//vvh-09/30/24 - set maxStrikes to 3
                System.out.println("Hard mode is on. You have 3 strikes. Good luck!");//vvh-09/30/24 - displays message confirming that hard mode was selected
                validInput = true; //vvh-09/30/24 - input is valid, loop stops
                break;//vvh-09/30/24 - if the input was hard, the program will skip the other cases and exit the switch statement 
            default://vvh-09/30/24 - displys if invalid option was entered 
                System.out.println("Invalid choice. Please enter: 'easy', 'medium', 'hard'");//vvh-09/30/24 - displays if wrong or no input was entered
                break;//vvh-09/30/24 - stops the switch  
            }
        }
    }
    public void start() {
        try (Scanner input = new Scanner(System.in);) {
            System.out.println("Welcome to NumberGuesser4.0");
            System.out.println("To exit, type the word 'quit'.");
            
            selectDifficulty();//vvh-09/30/24 - calls method 

            loadState();
            do {
                if (pickNewRandom) {
                    generateNewNumber(level);
                    saveState();
                    pickNewRandom = false;
                }
                System.out.println("Type a number and press enter");
                // we'll want to use a local variable here
                // so we can feed it into multiple functions
                String message = input.nextLine();
                // early termination check
                if (processCommands(message)) {
                    // command handled; don't proceed with game logic
                    break;
                }
                // this is just to demonstrate we can return a value and pass it into another
                // method
                int guess = strToNum(message);
                processGuess(guess);
                // the following line is the same as the above two lines
                // processGuess(getGuess(message));
            } while (true);
        } catch (Exception e) {
            System.out.println("An unexpected error occurred. Goodbye.");
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        System.out.println("Thanks for playing!");
    }

    public static void main(String[] args) {
        NumberGuesser4 ng = new NumberGuesser4();
        ng.start();
    }
}