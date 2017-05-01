import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * Created as part of the class project for Mobile Computing
 */
public class NetworkRTTInterpolator {
    LinearInterpolator interpolator;
    PolynomialSplineFunction function;

    public NetworkRTTInterpolator(long x_length, double[] rand) {
        double[] x = init_x(x_length);
        double[] y = init_y(x.length, rand);
        interpolator = new LinearInterpolator();
        function = interpolator.interpolate(x, y);


    }

    public double getY(double x, String endPoint) {
        return function.value(x);
    }

    private double[] init_y(int length, double[] rand) {
        double[] vals = new double[length];
        for (int i = 0; i < length; i++)
            vals[i] = rand[i % rand.length];
        return vals;
    }

    private double[] init_x(long x_length) {
        int i = 16;
        while (x_length % i != 0)
            i++;
        double[] vals = new double[i];

        for (int j = 0; j < i; j++)
            vals[j] = ((double) (j) / i) * x_length;

        return vals;
    }
}
