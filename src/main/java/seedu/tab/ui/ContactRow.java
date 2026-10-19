package seedu.tab.ui;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.layout.FlowPane;

/**
 * Wraps card details while reporting the width needed to fit them all on one line.
 *
 * <p>FlowPane's preferred wrap length is independent of its content. Reporting the
 * unwrapped width lets the containing boxes measure height at the width they actually
 * allocate, instead of reserving wrapped rows even after the card becomes wide enough.
 */
public class ContactRow extends FlowPane {

    @Override
    protected double computePrefWidth(double height) {
        List<Node> fields = getManagedChildren();
        double fieldsWidth = fields.stream().mapToDouble(field -> snapSizeX(field.prefWidth(-1))).sum();
        double gapsWidth = snapSpaceX(getHgap()) * Math.max(0, fields.size() - 1);
        return snappedLeftInset() + fieldsWidth + gapsWidth + snappedRightInset();
    }
}
