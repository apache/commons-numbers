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

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link LanczosApproximation}.
 */
public class LanczosApproximationTest {

	@Test
	public void testG() {
		Assert.assertEquals(607d / 128d, LanczosApproximation.g(), 0d);
	}

	@Test
	public void testLanczosApproximation() {

		for (double index = 0.1; index < 50.0; index += 0.1) {
			double randomfractionalNumber = getRandomFlotingPoints();

			String expected = String.format("%.10f", generateCoefficientsValue(randomfractionalNumber));
			String actual = String.format("%.10f", LanczosApproximation.value(randomfractionalNumber));

			Assert.assertEquals(expected, actual);

		}
	}

	/**
	 * method for generating random Coefficients for testing
	 * LanczosApproximation below code is the fraction of code taken from
	 * https://rosettacode.org/wiki/Gamma_function#Java
	 *
	 * @param val
	 * @return a
	 */
	private double generateCoefficientsValue(double val) {

		double[] p = { 0.99999999999999709182, 57.156235665862923517, -59.597960355475491248, 14.136097974741747174,
				-0.49191381609762019978, .33994649984811888699e-4, .46523628927048575665e-4, -.98374475304879564677e-4,
				.15808870322491248884e-3, -.21026444172410488319e-3, .21743961811521264320e-3,
				-.16431810653676389022e-3, .84418223983852743293e-4, -.26190838401581408670e-4,
				.36899182659531622704e-5, };

		double a = p[0];
		for (int i = 1; i < p.length; i++) {
			a += p[i] / (val + i);
		}
		return a;
	}

	/**
	 * method for getting random floating points.
	 * @return
	 */
	private double getRandomFlotingPoints() {
		double MEAN = 50.0f;
		double VARIANCE = 50.0f;
		return getGaussian(MEAN, VARIANCE);
	}

	private Random fRandom = new Random();

	/**
	 * method for getting Gaussian
	 * @param aMean
	 * @param aVariance
	 * @return
	 */
	private double getGaussian(double aMean, double aVariance) {
		return aMean + fRandom.nextGaussian() * aVariance;
	}
}
