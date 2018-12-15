package id.ac.ukdw.securedoor.utils;

import java.util.Random;

public class StringUtil {

    /**
     * Generate a 6 random generate number string
     *
     * @return String
     */
    public static String getRandomNumberString() {
        // It will generate 6 digit random Number.
        // from 0 to 999999
        Random rnd = new Random();
        int number = rnd.nextInt(999999);

        // this will convert any number sequence into 6 character.
        return String.format("%06d", number);
    }
}
