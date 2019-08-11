import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javafx.util.Pair;

/**
 * Class that prepares ad reconstructs the secret keys
 */
public class SecretSharing {
    private static String secretString;
    private static BigInteger secret; // s
    private static int n = 5;  // NUM_SHARES
    private static int k = 3; // NUM_SUBSET_REQUIRED
    private static BigInteger p; //field size
    //private static int fx; // polynomial

//    public SecretSharing(String secret) {
//        this.secret = secret;
//    }

    //Prepartion:
//    1.) Randomly obtain k-1 numbers
//    2.) Construct polynomial
//    3.) Construct n points
//    4.) Create list of n keys containing point and value p

    // Reconstruction:
//    1.) Need k keys to reconstruct secret
//    2.) Reconstruct polynomial using Lagrange Polynomial Interpolation

    /**
     * Preparation phase of Secret Sharing
     *  1.) Randomly obtain k-1 numbers
     *  2.) Construct polynomial
     *  3.) Construct n points
     *  4.) Create list of n keys containing point and value p
     * @param s
     */
    public static List<Key> preparation(String s) {
        secretString = s;
        // Convert input secret into BigInt format
        secret = new BigInteger(1, s.getBytes());
        //secret = BigInteger.valueOf(1234);
        System.out.println("Secret: " + secret);


        // obtain k - 1 random numbers (a1, a2, a3, etc.) to construct polynomial:
        // f(x) = a0 + a1x + a2x^2 + ...
        List <BigInteger> coefs = getCoefficients();

        // Generate field size, p
        p = getFieldSize();

        // Generate list of Keys
        return generateKeys(coefs, p);
    }

    public static void reconstruction(List<Key> clientKeyList) {
        // Does client have enough keys to reconstruct secret?
        if (clientKeyList.size() != k) {
            //return "Insufficient key set.";
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
            for (int m = 0; m < clientKeyList.size(); m++) {
                BigDecimal xm = BigDecimal.valueOf(clientKeyList.get(m).getPoint().getKey());
                if (j != m) {
//                    l = l * ((x - xm) / (xj - xm));
                    BigDecimal numerator = x.subtract(xm).setScale(12, BigDecimal.ROUND_HALF_UP);
                    BigDecimal denominator = xj.subtract(xm).setScale(12, BigDecimal.ROUND_HALF_UP);
                    BigDecimal result = numerator.divide(denominator, BigDecimal.ROUND_HALF_UP);
                            //(numerator.divide(denominator.setScale(12, BigDecimal.ROUND_HALF_UP)));
                    l = l.multiply(result);

                }
            }
            y = y.add(yj.multiply(l));
        }
        //y = y.mod(p);
        System.out.println("Y: " + y);
        y = y.setScale(0, BigDecimal.ROUND_HALF_UP);
        System.out.println("Yrounded: " + y);
        BigInteger ymod = y.toBigInteger().mod(p);
        System.out.println("Y mod: " + ymod);
        // Convert y back to String secret
        //return y;
    }

    private static List<BigInteger> getCoefficients() {
        Set<BigInteger> setBigInt = new LinkedHashSet<>();
        // a0 = secret big int value
        setBigInt.add(secret);

        BigInteger bigInteger = secret;// upper limit
        BigInteger min = BigInteger.ONE;// lower limit
        BigInteger bigInteger1 = bigInteger.subtract(min);
        Random rnd = new Random();
        int maxNumBitLength = bigInteger.bitLength();

        BigInteger aRandomBigInt;


        while (setBigInt.size() < k) {
            aRandomBigInt = new BigInteger(maxNumBitLength, rnd);
            if (aRandomBigInt.compareTo(min) < 0)
                aRandomBigInt = aRandomBigInt.add(min);
            if (aRandomBigInt.compareTo(bigInteger) >= 0)
                aRandomBigInt = aRandomBigInt.mod(bigInteger1).add(min);
            setBigInt.add(aRandomBigInt);
            System.out.println("Coef: " + aRandomBigInt);
        }

        //System.out.println(setBigInt.size());
        // Convert Set to List to allow for iteration by index
        List<BigInteger> coefsList = new ArrayList<BigInteger>(setBigInt);

        return coefsList;

    }

    private static BigInteger getFieldSize() {
        // get field value size, p
        // p = random prime number > S

        BigInteger max = secret.multiply(new BigInteger("10"));
        BigInteger min = secret;// lower limit
        BigInteger bigInteger1 = max.subtract(min);
        Random rnd = new Random();
        int maxNumBitLength = max.bitLength();

        BigInteger p;

        p = new BigInteger(maxNumBitLength, rnd);
        if (p.compareTo(min) < 0)
        {
            p = p.add(min);
        }
        if (p.compareTo(max) >= 0)
        {
            p = p.mod(bigInteger1).add(min);
        }
        //System.out.println(p);

        return p;

    }

    /**
     * Constructs n points from the polynomial
      * @return
     */
    private static List<Key> generateKeys(List<BigInteger> coefs, BigInteger p) {
        List<Key> keys = new LinkedList<>();
        Key key;
        // f(x) = a0 + a1x + a2x^2 + ...
        for (int i = 1; i<= n; i++) {
            BigInteger x = BigInteger.valueOf(i);
            // create key with values point and p
            // Pair<x,y<, where y = f(x) mod p
            //key = new Key(new Pair<>(i, getY(coefs, x).mod(p)), p);
            key = new Key(new Pair<>(i, getY(coefs, x)), p);
            keys.add(key);
        }

        return keys;
    }

    private static BigInteger getY(List<BigInteger> coefs, BigInteger x) {
        //f(x) = a0 + a1x + a2x^2 + ...anx^n
        BigInteger y = BigInteger.ZERO;

        for (int i = 0; i < coefs.size(); i++) {
            BigInteger xp = x.pow(i);
            y = y.add(coefs.get(i).multiply(xp));
        }
        return y;
    }

    public static void main(String[] args)
    {
        //System.out.println(preparation("potato"));
        List<Key> keys = preparation("orange marmalade");
        //getRandomNums();

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

        System.out.println("Subetkeylist size: " + subsetKeyList.size());

        List<Key> testKeyList = new ArrayList<>();
        testKeyList.add(new Key(new Pair<>(2, BigInteger.valueOf(1942)), p));
        testKeyList.add(new Key(new Pair<>(4, BigInteger.valueOf(3402)), p));
        testKeyList.add(new Key(new Pair<>(5, BigInteger.valueOf(4414)), p));

        System.out.println("P: " + p);

        reconstruction(subsetKeyList);
    }

}
