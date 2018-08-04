package rocks.voss.androidutils.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import rocks.voss.androidutils.R;

public class DoubleNumberPicker extends LinearLayout {

    @Getter
    private int minValue;
    @Getter
    private int maxValue;
    @Getter
    private float defaultValue;
    @Getter
    private int decimals;
    @Setter
    @Getter
    private boolean autoIntegerUpdate;

    private NumberPicker integerPicker;
    private NumberPicker fractionPicker;
    private TextView seperator;
    private TextView unit;


    public DoubleNumberPicker(Context context) {
        this(context, null);
    }

    public DoubleNumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DoubleNumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DoubleNumberPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.widget_doublepicker, this);
        integerPicker = findViewById(R.id.integer_picker);
        fractionPicker = findViewById(R.id.fraction_picker);
        seperator = findViewById(R.id.separator);
        unit = findViewById(R.id.unit);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DoubleNumberPicker, defStyleAttr, defStyleRes);
        setMinValue(a.getInt(R.styleable.DoubleNumberPicker_min_value, 0));
        setMaxValue(a.getInt(R.styleable.DoubleNumberPicker_max_value, 1000));
        setDecimals(a.getInt(R.styleable.DoubleNumberPicker_decimals, 1));
        setDefaultValue(a.getFloat(R.styleable.DoubleNumberPicker_default_value, 0));
        setSeparator(a.getString(R.styleable.DoubleNumberPicker_separator));
        setUnit(a.getString(R.styleable.DoubleNumberPicker_unit));
        setAutoIntegerUpdate(a.getBoolean(R.styleable.DoubleNumberPicker_auto_integer_update, false));
        a.recycle();

        fractionPicker.setOnValueChangedListener(
                (picker, oldVal, newVal) -> {
                    if (autoIntegerUpdate) {
                        if (oldVal == picker.getMaxValue() && newVal == picker.getMinValue()) {
                            integerPicker.setValue(integerPicker.getValue() + 1);
                        } else if (newVal == picker.getMaxValue() && oldVal == picker.getMinValue()) {
                            integerPicker.setValue(integerPicker.getValue() - 1);
                        }
                    }
                }
        );
    }

    public void setSeparator(String separator) {
        this.seperator.setText(separator);
    }

    public void setUnit(String unit) {
        this.unit.setText(unit);
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
        integerPicker.setMinValue(minValue);
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
        integerPicker.setMaxValue(maxValue);
    }

    public void setDefaultValue(float defaultValue) {
        this.defaultValue = defaultValue;
        setValue(defaultValue);
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
        if (this.decimals < 1) {
            this.decimals = 1;
        }

        String baseString = "";
        for (int i = 0; i < decimals; i++) {
            baseString += "0";
        }

        String finalBaseString = baseString;
        fractionPicker.setFormatter(value -> {
            String tmp = finalBaseString + value;
            return tmp.substring(tmp.length() - this.decimals);
        });

        fractionPicker.setMaxValue((int) Math.pow(10, decimals) - 1);
        fractionPicker.setMinValue(0);
    }

    public BigDecimal getValue() {
        return BigDecimal.valueOf((long) (integerPicker.getValue() * Math.pow(10, decimals) + fractionPicker.getValue()), decimals);
    }

    public void setValue(BigDecimal bigDecimal) {
        integerPicker.setValue(bigDecimal.intValue());
        fractionPicker.setValue(bigDecimal.subtract(new BigDecimal(bigDecimal.intValue())).multiply(new BigDecimal(Math.pow(10, decimals))).intValue());
    }

    public void setValue(float value) {
        integerPicker.setValue((int) value);
        fractionPicker.setValue((int) ((value - (int) value) * Math.pow(10, decimals)));
    }
}
