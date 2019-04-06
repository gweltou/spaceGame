package com.gwel.ai;

import java.util.Random;

/**
 * Modified the original version of this class to use floats and removed unused functions
 * 
 * @author Deus Jeraldy
 * @Email: deusjeraldy@gmail.com
 */
public class Np {
    private static Random random;
    private static long seed;

    static {
        seed = System.currentTimeMillis();
        random = new Random(seed);
    }

    /**
     * Sets the seed of the pseudo-random number generator. This method enables
     * you to produce the same sequence of "random" number for each execution of
     * the program. Ordinarily, you should call this method at most once per
     * program.
     *
     * @param s the seed
     */
    public static void setSeed(long s) {
        seed = s;
        random = new Random(seed);
    }

    /**
     * Returns the seed of the pseudo-random number generator.
     *
     * @return the seed
     */
    public static long getSeed() {
        return seed;
    }

    /**
     * @param m
     * @param n
     * @return random m-by-n matrix with values between 0 and 1
     */
    public static float[][] random(int m, int n) {
        float[][] a = new float[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = random.nextFloat();
            }
        }
        return a;
    }

    /**
     * Transpose of a matrix
     *
     * @param a matrix
     * @return b = A^T
     */
    public static double[][] T(double[][] a) {
        int m = a.length;
        int n = a[0].length;
        double[][] b = new double[n][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                b[j][i] = a[i][j];
            }
        }
        return b;
    }

    /**
     * @param a matrix
     * @param b matrix
     * @return c = a + b
     */
    public static double[][] add(double[][] a, double[][] b) {
        int m = a.length;
        int n = a[0].length;
        double[][] c = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                c[i][j] = a[i][j] + b[i][j];
            }
        }
        return c;
    }

    /**
     * @param a matrix
     * @param b matrix
     * @return c = a - b
     */
    public static double[][] sub(double[][] a, double[][] b) {
        int m = a.length;
        int n = a[0].length;
        double[][] c = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                c[i][j] = a[i][j] - b[i][j];
            }
        }
        return c;
    }

    /**
     * Element wise subtraction
     *
     * @param a scaler
     * @param b matrix
     * @return c = a - b
     */
    public static float[][] sub(float a, float[][] b) {
        int m = b.length;
        int n = b[0].length;
        float[][] c = new float[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                c[i][j] = a - b[i][j];
            }
        }
        return c;
    }

    /**
     * @param a matrix
     * @param b matrix
     * @return c = a * b
     */
    public static double[][] dot(double[][] a, double[][] b) {
        int m1 = a.length;
        int n1 = a[0].length;
        int m2 = b.length;
        int n2 = b[0].length;
        if (n1 != m2) {
            throw new RuntimeException("Illegal matrix dimensions.");
        }
        double[][] c = new double[m1][n2];
        for (int i = 0; i < m1; i++) {
            for (int j = 0; j < n2; j++) {
                for (int k = 0; k < n1; k++) {
                    c[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return c;
    }

    /**
     * Element wise multiplication
     *
     * @param a matrix
     * @param x matrix
     * @return y = a * x
     */
    public static double[][] mul(double[][] x, double[][] a) {
        int m = a.length;
        int n = a[0].length;

        if (x.length != m || x[0].length != n) {
            throw new RuntimeException("Illegal matrix dimensions.");
        }
        double[][] y = new double[m][n];
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                y[j][i] = a[j][i] * x[j][i];
            }
        }
        return y;
    }

    /**
     * Element wise multiplication
     *
     * @param a matrix
     * @param x scaler
     * @return y = a * x
     */
    public static float[][] mul(float x, float[][] a) {
        int m = a.length;
        int n = a[0].length;

        float[][] y = new float[m][n];
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                y[j][i] = a[j][i] * x;
            }
        }
        return y;
    }

    /**
     * Matrix-Vector multiplication
     * 
     * @param matrix
     * @param vector
     * @return
     */
    public static float[] mul(float[][] matrix, float[] vector) {
        int rows = matrix.length;
        int columns = matrix[0].length;

        float[] result = new float[rows];

        for (int row = 0; row < rows; row++) {
            float sum = 0;
            for (int column = 0; column < columns; column++) {
                sum += matrix[row][column]
                        * vector[column];
            }
            result[row] = sum;
        }
        return result;
    }
    
    /**
     * Element wise power
     *
     * @param x matrix
     * @param a scaler
     * @return y
     */
    public static double[][] pow(double[][] x, int a) {
        int m = x.length;
        int n = x[0].length;

        double[][] y = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                y[i][j] = Math.pow(x[i][j], a);
            }
        }
        return y;
    }

    /**
     * @param a matrix
     * @return shape of matrix a
     */
    public static String shape(double[][] a) {
        int m = a.length;
        int n = a[0].length;
        String Vshape = "(" + m + "," + n + ")";
        return Vshape;
    }

    /**
     * @param a matrix
     * @return sigmoid of matrix a
     */
    public static double[][] sigmoid(double[][] a) {
        int m = a.length;
        int n = a[0].length;
        double[][] z = new double[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                z[i][j] = (1.0 / (1 + Math.exp(-a[i][j])));
            }
        }
        return z;
    }

    /**
     * @param a matrix
     * @return tanh of vector a
     */
    public static float[] tanh(float[] a) {
        float[] z = new float[a.length];

        for (int i = 0; i < a.length; i++) {
            z[i] = (float) ((Math.exp(a[i]) - Math.exp(-a[i])) / (Math.exp(a[i]) + Math.exp(-a[i])));
        }
        return z;
    }
    
    /**
     * @param a matrix
     * @return relu of vector a
     */
    public static float[] relu(float[] a) {
        float[] z = new float[a.length];

        for (int i = 0; i < a.length; i++) {
            z[i] = (float) (Math.max(a[i], 0.0f));
        }
        return z;
    }
    
    /**
     * Element wise division
     *
     * @param a scaler
     * @param x matrix
     * @return x / a
     */
    public static double[][] div(double[][] x, int a) {
        int m = x.length;
        int n = x[0].length;

        double[][] z = new double[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                z[i][j] = (x[i][j] / a);
            }
        }
        return z;
    }
    /**
     * Element wise division
     *
     * @param A matrix
     * @param Y matrix
     * @param batch_size scaler
     * @return loss
     */
    public static double cross_entropy(int batch_size, double[][] Y, double[][] A) {
        int m = A.length;
        int n = A[0].length;
        double[][] z = new double[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                z[i][j] = (Y[i][j] * Math.log(A[i][j])) + ((1 - Y[i][j]) * Math.log(1 - A[i][j]));
            }
        }

        double sum = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                sum += z[i][j];
            }
        }
        return -sum / batch_size;
    }
    
    public static double[][] softmax(double[][] z) {
        double[][] zout = new double[z.length][z[0].length];
        double sum = 0.;
        for (int i = 0; i < z.length; i++) {
            for (int j = 0; j < z[0].length; j++) {
                sum += Math.exp(z[i][j]);
            }
        }
        for (int i = 0; i < z.length; i++) {
            for (int j = 0; j < z[0].length; j++) {
                zout[i][j] = Math.exp(z[i][j]) / sum;
            }
        }
        return zout;
    }
}