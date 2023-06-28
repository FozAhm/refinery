import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQuery;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.query.ResultSet;
import tools.refinery.store.tuple.Tuple;

import java.util.Random;

public class CRAModel {
	private final Random random;
	private final Model model;
	private final ModelQueryAdapter queryEngine;
	private final Interpretation<String> nameInterpretation;
	private final Interpretation<Boolean> attributeInterpretation;
	private final Interpretation<Boolean> methodInterpretation;
	private final Interpretation<Boolean> classInterpretation;
	private final Interpretation<Integer> nextIDInterpretation;

	private final Interpretation<Boolean> functionalDependencyInterpretation;
	private final Interpretation<Boolean> dataDependencyInterpretation;
	private final Interpretation<Boolean> isEncapsulatedByInterpretation;

	private final ResultSet<Boolean> classesResultSet;
	private final ResultSet<Boolean> featuresResultSet;
	private final ResultSet<Boolean> methodsForClassResultSet;
	private final ResultSet<Boolean> attributesForClassResultSet;
	private final ResultSet<Boolean> nonEncapsulatedFeaturesResultSet;
	private final ResultSet<Double> CRAResult;

	public CRAModel(CRAStore craStore, Model model){
		random = new Random(1);
		this.model = model;
		this.queryEngine = model.getAdapter(ModelQuery.ADAPTER);

		this.nameInterpretation = model.getInterpretation(craStore.getName());
		this.attributeInterpretation = model.getInterpretation(craStore.getAttribute());
		this.methodInterpretation = model.getInterpretation(craStore.getMethod());
		this.classInterpretation = model.getInterpretation(craStore.getClass_());
		this.nextIDInterpretation = model.getInterpretation(craStore.getNextID());

		this.functionalDependencyInterpretation = model.getInterpretation(craStore.getFunctionalDependency());
		this.dataDependencyInterpretation = model.getInterpretation(craStore.getDataDependency());
		this.isEncapsulatedByInterpretation = model.getInterpretation(craStore.getIsEncapsulatedBy());

		this.classesResultSet = this.queryEngine.getResultSet(craStore.getClasses());
		this.featuresResultSet = this.queryEngine.getResultSet(craStore.getFeatures());
		this.methodsForClassResultSet = this.queryEngine.getResultSet(craStore.getMethodsForClass());
		this.attributesForClassResultSet = this.queryEngine.getResultSet(craStore.getAttributesForClass());

		this.nonEncapsulatedFeaturesResultSet = this.queryEngine.getResultSet(craStore.getNonEncapsulatedFeatures());
		this.CRAResult = this.queryEngine.getResultSet(craStore.getCRA());
	}

	public Interpretation<String> getNameInterpretation() {
		return nameInterpretation;
	}
	public Interpretation<Boolean> getAttributeInterpretation() {
		return attributeInterpretation;
	}
	public Interpretation<Boolean> getMethodInterpretation() {
		return methodInterpretation;
	}
	public Interpretation<Boolean> getClassInterpretation() {
		return classInterpretation;
	}
	public Interpretation<Integer> getNextIDInterpretation() {
		return  nextIDInterpretation;
	}

	public Interpretation<Boolean> getFunctionalDependencyInterpretation() {
		return  functionalDependencyInterpretation;
	}
	public Interpretation<Boolean> getDataDependencyInterpretation() {
		return dataDependencyInterpretation;
	}
	public Interpretation<Boolean> getIsEncapsulatedByInterpretation() {
		return isEncapsulatedByInterpretation;
	}

	public ResultSet<Boolean> getMethodsForClassResultSet() {
		return methodsForClassResultSet;
	}
	public ResultSet<Boolean> getAttributesForClassResultSet() {
		return attributesForClassResultSet;
	}
	public ResultSet<Boolean> getNonEncapsulatedFeaturesResultSet() {
		return nonEncapsulatedFeaturesResultSet;
	}
	public ResultSet<Double> getCRAResult() {
		return CRAResult;
	}

	public long commit(){
		return this.model.commit();
	}
	public void updateResultSets(){
		this.queryEngine.flushChanges();
	}

	public void assignFeature(Tuple activation) {
		int featureID = activation.get(0);
		int targetClassID = activation.get(1);
		this.isEncapsulatedByInterpretation.put(Tuple.of(featureID, targetClassID), true);
	}

	public void createClass(Tuple activation){
		int featureID = activation.get(0);
		int newClassID = this.nextIDInterpretation.get(Tuple.of());
		this.nextIDInterpretation.put(Tuple.of(), newClassID+1);
		this.classInterpretation.put(Tuple.of(newClassID), true);
		this.isEncapsulatedByInterpretation.put(Tuple.of(featureID, newClassID), true);
	}

	public void deleteEmptyClass(Tuple activation){
		int deleteClassID = activation.get(0);
		this.classInterpretation.put(Tuple.of(deleteClassID), false);
	}

	public void moveFeature(Tuple activation){
		int featureID = activation.get(0);
		int sourceClassID = activation.get(1);
		int targetClassID = activation.get(2);
		this.isEncapsulatedByInterpretation.put(Tuple.of(featureID, sourceClassID), false);
		this.isEncapsulatedByInterpretation.put(Tuple.of(featureID, targetClassID), true);
	}
}
