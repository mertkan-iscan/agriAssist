package old_classes;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Rain {
    // Use @JsonProperty to map the JSON field "1h" to this field
    @JsonProperty("1h")
    public double _1h;

    // Add getter and setter if needed
    public double get_1h() {
        return _1h;
    }

    public void set_1h(double _1h) {
        this._1h = _1h;
    }
}