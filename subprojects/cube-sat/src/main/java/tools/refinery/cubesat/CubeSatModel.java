package tools.refinery.cubesat;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.query.ResultSet;
import tools.refinery.store.tuple.Tuple;

public class CubeSatModel {
	private final Model model;
	private final ModelQueryAdapter queryEngine;

	private final Interpretation<Boolean> spacecraftInterpretation;
	private final Interpretation<Boolean> hasPayloadInterpretation;
	private final Interpretation<Boolean> groundStationInterpretation;

	private final Interpretation<Boolean> cube3UInterpretation;
	private final Interpretation<Boolean> cube6UInterpretation;
	private final Interpretation<Boolean> smallSatInterpretation;

	private final Interpretation<Boolean> kaCommInterpretation;
	private final Interpretation<Boolean> xCommInterpretation;
	private final Interpretation<Boolean> uhfCommInterpretation;

	private final Interpretation<Boolean> commSubSystemInterpretation;
	private final Interpretation<Boolean> commLinkInterpretation;

	private final Interpretation<Double> tObservationInterpretation;
	private final Interpretation<Integer> nextIDInterpretation;

	private final ResultSet<Boolean> misconfiguredSpacecraftResultSet;
	private final ResultSet<Boolean> misconfiguredCommCountResultSet;
	private final ResultSet<Boolean> misconfiguredSatCommResultSet;
	private final ResultSet<Boolean> misconfiguredCommLinkResultSet;
	private final ResultSet<Boolean> misconfiguredCommResultSet;
	private final ResultSet<Boolean> misconfiguredSatCommLinkResultSet;
	private final ResultSet<Boolean> misconfiguredGroundPathResultSet;
	private final ResultSet<Boolean> misconfiguredPayloadCountResultSet;
	private final ResultSet<Boolean> unusedCommResultSet;
	private final ResultSet<Boolean> deadCommResultSet;
	private final ResultSet<Boolean> commLoopResultSet;

	private final ResultSet<Boolean> allCommsResultSet;
	private final ResultSet<Boolean> communicatingElementResultSet;
	private final ResultSet<Boolean> satLinkResultSet;

	private final ResultSet<Double> coverageResult;
	private final ResultSet<Double> tMissionResult;
	private final ResultSet<Double> satCostResultSet;
	private final ResultSet<Double> missionCostResult;


	public CubeSatModel(CubeSatStore store, Model model){
		this.model = model;
		this.queryEngine = model.getAdapter(ModelQueryAdapter.class);

		this.spacecraftInterpretation = model.getInterpretation(store.getSpacecraft());
		this.hasPayloadInterpretation = model.getInterpretation(store.getHasPayload());
		this.groundStationInterpretation = model.getInterpretation(store.getGroundStation());

		this.cube3UInterpretation = model.getInterpretation(store.getCube3U());
		this.cube6UInterpretation = model.getInterpretation(store.getCube6U());
		this.smallSatInterpretation = model.getInterpretation(store.getSmallSat());

		this.kaCommInterpretation = model.getInterpretation(store.getKaComm());
		this.xCommInterpretation = model.getInterpretation(store.getXComm());
		this.uhfCommInterpretation = model.getInterpretation(store.getUhfComm());

		this.commSubSystemInterpretation = model.getInterpretation(store.getCommSubSystem());
		this.commLinkInterpretation = model.getInterpretation(store.getCommLink());

		this.tObservationInterpretation = model.getInterpretation(store.getTObservation());
		this.nextIDInterpretation = model.getInterpretation(store.getNextID());

		this.misconfiguredSpacecraftResultSet = this.queryEngine.getResultSet(store.getMisconfiguredSpacecraft());
		this.misconfiguredCommCountResultSet = this.queryEngine.getResultSet(store.getMisconfiguredCommCount());
		this.misconfiguredSatCommResultSet = this.queryEngine.getResultSet(store.getMisconfiguredSatComm());
		this.misconfiguredCommLinkResultSet = this.queryEngine.getResultSet(store.getMisconfiguredCommLink());
		this.misconfiguredCommResultSet = this.queryEngine.getResultSet(store.getMisconfiguredComm());
		this.misconfiguredSatCommLinkResultSet = this.queryEngine.getResultSet(store.getMisconfiguredSatCommLink());
		this.misconfiguredGroundPathResultSet = this.queryEngine.getResultSet(store.getMisconfiguredGroundPath());
		this.misconfiguredPayloadCountResultSet = this.queryEngine.getResultSet(store.getMisconfiguredPayloadCount());
		this.unusedCommResultSet = this.queryEngine.getResultSet(store.getUnusedComm());
		this.deadCommResultSet = this.queryEngine.getResultSet(store.getDeadComm());
		this.commLoopResultSet = this.queryEngine.getResultSet(store.getCommLoop());

		this.allCommsResultSet = this.queryEngine.getResultSet(store.getAllComms());
		this.communicatingElementResultSet = this.queryEngine.getResultSet(store.getCommunicatingElement());
		this.satLinkResultSet = this.queryEngine.getResultSet(store.getSatLink());

		this.coverageResult = this.queryEngine.getResultSet(store.getCoverage());
		this.tMissionResult = this.queryEngine.getResultSet(store.getTMission());
		this.satCostResultSet = this.queryEngine.getResultSet(store.getSatCost());
		this.missionCostResult = this.queryEngine.getResultSet(store.getMissionCost());
	}

	// Standard Access Functions
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
	public void addXCommToSpacecraft(Tuple activation){
		int spacecraftID = activation.get(0);
		int newXCommID = newID();
		this.xCommInterpretation.put(Tuple.of(newXCommID), true);
		this.commSubSystemInterpretation.put(Tuple.of(spacecraftID, newXCommID), true);
	}

	public void removeXCommFromSpacecraft(Tuple activation){
		int spacecraftID = activation.get(0);
		int spacecraftXCommID = activation.get(1);
		this.xCommInterpretation.put(Tuple.of(spacecraftXCommID), false);
		this.commSubSystemInterpretation.put(Tuple.of(spacecraftID, spacecraftXCommID), false);
	}

	public void addUhfCommToSpacecraft(Tuple activation){
		int spacecraftID = activation.get(0);
		int newUhfCommID = newID();
		this.uhfCommInterpretation.put(Tuple.of(newUhfCommID), true);
		this.commSubSystemInterpretation.put(Tuple.of(spacecraftID, newUhfCommID), true);
	}

	public void removeUhfCommFromSpacecraft(Tuple activation){
		int spacecraftID = activation.get(0);
		int spacecraftUhfCommID = activation.get(1);
		this.xCommInterpretation.put(Tuple.of(spacecraftUhfCommID), false);
		this.commSubSystemInterpretation.put(Tuple.of(spacecraftID, spacecraftUhfCommID), false);
	}

	public void addKaCommToSpacecraft(Tuple activation){
		int spacecraftID = activation.get(0);
		int newKaCommID = newID();
		this.kaCommInterpretation.put(Tuple.of(newKaCommID), true);
	}

	public void removeKaCommFromSpacecraft(Tuple activation){
		int spacecraftID = activation.get(0);
		int spacecraftKaCommID = activation.get(1);
		this.xCommInterpretation.put(Tuple.of(spacecraftKaCommID), false);
		this.commSubSystemInterpretation.put(Tuple.of(spacecraftID, spacecraftKaCommID), false);
	}

	// Use different preconditions but the action is the same
	public void addCommLink(Tuple activation){
		int sourceComm = activation.get(0);
		int targetComm = activation.get(1);
		this.commLinkInterpretation.put(Tuple.of(sourceComm, targetComm), true);
	}

	public void removeCommLink(Tuple activation){
		int sourceComm = activation.get(0);
		int targetComm = activation.get(1);
		this.commLinkInterpretation.put(Tuple.of(sourceComm, targetComm), false);
	}

	public void addPayloadToSpacecraft(Tuple activation){
		int spacecraftID = activation.get(0);
		this.hasPayloadInterpretation.put(Tuple.of(spacecraftID), true);
	}

	public void removePayloadFromSpacecraft(Tuple activation){
		int spacecraftID = activation.get(0);
		this.hasPayloadInterpretation.put(Tuple.of(spacecraftID), false);
	}

	public void addSmallSatSpacecraft(Tuple ignoreActivation){
		int newSpacecraftID = newID();
		this.spacecraftInterpretation.put(Tuple.of(newSpacecraftID), true);
		this.smallSatInterpretation.put(Tuple.of(newSpacecraftID), true);
	}

	public void removeSmallSatSpacecraft(Tuple activation){
		int spacecraftID = activation.get(0);
		this.spacecraftInterpretation.put(Tuple.of(spacecraftID), false);
		this.smallSatInterpretation.put(Tuple.of(spacecraftID), false);
		this.hasPayloadInterpretation.put(Tuple.of(spacecraftID), false);
	}

	public void addCube3USpacecraft(Tuple ignoreActivation){
		int newSpacecraftID = newID();
		this.spacecraftInterpretation.put(Tuple.of(newSpacecraftID), true);
		this.cube3UInterpretation.put(Tuple.of(newSpacecraftID), true);
	}

	public void removeCube3USpacecraft(Tuple activation){
		int spacecraftID = activation.get(0);
		this.spacecraftInterpretation.put(Tuple.of(spacecraftID), false);
		this.cube3UInterpretation.put(Tuple.of(spacecraftID), false);
		this.hasPayloadInterpretation.put(Tuple.of(spacecraftID), false);
	}

	public void addCube6USpacecraft(Tuple ignoreActivation){
		int newSpacecraftID = newID();
		this.spacecraftInterpretation.put(Tuple.of(newSpacecraftID), true);
		this.cube6UInterpretation.put(Tuple.of(newSpacecraftID), true);
	}

	public void removeCube6USpacecraft(Tuple activation){
		int spacecraftID = activation.get(0);
		this.spacecraftInterpretation.put(Tuple.of(spacecraftID), false);
		this.cube6UInterpretation.put(Tuple.of(spacecraftID), false);
		this.hasPayloadInterpretation.put(Tuple.of(spacecraftID), false);
	}

	public Interpretation<Boolean> getSpacecraftInterpretation() {
		return spacecraftInterpretation;
	}
	public Interpretation<Boolean> getHasPayloadInterpretation() {
		return hasPayloadInterpretation;
	}
	public Interpretation<Boolean> getGroundStationInterpretation() {
		return groundStationInterpretation;
	}

	public Interpretation<Boolean> getCube3UInterpretation() {
		return cube3UInterpretation;
	}
	public Interpretation<Boolean> getCube6UInterpretation() {
		return cube6UInterpretation;
	}
	public Interpretation<Boolean> getSmallSatInterpretation() {
		return smallSatInterpretation;
	}

	public Interpretation<Boolean> getKaCommInterpretation() {
		return kaCommInterpretation;
	}
	public Interpretation<Boolean> getxCommInterpretation() {
		return xCommInterpretation;
	}
	public Interpretation<Boolean> getUhfCommInterpretation() {
		return uhfCommInterpretation;
	}

	public Interpretation<Boolean> getCommSubSystemInterpretation() {
		return commSubSystemInterpretation;
	}
	public Interpretation<Boolean> getCommLinkInterpretation() {
		return commLinkInterpretation;
	}

	public Interpretation<Double> getTObservationInterpretation() {
		return tObservationInterpretation;
	}
	public Interpretation<Integer> getNextIDInterpretation() {
		return nextIDInterpretation;
	}

	public ResultSet<Boolean> getMisconfiguredSpacecraftResultSet() {
		return misconfiguredSpacecraftResultSet;
	}
	public ResultSet<Boolean> getMisconfiguredCommCountResultSet() {
		return misconfiguredCommCountResultSet;
	}
	public ResultSet<Boolean> getMisconfiguredSatCommResultSet() {
		return misconfiguredSatCommResultSet;
	}
	public ResultSet<Boolean> getMisconfiguredCommLinkResultSet() {
		return misconfiguredCommLinkResultSet;
	}
	public ResultSet<Boolean> getMisconfiguredCommResultSet() {
		return misconfiguredCommResultSet;
	}
	public ResultSet<Boolean> getMisconfiguredSatCommLinkResultSet() {
		return misconfiguredSatCommLinkResultSet;
	}
	public ResultSet<Boolean> getMisconfiguredGroundPathResultSet() {
		return misconfiguredGroundPathResultSet;
	}
	public ResultSet<Boolean> getMisconfiguredPayloadCountResultSet() {
		return misconfiguredPayloadCountResultSet;
	}
	public ResultSet<Boolean> getUnusedCommResultSet() {
		return unusedCommResultSet;
	}
	public ResultSet<Boolean> getDeadCommResultSet() {
		return deadCommResultSet;
	}
	public ResultSet<Boolean> getCommLoopResultSet() {
		return commLoopResultSet;
	}

	public ResultSet<Boolean> getAllCommsResultSet() {
		return allCommsResultSet;
	}
	public ResultSet<Boolean> getCommunicatingElementResultSet() {
		return communicatingElementResultSet;
	}
	public ResultSet<Boolean> getSatLinkResultSet() {
		return satLinkResultSet;
	}

	public ResultSet<Double> getCoverageResult() {
		return coverageResult;
	}
	public ResultSet<Double> getTMissionResult() {
		return tMissionResult;
	}
	public ResultSet<Double> getSatCostResultSet() {
		return satCostResultSet;
	}
	public ResultSet<Double> getMissionCostResult() {
		return missionCostResult;
	}
}
