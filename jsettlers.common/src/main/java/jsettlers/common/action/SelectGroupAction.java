package jsettlers.common.action;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Local selection recall; IDs are resolved to current units when the action is handled. */
public final class SelectGroupAction extends Action {
    private final List<Integer> ids;
    private final boolean center;

    public SelectGroupAction(Collection<Integer> ids, boolean center) {
        super(EActionType.SELECT_GROUP);
        LinkedHashSet<Integer> unique = new LinkedHashSet<>(Objects.requireNonNull(ids, "ids"));
        unique.remove(null);
        this.ids = List.copyOf(unique);
        this.center = center;
    }

    public List<Integer> getIds() { return ids; }
    public boolean isCenter() { return center; }
}
