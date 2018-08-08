package rocks.voss.androidutils.database;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class ExportData implements Serializable {
    private char separator = ',';
    private String newLine = "\r\n";
    private List<String> header;
    private List<ExportDataSet> dataSets;
}
