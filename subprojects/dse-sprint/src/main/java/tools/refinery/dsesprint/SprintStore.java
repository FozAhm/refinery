package tools.refinery.dsesprint;

import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.FunctionalQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.term.int_.IntTerms;
import tools.refinery.store.query.term.real.RealTerms;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;

import java.util.List;

import static tools.refinery.store.query.literal.Literals.assume;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.term.int_.IntTerms.*;
import static tools.refinery.store.query.term.real.RealTerms.*;

// TO-DO
// *** Need to make sure every work item has only ONE effort and/or importance value

public class SprintStore {

	// Nodes
	private final Symbol<Boolean> sprint = new Symbol<>("sprint", 1, Boolean.class, false);
	private final Symbol<Boolean> workItem = new Symbol<>("workItem", 1, Boolean.class, false);
	private final Symbol<Boolean> stakeholder = new Symbol<>("stakeholder", 1, Boolean.class, false);
	private final Symbol<Integer> workItemImportance = new Symbol<>("workItemImportance", 1, Integer.class, null);
	private final Symbol<Integer> workItemEffort = new Symbol<>("workItemEffort", 1, Integer.class, null);


	// Relationships
	private final Symbol<Boolean> sprintsItem = new Symbol<>("sprintsItem", 2, Boolean.class, false);
	private final Symbol<Boolean> stakeholdersItem = new Symbol<>("stakeholdersItem", 2, Boolean.class, false);

	// Variables

	private final Symbol<Integer> nextID = new Symbol<>("nextID", 0, Integer.class, 0);


	// Violations
	private final RelationalQuery misconfiguredSprints;
	private final RelationalQuery misconfiguredStakeholders;
	private final RelationalQuery misconfiguredWorkItemImportance;
	private final RelationalQuery misconfiguredWorkItemEffort;
	private final RelationalQuery misconfiguredSprintsItem;
	private final RelationalQuery misconfiguredStakeholdersItem;
	private final RelationalQuery constraint1;
	private final FunctionalQuery<Integer> constraint2;

	// Objectives
	private final FunctionalQuery<Double> objectiveOne;
	private final FunctionalQuery<Double> objectiveTwo;

	// Transformation Rules Preconditions
	private final RelationalQuery createSprintPrecondition;
	private final RelationalQuery deleteSprintPrecondition;
	private final RelationalQuery addWorkItemToSprintPrecondition;
	private final RelationalQuery moveWorkItemFromSprintPrecondition;

	// Model Store
	private final ModelStore store;

	public SprintStore(){

		// Views

		var sprintView = new KeyOnlyView<>(sprint);
		var workItemView = new KeyOnlyView<>(workItem);
		var stakeholderView = new KeyOnlyView<>(stakeholder);

		var workItemImportanceView = new KeyOnlyView<>(workItemImportance);
		var workItemEffortView = new KeyOnlyView<>(workItemEffort);

		var sprintsItemView = new KeyOnlyView<>(sprintsItem);
		var stakeholdersItemView = new KeyOnlyView<>(stakeholdersItem);

		// Violation Queries

		// Cannot have empty sprint (according to Meta Model)
		var emptySprint = Variable.of("emptySprint");
		misconfiguredSprints = Query.builder("misconfiguredSprints")
				.parameters(emptySprint)
				.clause(
						sprintView.call(emptySprint),
						not(sprintsItemView.call(emptySprint,Variable.of()))
				)
				.build();

		// Cannot have stakeholder without work item (according to Meta Model)
		var emptyStakeholder = Variable.of("emptyStakeholder");
		misconfiguredStakeholders = Query.builder("misconfiguredStakeholders")
				.parameters(emptyStakeholder)
				.clause(
						stakeholderView.call(emptyStakeholder),
						not(stakeholdersItemView.call(emptyStakeholder, Variable.of()))
				)
				.build();

		// Makes sure every work item has an effort value
		misconfiguredWorkItemEffort = Query.of("misconfiguredWorkItemEffort",
				(builder, tempWorkItem) ->
						builder.clause(
								workItemView.call(tempWorkItem),
								not(workItemEffortView.call())
						));

		// Makes sure every work item has an importnace value
		misconfiguredWorkItemImportance = Query.of("misconfiguredWorkItemImportance",
				(builder, tempWorkItem) ->
						builder.clause(
								workItemView.call(tempWorkItem),
								not(workItemImportanceView.call(tempWorkItem))
						));

		misconfiguredSprintsItem = Query.of("misconfiguredSprintsItem",
				(builder, tempWorkItem) ->
						builder.clause(Integer.class, (sprintCount) -> List.of(
								workItemView.call(tempWorkItem),
								sprintCount.assign(sprintsItemView.count(Variable.of(),tempWorkItem)),
								assume(IntTerms.greater(sprintCount, constant(1)))
						)));

		misconfiguredStakeholdersItem = Query.of("misconfiguredStakeholdersItem",
				(builder, tempWorkItem) ->
						builder.clause(Integer.class, (stakeholderCount) -> List.of(
								workItemView.call(tempWorkItem),
								stakeholderCount.assign(stakeholdersItemView.count(Variable.of(), tempWorkItem)),
								assume(IntTerms.greater(stakeholderCount, constant(1)))
						)));

		// Cannot have work item not assigned to a sprint (according to Constrain#1)
		var unassignedWorkItems = Variable.of("unassignedWorkItems");
		constraint1 = Query.builder("constraint1")
				.parameters(unassignedWorkItems)
				.clause(
						workItemView.call(unassignedWorkItems),
						not(sprintsItemView.call(Variable.of(), unassignedWorkItems))
				)
				.build();

		var constraint2Helper1 = Query.of("constraint2Helper1",
				Integer.class, (builder, tempWorkItem, output) ->
						builder.clause(
								workItemView.call(tempWorkItem),
								workItemEffortView.call(tempWorkItem, output)
						));
		var constraint2Helper2 = Query.of("constraint2Helper2",
				Double.class, (builder, output) ->
						builder.clause(Integer.class, (tempOutput) -> List.of(
								tempOutput.assign(constraint2Helper1.aggregate(INT_SUM, Variable.of())),
								output.assign(asReal(tempOutput))
						)));

		// Cannot have more sprints that "humanely" possible (according to Constrain#2)
		var constraint2Helper3 = Query.of("constraint2Helper3",
				(builder, tempSprint) ->
						builder.clause(
								sprintView.call(tempSprint),
								sprintsItemView.call(tempSprint, Variable.of())
						));

		constraint2 = Query.of("constraint2",
				Integer.class, (builder, output) ->
						builder.clause(Double.class, Integer.class, Integer.class,
								(totalEffort,desiredSprints,totalNonEmptySprints) -> List.of(
										totalEffort.assign(constraint2Helper2.call()),
										desiredSprints.assign(asInt(div(totalEffort, RealTerms.constant(25.0)))),
										totalNonEmptySprints.assign(constraint2Helper3.count(Variable.of())),
										output.assign(max(constant(0),sub(desiredSprints, totalNonEmptySprints)))
						)));


		// Objective Queries

		// Create table with sprint, work item and effort per row
		// The Integer.class in the beginning of the Query dictates the last variable of the subsequent lambda
		// In the last clause, it's an implicit assign for the variable output
		var objectiveOneHelper1 = Query.of("objectiveOneHelper1",
				Integer.class, (builder, tempSprint, tempWorkItem,output) ->
						builder.clause(
								sprintView.call(tempSprint),
								workItemView.call(tempWorkItem),
								sprintsItemView.call(tempSprint, tempWorkItem),
								workItemEffortView.call(tempWorkItem, output)
						));

		var objectiveOneHelper2 = Query.of("objectiveOneHelper2",
				Double.class, (builder, tempSprint, output) ->
						builder.clause(Integer.class, (tempOutput) -> List.of(
								sprintView.call(tempSprint),
								tempOutput.assign(objectiveOneHelper1.aggregate(INT_SUM, tempSprint, Variable.of())),
								output.assign(asReal(tempOutput))
						)));

		var objectiveOneHelper3 = Query.of("objectiveOneHelper3",
				Double.class, (builder, tempSprint, output) ->
						builder.clause(Double.class, (tempOutput) -> List.of(
								tempOutput.assign(objectiveOneHelper2.call(tempSprint)),
								output.assign(mul(tempOutput, tempOutput))
						)));

		objectiveOne = Query.of("objectiveOne",
				Double.class, (builder, output) ->
						builder.clause(Integer.class, Double.class, Double.class,
								(numOfSprints, sumOfSquares, sum) -> List.of(
										numOfSprints.assign(sprintView.count(Variable.of())),
										sumOfSquares.assign(objectiveOneHelper3.aggregate(REAL_SUM, Variable.of())),
										sum.assign(objectiveOneHelper2.aggregate(REAL_SUM, Variable.of())),
										output.assign(pow(
												RealTerms.sub(
														div(sumOfSquares, asReal(numOfSprints)),
														pow(div(sum,asReal(numOfSprints)), RealTerms.constant(2.0))),
												RealTerms.constant(0.5)))
						)));

		// Create table with sprint, work item and effort per row
		var objectiveTwoHelper1 = Query.of("objectiveTwoHelper1",
				Integer.class, (builder, tempStakeholder, tempSprint, tempWorkItem,output) ->
						builder.clause(
								stakeholderView.call(tempStakeholder),
								sprintView.call(tempSprint),
								stakeholdersItemView.call(tempStakeholder, tempWorkItem),
								sprintsItemView.call(tempSprint, tempWorkItem),
								workItemImportanceView.call(tempWorkItem, output)
						));

		var objectiveTwoHelper2 = Query.of("objectiveTwoHelper2",
				Double.class, (builder, tempStakeholder, tempSprint,output) ->
						builder.clause(Integer.class, (tempOutput) -> List.of(
								stakeholderView.call(tempStakeholder),
								sprintView.call(tempSprint),
								tempOutput.assign(objectiveTwoHelper1.aggregate(INT_SUM, tempStakeholder, tempSprint,
										Variable.of())),
								output.assign(asReal(tempOutput))
						)));

		var objectiveTwoHelper3 = Query.of("objectiveTwoHelper3",
				Double.class, (builder, tempStakeholder, tempSprint,output) ->
						builder.clause(Double.class, (tempOutput) -> List.of(
								tempOutput.assign(objectiveTwoHelper2.call(tempStakeholder, tempSprint)),
								output.assign(mul(tempOutput, tempOutput))
						)));

		var objectiveTwoHelper4 = Query.of("objectiveTwoHelper4",
				Double.class, (builder, tempStakeholder, output) ->
						builder.clause(Integer.class, Double.class, Double.class,
								(numOfSprints, sumOfSquares, sum) -> List.of(
										stakeholderView.call(tempStakeholder),
										numOfSprints.assign(sprintView.count(Variable.of())),
										sumOfSquares.assign(objectiveTwoHelper3.aggregate(REAL_SUM, tempStakeholder,
												Variable.of())),
										sum.assign(objectiveTwoHelper2.aggregate(REAL_SUM, tempStakeholder,
												Variable.of())),
										output.assign(pow(
												RealTerms.sub(
														div(sumOfSquares, asReal(numOfSprints)),
														pow(div(sum,asReal(numOfSprints)), RealTerms.constant(2.0))),
												RealTerms.constant(0.5)))
								)));

		var objectiveTwoHelper5 = Query.of("objectiveTwoHelper5",
				Double.class, (builder, tempStakeholder, output) ->
						builder.clause(Double.class, (tempOutput) -> List.of(
								tempOutput.assign(objectiveTwoHelper4.call(tempStakeholder)),
								output.assign(mul(tempOutput, tempOutput))
						)));

		objectiveTwo = Query.of("objectiveTwo",
				Double.class, (builder, output) ->
						builder.clause(Integer.class, Double.class, Double.class,
								(numOfStakeholders, sumOfSquares,sum) -> List.of(
										numOfStakeholders.assign(stakeholderView.count(numOfStakeholders)),
										sumOfSquares.assign(objectiveTwoHelper5.aggregate(REAL_SUM, Variable.of())),
										sum.assign(objectiveTwoHelper4.aggregate(REAL_SUM, Variable.of())),
										output.assign(pow(
												RealTerms.sub(
														div(sumOfSquares, asReal(numOfStakeholders)),
														pow(div(sum,asReal(numOfStakeholders)), RealTerms.constant(2.0))),
												RealTerms.constant(0.5)))
								)));

		// Precondition Queries
		createSprintPrecondition = Query.of("createSprintPrecondition",
				(builder, tempWorkItem) ->
						builder.clause(
								workItemView.call(tempWorkItem),
								not(sprintsItemView.call(Variable.of(), tempWorkItem))
						));

		deleteSprintPrecondition = Query.of("deleteSprintPrecondition",
				(builder, tempSprint) ->
						builder.clause(
								sprintView.call(tempSprint),
								not(sprintsItemView.call(tempSprint, Variable.of()))
						));

		addWorkItemToSprintPrecondition = Query.of("addWorkItemToSprintPrecondition",
				(builder, tempSprint, tempWorkItem) ->
						builder.clause(
								sprintView.call(tempSprint),
								workItemView.call(tempWorkItem),
								not(sprintsItemView.call(Variable.of(), tempWorkItem))
						));

		moveWorkItemFromSprintPrecondition = Query.of("moveWorkItemFromSprintPrecondition",
				(builder, sourceSprint, targetSprint, tempWorkItem) ->
						builder.clause(
								sprintsItemView.call(sourceSprint, tempWorkItem),
								sprintView.call(targetSprint),
								not(sprintsItemView.call(targetSprint, tempWorkItem))
						));


		store = ModelStore.builder()
				.symbols(sprint, workItem,stakeholder,workItemImportance,workItemEffort,
						sprintsItem,stakeholdersItem,nextID)
				.with(ViatraModelQueryAdapter.builder().queries(
						misconfiguredSprints, misconfiguredStakeholders,
						misconfiguredWorkItemImportance, misconfiguredWorkItemEffort,
						misconfiguredSprintsItem, misconfiguredStakeholders,
						constraint1, constraint2, objectiveOne, objectiveTwo,
						createSprintPrecondition, deleteSprintPrecondition,
						addWorkItemToSprintPrecondition, moveWorkItemFromSprintPrecondition
				))
				.build();
	}

	public Symbol<Boolean> getSprint() {
		return sprint;
	}

	public Symbol<Boolean> getWorkItem() {
		return workItem;
	}

	public Symbol<Boolean> getStakeholder() {
		return stakeholder;
	}


	public Symbol<Integer> getWorkItemEffort() {
		return workItemEffort;
	}

	public Symbol<Integer> getWorkItemImportance() {
		return workItemImportance;
	}


	public Symbol<Boolean> getSprintsItem() {
		return sprintsItem;
	}

	public Symbol<Boolean> getStakeholdersItem() {
		return stakeholdersItem;
	}


	public Symbol<Integer> getNextID() {
		return nextID;
	}


	public RelationalQuery getMisconfiguredSprints() {
		return misconfiguredSprints;
	}

	public RelationalQuery getMisconfiguredStakeholders() {
		return misconfiguredStakeholders;
	}

	public RelationalQuery getMisconfiguredWorkItemEffort() {
		return misconfiguredWorkItemEffort;
	}

	public RelationalQuery getMisconfiguredWorkItemImportance() {
		return misconfiguredWorkItemImportance;
	}

	public RelationalQuery getMisconfiguredSprintsItem() {
		return misconfiguredSprintsItem;
	}

	public RelationalQuery getMisconfiguredStakeholdersItem() {
		return misconfiguredStakeholdersItem;
	}

	public RelationalQuery getConstraint1() {
		return constraint1;
	}

	public FunctionalQuery<Integer> getConstraint2() {
		return constraint2;
	}


	public FunctionalQuery<Double> getObjectiveOne() {
		return objectiveOne;
	}

	public FunctionalQuery<Double> getObjectiveTwo() {
		return objectiveTwo;
	}

	public RelationalQuery getCreateSprintPrecondition() {
		return createSprintPrecondition;
	}

	public RelationalQuery getDeleteSprintPrecondition() {
		return deleteSprintPrecondition;
	}

	public RelationalQuery getAddWorkItemToSprintPrecondition() {
		return addWorkItemToSprintPrecondition;
	}

	public RelationalQuery getMoveWorkItemFromSprintPrecondition() {
		return moveWorkItemFromSprintPrecondition;
	}

	public SprintModel createEmptyModel(){
		return new SprintModel(this, store.createEmptyModel());
	}
}
