/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//  (C) Copyright John Maddock 2006.
//  Use, modification and distribution are subject to the
//  Boost Software License, Version 1.0. (See accompanying file
//  LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)

package org.apache.commons.numbers.gamma;

/**
 * Implementation of the inverse of the <a href="http://mathworld.wolfram.com/Erf.html">error function</a>.
 *
 * <p>This code has been adapted from the <a href="https://www.boost.org/">Boost</a>
 * {@code c++} implementation {@code <boost/math/cspecial_functions/erf.hpp>}.
 * All work is copyright John Maddock 2006 and subject to the Boost Software License.
 *
 * @see
 * <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/math_toolkit/sf_erf/error_function.html">
 * Boost C++ Error functions</a>
 */
final class BoostErf {
    /** Private constructor. */
    private BoostErf() {
        // intentionally empty.
    }

    // Code ported from Boost 1.77.0
    //
    // boost/math/special_functions/erf.hpp
    // boost/math/special_functions/detail/erf_inv.hpp
    //
    // Original code comments, including measured deviations, are preserved.
    //
    // Changes to the Boost implementation:
    // - Update method names to replace underscores with camel case
    // - Explicitly inline the polynomial function evaluation
    //   using Horner's method (https://en.wikipedia.org/wiki/Horner%27s_method)
    // - Support odd function for f(0.0) = -f(-0.0)
    // Inverse erf:
    // - Change inverse erf edge case detection to include NaN
    //
    // Note:
    // Constants using the 'f' suffix are machine
    // representable as a float, e.g.
    // assert 0.0891314744949340820313f == 0.0891314744949340820313;
    // The values are unchanged from the Boost reference.

    /**
     * Returns the inverse complementary error function.
     *
     * @param z Value (in {@code [0, 2]}).
     * @return t such that {@code z = erfc(t)}
     */
    static double erfcInv(double z) {
        //
        // Begin by testing for domain errors, and other special cases:
        //
        if (z < 0 || z > 2 || Double.isNaN(z)) {
            // Argument outside range [0,2] in inverse erfc function
            return Double.NaN;
        }
        // Domain bounds must be detected as the implementation computes NaN.
        // (log(q=0) creates infinity and the rational number is
        // infinity / infinity)
        if (z == (int) z) {
            // z   return
            // 2   -inf
            // 1   0
            // 0   inf
            return z == 1 ? 0 : (1 - z) * Double.POSITIVE_INFINITY;
        }

        //
        // Normalise the input, so it's in the range [0,1], we will
        // negate the result if z is outside that range. This is a simple
        // application of the erfc reflection formula: erfc(-z) = 2 - erfc(z)
        //
        double p;
        double q;
        double s;
        if (z > 1) {
            q = 2 - z;
            p = 1 - q;
            s = -1;
        } else {
            p = 1 - z;
            q = z;
            s = 1;
        }

        //
        // And get the result, negating where required:
        //
        return s * erfInvImp(p, q);
    }

    /**
     * Returns the inverse error function.
     *
     * @param z Value (in {@code [-1, 1]}).
     * @return t such that {@code z = erf(t)}
     */
    static double erfInv(double z) {
        //
        // Begin by testing for domain errors, and other special cases:
        //
        if (z < -1 || z > 1 || Double.isNaN(z)) {
            // Argument outside range [-1, 1] in inverse erf function
            return Double.NaN;
        }
        // Domain bounds must be detected as the implementation computes NaN.
        // (log(q=0) creates infinity and the rational number is
        // infinity / infinity)
        if (z == (int) z) {
            // z   return
            // -1  -inf
            // -0  -0
            // 0   0
            // 1   inf
            return z == 0 ? z : z * Double.POSITIVE_INFINITY;
        }

        //
        // Normalise the input, so it's in the range [0,1], we will
        // negate the result if z is outside that range. This is a simple
        // application of the erf reflection formula: erf(-z) = -erf(z)
        //
        double p;
        double q;
        double s;
        if (z < 0) {
            p = -z;
            q = 1 - p;
            s = -1;
        } else {
            p = z;
            q = 1 - z;
            s = 1;
        }
        //
        // And get the result, negating where required:
        //
        return s * erfInvImp(p, q);
    }

    /**
     * Common implementation for inverse erf and erfc functions.
     *
     * @param p P-value
     * @param q Q-value (1-p)
     * @return the inverse
     */
    private static double erfInvImp(double p, double q) {
        double result = 0;

        if (p <= 0.5) {
            //
            // Evaluate inverse erf using the rational approximation:
            //
            // x = p(p+10)(Y+R(p))
            //
            // Where Y is a constant, and R(p) is optimised for a low
            // absolute error compared to |Y|.
            //
            // double: Max error found: 2.001849e-18
            // long double: Max error found: 1.017064e-20
            // Maximum Deviation Found (actual error term at infinite precision) 8.030e-21
            //
            final float Y = 0.0891314744949340820313f;
            double P;
            P =  -0.00538772965071242932965;
            P =   0.00822687874676915743155 + P * p;
            P =    0.0219878681111168899165 + P * p;
            P =   -0.0365637971411762664006 + P * p;
            P =   -0.0126926147662974029034 + P * p;
            P =    0.0334806625409744615033 + P * p;
            P =  -0.00836874819741736770379 + P * p;
            P = -0.000508781949658280665617 + P * p;
            double Q;
            Q = 0.000886216390456424707504;
            Q = -0.00233393759374190016776 + Q * p;
            Q =   0.0795283687341571680018 + Q * p;
            Q =  -0.0527396382340099713954 + Q * p;
            Q =    -0.71228902341542847553 + Q * p;
            Q =    0.662328840472002992063 + Q * p;
            Q =     1.56221558398423026363 + Q * p;
            Q =    -1.56574558234175846809 + Q * p;
            Q =   -0.970005043303290640362 + Q * p;
            Q =                        1.0 + Q * p;
            final double g = p * (p + 10);
            final double r = P / Q;
            result = g * Y + g * r;
        } else if (q >= 0.25) {
            //
            // Rational approximation for 0.5 > q >= 0.25
            //
            // x = sqrt(-2*log(q)) / (Y + R(q))
            //
            // Where Y is a constant, and R(q) is optimised for a low
            // absolute error compared to Y.
            //
            // double : Max error found: 7.403372e-17
            // long double : Max error found: 6.084616e-20
            // Maximum Deviation Found (error term) 4.811e-20
            //
            final float Y = 2.249481201171875f;
            final double xs = q - 0.25f;
            double P;
            P =  -3.67192254707729348546;
            P =   21.1294655448340526258 + P * xs;
            P =    17.445385985570866523 + P * xs;
            P =  -44.6382324441786960818 + P * xs;
            P =  -18.8510648058714251895 + P * xs;
            P =   17.6447298408374015486 + P * xs;
            P =   8.37050328343119927838 + P * xs;
            P =  0.105264680699391713268 + P * xs;
            P = -0.202433508355938759655 + P * xs;
            double Q;
            Q =  1.72114765761200282724;
            Q = -22.6436933413139721736 + Q * xs;
            Q =  10.8268667355460159008 + Q * xs;
            Q =  48.5609213108739935468 + Q * xs;
            Q = -20.1432634680485188801 + Q * xs;
            Q = -28.6608180499800029974 + Q * xs;
            Q =   3.9713437953343869095 + Q * xs;
            Q =  6.24264124854247537712 + Q * xs;
            Q =                     1.0 + Q * xs;
            final double g = Math.sqrt(-2 * Math.log(q));
            final double r = P / Q;
            result = g / (Y + r);
        } else {
            //
            // For q < 0.25 we have a series of rational approximations all
            // of the general form:
            //
            // let: x = sqrt(-log(q))
            //
            // Then the result is given by:
            //
            // x(Y+R(x-B))
            //
            // where Y is a constant, B is the lowest value of x for which
            // the approximation is valid, and R(x-B) is optimised for a low
            // absolute error compared to Y.
            //
            // Note that almost all code will really go through the first
            // or maybe second approximation. After than we're dealing with very
            // small input values indeed.
            //
            // Limit for a double: Math.sqrt(-Math.log(Double.MIN_VALUE)) = 27.28...
            // Branches for x >= 44 (supporting 80 and 128 bit long double) have been removed.
            final double x = Math.sqrt(-Math.log(q));
            if (x < 3) {
                // Max error found: 1.089051e-20
                final float Y = 0.807220458984375f;
                final double xs = x - 1.125f;
                double P;
                P = -0.681149956853776992068e-9;
                P =  0.285225331782217055858e-7 + P * xs;
                P = -0.679465575181126350155e-6 + P * xs;
                P =   0.00214558995388805277169 + P * xs;
                P =    0.0290157910005329060432 + P * xs;
                P =     0.142869534408157156766 + P * xs;
                P =     0.337785538912035898924 + P * xs;
                P =     0.387079738972604337464 + P * xs;
                P =     0.117030156341995252019 + P * xs;
                P =    -0.163794047193317060787 + P * xs;
                P =    -0.131102781679951906451 + P * xs;
                double Q;
                Q =  0.01105924229346489121;
                Q = 0.152264338295331783612 + Q * xs;
                Q = 0.848854343457902036425 + Q * xs;
                Q =  2.59301921623620271374 + Q * xs;
                Q =  4.77846592945843778382 + Q * xs;
                Q =  5.38168345707006855425 + Q * xs;
                Q =  3.46625407242567245975 + Q * xs;
                Q =                     1.0 + Q * xs;
                final double R = P / Q;
                result = Y * x + R * x;
            } else if (x < 6) {
                // Max error found: 8.389174e-21
                final float Y = 0.93995571136474609375f;
                final double xs = x - 3;
                double P;
                P = 0.266339227425782031962e-11;
                P = -0.230404776911882601748e-9 + P * xs;
                P =  0.460469890584317994083e-5 + P * xs;
                P =  0.000157544617424960554631 + P * xs;
                P =   0.00187123492819559223345 + P * xs;
                P =   0.00950804701325919603619 + P * xs;
                P =    0.0185573306514231072324 + P * xs;
                P =  -0.00222426529213447927281 + P * xs;
                P =   -0.0350353787183177984712 + P * xs;
                double Q;
                Q = 0.764675292302794483503e-4;
                Q =  0.00263861676657015992959 + Q * xs;
                Q =   0.0341589143670947727934 + Q * xs;
                Q =    0.220091105764131249824 + Q * xs;
                Q =    0.762059164553623404043 + Q * xs;
                Q =      1.3653349817554063097 + Q * xs;
                Q =                        1.0 + Q * xs;
                final double R = P / Q;
                result = Y * x + R * x;
            } else if (x < 18) {
                // Max error found: 1.481312e-19
                final float Y = 0.98362827301025390625f;
                final double xs = x - 6;
                double P;
                P =   0.99055709973310326855e-16;
                P = -0.281128735628831791805e-13 + P * xs;
                P =   0.462596163522878599135e-8 + P * xs;
                P =   0.449696789927706453732e-6 + P * xs;
                P =   0.149624783758342370182e-4 + P * xs;
                P =   0.000209386317487588078668 + P * xs;
                P =    0.00105628862152492910091 + P * xs;
                P =   -0.00112951438745580278863 + P * xs;
                P =    -0.0167431005076633737133 + P * xs;
                double Q;
                Q = 0.282243172016108031869e-6;
                Q = 0.275335474764726041141e-4 + Q * xs;
                Q = 0.000964011807005165528527 + Q * xs;
                Q =   0.0160746087093676504695 + Q * xs;
                Q =    0.138151865749083321638 + Q * xs;
                Q =    0.591429344886417493481 + Q * xs;
                Q =                        1.0 + Q * xs;
                final double R = P / Q;
                result = Y * x + R * x;
            } else {
                // x < 44
                // Max error found: 5.697761e-20
                final float Y = 0.99714565277099609375f;
                final double xs = x - 18;
                double P;
                P = -0.116765012397184275695e-17;
                P =  0.145596286718675035587e-11 + P * xs;
                P =   0.411632831190944208473e-9 + P * xs;
                P =   0.396341011304801168516e-7 + P * xs;
                P =   0.162397777342510920873e-5 + P * xs;
                P =   0.254723037413027451751e-4 + P * xs;
                P =  -0.779190719229053954292e-5 + P * xs;
                P =    -0.0024978212791898131227 + P * xs;
                double Q;
                Q = 0.509761276599778486139e-9;
                Q = 0.144437756628144157666e-6 + Q * xs;
                Q = 0.145007359818232637924e-4 + Q * xs;
                Q = 0.000690538265622684595676 + Q * xs;
                Q =   0.0169410838120975906478 + Q * xs;
                Q =    0.207123112214422517181 + Q * xs;
                Q =                        1.0 + Q * xs;
                final double R = P / Q;
                result = Y * x + R * x;
            }
        }
        return result;
    }
}

