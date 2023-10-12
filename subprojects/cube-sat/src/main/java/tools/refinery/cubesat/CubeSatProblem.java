package tools.refinery.cubesat;

import org.moeaframework.core.Solution;
import org.moeaframework.problem.AbstractProblem;
import org.moeaframework.problem.AnalyticalProblem;

public abstract class CubeSatProblem extends AbstractProblem implements AnalyticalProblem {
	public CubeSatProblem(int numberOfVariables, int numberOfObjectives){
		super(numberOfVariables, numberOfObjectives);
	}

	public Solution newSolution() {
		Solution solution = new Solution(numberOfVariables, numberOfObjectives);
		return solution;
	}

	public void evaluate(Solution solution){

	}

	public Solution generate(){
		Solution solution = newSolution();

		evaluate(solution);

		return solution;
	}


}
