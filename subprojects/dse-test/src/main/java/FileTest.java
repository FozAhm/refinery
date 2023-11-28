import tools.refinery.store.dse.modification.DanglingEdges;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.dse.transition.objectives.Objectives;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.FunctionalQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.tuple.Tuple;

import static tools.refinery.store.dse.transition.actions.ActionLiterals.add;
import static tools.refinery.store.dse.transition.actions.ActionLiterals.remove;
import static tools.refinery.store.dse.modification.actions.ModificationActionLiterals.create;
import static tools.refinery.store.dse.modification.actions.ModificationActionLiterals.delete;
import static tools.refinery.store.query.term.int_.IntTerms.*;
import static tools.refinery.store.query.literal.Literals.check;
import static tools.refinery.store.query.literal.Literals.not;

import java.util.List;

public class FileTest {

	// Symbols
	private final Symbol<Boolean> folder = new Symbol<>("folder", 1, Boolean.class, false);
	private final Symbol<Boolean> file = new Symbol<>("file", 1, Boolean.class, false);

	private final Symbol<Boolean> child = new Symbol<>("childFolder", 2, Boolean.class, false);

	// Helper
	private final RelationalQuery fileSystemObject;
	private final RelationalQuery nonEncapsulatedFolders;

	// Well Formedness Constraints
	private final RelationalQuery nonEncapsulatedFiles;
	private final RelationalQuery objectsWithTooManyParents;
	private final RelationalQuery tooManyRootFolders;
	private final RelationalQuery objectsWithLoops;

	// Objective Value
	private final FunctionalQuery<Integer> fileSystemScore;

	// Transformation Rules
	private final Rule deleteFolder;
	private final Rule createFolder;
	private final Rule deleteFile;
	private final Rule createFile;


	public FileTest(){

		var folderView = new KeyOnlyView<>(folder);
		var fileView = new KeyOnlyView<>(file);

		var childView = new KeyOnlyView<>(child);


		// Helper Queries
		// Good Example of "OR" Query - Follows Fluent Pattern
		fileSystemObject = Query.of("fileSystemObject", (builder, tempObject) ->
				builder.clause(() -> List.of(
						fileView.call(tempObject)
				)).clause(() -> List.of(
						folderView.call(tempObject)
				)));

		nonEncapsulatedFolders = Query.of("nonEncapsulatedFolders", (builder, tempFolder) ->
				builder.clause(() -> List.of(
						folderView.call(tempFolder),
						not(childView.call(Variable.of(), tempFolder))
				)));


		// Well Formedness Constraints Queries
		nonEncapsulatedFiles = Query.of("nonEncapsulatedFiles", (builder, tempFile) ->
				builder.clause(() -> List.of(
						fileView.call(tempFile),
						not(childView.call(Variable.of(), tempFile))
				)));

		objectsWithTooManyParents = Query.of("objectsWithTooManyParents", (builder, tempObject) ->
				builder.clause(Integer.class, (parentFolderCount) -> List.of(
						fileSystemObject.call(tempObject),
						parentFolderCount.assign(childView.count(Variable.of(), tempObject)),
						check(greater(parentFolderCount,constant(1)))
				)));

		tooManyRootFolders = Query.of("tooManyRootFolders", (builder) ->
				builder.clause(Integer.class, (rootFolderCount) -> List.of(
						rootFolderCount.assign(nonEncapsulatedFolders.count(Variable.of())),
						check(greater(rootFolderCount, constant(1)))
				)));

		// Transitive does a positive assignment
		objectsWithLoops = Query.of("objectsWithLoops", (builder, tempObject) ->
				builder.clause(() -> List.of(
						childView.callTransitive(tempObject, tempObject)
				)));

		// Objective Queries
		fileSystemScore = Query.of("fileSystemScore", Integer.class, (builder, score) ->
				builder.clause(Integer.class, (tempCount) -> List.of(
						tempCount.assign(fileSystemObject.count(Variable.of())),
						score.assign(pow(sub(tempCount,constant(10)),constant(2)))
				)));


		// Transformation Rules Queries
		createFolder = Rule.of("createFolder", (builder, parentFolderID) -> builder
				.clause(
						folderView.call(parentFolderID))
				.action((newFolderID) -> List.of(
						create(newFolderID),
						add(folder, newFolderID),
						add(child, parentFolderID, newFolderID)
				)));

		deleteFolder = Rule.of("deleteFolder", (builder, folderID, parentFolderID) -> builder
				.clause(
						folderView.call(folderID),
						childView.call(parentFolderID, folderID),
						not(childView.call(folderID, Variable.of())))
				.action(
						remove(folder, folderID),
						remove(child, parentFolderID, folderID),
						delete(folderID, DanglingEdges.IGNORE)
				));

		createFile = Rule.of("createFile", (builder, parentFolderID) -> builder
				.clause(
						folderView.call(parentFolderID))
				.action((newFileID) -> List.of(
						create(newFileID),
						add(file, newFileID),
						add(child, parentFolderID, newFileID)
				)));

		deleteFile = Rule.of("deleteFile", (builder, fileID, parentFolderID) -> builder
				.clause(
						fileView.call(fileID),
						childView.call(parentFolderID, fileID))
				.action(
						remove(file, fileID),
						remove(child, parentFolderID, fileID),
						delete(fileID, DanglingEdges.IGNORE)
				));
	}

	public static void main(String[] args) {
		new FileTest().run();
	}

	private void run() {
		var store = ModelStore.builder()
				.symbols(file, folder, child)
				.with(QueryInterpreterAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(ModificationAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder()
						.transformations(deleteFile, createFile, deleteFolder, createFolder)
						.objectives(Objectives.value(fileSystemScore))
						.accept(Criteria.whenNoMatch(nonEncapsulatedFiles))
						.accept(Criteria.whenNoMatch(objectsWithTooManyParents))
						.accept(Criteria.whenNoMatch(tooManyRootFolders)))
				.build();

		var model = store.createEmptyModel();
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);

		var fileInterpretation = model.getInterpretation(file);
		var folderInterpretation = model.getInterpretation(folder);

		var childInterpretation = model.getInterpretation(child);

		var modificationAdapter = model.getAdapter(ModificationAdapter.class);

		// Add Root
		var rootFolder = modificationAdapter.createObject();
		var rootFolderID = rootFolder.get(0);
		folderInterpretation.put(rootFolder, true);

		var initialVersion = model.commit();
		queryEngine.flushChanges();

		int initialScore = queryEngine.getResultSet(fileSystemScore).get(Tuple.of());
		System.out.println("Initial Score: " + initialScore);
	}
}
