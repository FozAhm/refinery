/*
 * generated by Xtext 2.25.0
 */
package tools.refinery.language.validation;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.validation.Check;

import com.google.inject.Inject;

import tools.refinery.language.model.ProblemUtil;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.model.problem.Variable;
import tools.refinery.language.model.problem.VariableOrNodeArgument;
import tools.refinery.language.resource.ReferenceCounter;

/**
 * This class contains custom validation rules.
 *
 * See
 * https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 */
public class ProblemValidator extends AbstractProblemValidator {
	private static final String ISSUE_PREFIX = "tools.refinery.language.validation.ProblemValidator.";

	public static final String SINGLETON_VARIABLE_ISSUE = ISSUE_PREFIX + "SINGLETON_VARIABLE";

	public static final String NON_INDIVIDUAL_NODE_ISSUE = ISSUE_PREFIX + "NON_INDIVIDUAL_NODE";

	@Inject
	private ReferenceCounter referenceCounter;

	@Check
	public void checkUniqueVariable(VariableOrNodeArgument argument) {
		var variableOrNode = argument.getVariableOrNode();
		if (variableOrNode instanceof Variable variable && ProblemUtil.isImplicitVariable(variable)
				&& !ProblemUtil.isSingletonVariable(variable)) {
			var problem = EcoreUtil2.getContainerOfType(variable, Problem.class);
			if (problem != null && referenceCounter.countReferences(problem, variable) <= 1) {
				var name = variable.getName();
				var message = "Variable '%s' has only a single reference. Add another reference or mark is as a singleton variable: '_%s'"
						.formatted(name, name);
				warning(message, argument, ProblemPackage.Literals.VARIABLE_OR_NODE_ARGUMENT__VARIABLE_OR_NODE,
						INSIGNIFICANT_INDEX, SINGLETON_VARIABLE_ISSUE);
			}
		}
	}

	@Check
	public void checkNonUniqueNode(VariableOrNodeArgument argument) {
		var variableOrNode = argument.getVariableOrNode();
		if (variableOrNode instanceof Node node && !ProblemUtil.isIndividualNode(node)) {
			var name = node.getName();
			var message = "Only individual nodes can be referenced in predicates. Mark '%s' as individual with the declaration 'individual %s.'"
					.formatted(name, name);
			error(message, argument, ProblemPackage.Literals.VARIABLE_OR_NODE_ARGUMENT__VARIABLE_OR_NODE,
					INSIGNIFICANT_INDEX, NON_INDIVIDUAL_NODE_ISSUE);
		}
	}
}
