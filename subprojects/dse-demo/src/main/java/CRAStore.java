import tools.refinery.store.query.dnf.*;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.term.real.RealTerms;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.RelationalQuery;

import static tools.refinery.store.query.literal.Literals.assume;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.term.int_.IntTerms.constant;
import static tools.refinery.store.query.term.int_.IntTerms.greater;
import static tools.refinery.store.query.term.int_.IntTerms.sub;
import static tools.refinery.store.query.term.real.RealTerms.*;
import static tools.refinery.store.query.term.real.RealTerms.asReal;

public class CRAStore {
	private final Symbol<String> name = new Symbol<>("name", 1, String.class, null);
	private final Symbol<Boolean> attribute = new Symbol<>("attribute", 1, Boolean.class, false);
	private final Symbol<Boolean> method = new Symbol<>("method", 1, Boolean.class, false);
	private final Symbol<Boolean> class_ = new Symbol<>("class", 1, Boolean.class, false);
	private final Symbol<Integer> nextID = new Symbol<>("nextID", 0, Integer.class, 0);

	private final Symbol<Boolean> functionalDependency = new Symbol<>("functionalDependency", 2, Boolean.class, false);
	private final Symbol<Boolean> dataDependency = new Symbol<>("dataDependency", 2, Boolean.class, false);
	private final Symbol<Boolean> isEncapsulatedBy = new Symbol<>("isEncapsulatedBy", 2, Boolean.class, false);

	// *** What is a Relational Query
	private final RelationalQuery classes;
	private final RelationalQuery features;
	private final RelationalQuery nonEncapsulatedFeatures;
	private final RelationalQuery methodsForClass;
	private final RelationalQuery attributesForClass;
	private final RelationalQuery deleteClassPrecondition;
	private final RelationalQuery createClassPrecondition;
	private final RelationalQuery moveFeaturePrecondition;
	private final RelationalQuery assignFeaturePrecondition;
	private final FunctionalQuery<Double> NormalizedMAI;
	private final FunctionalQuery<Double> NormalizedMMI;
	private final FunctionalQuery<Double> CRA;
	private final ModelStore store;

	public CRAStore() {
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
				.clause(features.call(feature2), not(isEncapsulatedByView.call(feature2,class2)))
				.build();

		// Return a table with all the methods for each given class
		var given_class_for_method = Variable.of("given_class_for_method");
		var associated_method = Variable.of("associated_method");
		methodsForClass = Query.builder("methodsForClass")
				.parameters(given_class_for_method, associated_method)
				.clause(isEncapsulatedByView.call(associated_method,given_class_for_method),
						methodView.call(associated_method))
				.build();

		// Return a table with all the attributes for each given class
		var given_class_for_attribute = Variable.of("given_class_for_attribute");
		var associated_attribute = Variable.of("associated_attribute");
		attributesForClass = Query.builder("attributesForClass")
				.parameters(given_class_for_attribute, associated_attribute)
				.clause(isEncapsulatedByView.call(associated_attribute,given_class_for_attribute),
						attributeView.call(associated_attribute))
				.build();

		// Gives us a table for all class pairs and their data dependencies within
		var class_1_mai = Variable.of("class_1_mai");
		var class_2_mai = Variable.of("class_2_mai");
		var method_1_mai = Variable.of("method_1_mai");
		var attribute_1_mai = Variable.of("attribute_1_mai");
		var classDataDependencies = Dnf.builder("ClassDataDependencies")
				.parameters(class_1_mai, class_2_mai, method_1_mai, attribute_1_mai)
				.clause(classView.call(class_1_mai), methodView.call(method_1_mai), isEncapsulatedByView.call(method_1_mai,class_1_mai), classView.call(class_2_mai),
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
				.clause(classDataDependencies.call(class_1_normalized_mai,class_2_normalized_mai,
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
				.clause(classView.call(class_1_mmi), methodView.call(method_1_mmi),isEncapsulatedByView.call(method_1_mmi,
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
				.clause(classFunctionalDependencies.call(class_1_normalized_mmi,class_2_normalized_mmi,
								method_1_normalized_mmi, method_2_normalized_mmi),
						MMIOutput.assign(classFunctionalDependencies.count(class_1_normalized_mmi,
								class_2_normalized_mmi, Variable.of(), Variable.of())),
						NumMethodsMMI_1.assign(methodsForClass.count(class_1_normalized_mmi, Variable.of())),
						NumMethodsMMI_2.assign(methodsForClass.count(class_2_normalized_mmi, Variable.of())),
						assume(greater(NumMethodsMMI_2, constant(1))),
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
						CRA_Index.assign(RealTerms.sub(add(Cohesion_MAI, Cohesion_MMI), add(Coupling_MAI, Coupling_MMI))))
				.build();

		// Created a Query that is the precondition for deleting a class
		// Will return a table that contians two colums, one colum with classID, another with boolean existence
		// Each row should contain a class does encapsulates no features
		deleteClassPrecondition = Query.of("deleteClassPrecondition", (builder, classID) -> builder.clause(
				classView.call(classID),
				not(isEncapsulatedByView.call(Variable.of(), classID))
		));

		createClassPrecondition = Query.of("createClassPrecondition", (builder, featureID) -> builder.clause(
				features.call(featureID),
				not(isEncapsulatedByView.call(featureID, Variable.of()))
		));

		moveFeaturePrecondition = Query.of("moveFeaturePrecondition", (builder, featureID, sourceClassID,
				targetClassID) -> builder.clause(
						features.call(featureID),
				isEncapsulatedByView.call(featureID, sourceClassID),
				classView.call(targetClassID),
				sourceClassID.notEquivalent(targetClassID)
		));

		assignFeaturePrecondition = Query.of("assignFeaturePrecondition", (builder, featureID, targetClassID) ->
				builder.clause(
						nonEncapsulatedFeatures.call(featureID),
						classView.call(targetClassID)
				));

		store = ModelStore.builder()
				.symbols(name, attribute, method, class_, nextID, functionalDependency, dataDependency,
						isEncapsulatedBy)
				.with(ViatraModelQueryAdapter.builder().queries(createClassPrecondition, deleteClassPrecondition,
						moveFeaturePrecondition, assignFeaturePrecondition, nonEncapsulatedFeatures, CRA))
				.build();
	}

	public Symbol<String> getName(){
		return name;
	}
	public Symbol<Boolean> getAttribute(){
		return attribute;
	}

	public Symbol<Boolean> getMethod(){
		return method;
	}

	public Symbol<Boolean> getClass_(){
		return class_;
	}

	public Symbol<Integer> getNextID(){
		return nextID;
	}

	public Symbol<Boolean> getFunctionalDependency() {
		return functionalDependency;
	}

	public Symbol<Boolean> getDataDependency() {
		return dataDependency;
	}

	public Symbol<Boolean> getIsEncapsulatedBy() {
		return isEncapsulatedBy;
	}


	public RelationalQuery getDeleteClassPrecondition() {
		return deleteClassPrecondition;
	}

	public RelationalQuery getCreateClassPrecondition() {
		return createClassPrecondition;
	}

	public RelationalQuery getMoveFeaturePrecondition() {
		return moveFeaturePrecondition;
	}

	public RelationalQuery getAssignFeaturePrecondition() {
		return assignFeaturePrecondition;
	}
	public RelationalQuery getNonEncapsulatedFeatures() {
		return nonEncapsulatedFeatures;
	}

	public FunctionalQuery<Double> getCRA() {
		return CRA;
	}

	public CRAModel createEmptyModel() {
		return new CRAModel(this, store.createEmptyModel());
	}
}
