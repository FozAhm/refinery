/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.tuple;

import static tools.refinery.store.tuple.TupleConstants.*;

public record Tuple4(int value0, int value1, int value2, int value3) implements Tuple {
	@Override
	public int getSize() {
		return 4;
	}

	@Override
	public int get(int element) {
		return switch (element) {
			case 0 -> value0;
			case 1 -> value1;
			case 2 -> value2;
			case 3 -> value3;
			default -> throw new ArrayIndexOutOfBoundsException(element);
		};
	}

	@Override
	public String toString() {
		return TUPLE_BEGIN + value0 + TUPLE_SEPARATOR + value1 + TUPLE_SEPARATOR + value2 + TUPLE_SEPARATOR + value3 +
				TUPLE_END;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Tuple4 tuple4 = (Tuple4) o;
		return value0 == tuple4.value0 && value1 == tuple4.value1 && value2 == tuple4.value2 && value3 == tuple4.value3;
	}

	@Override
	public int hashCode() {
		int hash = 31 + value0;
		hash = 31 * hash + value1;
		hash = 31 * hash + value2;
		hash = 31 * hash + value3;
		return hash;
	}
}
