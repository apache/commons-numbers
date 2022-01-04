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

package org.apache.commons.numbers.gamma;

/**
 * Encapsulate the policy for function evaluation.
 * This is a reduced implementation of the Boost {@code boost::math::policies}
 * functionality. No settings are preserved for the error handling policy or
 * promotion of data types for computations.
 * This controls the convergence criteria and maximum iterations for series evaluations.
 *
 * @see <a href="https://www.boost.org/doc/libs/1_77_0/libs/math/doc/html/policy.html">
 * Policies: Controlling Precision, Error Handling etc</a>
 */
final class Policy {
    /** Default policy. The Boost default uses 2^-52 for the epsilon. This uses
     * 2^-53 to use an extra guard digit in the Kahan series summations.
     * The minimum value for the Commons continued fraction epsilon is also 2^-53. */
    private static final Policy DEFAULT = new Policy(0x1.0p-53, 1000000);

    /** Epsilon value for relative error. */
    private final double eps;
    /** The maximum number of iterations permitted in a series evaluation. */
    private final int maxIterations;

    /**
     * Instantiates a new policy.
     *
     * @param eps the eps
     * @param maxIterations the maximum number of iterations permitted in a series
     * evaluation
     */
    Policy(double eps, int maxIterations) {
        this.eps = eps;
        this.maxIterations = maxIterations;
    }

    /**
     * Gets the default.
     *
     * @return the default policy
     */
    static Policy getDefault() {
        return DEFAULT;
    }

    /**
     * Gets the epsilon value for relative error.
     *
     * @return the epsilon
     */
    double getEps() {
        return eps;
    }

    /**
     * Gets the maximum number of iterations permitted in a series evaluation.
     *
     * @return max iterations
     */
    int getMaxIterations() {
        return maxIterations;
    }
}
