package tools.refinery.cubesat;

import tools.refinery.store.tuple.Tuple;

public class CubeSat {
	public static void main(String[] args){

		var cubeSatStore = new CubeSatStore();
		var model = cubeSatStore.createEmptyModel();

		var spacecraftInterpretation = model.getSpacecraftInterpretation();
		var hasPayloadInterpretation = model.getHasPayloadInterpretation();
		var groundStationInterpretation = model.getGroundStationInterpretation();

		var cube3UInterpretation = model.getCube3UInterpretation();
		var cube6UInterpretation = model.getCube6UInterpretation();
		var smallSatInterpretation = model.getSmallSatInterpretation();

		var kaCommInterpretation = model.getKaCommInterpretation();
		var xCommInterpretation = model.getxCommInterpretation();
		var uhfCommInterpretation = model.getUhfCommInterpretation();

		var commSubSystemInterpretation = model.getCommSubSystemInterpretation();
		var commLinkInterpretation = model.getCommLinkInterpretation();

		var tObservationInterpretation = model.getTObservationInterpretation();

		var misconfiguredSpacecraftResultSet = model.getMisconfiguredSpacecraftResultSet();
		var misconfiguredCommCountResultSet = model.getMisconfiguredCommCountResultSet();
		var misconfiguredSatCommResultSet = model.getMisconfiguredSatCommResultSet();
		var misconfiguredCommLinkResultSet = model.getMisconfiguredCommLinkResultSet();
		var misconfiguredCommResultSet = model.getMisconfiguredCommResultSet();
		var misconfiguredSatCommLinkResultSet = model.getMisconfiguredSatCommLinkResultSet();
		var misconfiguredGroundPathResultSet = model.getMisconfiguredGroundPathResultSet();
		var misconfiguredPayloadCountResultSet = model.getMisconfiguredPayloadCountResultSet();
		var unusedCommResultSet = model.getUnusedCommResultSet();
		var deadCommResultSet = model.getDeadCommResultSet();
		var commLoopResultSet = model.getCommLoopResultSet();

		var allCommsResultSet = model.getAllCommsResultSet();
		var communicatingElementResultSet = model.getCommunicatingElementResultSet();
		var satLinkResultSet = model.getSatLinkResultSet();

		var coverageResult = model.getCoverageResult();
		var tMissionResult = model.getTMissionResult();
		var satCostResultSet = model.getSatCostResultSet();
		var missionCostResult = model.getMissionCostResult();

		int groundStation = 0;
		int groundStationKaComm = 1;
		int groundStationXComm = 2;

		// Basic Setup
		groundStationInterpretation.put(Tuple.of(groundStation), true);
		kaCommInterpretation.put(Tuple.of(groundStationKaComm), true);
		xCommInterpretation.put(Tuple.of(groundStationXComm), true);
		commSubSystemInterpretation.put(Tuple.of(groundStation, groundStationKaComm), true);
		commSubSystemInterpretation.put(Tuple.of(groundStation, groundStationXComm), true);
		tObservationInterpretation.put(Tuple.of(), 1.0); // Observation Time is 1 hour to begin with



	}
}
