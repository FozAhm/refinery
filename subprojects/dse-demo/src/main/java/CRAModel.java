import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.query.ResultSet;
import tools.refinery.store.tuple.Tuple;

import java.util.Random;

public class CRAModel {
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

	private final ResultSet<Boolean> createClassPreconditionResultSet;
	private final ResultSet<Boolean> deleteClassPreconditionResultSet;
	private final ResultSet<Boolean> moveFeaturePreconditionResultSet;
	private final ResultSet<Boolean> assignFeaturePreconditionResultSet;
	private final ResultSet<Boolean> nonEncapsulatedFeaturesResultSet;
	private final ResultSet<Double> CRAResult;

	public CRAModel(CRAStore craStore, Model model){
		this.model = model;
		this.queryEngine = model.getAdapter(ModelQueryAdapter.class);

		this.nameInterpretation = model.getInterpretation(craStore.getName());
		this.attributeInterpretation = model.getInterpretation(craStore.getAttribute());
		this.methodInterpretation = model.getInterpretation(craStore.getMethod());
		this.classInterpretation = model.getInterpretation(craStore.getClass_());
		this.nextIDInterpretation = model.getInterpretation(craStore.getNextID());

		this.functionalDependencyInterpretation = model.getInterpretation(craStore.getFunctionalDependency());
		this.dataDependencyInterpretation = model.getInterpretation(craStore.getDataDependency());
		this.isEncapsulatedByInterpretation = model.getInterpretation(craStore.getIsEncapsulatedBy());

		this.createClassPreconditionResultSet = this.queryEngine.getResultSet(craStore.getCreateClassPrecondition());
		this.deleteClassPreconditionResultSet = this.queryEngine.getResultSet(craStore.getDeleteClassPrecondition());
		this.moveFeaturePreconditionResultSet = this.queryEngine.getResultSet(craStore.getMoveFeaturePrecondition());
		this.assignFeaturePreconditionResultSet =
				this.queryEngine.getResultSet(craStore.getAssignFeaturePrecondition());

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

	public ResultSet<Boolean> getCreateClassPreconditionResultSet() {
		return createClassPreconditionResultSet;
	}
	public ResultSet<Boolean> getDeleteClassPreconditionResultSet() {
		return deleteClassPreconditionResultSet;
	}
	public ResultSet<Boolean> getMoveFeaturePreconditionResultSet() {
		return moveFeaturePreconditionResultSet;
	}
	public ResultSet<Boolean> getAssignFeaturePreconditionResultSet() {
		return assignFeaturePreconditionResultSet;
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
	public void restoreModel(long commitID) {
		this.model.restore(commitID);
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
