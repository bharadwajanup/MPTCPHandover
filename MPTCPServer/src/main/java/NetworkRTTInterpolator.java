import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * Created as part of the class project for Mobile Computing
 */
public class NetworkRTTInterpolator {
    LinearInterpolator interpolator;
    PolynomialSplineFunction function;

    public NetworkRTTInterpolator(long x_length) {
        double[] x = init_x(x_length);
        double[] y = init_y(x.length);
        interpolator = new LinearInterpolator();
        function = interpolator.interpolate(x, y);

    }

    public double getY(double x) {
        return function.value(x);
    }

    private double[] init_y(int length) {
        double[] vals = new double[length];
        double[] rand = {155.0, 235.6, 3600, 455.0, 400, 350, 2400.0, 800.0, 155, 165, 162};
//        double[] rand = {155.0,800};
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
