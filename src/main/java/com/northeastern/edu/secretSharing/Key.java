import java.math.BigInteger;
import javafx.util.Pair;

public class Key
{
    private Pair<Integer, BigInteger> point;
    private BigInteger p;

    public Key(Pair<Integer, BigInteger> point, BigInteger p) {
        this.point = point;
        this.p = p;
    }

    public Pair<Integer, BigInteger> getPoint() {
        return point;
    }

    public BigInteger getP() {
        return p;
    }
}
