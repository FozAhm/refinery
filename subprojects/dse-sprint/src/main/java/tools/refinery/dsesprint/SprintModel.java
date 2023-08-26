package tools.refinery.dsesprint;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.ResultSet;
import tools.refinery.store.tuple.Tuple;

public class SprintModel {
	// Base Model Objects
	private final Model model;
	private final ModelQueryAdapter queryEngine;


	// Interpretations
	private final Interpretation<Boolean> sprintInterpretation;
	private final Interpretation<Boolean> workItemInterpretation;
	private final Interpretation<Boolean> stakeholderInterpretation;
	private final Interpretation<Integer> workItemImportanceInterpretation;
	private final Interpretation<Integer> workItemEffortInterpretation;

	private final Interpretation<Boolean> sprintsItemInterpretation;
	private final Interpretation<Boolean> stakeholdersItemInterpretation;

	private final Interpretation<Integer> nextIDInterpretation;


	// Result Sets from Queries
	private final ResultSet<Boolean> misconfiguredSprintsResultSet;
	private final ResultSet<Boolean> misconfiguredStakeholdersResultSet;
	private final ResultSet<Boolean> misconfiguredWorkItemImportanceResultSet;
	private final ResultSet<Boolean> misconfiguredWorkItemEffortResultSet;
	private final ResultSet<Boolean> misconfiguredSprintsItemResultSet;
	private final ResultSet<Boolean> misconfiguredStakeholdersItemResultSet;
	private final ResultSet<Boolean> constraint1ResultSet;
	private final ResultSet<Integer> constraint2ResultSet;

	private final ResultSet<Double> objectiveOneResultSet;
	private final ResultSet<Double> objectiveTwoResultSet;


	// Result Sets for Preconditions
	private final ResultSet<Boolean> createSprintPreconditionResultSet;
	private final ResultSet<Boolean> deleteSprintPreconditionResultSet;
	private final ResultSet<Boolean> addWorkItemToSprintPreconditionResultSet;
	private final ResultSet<Boolean> moveWorkItemFromSprintPreconditionResultSet;



	public SprintModel(SprintStore store, Model model){
		this.model = model;
		this.queryEngine = model.getAdapter(ModelQueryAdapter.class);

		this.sprintInterpretation = model.getInterpretation(store.getSprint());
		this.workItemInterpretation = model.getInterpretation(store.getWorkItem());
		this.stakeholderInterpretation = model.getInterpretation(store.getStakeholder());
		this.workItemImportanceInterpretation = model.getInterpretation(store.getWorkItemImportance());
		this.workItemEffortInterpretation = model.getInterpretation(store.getWorkItemEffort());

		this.sprintsItemInterpretation = model.getInterpretation(store.getSprintsItem());
		this.stakeholdersItemInterpretation = model.getInterpretation(store.getStakeholdersItem());

		this.nextIDInterpretation = model.getInterpretation(store.getNextID());

		this.misconfiguredSprintsResultSet = this.queryEngine.getResultSet(store.getMisconfiguredSprints());
		this.misconfiguredStakeholdersResultSet = this.queryEngine.getResultSet(store.getMisconfiguredStakeholders());
		this.misconfiguredWorkItemImportanceResultSet = this.queryEngine.getResultSet(store.getMisconfiguredWorkItemImportance());
		this.misconfiguredWorkItemEffortResultSet = this.queryEngine.getResultSet(store.getMisconfiguredWorkItemEffort());
		this.misconfiguredSprintsItemResultSet = this.queryEngine.getResultSet(store.getMisconfiguredSprintsItem());
		this.misconfiguredStakeholdersItemResultSet = this.queryEngine.getResultSet(store.getMisconfiguredStakeholdersItem());
		this.constraint1ResultSet = this.queryEngine.getResultSet(store.getConstraint1());
		this.constraint2ResultSet = this.queryEngine.getResultSet(store.getConstraint2());

		this.objectiveOneResultSet = this.queryEngine.getResultSet(store.getObjectiveOne());
		this.objectiveTwoResultSet = this.queryEngine.getResultSet(store.getObjectiveTwo());

		this.createSprintPreconditionResultSet = this.queryEngine.getResultSet(store.getCreateSprintPrecondition());
		this.deleteSprintPreconditionResultSet = this.queryEngine.getResultSet(store.getDeleteSprintPrecondition());
		this.addWorkItemToSprintPreconditionResultSet = this.queryEngine.getResultSet(store.getAddWorkItemToSprintPrecondition());
		this.moveWorkItemFromSprintPreconditionResultSet = this.queryEngine.getResultSet(store.getMoveWorkItemFromSprintPrecondition());

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
	private int newID(){
		int newClassID = this.nextIDInterpretation.get(Tuple.of());
		this.nextIDInterpretation.put(Tuple.of(), newClassID+1);
		return newClassID;
	}

	// Transformation Functions

	public void createSprint(Tuple activation){
		int workItemID = activation.get(0);
		int newSprintID = newID();
		this.sprintInterpretation.put(Tuple.of(newSprintID), true);
		this.sprintsItemInterpretation.put(Tuple.of(newSprintID, workItemID), true);
	}

	public void deleteSprint(Tuple activation) {
		int deleteSprintID = activation.get(0);
		this.sprintInterpretation.put(Tuple.of(deleteSprintID), false);
	}

	public void addWorkItemToSprint(Tuple activation) {
		int tempSprintID = activation.get(0);
		int tempWorkItemID = activation.get(1);
		this.sprintsItemInterpretation.put(Tuple.of(tempSprintID, tempWorkItemID), true);
	}

	public void moveWorkItemFromSprint(Tuple activation) {
		int sourceSprint = activation.get(0);
		int targetSprint = activation.get(1);
		int tempWorkItem = activation.get(2);
		this.sprintsItemInterpretation.put(Tuple.of(sourceSprint, tempWorkItem), false);
		this.sprintsItemInterpretation.put(Tuple.of(targetSprint, tempWorkItem), true);
	}
}
