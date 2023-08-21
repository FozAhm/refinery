package tools.refinery.cubesat;

import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.*;
import tools.refinery.store.query.literal.Literals;
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
import static tools.refinery.store.query.term.int_.IntTerms.constant;
import static tools.refinery.store.query.term.int_.IntTerms.eq;
import static tools.refinery.store.query.term.int_.IntTerms.less;
import static tools.refinery.store.query.term.int_.IntTerms.notEq;
import static tools.refinery.store.query.term.real.RealTerms.*;
import static tools.refinery.store.query.term.real.RealTerms.constant;

// *** Misconfigured Checks
// *** non comformant inital model
// *** Make sure that all payload have a correponding spacecraft
// *** make sure that all comm subsystem are attached to a satellite properly
// *** make sure that a sat with a cat has an actual corresponsing sat


// *** Transformation Rules
// *** Delete Outgoing Edge
// Remove Edge if no Incoming Edge
// Remove all incoming edges


public class CubeSatStore {
	// Spacecraft & Equipment
	private final Symbol<Boolean> spacecraft = new Symbol<>("spacecraft", 1, Boolean.class, false);
	private final Symbol<Boolean> hasPayload = new Symbol<>("hasPayload", 1, Boolean.class, false);
	private final Symbol<Boolean> groundStation = new Symbol<>("groundStation", 1, Boolean.class, false);

	// Spacecraft Type
	private final Symbol<Boolean> cube3U = new Symbol<>("cube3U", 1, Boolean.class, false);
	private final Symbol<Boolean> cube6U = new Symbol<>("cube6U", 1, Boolean.class, false);
	private final Symbol<Boolean> smallSat = new Symbol<>("smallSat", 1, Boolean.class, false);

	// Communication System
	private final Symbol<Boolean> kaComm = new Symbol<>("kaComm", 1, Boolean.class, false);
	private final Symbol<Boolean> xComm = new Symbol<>("xComm", 1, Boolean.class, false);
	private final Symbol<Boolean> uhfComm = new Symbol<>("uhfComm", 1, Boolean.class, false);


	// Relationships
	// Relationship between a satellite and its comm sub system
	private final Symbol<Boolean> commSubSystem = new Symbol<>("commSubSystem", 2, Boolean.class, false);
	// Relationship between two comm sub systems
	private final Symbol<Boolean> commLink = new Symbol<>("commLink", 2, Boolean.class, false);

	// Symbols
	private final Symbol<Double> tObservation = new Symbol<>("tObservation", 0, Double.class, null);
	private final Symbol<Integer> nextID = new Symbol<>("nextID", 0, Integer.class, 0);

	// Queries
	private final RelationalQuery misconfiguredSpacecraftHelper;
	private final RelationalQuery misconfiguredSpacecraft;

	private final FunctionalQuery<Double> coverage;
	private final FunctionalQuery<Double> dataCollected;

	private final RelationalQuery communicatingElement;
	private final RelationalQuery misconfiguredCommCount;
	private final RelationalQuery misconfiguredSatComm;
	private final RelationalQuery satLinkHelper;
	private final RelationalQuery misconfiguredCommLink;

	private final RelationalQuery misconfiguredComm;
	private final RelationalQuery unusedComm;
	private final RelationalQuery allComms;
	private final RelationalQuery deadComm;
	private final RelationalQuery misconfiguredPayloadCount;
	private final RelationalQuery misconfiguredSatCommLink;
	private final RelationalQuery groundPathHelper;
	private final RelationalQuery misconfiguredGroundPath;
	private final RelationalQuery satLink;
	private final RelationalQuery commLoop;

	private final RelationalQuery tTransmitHelper1;
	private final FunctionalQuery<Integer> tTransmitHelper2;
	private final FunctionalQuery<Double> linkSpeed;
	private final FunctionalQuery<Double> tTransmit;
	private final FunctionalQuery<Double> tMission;

	private final FunctionalQuery<Double> satCostHelper1;
	private final FunctionalQuery<Double> satCostHelper2;
	private final FunctionalQuery<Double> satCostHelper3;
	private final FunctionalQuery<Double> satCost;
	private final FunctionalQuery<Double> missionCost;

	// Precondition Queries
	private final RelationalQuery hasSpaceForComm;
	private final RelationalQuery hasXComm;
	private final RelationalQuery addXCommToSpacecraftPrecondition;
	private final RelationalQuery removeXCommFromSpacecraftPrecondition;
	private final RelationalQuery hasUhfComm;
	private final RelationalQuery addUhfCommToSpacecraftPrecondition;
	private final RelationalQuery removeUhfCommFromSpacecraftPrecondition;
	private final RelationalQuery hasKaComm;
	private final RelationalQuery addKaCommToSpacecraftPrecondition;
	private final RelationalQuery removeKaCommFromSpacecraftPrecondition;
	private final RelationalQuery hasNoOutCommLink;
	private final RelationalQuery addXCommLinkPrecondition;
	private final RelationalQuery addUhfCommLinkPrecondition;
	private final RelationalQuery addKaCommLinkPrecondition;
	private final RelationalQuery removeCommLinkPrecondition;
	private final RelationalQuery addPayloadToSpacecraftPrecondition;
	private final RelationalQuery removePayloadFromSpacecraftPrecondition;
	private final RelationalQuery removeSmallSatSpacecraftPrecondition;
	private final RelationalQuery removeCube3USpacecraftPrecondition;
	private final RelationalQuery removeCube6USpacecraftPrecondition;



	private final ModelStore store;

	public CubeSatStore() {
		var spacecraftView = new KeyOnlyView<>(spacecraft);
		var hasPayloadView = new KeyOnlyView<>(hasPayload);
		var groundStationView = new KeyOnlyView<>(groundStation);

		var cube3UView = new KeyOnlyView<>(cube3U);
		var cube6UView = new KeyOnlyView<>(cube6U);
		var smallSatView = new KeyOnlyView<>(smallSat);

		var kaCommView = new KeyOnlyView<>(kaComm);
		var xCommView = new KeyOnlyView<>(xComm);
		var uhfCommView = new KeyOnlyView<>(uhfComm);

		var commSubSystemView = new KeyOnlyView<>(commSubSystem);
		var commLinkView = new KeyOnlyView<>(commLink);

		var tObservationView = new KeyOnlyView<>(tObservation);

		// Finds all cases of a Satellite belonging to more than one type
		var sat0 = Variable.of("Sat0");
		misconfiguredSpacecraftHelper = Query.builder("misconfiguredSpacecraftHelper")
				.parameters(sat0)
				.clause(cube3UView.call(sat0), cube6UView.call(sat0))
				.clause(cube6UView.call(sat0), smallSatView.call(sat0))
				.clause(smallSatView.call(sat0), cube3UView.call(sat0))
				.build();

		// Ensure that a spacecraft only appears as one satellite
		var sat1 = Variable.of("sat1");
		misconfiguredSpacecraft = Query.builder("misconfiguredSpacecraft")
				.parameters(sat1)
				.clause(spacecraftView.call(sat1), misconfiguredSpacecraftHelper.call(sat1))
				.clause(spacecraftView.call(sat1), not(cube3UView.call(sat0)), not(cube6UView.call(sat0)), not(smallSatView.call(sat0)))
				.build();

		// We cannot assign a value to temp_tObservation as tObservationView is an arity zero table (one row)
		// therefore we must assign all values from tObservationView table to the temp_tObservation variable
		// Since there is only one variable in the table, temp_tObservation will take the value of the single variable
		var coverage_score = Variable.of("coverage_score", Double.class);
		var temp_tObservation = Variable.of("temp_tObservation", Double.class);
		var numOfSpacecraftwithPayload = Variable.of("numOfSpacecraftwithPayload", Integer.class);
		coverage = Query.builder("coverage")
				.output(coverage_score)
				.clause(tObservationView.call(temp_tObservation),
						numOfSpacecraftwithPayload.assign(hasPayloadView.count(Variable.of())),
						coverage_score.assign(add(pow(sub(constant(1.0), div(constant(2.0),
								asReal(numOfSpacecraftwithPayload))), add(constant(1.0), div(constant(9.0),
								temp_tObservation))), mul(constant(0.05), div(temp_tObservation, constant(3.0))))))
				.build();

		// Get the amount of data collected by any given Satellite
		// Return data amount in megabytes
		var temp_tObservation_1 = Variable.of("temp_tObservation", Double.class);
		var dScience = Variable.of("dScience", Double.class);
		dataCollected = Query.builder("dataCollected")
				.output(dScience)
				.clause(tObservationView.call(temp_tObservation_1),
						dScience.assign(mul(constant(5400.0), temp_tObservation_1)))
				.build();

		// Returns all the satellites & ground stations
		var commElement1 = Variable.of("commElement1");
		communicatingElement = Query.builder("communicatingElement")
				.parameters(commElement1)
				.clause(spacecraftView.call(commElement1))
				.clause(groundStationView.call(commElement1))
				.build();

		// Returns all Satellites/GS with more than two communication sub systems
		var sat2 = Variable.of("sat2");
		var commCount = Variable.of("commCount", Integer.class);
		misconfiguredCommCount = Query.builder("misconfiguredCommCount")
				.parameters(sat2)
				.clause(communicatingElement.call(sat2),
						commCount.assign(commSubSystemView.count(sat2, Variable.of())),
						assume(greater(commCount, constant(2))))
				.build();

		// Need Query for misconfigured comm sub system in relation to a satellite
		var sat18 = Variable.of("sat18");
		var commSubSys0 = Variable.of("commSubSys0");
		misconfiguredSatComm = Query.builder("misconfiguredSatComm")
				.parameters(sat18, commSubSys0)
				.clause(groundStationView.call(sat18), uhfCommView.call(commSubSys0),
						commSubSystemView.call(sat18, commSubSys0))
				.clause(spacecraftView.call(sat18), kaCommView.call(commSubSys0),
						commSubSystemView.call(sat18, commSubSys0), not(smallSatView.call(sat18)))
				.build();

		// Helper for misconfigured communication links
		var commSubSys1 = Variable.of("commSubSys1");
		var commSubSys2 = Variable.of("commSubSys2");
		misconfiguredCommLink = Query.builder("misconfiguredCommLink")
				.parameters(commSubSys1, commSubSys2)
				.clause(commLinkView.call(commSubSys1, commSubSys2), kaCommView.call(commSubSys1),
						not(kaCommView.call(commSubSys2)))
				.clause(commLinkView.call(commSubSys1, commSubSys2), uhfCommView.call(commSubSys1),
						not(uhfCommView.call(commSubSys2)))
				.clause(commLinkView.call(commSubSys1, commSubSys2), xCommView.call(commSubSys1),
						not(xCommView.call(commSubSys2)))
				.build();

		var commSubSys10 = Variable.of("commSubSys10");
		allComms = Query.builder("allComms")
				.parameters(commSubSys10)
				.clause(kaCommView.call(commSubSys10))
				.clause(xCommView.call(commSubSys10))
				.clause(uhfCommView.call(commSubSys10))
				.build();

		// Returns comms that appear in more than one type of system
		var commSubSys14 = Variable.of("commSubSys14");
		misconfiguredComm = Query.builder("misconfiguredComm")
				.parameters(commSubSys14)
				.clause(uhfCommView.call(commSubSys14), xCommView.call(commSubSys14))
				.clause(xCommView.call(commSubSys14), kaCommView.call(commSubSys14))
				.clause(kaCommView.call(commSubSys14), uhfCommView.call(commSubSys14))
				.build();

		// Must only appear once in payload
		var tempPayloadCount = Variable.of("tempPayloadCount", Integer.class);
		misconfiguredPayloadCount = Query.builder("misconfiguredPayloadCount")
				.parameters()
				.clause(tempPayloadCount.assign(hasPayloadView.count(Variable.of())),
						assume(less(tempPayloadCount, constant(2))))
				.build();

		// Returns all the comms not attached to a satelite
		var commSubSys9 = Variable.of("commSubSys9");
		unusedComm = Query.builder("unusedComm")
				.parameters(commSubSys9)
				.clause(allComms.call(commSubSys9), not(commSubSystemView.call(Variable.of(), commSubSys9)))
				.build();

		// Returns a commsub system that is not being used
		var commSubSys11 = Variable.of("commSubSys11");
		deadComm = Query.builder("deadComm")
				.parameters(commSubSys11)
				.clause(allComms.call(commSubSys11),
						not(commLinkView.call(commSubSys11, Variable.of())),
						not(commLinkView.call(Variable.of(), commSubSys11)))
				.build();

		var sat21 = Variable.of("sat21");
		var sat22 = Variable.of("sat22");
		var commSubSys12 = Variable.of("commSubSys12");
		var commSubSys13 = Variable.of("commSubSys13");
		satLinkHelper = Query.builder("satLinkHelper")
				.parameters(sat21, sat22, commSubSys12, commSubSys13)
				.clause(commSubSystemView.call(sat21, commSubSys12), commSubSystemView.call(sat22, commSubSys13),
						commLinkView.call(commSubSys12, commSubSys13))
				.build();

		// Returns all satellites with more than one outgoing connection or a GS with any outgoing link
		var sat19 = Variable.of("sat19");
		var tempOutgoingCount = Variable.of("tempOutgoingCount", Integer.class);
		misconfiguredSatCommLink = Query.builder("misconfiguredSatCommLink")
				.parameters(sat19)
				.clause(spacecraftView.call(sat19),
						tempOutgoingCount.assign(satLinkHelper.count(sat19, Variable.of(), Variable.of(),
								Variable.of())),
						assume(notEq(tempOutgoingCount, constant(1))))
				.clause(groundStationView.call(sat19),
						satLinkHelper.call(sat19, Variable.of(), Variable.of(), Variable.of()))
				.build();

		// Returns all the links between two Satellites (or GS)
		var sat3 = Variable.of("sat3");
		var sat4 = Variable.of("sat4");
		var commSubSys3 = Variable.of("commSubSys3");
		var commSubSys4 = Variable.of("commSubSys4");
		satLink = Query.builder("satLink")
				.parameters(sat3, sat4)
				.clause(commSubSystemView.call(sat3, commSubSys3), commSubSystemView.call(sat4, commSubSys4),
						commLinkView.call(commSubSys3, commSubSys4))
				.build();

		// Gets all the Satellites that have a loop
		var sat5 = Variable.of("sat5");
		commLoop = Query.builder("commLoop")
				.parameters(sat5)
				.clause(satLink.callTransitive(sat5, sat5))
				.build();

		// Gets a list of all the Satellites communicating to a given satellite
		// *** the second clause needs to be reviewed
		var sat6 = Variable.of("sat6");
		var sat7 = Variable.of("sat7");
		tTransmitHelper1 = Query.builder("tTransmitHelper1")
				.parameters(sat6, sat7)
				.clause(satLink.callTransitive(sat7, sat6), hasPayloadView.call(sat7))
				.clause(sat7.isEquivalent(sat6), hasPayloadView.call(sat6))
				.build();

		// Output the number of satellite data you have to output
		var sat8 = Variable.of("sat8");
		var numSatDataToOutput = Variable.of("numSatDataToOutput", Integer.class);
		tTransmitHelper2 = Query.builder("tTransmitHelper2")
				.parameters(sat8)
				.output(numSatDataToOutput)
				.clause(numSatDataToOutput.assign(tTransmitHelper1.count(sat8, Variable.of())))
				.build();

		// Returns the speed between two satellites (or GS) depending on their comm types
		var sat9 = Variable.of("sat9");
		var commSubSys7 = Variable.of("commSubSys7");
		var sat10 = Variable.of("sat10");
		var commSubSys8 = Variable.of("commSubSys8");
		var outputSpeed = Variable.of("outputSpeed", Double.class);
		linkSpeed = Query.builder("linkSpeed")
				.parameters(sat9)
				.output(outputSpeed)
				.clause(commSubSystemView.call(sat9, commSubSys7), kaCommView.call(commSubSys7),
						satLink.call(sat9, sat10), spacecraftView.call(sat10),
						outputSpeed.assign(constant(200.0)))
				.clause(commSubSystemView.call(sat9, commSubSys7), kaCommView.call(commSubSys7),
						satLink.call(sat9, sat10), groundStationView.call(sat10),
						outputSpeed.assign(constant(80.0)))
				.clause(commSubSystemView.call(sat9, commSubSys7), xCommView.call(commSubSys7),
						satLink.call(sat9, sat10), spacecraftView.call(sat10),
						outputSpeed.assign(constant(1.6)))
				.clause(commSubSystemView.call(sat9, commSubSys7), xCommView.call(commSubSys7),
						satLink.call(sat9, sat10), groundStationView.call(sat10),
						outputSpeed.assign(constant(0.7)))
				.clause(commSubSystemView.call(sat9, commSubSys7), uhfCommView.call(commSubSys7),
						satLink.call(sat9, sat10), spacecraftView.call(sat10),
						outputSpeed.assign(constant(5.0)))
				.build();

		var sat11 = Variable.of("sat11");
		var outputData = Variable.of("outputSpeed", Double.class);
		var tempDScience = Variable.of("tempDScience", Double.class);
		var tempNumOut = Variable.of("tempNumOut", Integer.class);
		var tempLinkSpeed = Variable.of("tempLinkSpeed", Double.class);
		tTransmit = Query.builder("tTransmit")
				.parameters(sat11)
				.output(outputData)
				.clause(spacecraftView.call(sat11),
						tempDScience.assign(dataCollected.call()),
						tempNumOut.assign(tTransmitHelper2.call(sat11)),
						tempLinkSpeed.assign(linkSpeed.call(sat11)),
						outputData.assign(div(mul(tempDScience, asReal(tempNumOut)), mul(constant(7.5), tempLinkSpeed))))
				.build();

		var tMissionOutput = Variable.of("tMissionOutput", Double.class);
		var tTransmitAggregate = Variable.of("tTransmitAggregate", Double.class);
		var tempTObservation2 = Variable.of("tempTObservation2", Double.class);
		tMission = Query.builder("tMission")
				.output(tMissionOutput)
				.clause(tTransmitAggregate.assign(tTransmit.aggregate(REAL_SUM, Variable.of())),
						tObservationView.call(tempTObservation2),
						tMissionOutput.assign(add(tTransmitAggregate, mul(constant(60.0), tempTObservation2))))
				.build();

		var sat12 = Variable.of("sat12");
		var payloadCost = Variable.of("payloadCost", Double.class);
		satCostHelper1 = Query.builder("costHelper1")
				.parameters(sat12)
				.output(payloadCost)
				.clause(spacecraftView.call(sat12), hasPayloadView.call(sat12), payloadCost.assign(constant(50.0)))
				.clause(spacecraftView.call(sat12), not(hasPayloadView.call(sat12)),
						payloadCost.assign(constant(0.0)))
				.build();

		var sat13 = Variable.of("sat13");
		var commCount2 = Variable.of("commCount2", Integer.class);
		var commCost = Variable.of("commCost", Double.class);
		satCostHelper2 = Query.builder("costHelper2")
				.parameters(sat13)
				.output(commCost)
				.clause(spacecraftView.call(sat13), commCount2.assign(commSubSystemView.count(sat13, Variable.of())),
						assume(eq(commCount2, constant(2))), commCost.assign(constant(100000.0)))
				.clause(spacecraftView.call(sat13), commCount2.assign(commSubSystemView.count(sat13, Variable.of())),
						assume(less(commCount2, constant(2))), commCost.assign(constant(0.0)))
				.build();

		var sat14 = Variable.of("sat14");
		var satTypeCost = Variable.of("satTypeCost", Double.class);
		var numOfSpacecraftType = Variable.of("numOfSpacecraftType", Integer.class);
		satCostHelper3 = Query.builder("satCostHelper3")
				.parameters(sat14)
				.output(satTypeCost)
				.clause(spacecraftView.call(sat14), cube3UView.call(sat14),
						numOfSpacecraftType.assign(cube3UView.count(Variable.of())),
						satTypeCost.assign(mul(constant(250000.0),
								pow(asReal(numOfSpacecraftType), constant(-0.25)))))
				.clause(spacecraftView.call(sat14), cube6UView.call(sat14),
						numOfSpacecraftType.assign(cube6UView.count(Variable.of())),
						satTypeCost.assign(mul(constant(750000.0),
								pow(asReal(numOfSpacecraftType), constant(-0.25)))))
				.clause(spacecraftView.call(sat14), smallSatView.call(sat14),
						numOfSpacecraftType.assign(smallSatView.count(Variable.of())),
						satTypeCost.assign(mul(constant(3000000.0),
								pow(asReal(numOfSpacecraftType), constant(-0.25)))))
				.build();

		var sat15 = Variable.of("sat15");
		var totalSatCost = Variable.of("totalSatCost", Double.class);
		var tempPayloadCost = Variable.of("tempPayloadCost", Double.class);
		var tempCommCost = Variable.of("tempCommCost", Double.class);
		var tempSatTypeCost = Variable.of("tempSatTypeCost", Double.class);
		satCost = Query.builder("satCost").
				parameters(sat15)
				.output(totalSatCost)
				.clause(spacecraftView.call(sat15),
						tempPayloadCost.assign(satCostHelper1.call(sat15)),
						tempCommCost.assign(satCostHelper2.call(sat15)),
						tempSatTypeCost.assign(satCostHelper3.call(sat15)),
						totalSatCost.assign(add(tempSatTypeCost, add(tempCommCost, tempPayloadCost))))
				.build();

		var totalCost = Variable.of("totalCost", Double.class);
		var tempTObservation3 = Variable.of("tempTObservation3", Double.class);
		var satCostAggregate = Variable.of("satCostAggregate", Double.class);
		missionCost = Query.builder("missionCost")
				.output(totalCost)
				.clause(satCostAggregate.assign(satCost.aggregate(REAL_SUM, Variable.of())),
						tObservationView.call(tempTObservation3),
						totalCost.assign(add(satCostAggregate, mul(constant(100000.0), tempTObservation3))))
				.build();

		// Checks all satellites with path to ground
		var gs1 = Variable.of("gs1");
		var sat16 = Variable.of("sat16");
		groundPathHelper = Query.builder("groundPathHelper")
				.parameters(sat16)
				.clause(groundStationView.call(gs1), spacecraftView.call(sat16),
						satLink.callTransitive(sat16, gs1))
				.build();

		// Detects all satellites with no path to ground
		var sat17 = Variable.of("sat17");
		misconfiguredGroundPath = Query.builder("misconfiguredGroundPath")
				.parameters(sat17)
				.clause(spacecraftView.call(sat17), not(groundPathHelper.call(sat17)))
				.build();


		// Transformation Rule Precondition Queries
		hasSpaceForComm = Query.of("hasSpaceForComm", (builder, tempSpacecraft) ->
				builder.clause(Integer.class, (tempCount) -> List.of(
						spacecraftView.call(tempSpacecraft),
						tempCount.assign(commSubSystemView.count(tempSpacecraft, Variable.of())),
						assume(less(tempCount, constant(2)))
				)));

		hasXComm = Query.of("hasXComm", (builder, tempSpacecraft) ->
				builder.clause((tempComm) -> List.of(
						spacecraftView.call(tempSpacecraft),
						xCommView.call(tempComm),
						commSubSystemView.call(tempSpacecraft, tempComm)
				)));

		addXCommToSpacecraftPrecondition = Query.of("addXCommToSpacecraftPrecondition",
				(builder, tempSpacecraft) ->
						builder.clause(() -> List.of(
								spacecraftView.call(tempSpacecraft),
								not(hasXComm.call(tempSpacecraft)),
								hasSpaceForComm.call(tempSpacecraft)
						)));

		removeXCommFromSpacecraftPrecondition = Query.of("removeXCommFromSpacecraftPrecondition",
				(builder, inputSpacecraft, inputCommSubSystem) ->
						builder.clause(
								commSubSystemView.call(inputSpacecraft, inputCommSubSystem),
								xCommView.call(inputCommSubSystem),
								not(commLinkView.call(Variable.of(), inputCommSubSystem)),
								not(commLinkView.call(inputCommSubSystem, Variable.of()))
						));

		hasUhfComm = Query.of("hasUhfComm", (builder, tempSpacecraft) ->
				builder.clause((tempComm) -> List.of(
						spacecraftView.call(tempSpacecraft),
						uhfCommView.call(tempComm),
						commSubSystemView.call(tempSpacecraft, tempComm)
				)));

		addUhfCommToSpacecraftPrecondition = Query.of("addUhfCommToSpacecraftPrecondition",
				(builder, tempSpacecraft) ->
						builder.clause(() -> List.of(
								spacecraftView.call(tempSpacecraft),
								not(hasUhfComm.call(tempSpacecraft)),
								hasSpaceForComm.call(tempSpacecraft)
						)));

		removeUhfCommFromSpacecraftPrecondition = Query.of("removeUhfCommFromSpacecraftPrecondition",
				(builder, inputSpacecraft, inputCommSubSystem) ->
						builder.clause(
								commSubSystemView.call(inputSpacecraft, inputCommSubSystem),
								uhfCommView.call(inputCommSubSystem),
								not(commLinkView.call(Variable.of(), inputCommSubSystem)),
								not(commLinkView.call(inputCommSubSystem, Variable.of()))
						));

		hasKaComm = Query.of("hasKaComm", (builder, tempSpacecraft) ->
				builder.clause((tempComm) -> List.of(
						spacecraftView.call(tempSpacecraft),
						kaCommView.call(tempComm),
						commSubSystemView.call(tempSpacecraft, tempComm)
				)));

		// Makes sure that satellite is small
		addKaCommToSpacecraftPrecondition = Query.of("addKaCommToSpacecraftPrecondition",
				(builder, tempSpacecraft) ->
						builder.clause(() -> List.of(
								spacecraftView.call(tempSpacecraft),
								smallSatView.call(tempSpacecraft),
								not(hasKaComm.call(tempSpacecraft)),
								hasSpaceForComm.call(tempSpacecraft)
						)));

		removeKaCommFromSpacecraftPrecondition = Query.of("removeUhfCommFromSpacecraftPrecondition",
				(builder, inputSpacecraft, inputCommSubSystem) ->
						builder.clause(
								commSubSystemView.call(inputSpacecraft, inputCommSubSystem),
								kaCommView.call(inputCommSubSystem),
								not(commLinkView.call(Variable.of(), inputCommSubSystem)),
								not(commLinkView.call(inputCommSubSystem, Variable.of()))
						));

		// Lists all comms whos satelites have no preexisitng outgoing satlink
		hasNoOutCommLink = Query.of("hasNoOutCommLink",
				(builder, inputSourceComm) ->
						builder.clause((tempSpacecraft) -> List.of(
								commSubSystemView.call(tempSpacecraft, inputSourceComm),
								not(satLink.call(tempSpacecraft, Variable.of()))
						)));

		addXCommLinkPrecondition = Query.of("addXCommLinkPrecondition",
				(builder, inputSourceComm, inputTargetComm) ->
						builder.clause((tempSpacecraft) -> List.of(
								xCommView.call(inputSourceComm),
								xCommView.call(inputTargetComm),
								hasNoOutCommLink.call(inputSourceComm)
						)));

		addUhfCommLinkPrecondition = Query.of("addUhfCommLinkPrecondition",
				(builder, inputSourceComm, inputTargetComm) ->
						builder.clause((tempSpacecraft) -> List.of(
								uhfCommView.call(inputSourceComm),
								uhfCommView.call(inputTargetComm),
								hasNoOutCommLink.call(inputSourceComm)
						)));

		addKaCommLinkPrecondition = Query.of("addKaCommLinkPrecondition",
				(builder, inputSourceComm, inputTargetComm) ->
						builder.clause((tempSpacecraft) -> List.of(
								kaCommView.call(inputSourceComm),
								kaCommView.call(inputTargetComm),
								hasNoOutCommLink.call(inputSourceComm)
						)));

		removeCommLinkPrecondition = Query.of("removeCommLinkPrecondition", (builder, sourceComm, targetComm) ->
				builder.clause(commLinkView.call(sourceComm, targetComm)));

		addPayloadToSpacecraftPrecondition = Query.of("addPayloadToSpacecraftPrecondition",
				(builder, tempSpacecraft) ->
						builder.clause(
								spacecraftView.call(tempSpacecraft),
								not(hasPayloadView.call(tempSpacecraft))
						));

		removePayloadFromSpacecraftPrecondition = Query.of("removePayloadFromSpacecraftPrecondition",
				(builder, tempSpacecraft) ->
						builder.clause(
								spacecraftView.call(tempSpacecraft),
								hasPayloadView.call(tempSpacecraft)
						));

		removeSmallSatSpacecraftPrecondition = Query.of("removeSmallSatSpacecraftPrecondition",
				(builder, tempSpacecraft) ->
						builder.clause(
								spacecraftView.call(tempSpacecraft),
								smallSatView.call(tempSpacecraft),
								not(commSubSystemView.call(tempCommCost, Variable.of()))
						));

		removeCube3USpacecraftPrecondition = Query.of("removeCube3USpacecraftPrecondition",
				(builder, tempSpacecraft) ->
						builder.clause(
								spacecraftView.call(tempSpacecraft),
								cube3UView.call(tempSpacecraft),
								not(commSubSystemView.call(tempCommCost, Variable.of()))
						));

		removeCube6USpacecraftPrecondition = Query.of("removeCube6USpacecraftPrecondition",
				(builder, tempSpacecraft) ->
						builder.clause(
								spacecraftView.call(tempSpacecraft),
								cube6UView.call(tempSpacecraft),
								not(commSubSystemView.call(tempCommCost, Variable.of()))
						));


		store = ModelStore.builder()
				.symbols(spacecraft, hasPayload, groundStation, cube3U, cube6U, smallSat, kaComm, xComm, uhfComm,
						commSubSystem, commLink, tObservation, nextID)
				.with(ViatraModelQueryAdapter.builder().queries(misconfiguredSpacecraft, misconfiguredCommCount,
						misconfiguredSatComm, misconfiguredCommLink, misconfiguredComm, misconfiguredSatCommLink,
						misconfiguredGroundPath, unusedComm, deadComm, commLoop,
						allComms, communicatingElement, satLink,
						coverage, tMission, satCost, missionCost))
				.build();
	}

	public Symbol<Boolean> getSpacecraft() {
		return spacecraft;
	}

	public Symbol<Boolean> getHasPayload() {
		return hasPayload;
	}

	public Symbol<Boolean> getGroundStation() {
		return groundStation;
	}

	public Symbol<Boolean> getCube3U() {
		return cube3U;
	}

	public Symbol<Boolean> getCube6U() {
		return cube6U;
	}

	public Symbol<Boolean> getSmallSat() {
		return smallSat;
	}

	public Symbol<Boolean> getKaComm() {
		return kaComm;
	}

	public Symbol<Boolean> getXComm() {
		return xComm;
	}

	public Symbol<Boolean> getUhfComm() {
		return uhfComm;
	}

	public Symbol<Boolean> getCommSubSystem() {
		return commSubSystem;
	}

	public Symbol<Boolean> getCommLink() {
		return commLink;
	}

	public Symbol<Double> getTObservation() {
		return tObservation;
	}

	public Symbol<Integer> getNextID() {
		return nextID;
	}


	public RelationalQuery getMisconfiguredSpacecraft() {
		return misconfiguredSpacecraft;
	}

	public RelationalQuery getMisconfiguredCommCount() {
		return misconfiguredCommCount;
	}

	public RelationalQuery getMisconfiguredSatComm() {
		return misconfiguredSatComm;
	}

	public RelationalQuery getMisconfiguredCommLink() {
		return misconfiguredCommLink;
	}

	public RelationalQuery getMisconfiguredComm() {
		return misconfiguredComm;
	}

	public RelationalQuery getMisconfiguredSatCommLink() {
		return misconfiguredSatCommLink;
	}

	public RelationalQuery getMisconfiguredGroundPath() {
		return misconfiguredGroundPath;
	}

	public RelationalQuery getMisconfiguredPayloadCount() {
		return misconfiguredPayloadCount;
	}

	public RelationalQuery getUnusedComm() {
		return unusedComm;
	}

	public RelationalQuery getDeadComm() {
		return deadComm;
	}

	public RelationalQuery getCommLoop() {
		return commLoop;
	}

	public RelationalQuery getAllComms() {
		return allComms;
	}

	public RelationalQuery getCommunicatingElement() {
		return communicatingElement;
	}

	public RelationalQuery getSatLink() {
		return satLink;
	}


	public FunctionalQuery<Double> getCoverage() {
		return coverage;
	}

	public FunctionalQuery<Double> getTMission() {
		return tMission;
	}

	public FunctionalQuery<Double> getSatCost() {
		return satCost;
	}

	public FunctionalQuery<Double> getMissionCost() {
		return missionCost;
	}


	public CubeSatModel createEmptyModel() {
		return new CubeSatModel(this, store.createEmptyModel());
	}
}
