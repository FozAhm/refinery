import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.dse.transition.actions.ActionLiterals;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.dse.transition.objectives.Objectives;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.FunctionalQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.term.real.RealTerms;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;

import java.util.List;

import static tools.refinery.store.dse.transition.actions.ActionLiterals.add;
import static tools.refinery.store.dse.transition.actions.ActionLiterals.remove;
import static tools.refinery.store.dse.modification.actions.ModificationActionLiterals.create;
import static tools.refinery.store.dse.modification.actions.ModificationActionLiterals.delete;
import static tools.refinery.store.query.literal.Literals.check;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.term.int_.IntTerms.constant;
import static tools.refinery.store.query.term.int_.IntTerms.greater;
import static tools.refinery.store.query.term.int_.IntTerms.sub;
import static tools.refinery.store.query.term.real.RealTerms.*;
import static tools.refinery.store.query.term.real.RealTerms.add;

import tools.refinery.store.dse.modification.DanglingEdges;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

public class CRAV2 {

	// Symbols
	private final Symbol<String> name = new Symbol<>("name", 1, String.class, null);
	private final Symbol<Boolean> attribute = new Symbol<>("attribute", 1, Boolean.class, false);
	private final Symbol<Boolean> method = new Symbol<>("method", 1, Boolean.class, false);
	private final Symbol<Boolean> class_ = new Symbol<>("class", 1, Boolean.class, false);

	private final Symbol<Boolean> functionalDependency = new Symbol<>("functionalDependency", 2, Boolean.class, false);
	private final Symbol<Boolean> dataDependency = new Symbol<>("dataDependency", 2, Boolean.class, false);
	private final Symbol<Boolean> isEncapsulatedBy = new Symbol<>("isEncapsulatedBy", 2, Boolean.class, false);


	// Queries
	private final RelationalQuery classes;
	private final RelationalQuery features;
	private final RelationalQuery nonEncapsulatedFeatures;
	private final RelationalQuery methodsForClass;
	private final RelationalQuery attributesForClass;
	private final Rule deleteClass;
	private final Rule createClass;
	private final Rule moveFeature;
	private final Rule assignFeature;
	private final FunctionalQuery<Double> NormalizedMAI;
	private final FunctionalQuery<Double> NormalizedMMI;
	private final FunctionalQuery<Double> CRA;


	public CRAV2() {
		var attributeView = new KeyOnlyView<>(attribute);
		var methodView = new KeyOnlyView<>(method);
		var classView = new KeyOnlyView<>(class_);

		var functionalDependencyView = new KeyOnlyView<>(functionalDependency);
		var dataDependencyView = new KeyOnlyView<>(dataDependency);
		var isEncapsulatedByView = new KeyOnlyView<>(isEncapsulatedBy);

		// Create a query that will return all classes
		var class1 = Variable.of("class1");
		classes = Query.builder("AllClasses")
				.parameters(class1)
				.clause(classView.call(class1))
				.build();

		// Create a query that will return the id's of all features, this query is designed to be used by other
		// queries hence the DNF class
		var feature1 = Variable.of("feature1");
		features = Query.builder("AllFeatures")
				.parameters(feature1)
				.clause(attributeView.call(feature1))
				.clause(methodView.call(feature1))
				.build();

		// Create a query that will return the id's of all features that are not encapsulated by a class
		var feature2 = Variable.of("feature2");
		var class2 = Variable.of("class2");
		nonEncapsulatedFeatures = Query.builder("NonEncapsulatedFeatures")
				.parameters(feature2)
				.clause(features.call(feature2), not(isEncapsulatedByView.call(feature2, class2)))
				.build();

		// Return a table with all the methods for each given class
		var given_class_for_method = Variable.of("given_class_for_method");
		var associated_method = Variable.of("associated_method");
		methodsForClass = Query.builder("methodsForClass")
				.parameters(given_class_for_method, associated_method)
				.clause(isEncapsulatedByView.call(associated_method, given_class_for_method),
						methodView.call(associated_method))
				.build();

		// Return a table with all the attributes for each given class
		var given_class_for_attribute = Variable.of("given_class_for_attribute");
		var associated_attribute = Variable.of("associated_attribute");
		attributesForClass = Query.builder("attributesForClass")
				.parameters(given_class_for_attribute, associated_attribute)
				.clause(isEncapsulatedByView.call(associated_attribute, given_class_for_attribute),
						attributeView.call(associated_attribute))
				.build();

		// Gives us a table for all class pairs and their data dependencies within
		var class_1_mai = Variable.of("class_1_mai");
		var class_2_mai = Variable.of("class_2_mai");
		var method_1_mai = Variable.of("method_1_mai");
		var attribute_1_mai = Variable.of("attribute_1_mai");
		var classDataDependencies = Dnf.builder("ClassDataDependencies")
				.parameters(class_1_mai, class_2_mai, method_1_mai, attribute_1_mai)
				.clause(classView.call(class_1_mai), methodView.call(method_1_mai), isEncapsulatedByView.call(method_1_mai, class_1_mai), classView.call(class_2_mai),
						attributeView.call(attribute_1_mai), isEncapsulatedByView.call(attribute_1_mai, class_2_mai), dataDependencyView.call(method_1_mai, attribute_1_mai))
				.build();

		var NormalizedMAIOutput = Variable.of("NormalizedMAIOutput", Double.class);
		var MAIOutput = Variable.of("MAIOutput", Integer.class);
		var NumMethodsMAI = Variable.of("NumMethodsMAI", Integer.class);
		var NumAttributesMAI = Variable.of("NumAttributesMAI", Integer.class);
		var class_1_normalized_mai = Variable.of("class_1_normalized_mai");
		var class_2_normalized_mai = Variable.of("class_2_normalized_mai");
		var method_1_normalized_mai = Variable.of("method_1_normalized_mai");
		var attribute_1_normalized_mai = Variable.of("attribute_1_normalized_mai");
		NormalizedMAI = Query.builder("MAI")
				.parameters(class_1_normalized_mai, class_2_normalized_mai)
				.output(NormalizedMAIOutput)
				.clause(classDataDependencies.call(class_1_normalized_mai, class_2_normalized_mai,
								method_1_normalized_mai, attribute_1_normalized_mai),
						MAIOutput.assign(classDataDependencies.count(class_1_normalized_mai, class_2_normalized_mai,
								Variable.of(), Variable.of())),
						NumMethodsMAI.assign(methodsForClass.count(class_1_normalized_mai, Variable.of())),
						NumAttributesMAI.assign(attributesForClass.count(class_2_normalized_mai, Variable.of())),
						NormalizedMAIOutput.assign(div(asReal(MAIOutput),
								mul(asReal(NumMethodsMAI), asReal(NumAttributesMAI)))))
				.build();

		// This create a table with 4 columns, and each row gives us an entry where there are two classes, two
		// features, such that the classes are encapsulating the two methods and there is a function dependency
		// between the methods
		var class_1_mmi = Variable.of("class_1_mmi");
		var class_2_mmi = Variable.of("class_2_mmi");
		var method_1_mmi = Variable.of("method_1_mmi");
		var method_2_mmi = Variable.of("method_2_mmi");
		var classFunctionalDependencies = Dnf.builder("ClassFunctionalDependencies")
				.parameters(class_1_mmi, class_2_mmi, method_1_mmi, method_2_mmi)
				.clause(classView.call(class_1_mmi), methodView.call(method_1_mmi), isEncapsulatedByView.call(method_1_mmi,
								class_1_mmi), classView.call(class_2_mmi), methodView.call(method_2_mmi),
						isEncapsulatedByView.call(method_2_mmi, class_2_mmi),
						functionalDependencyView.call(method_1_mmi, method_2_mmi))
				.build();

		// This calls the previous query and then sees for any given pair of classes in the table below, how many of
		// their encapsulated functions have dependencies between them. Note that c1,c2 are fixed from previously but
		// we are introducing a new param f3, f4 that are "new"
		// Counting is negative constraints because it doesn't assign/bound values to the variable
		// We do not pass f1-f4 because we do not want all values for them, they are free/floating based on the query
		// We needed to use an open "Varible.of()" instead of a predeclared variable
		var NormalizedMMIOutput = Variable.of("NormalizedMMIOutput", Double.class);
		var MMIOutput = Variable.of("MMIOutput", Integer.class);
		var NumMethodsMMI_1 = Variable.of("NumMethodsMMI_1", Integer.class);
		var NumMethodsMMI_2 = Variable.of("NumMethodsMMI_2", Integer.class);
		var class_1_normalized_mmi = Variable.of("class_1_normalized_mmi");
		var class_2_normalized_mmi = Variable.of("class_2_normalized_mmi");
		var method_1_normalized_mmi = Variable.of("method_1_normalized_mmi");
		var method_2_normalized_mmi = Variable.of("method_2_normalized_mmi");
		NormalizedMMI = Query.builder("MMI")
				.parameters(class_1_normalized_mmi, class_2_normalized_mmi)
				.output(NormalizedMMIOutput)
				.clause(classFunctionalDependencies.call(class_1_normalized_mmi, class_2_normalized_mmi,
								method_1_normalized_mmi, method_2_normalized_mmi),
						MMIOutput.assign(classFunctionalDependencies.count(class_1_normalized_mmi,
								class_2_normalized_mmi, Variable.of(), Variable.of())),
						NumMethodsMMI_1.assign(methodsForClass.count(class_1_normalized_mmi, Variable.of())),
						NumMethodsMMI_2.assign(methodsForClass.count(class_2_normalized_mmi, Variable.of())),
						check(greater(NumMethodsMMI_2, constant(1))),
						NormalizedMMIOutput.assign(div(asReal(MMIOutput),
								mul(asReal(NumMethodsMMI_1), asReal(sub(NumMethodsMMI_2, constant(1)))))))
				.build();

		var class_1_different_mai = Variable.of("class_1_different_mai");
		var class_2_different_mai = Variable.of("class_2_different_mai");
		var DifferentNormalizedMAIOutput = Variable.of("DifferentNormalizedMAIOutput", Double.class);
		var DifferentNormalizedMAI = Query.builder("DifferentNormalizedMAI")
				.parameters(class_1_different_mai, class_2_different_mai)
				.output(DifferentNormalizedMAIOutput)
				.clause(class_1_different_mai.notEquivalent(class_2_different_mai),
						DifferentNormalizedMAIOutput.assign(NormalizedMAI.call(class_1_different_mai,
								class_2_different_mai)))
				.build();

		var class_1_different_mmi = Variable.of("class_1_different_mmi");
		var class_2_different_mmi = Variable.of("class_2_different_mmi");
		var DifferentNormalizedMMIOutput = Variable.of("DifferentNormalizedMMIOutput", Double.class);
		var DifferentNormalizedMMI = Query.builder("DifferentNormalizedMMI")
				.parameters(class_1_different_mmi, class_2_different_mmi)
				.output(DifferentNormalizedMMIOutput)
				.clause(class_1_different_mmi.notEquivalent(class_2_different_mmi),
						DifferentNormalizedMMIOutput.assign(NormalizedMMI.call(class_1_different_mmi,
								class_2_different_mmi)))
				.build();

		// Depending on the Query we call, we need to pass in the right number of parameters
		// *** WARNING ***//
		// FOR OUR DSE FRAMEWORK WE ARE CALCULATION THE NEGATIVE CRA IN ORDER TO MINIMIZE IT INSTEAD OF MAX
		var CRA_Index = Variable.of("CRA_Index", Double.class);
		var Cohesion_MAI = Variable.of("Cohesion_MAI", Double.class);
		var Cohesion_MMI = Variable.of("Cohesion_MMI", Double.class);
		var Coupling_MAI = Variable.of("Coupling_MAI", Double.class);
		var Coupling_MMI = Variable.of("Coupling_MMI", Double.class);
		var given_class_cra_1 = Variable.of("given_class_cra_1");
		var given_class_cra_2 = Variable.of("given_class_cra_2");
		var given_class_cra_3 = Variable.of("given_class_cra_3");
		var given_class_cra_4 = Variable.of("given_class_cra_4");
		var given_class_cra_5 = Variable.of("given_class_cra_5");
		var given_class_cra_6 = Variable.of("given_class_cra_6");
		CRA = Query.builder("CRA")
				.output(CRA_Index)
				.clause(Cohesion_MAI.assign(NormalizedMAI.aggregate(REAL_SUM, given_class_cra_1, given_class_cra_1)),
						Cohesion_MMI.assign(NormalizedMMI.aggregate(REAL_SUM, given_class_cra_2, given_class_cra_2)),
						Coupling_MAI.assign(DifferentNormalizedMAI.aggregate(REAL_SUM, given_class_cra_3,
								given_class_cra_4)),
						Coupling_MMI.assign(DifferentNormalizedMMI.aggregate(REAL_SUM, given_class_cra_5,
								given_class_cra_6)),
						CRA_Index.assign(RealTerms.sub(add(Coupling_MAI, Coupling_MMI), add(Cohesion_MAI, Cohesion_MMI))))
				.build();

		// Created a Query that is the precondition for deleting a class
		// Will return a table that contians two colums, one colum with classID, another with boolean existence
		// Each row should contain a class does encapsulates no features
		deleteClass = Rule.of("deleteClassPrecondition", (builder, classID) -> builder
				.clause(
						classView.call(classID),
						not(isEncapsulatedByView.call(Variable.of(), classID)))
				.action(
						remove(class_, classID),
						delete(classID, DanglingEdges.IGNORE)
				));

		createClass = Rule.of("createClassPrecondition", (builder, featureID) -> builder
				.clause(
						features.call(featureID),
						not(isEncapsulatedByView.call(featureID, Variable.of())))
				.action((newClassID) -> List.of(
						create(newClassID),
						add(class_, newClassID),
						add(isEncapsulatedBy, featureID, newClassID)
				)));

		moveFeature = Rule.of("moveFeature",
				(builder, featureID, sourceClassID, targetClassID) -> builder
						.clause(
								features.call(featureID),
								isEncapsulatedByView.call(featureID, sourceClassID),
								classView.call(targetClassID),
								sourceClassID.notEquivalent(targetClassID)
						)
						.action(
								remove(isEncapsulatedBy, featureID, sourceClassID),
								add(isEncapsulatedBy, featureID, targetClassID)
						));

		assignFeature = Rule.of("assignFeaturePrecondition", (builder, featureID, targetClassID) ->
				builder.clause(
						nonEncapsulatedFeatures.call(featureID),
						classView.call(targetClassID)
						)
						.action(
								add(isEncapsulatedBy, featureID, targetClassID)
						));
	}

	public static void main(String[] args) {
		new CRAV2().run();
	}

	private void run() {
		var store = ModelStore.builder()
				.symbols(name, attribute, method, class_, functionalDependency, dataDependency, isEncapsulatedBy)
				.with(QueryInterpreterAdapter.builder())
				.with(ModelVisualizerAdapter.builder()
						.withOutputPath("test_output")
						.withFormat(FileFormat.DOT)
						//.withFormat(FileFormat.SVG)
						.saveStates()
						.saveDesignSpace())
				.with(StateCoderAdapter.builder())
				.with(ModificationAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder()
						.transformations(deleteClass, createClass, moveFeature, assignFeature)
						.objectives(Objectives.value(CRA))
						.accept(Criteria.whenNoMatch(nonEncapsulatedFeatures)))
				.build();

		var model = store.createEmptyModel();
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);

		var nameInterpretation = model.getInterpretation(name);
		var attributeInterpretation = model.getInterpretation(attribute);
		var methodInterpretation = model.getInterpretation(method);
		var classInterpretation = model.getInterpretation(class_);

		var functionalDependencyInterpretation = model.getInterpretation(functionalDependency);
		var dataDependencyInterpretation = model.getInterpretation(dataDependency);
		var isEncapsulatedByInterpretation = model.getInterpretation(isEncapsulatedBy);

		var modificationAdapter = model.getAdapter(ModificationAdapter.class);

		// Add all Methods
		var method1 = modificationAdapter.createObject();
		var method1Id = method1.get(0);
		var method2 = modificationAdapter.createObject();
		var method2Id = method2.get(0);
		var method3 = modificationAdapter.createObject();
		var method3Id = method3.get(0);
		var method4 = modificationAdapter.createObject();
		var method4Id = method4.get(0);
		var attribute1 = modificationAdapter.createObject();
		var attribute1Id = attribute1.get(0);
		var attribute2 = modificationAdapter.createObject();
		var attribute2Id = attribute2.get(0);
		var attribute3 = modificationAdapter.createObject();
		var attribute3Id = attribute3.get(0);
		var attribute4 = modificationAdapter.createObject();
		var attribute4Id = attribute4.get(0);
		var attribute5 = modificationAdapter.createObject();
		var attribute5Id = attribute5.get(0);

		nameInterpretation.put(method1, "M1");
		nameInterpretation.put(method2, "M2");
		nameInterpretation.put(method3, "M3");
		nameInterpretation.put(method4, "M4");
		nameInterpretation.put(attribute1, "A1");
		nameInterpretation.put(attribute2, "A2");
		nameInterpretation.put(attribute3, "A3");
		nameInterpretation.put(attribute4, "A4");
		nameInterpretation.put(attribute5, "A5");

		methodInterpretation.put(method1, true);
		methodInterpretation.put(method2, true);
		methodInterpretation.put(method3, true);
		methodInterpretation.put(method4, true);
		attributeInterpretation.put(attribute1, true);
		attributeInterpretation.put(attribute2, true);
		attributeInterpretation.put(attribute3, true);
		attributeInterpretation.put(attribute4, true);
		attributeInterpretation.put(attribute5, true);

		dataDependencyInterpretation.put(Tuple.of(method1Id, attribute1Id), true);
		dataDependencyInterpretation.put(Tuple.of(method1Id, attribute3Id), true);
		dataDependencyInterpretation.put(Tuple.of(method2Id, attribute2Id), true);
		dataDependencyInterpretation.put(Tuple.of(method3Id, attribute3Id), true);
		dataDependencyInterpretation.put(Tuple.of(method3Id, attribute4Id), true);
		dataDependencyInterpretation.put(Tuple.of(method4Id, attribute3Id), true);
		dataDependencyInterpretation.put(Tuple.of(method4Id, attribute5Id), true);

		functionalDependencyInterpretation.put(Tuple.of(method1Id, attribute3Id), true);
		functionalDependencyInterpretation.put(Tuple.of(method1Id, attribute4Id), true);
		functionalDependencyInterpretation.put(Tuple.of(method2Id, attribute1Id), true);
		functionalDependencyInterpretation.put(Tuple.of(method3Id, attribute1Id), true);
		functionalDependencyInterpretation.put(Tuple.of(method3Id, attribute4Id), true);
		functionalDependencyInterpretation.put(Tuple.of(method4Id, attribute2Id), true);

		var initialVersion = model.commit();
		queryEngine.flushChanges();

		var bestFirst = new BestFirstStoreManager(store, 500);
		bestFirst.startExploration(initialVersion);
		var resultStore = bestFirst.getSolutionStore();
		System.out.println("states size: " + resultStore.getSolutions().size());
		//model.getAdapter(ModelVisualizerAdapter.class).visualize(bestFirst.getVisualizationStore());

		double lowest_score = Double.POSITIVE_INFINITY;
		int lowest_index = 0;
		int counter = 0;
		for (var solution: resultStore.getSolutions()){
			//System.out.println("Score: " + solution.objectiveValue().get(0));
			if (solution.objectiveValue().get(0) < lowest_score){
				lowest_score = solution.objectiveValue().get(0);
				lowest_index = counter;
			}
			counter ++;
		}

		System.out.println("Best Score: " + lowest_score);
		System.out.println("Best Index: " + lowest_index);

		model.restore(resultStore.getSolutions().get(lowest_index).version());
	}
}
