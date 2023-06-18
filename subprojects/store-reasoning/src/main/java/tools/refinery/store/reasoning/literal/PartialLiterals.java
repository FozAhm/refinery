/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.store.query.literal.CallLiteral;

public final class PartialLiterals {
	private PartialLiterals() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static CallLiteral may(CallLiteral literal) {
		return addModality(literal, Modality.MAY);
	}

	public static CallLiteral must(CallLiteral literal) {
		return addModality(literal, Modality.MUST);
	}

	public static CallLiteral current(CallLiteral literal) {
		return addModality(literal, Modality.CURRENT);
	}

	public static CallLiteral addModality(CallLiteral literal, Modality modality) {
		var target = literal.getTarget();
		if (target instanceof ModalConstraint) {
			throw new IllegalArgumentException("Literal %s already has modality".formatted(literal));
		}
		var polarity = literal.getPolarity();
		var modalTarget = new ModalConstraint(modality.commute(polarity), target);
		return new CallLiteral(polarity, modalTarget, literal.getArguments());
	}
}
