package tools.refinery.dse.demo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQuery;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.QueryBuilder;
import tools.refinery.store.query.literal.Literals;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.term.int_.IntTerms;
import tools.refinery.store.query.term.real.RealTerms;
import tools.refinery.store.query.viatra.ViatraModelQuery;
import tools.refinery.store.query.view.KeyOnlyRelationView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import static org.junit.jupiter.api.Assertions.*;
import static tools.refinery.store.query.literal.Literals.*;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.term.int_.IntTerms.*;
import static tools.refinery.store.query.term.int_.IntTerms.constant;
import static tools.refinery.store.query.term.real.RealTerms.*;

public class ExampleTest {
	@Test
	void typeConstraintTest() {
		// Type can be of any class
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var asset = new Symbol<>("Asset", 1, Boolean.class, false);

		// All the people who exist, this is a view on a table
		var personView = new KeyOnlyRelationView<>(person);
		var assetView = new KeyOnlyRelationView<>(asset);

		// Defining a var
		var p1 = Variable.of("p1");
		var a1 = Variable.of("a2");
		// a query
		var predicate = Query.builder("TypeConstraint")
				.parameters(p1)
				.clause(personView.call(p1))
				.build();

		var predicate2 = Query.builder("AssetConstraint")
				.parameters(a1)
				.clause(assetView.call(a1), not(personView.call(a1)))
				.build();

		// Can only have a single engine, but multiple queries
		var store = ModelStore.builder()
				.symbols(person, asset)
				.with(ViatraModelQuery.ADAPTER)
				//.query(predicate)
				.queries(predicate, predicate2)
				.build();

		// Creating an empty model from the Model Store that we just created and configured
		var model = store.createEmptyModel();
		// Get the table of your version/working copy
		var personInterpretation = model.getInterpretation(person);
		var assetInterpretation = model.getInterpretation(asset);

		// Getting the query engine of your working/version
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
		// Get the current results
		var predicateResultSet = queryEngine.getResultSet(predicate);
		var predicateResultSet2 = queryEngine.getResultSet(predicate2);

		// Adding a person
		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		// The "key" has ot be globally unique
		assetInterpretation.put(Tuple.of(1), true);
		assetInterpretation.put(Tuple.of(2), true);
		assetInterpretation.put(Tuple.of(3), true);
		assetInterpretation.put(Tuple.of(4), true);

		// Updating the results of the queries
		queryEngine.flushChanges();
		assertEquals(2,predicateResultSet.size());
		assertEquals(3,predicateResultSet2.size());
	}

	@Test
	void craChallenge() {
		var name = new Symbol<>("name", 1, String.class, null);
		var attribute = new Symbol<>("attribute", 1, Boolean.class, false);
		var method = new Symbol<>("method", 1, Boolean.class, false);
		var class_ = new Symbol<>("class", 1, Boolean.class, false);
		var nextID = new Symbol<>("nextID", 0, Integer.class, 0);

		var functionalDependency = new Symbol<>("functionalDependency", 2, Boolean.class, false);
		var dataDependency = new Symbol<>("dataDependency", 2, Boolean.class, false);
		var isEncapsulatedBy = new Symbol<>("isEncapsulatedBy", 2, Boolean.class, false);

		var attributeView = new KeyOnlyRelationView<>(attribute);
		var methodView = new KeyOnlyRelationView<>(method);
		var classView = new KeyOnlyRelationView<>(class_);

		var functionalDependencyView = new KeyOnlyRelationView<>(functionalDependency);
		var dataDependencyView = new KeyOnlyRelationView<>(dataDependency);
		var isEncapsulatedByView = new KeyOnlyRelationView<>(isEncapsulatedBy);

		// Create a query that will return the id's of all features, this query is designed to be used by other
		// queries hence the DNF class
		var feature1 = Variable.of("feature1");
		var features = Query.builder("AllFeatures")
				.parameters(feature1)
				.clause(attributeView.call(feature1))
				.clause(methodView.call(feature1))
				.build();

		// Create a query that will return the id's of all features that are not encapsulated by a class
		var feature2 = Variable.of("feature2");
		var class2 = Variable.of("class2");
		var nonEncapsulatedFeatures = Query.builder("NonEncapsulatedFeatures")
				.parameters(feature2)
				.clause(features.call(feature2), not(isEncapsulatedByView.call(feature2,class2)))
				.build();


		// Return a table with all the methods for each given class
		var given_class_for_method = Variable.of("given_class_for_method");
		var associated_method = Variable.of("associated_method");
		var methodsForClass = Query.builder("methodsForClass")
				.parameters(given_class_for_method, associated_method)
				.clause(isEncapsulatedByView.call(associated_method,given_class_for_method),
						methodView.call(associated_method))
				.build();

		// Return a table with all the attributes for each given class
		var given_class_for_attribute = Variable.of("given_class_for_attribute");
		var associated_attribute = Variable.of("associated_attribute");
		var attributesForClass = Query.builder("attributesForClass")
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
		var given_method_mai_1 = Variable.of("given_method_mai_1");
		var given_attribute_mai_1 = Variable.of("given_attribute_mai_1");
		var given_method_mai_2 = Variable.of("given_method_mai_2");
		var given_attribute_mai_2 = Variable.of("given_attribute_mai_2");
		var NormalizedMAI = Query.builder("MAI")
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
		var given_method_mmi_1 = Variable.of("given_method_mmi_1");
		var given_method_mmi_2 = Variable.of("given_method_mmi_2");
		var NormalizedMMI = Query.builder("MMI")
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
		var CRA = Query.builder("CRA")
				.output(CRA_Index)
				.clause(Cohesion_MAI.assign(NormalizedMAI.aggregate(REAL_SUM, given_class_cra_1, given_class_cra_1)),
						Cohesion_MMI.assign(NormalizedMMI.aggregate(REAL_SUM, given_class_cra_2, given_class_cra_2)),
						Coupling_MAI.assign(DifferentNormalizedMAI.aggregate(REAL_SUM, given_class_cra_3,
								given_class_cra_4)),
						Coupling_MMI.assign(DifferentNormalizedMMI.aggregate(REAL_SUM, given_class_cra_5,
								given_class_cra_6)),
						CRA_Index.assign(sub(add(Cohesion_MAI, Cohesion_MMI), add(Coupling_MAI, Coupling_MMI))))
				.build();


		var store = ModelStore.builder()
				.symbols(name, attribute, method, class_, nextID, functionalDependency, dataDependency,
						isEncapsulatedBy)
				.with(ViatraModelQuery.ADAPTER)
				.queries(nonEncapsulatedFeatures, CRA)
				.build();

		var model = store.createEmptyModel();

		var nameInterpretation = model.getInterpretation(name);
		var attributeInterpretation = model.getInterpretation(attribute);
		var methodInterpretation = model.getInterpretation(method);
		var classInterpretation = model.getInterpretation(class_);
		var nextIDInterpretation = model.getInterpretation(nextID);

		var functionalDependencyInterpretation = model.getInterpretation(functionalDependency);
		var dataDependencyInterpretation = model.getInterpretation(dataDependency);
		var isEncapsulatedByInterpretation = model.getInterpretation(isEncapsulatedBy);

		// Getting the query engine of your working/version
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
		// Get the current results
		var nonEncapsulatedFeaturesResultSet = queryEngine.getResultSet(nonEncapsulatedFeatures);
		var CRAResult = queryEngine.getResultSet(CRA);

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

		// Calculated the changes
		queryEngine.flushChanges();

		// Number of non encapsulated features should be 9
		assertEquals(9,nonEncapsulatedFeaturesResultSet.size());
		// Return true if the "0th" item (in our case M4) is not encapsulated
		assertTrue(nonEncapsulatedFeaturesResultSet.get(Tuple.of(0)));

		// Commit our initial model
		var initalStateID = model.commit();

		// Get the next ID
		/*
		int tempID = nextIDInterpretation.get(Tuple.of());
		nextIDInterpretation.put(Tuple.of(), tempID+1);
		classInterpretation.put(Tuple.of(tempID), true);
		isEncapsulatedByInterpretation.put(Tuple.of(0, tempID), true);
		queryEngine.flushChanges();
		assertEquals(8,nonEncapsulatedFeaturesResultSet.size());
		assertFalse(nonEncapsulatedFeaturesResultSet.get(Tuple.of(0)));
		*/

		System.out.println("Adding Classes for all non Encapsulated Features");
		var cursor = nonEncapsulatedFeaturesResultSet.getAll();
		while (cursor.move()){
			//System.out.println(cursor.getKey());
			//System.out.println(cursor.getValue());
			int tempID = nextIDInterpretation.get(Tuple.of());
			nextIDInterpretation.put(Tuple.of(), tempID+1);
			classInterpretation.put(Tuple.of(tempID), true);
			// Our query will only result with tuple keys of arity 1, hence we take the 0th element
			isEncapsulatedByInterpretation.put(Tuple.of(cursor.getKey().get(0), tempID), true);
		}
		queryEngine.flushChanges();

		// Size gets the number of entries with a non-default value
		assertEquals(0,nonEncapsulatedFeaturesResultSet.size());
		assertFalse(nonEncapsulatedFeaturesResultSet.get(Tuple.of(0)));

		System.out.println("CRA Index for Dumb DSE");
		System.out.println(CRAResult.get(Tuple.of()));

		// Create encapsulating Class for all features

		// Look into functionalQuery

	}
}
