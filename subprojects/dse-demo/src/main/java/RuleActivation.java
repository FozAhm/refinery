import tools.refinery.store.tuple.Tuple;
import java.util.function.Consumer;

public record RuleActivation(Tuple activation, Consumer<Tuple> rule, String ruleName) {

}
