package rocks.voss.androidutils.database;

import java.io.Serializable;
import java.util.List;

public interface ExportDataSet extends Serializable {
    List<String> getValues();
}
