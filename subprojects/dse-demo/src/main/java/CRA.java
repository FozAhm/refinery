import tools.refinery.store.tuple.Tuple;

public class CRA {
	public static void main(String[] args) {
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

		long initalStateID = model.commit();

		System.out.println("CRA Index for Initial Model");
		System.out.println(CRAResult.get(Tuple.of()));
	}
}
