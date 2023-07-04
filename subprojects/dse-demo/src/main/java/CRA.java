import tools.refinery.store.query.ResultSet;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Consumer;

public class CRA {
	public static void main(String[] args) {
		var random = new Random(1);
		var craStore = new CRAStore();
		var model = craStore.createEmptyModel();

		var methodInterpretation = model.getMethodInterpretation();
		var nameInterpretation = model.getNameInterpretation();
		var attributeInterpretation = model.getAttributeInterpretation();
		var nextIDInterpretation = model.getNextIDInterpretation();

		var classInterpretation = model.getClassInterpretation();

		var functionalDependencyInterpretation = model.getFunctionalDependencyInterpretation();
		var dataDependencyInterpretation = model.getDataDependencyInterpretation();
		var isEncapsulatedByInterpretation = model.getIsEncapsulatedByInterpretation();

		var createClassPreconditionResultSet = model.getCreateClassPreconditionResultSet();
		var deleteClassPreconditionResultSet = model.getDeleteClassPreconditionResultSet();
		var moveFeaturePreconditionResultSet = model.getMoveFeaturePreconditionResultSet();
		var assignFeaturePreconditionResultSet = model.getAssignFeaturePreconditionResultSet();

		var nonEncapsulatedFeaturesResultSet = model.getNonEncapsulatedFeaturesResultSet();
		var CRAResult = model.getCRAResult();

		methodInterpretation.put(Tuple.of(0), true);
		nameInterpretation.put(Tuple.of(0), "M4");
		methodInterpretation.put(Tuple.of(1), true);
		nameInterpretation.put(Tuple.of(1), "M3");
		methodInterpretation.put(Tuple.of(2), true);
		nameInterpretation.put(Tuple.of(2), "M2");
		methodInterpretation.put(Tuple.of(3), true);
		nameInterpretation.put(Tuple.of(3), "M1");

		attributeInterpretation.put(Tuple.of(4), true);
		nameInterpretation.put(Tuple.of(4), "A2");
		attributeInterpretation.put(Tuple.of(5), true);
		nameInterpretation.put(Tuple.of(5), "A1");
		attributeInterpretation.put(Tuple.of(6), true);
		nameInterpretation.put(Tuple.of(6), "A4");
		attributeInterpretation.put(Tuple.of(7), true);
		nameInterpretation.put(Tuple.of(7), "A3");
		attributeInterpretation.put(Tuple.of(8), true);
		nameInterpretation.put(Tuple.of(8), "A5");

		functionalDependencyInterpretation.put(Tuple.of(0, 2), true);
		functionalDependencyInterpretation.put(Tuple.of(1, 0), true);
		functionalDependencyInterpretation.put(Tuple.of(1, 3), true);
		functionalDependencyInterpretation.put(Tuple.of(2, 3), true);
		functionalDependencyInterpretation.put(Tuple.of(3, 0), true);
		functionalDependencyInterpretation.put(Tuple.of(3, 1), true);

		dataDependencyInterpretation.put(Tuple.of(0, 7), true);
		dataDependencyInterpretation.put(Tuple.of(0, 8), true);
		dataDependencyInterpretation.put(Tuple.of(1, 6), true);
		dataDependencyInterpretation.put(Tuple.of(1, 7), true);
		dataDependencyInterpretation.put(Tuple.of(2, 4), true);
		dataDependencyInterpretation.put(Tuple.of(3, 5), true);
		dataDependencyInterpretation.put(Tuple.of(3, 7), true);

		// Setting up the NextID symbol
		nextIDInterpretation.put(Tuple.of(), 9);

		model.updateResultSets();

		long initalStateCommitID = model.commit();

		System.out.println("CRA Index for Initial Model");
		System.out.println(CRAResult.get(Tuple.of()));
		System.out.println("Initial Model Non Encapsulated Features");
		System.out.println(nonEncapsulatedFeaturesResultSet.size());

		// For Hill Search
//		ArrayList<RuleActivation> moves = new ArrayList<RuleActivation>();
//		ArrayList<RuleActivation> exploredMoves = new ArrayList<RuleActivation>();
//		Integer highScore = 0;
//
//		var cursor = createClassPreconditionResultSet.getAll();
//		while (cursor.move()){
//			moves.add(new RuleActivation(cursor.getKey(), model::createClass));
//		}
//
//		System.out.println("Number of unexplored moves: " + moves.size());
//
//		for (RuleActivation i : moves){
//			i.rule().accept(i.activation());
//			model.updateResultSets();
//			System.out.println("CRA Score: " + i.activation() + " -> " + CRAResult.get(Tuple.of()));
//			model.restoreModel(initalStateCommitID);
//		}

		double highestCRA = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < 10000; i++){
			ArrayList<RuleActivation> moves = new ArrayList<RuleActivation>();

			addMoves(createClassPreconditionResultSet, model::createClass, "createClass", moves);
			addMoves(deleteClassPreconditionResultSet, model::deleteEmptyClass, "deleteEmptyClass", moves);
			addMoves(moveFeaturePreconditionResultSet, model::moveFeature, "moveFeature", moves);
			addMoves(assignFeaturePreconditionResultSet, model::assignFeature, "assignFeature", moves);

			//System.out.println("Number of all possible moves: " + moves.size());

			int moveID = random.nextInt(moves.size());
			RuleActivation move = moves.get(moveID);
			move.rule().accept(move.activation());
			model.updateResultSets();
			double currentScore = CRAResult.get(Tuple.of());
			System.out.println("CRA Score: " + move.activation() + " for " + move.ruleName() + " -> " + currentScore);
			if ((currentScore > highestCRA) && (nonEncapsulatedFeaturesResultSet.size() == 0)){
				highestCRA = currentScore;
			}

		}
		System.out.println("Highest CRA Recorded: " + highestCRA);

	}

	private static void addMoves(ResultSet<Boolean> resultSet, Consumer<Tuple> rule, String ruleName,
								 ArrayList<RuleActivation> moves) {
		var cursor =resultSet.getAll();
		while (cursor.move()){
			moves.add(new RuleActivation(cursor.getKey(), rule, ruleName));
		}
	}
}
