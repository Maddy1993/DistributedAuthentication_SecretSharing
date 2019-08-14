package com.northeastern.edu.secretSharing;

import javafx.util.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

/**
 * Class prepares and reconstructs the secret keys
 */
public class SecretSharing {
    //Logger for the class.
    private static Logger LOGGER = Logger.getLogger(SecretSharing.class.getName());

    private static String secretString;
    private static BigInteger secret; // s
    private static int n = 5;  // NUM_SHARES
    private static int k = 3; // NUM_SUBSET_REQUIRED

    /**
     * Preparation phase of Secret Sharing
     * 1.) Randomly obtain k-1 numbers
     * 2.) Construct polynomial
     * 3.) Construct n points
     * 4.) Create list of n keys containing point and value p
     *
     * @param s
     */
    public static List<Key> preparation(String s) {
        secretString = s;
        // Convert input secret into BigInt format
        secret = new BigInteger(1, s.getBytes());
        System.out.println("Secret: " + secret);

        // obtain k - 1 random numbers (a1, a2, a3, etc.) to construct polynomial:
        // f(x) = a0 + a1x + a2x^2 + ...
        List<BigInteger> coefficients = getCoefficients();

        // Generate list of Keys
        return generateKeys(coefficients);
    }

    /**
     * Reconstruction phase of Secret Sharing.
     * 1.) Need k keys to reconstruct secret -> ELSE reconstruction fails
     * 2.) Reconstruct polynomial using Lagrange Polynomial Interpolation
     * 3.) Solve for constant value to obtain original secret
     * 4.) If reconstructd constant does not match original secret -> reconstruction fails
     *
     * @param clientKeyList
     */
    public static boolean reconstruction(List<Key> clientKeyList) {
        // Does client have enough keys to reconstruct secret?
        if (clientKeyList.size() != k) {
            return false;
        }

        // Calculate Lagrange Basis Polynomials l(x)
        List<Integer> lagrangeBasisPoly = new ArrayList<>();
        BigDecimal y = BigDecimal.ZERO;
        for (int j = 0; j < clientKeyList.size(); j++) {
            System.out.println("x" + j + " " + clientKeyList.get(j).getPoint().getKey());
            System.out.println("y" + j + " " + clientKeyList.get(j).getPoint().getValue());

            BigDecimal xj = BigDecimal.valueOf(clientKeyList.get(j).getPoint().getKey());
            BigDecimal yj = new BigDecimal(clientKeyList.get(j).getPoint().getValue());
            //int l = 1;
            BigDecimal l = BigDecimal.ONE;
            //int x = 0;
            BigDecimal x = BigDecimal.ZERO;

            // Calculate the constants in the polynomial for each point and sum them together
            for (int m = 0; m < clientKeyList.size(); m++) {
                BigDecimal xm = BigDecimal.valueOf(clientKeyList.get(m).getPoint().getKey());
                if (j != m) {
                    // Lagrange Basis: l = l * ((x - xm) / (xj - xm));
                    BigDecimal numerator = x.subtract(xm).setScale(12, BigDecimal.ROUND_HALF_UP);
                    BigDecimal denominator = xj.subtract(xm).setScale(12, BigDecimal.ROUND_HALF_UP);
                    BigDecimal result = numerator.divide(denominator, BigDecimal.ROUND_HALF_UP);
                    l = l.multiply(result);
                }
            }
            y = y.add(yj.multiply(l));
        }
        System.out.println("Y: " + y);

        // Round big decimal Y
        y = y.setScale(0, BigDecimal.ROUND_HALF_UP);
        System.out.println("Yrounded: " + y);

        // Convert y to BigInteger for comparison to secret (BigInteger)
        BigInteger yConverted = y.toBigInteger();

        System.out.println("Y big Integer: " + yConverted);

        // Check that constructd Y value is equal to Secret value
        if (yConverted.equals(secret)) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Creates the polynomial to the k-1 term: f(x) = a0 + a1x + a2x^2 + ... + (ak-1(x^k-1
     *
     * @return list of the coeficients in the polynomial, where a0 = value of secret
     */
    private static List<BigInteger> getCoefficients() {
        Set<BigInteger> setBigInt = new LinkedHashSet<>();
        // a0 = secret big integer value
        setBigInt.add(secret);

        BigInteger bigInteger = secret;// upper limit
        BigInteger min = BigInteger.ONE;// lower limit
        BigInteger bigInteger1 = bigInteger.subtract(min);
        Random rnd = new Random();
        int maxNumBitLength = bigInteger.bitLength();

        BigInteger aRandomBigInt;

        // Randomly choose coefficients between 1 and value of secret
        while (setBigInt.size() < k) {
            aRandomBigInt = new BigInteger(maxNumBitLength, rnd);
            if (aRandomBigInt.compareTo(min) < 0)
                aRandomBigInt = aRandomBigInt.add(min);
            if (aRandomBigInt.compareTo(bigInteger) >= 0)
                aRandomBigInt = aRandomBigInt.mod(bigInteger1).add(min);
            setBigInt.add(aRandomBigInt);
            System.out.println("Coef: " + aRandomBigInt);
        }

        // Convert Set to List to allow for iteration by index
        List<BigInteger> coefsList = new ArrayList<BigInteger>(setBigInt);

        return coefsList;

    }

    /**
     * Constructs n points from the polynomial
     *
     * @return List of n points along the polynomial
     */
    private static List<Key> generateKeys(List<BigInteger> coefs) {
        List<Key> keys = new LinkedList<>();
        Key key;
        // f(x) = a0 + a1x + a2x^2 + ...
        // Calculates points for when x = 1, 2, ...n
        for (int i = 1; i <= n; i++) {
            BigInteger x = BigInteger.valueOf(i);
            // create key with values point and p
            // Pair<x,y<, where y = f(x) mod p
            //key = new Key(new Pair<>(i, getY(coefs, x).mod(p)), p);
            key = new Key(new Pair<>(i, getY(coefs, x)));
            keys.add(key);
        }
        return keys;
    }

    /**
     * Calculates the Y value for a given X from the polynomial f(x)
     *
     * @param coefs
     * @param x
     * @return y value from f(x)
     */
    private static BigInteger getY(List<BigInteger> coefs, BigInteger x) {
        //f(x) = a0 + a1x + a2x^2 + ...anx^n
        BigInteger y = BigInteger.ZERO;

        for (int i = 0; i < coefs.size(); i++) {
            BigInteger xp = x.pow(i);
            y = y.add(coefs.get(i).multiply(xp));
        }

        return y;
    }

    public static void main(String[] args) {
        List<Key> keys = preparation("dog");

        // get random 3 out of 5 keys
        HashSet<Integer> subsetKeys = new LinkedHashSet<>();
        while (subsetKeys.size() < 3) {
            Random r = new Random();
            subsetKeys.add(r.nextInt(n));  // [0...n-1])
        }
        List<Key> subsetKeyList = new ArrayList<>();

        for (Integer ind : subsetKeys) {
            subsetKeyList.add(keys.get(ind));
        }

        System.out.println("Subset key list size: " + subsetKeyList.size());


        System.out.println(reconstruction(subsetKeyList));
    }

}
