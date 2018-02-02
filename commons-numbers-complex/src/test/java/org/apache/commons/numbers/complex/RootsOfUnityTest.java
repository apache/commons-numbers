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
package org.apache.commons.numbers.complex;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;


/**
 * Unit tests for the {@link RootsOfUnity} class.
 *
 */
public class RootsOfUnityTest {
    @Test(expected = IllegalArgumentException.class)
    public void testPrecondition() {
        new RootsOfUnity(0);
    }
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetRootPrecondition1() {
        final int n = 3;
        final RootsOfUnity roots = new RootsOfUnity(n);
        roots.getRoot(-1);
    }
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetRootPrecondition2() {
        final int n = -2;
        final RootsOfUnity roots = new RootsOfUnity(n);
        roots.getRoot(2);
    }

    @Test
    public void testGetNumberOfRoots1() {
        final int n = 5;
        final RootsOfUnity roots = new RootsOfUnity(n);
        Assert.assertEquals(n, roots.getNumberOfRoots());
        Assert.assertTrue(roots.isCounterClockwise());
    }
    @Test
    public void testGetNumberOfRoots2() {
        final int n = -4;
        final RootsOfUnity roots = new RootsOfUnity(n);
        Assert.assertEquals(Math.abs(n), roots.getNumberOfRoots());
        Assert.assertFalse(roots.isCounterClockwise());
    }

    @Test
    public void testComputeRoots() {
        final double tol = Math.ulp(1d);
        final org.apache.commons.math3.complex.RootsOfUnity cmRoots =
            new org.apache.commons.math3.complex.RootsOfUnity();
        for (int n = -10; n < 11; n++) {
            final int absN = Math.abs(n);
            if (n != 0) {
                cmRoots.computeRoots(n);
                final RootsOfUnity roots = new RootsOfUnity(n);
                for (int k = 0; k < absN; k++) {
                    final Complex z = roots.getRoot(k);
                    Assert.assertEquals("n=" + n + " k=" + k,
                                        cmRoots.getReal(k),
                                        z.getReal(),
                                        tol);
                    Assert.assertEquals("n=" + n + " k=" + k,
                                        cmRoots.getImaginary(k),
                                        z.getImaginary(),
                                        tol);
                }

                if (n > 0) {
                    final List<Complex> list = Complex.ONE.nthRoot(n);
                    for (int k = 0; k < n; k++) {
                        final Complex c1 = list.get(k);
                        final Complex c2 = roots.getRoot(k);
                        Assert.assertTrue("k=" + k + ": " + c1 + " != " + c2, Complex.equals(c1, c2, 1e-15));
                    }
                }
            }
        }
    }
}
