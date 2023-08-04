/*******************************************************************************
 * Copyright (c) 2010-2015, Andras Szabolcs Nagy, Abel Hegedus, Akos Horvath, Zoltan Ujhelyi and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.dse.objectives;

import java.util.Comparator;

public class Comparators {

	private Comparators() { /*Utility class constructor*/ }

	public static final Comparator<Double> HIGHER_IS_BETTER = (o1, o2) -> o1.compareTo(o2);

	public static final Comparator<Double> LOWER_IS_BETTER = (o1, o2) -> o2.compareTo(o1);

	private static final Double ZERO = (double) 0;

	public static final Comparator<Double> DIFFERENCE_TO_ZERO_IS_BETTER = (o1, o2) -> ZERO.compareTo(Math.abs(o1)-Math.abs(o2));

}
