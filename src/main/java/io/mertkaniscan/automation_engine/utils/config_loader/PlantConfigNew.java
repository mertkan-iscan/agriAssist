package io.mertkaniscan.automation_engine.utils.config_loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlantConfigNew {

    private String plantType;
    private List<StageConfig> stages;

    public PlantConfigNew() {
        // No-args constructor for Jackson
    }

    @Getter
    @Setter
    public static class StageConfig {
        private String stageName;
        private Double rootZoneDepth;
        private Double allowableDepletion;
        private Double kcValue;
        private DayValues dayValues;
        private NightValues nightValues;

        public StageConfig() {
            // No-args constructor for Jackson
        }

        @Getter
        @Setter
        public static class DayValues {
            @JsonProperty("Tmax")
            private Double tmax;
            @JsonProperty("Tmin")
            private Double tmin;
            @JsonProperty("RHMax")
            private Double rhmax;
            @JsonProperty("RHMin")
            private Double rhmin;

            public DayValues() {
                // No-args constructor for Jackson
            }
        }

        @Getter
        @Setter
        public static class NightValues {
            @JsonProperty("Tmax")
            private Double tmax;
            @JsonProperty("Tmin")
            private Double tmin;
            @JsonProperty("RHMax")
            private Double rhmax;
            @JsonProperty("RHMin")
            private Double rhmin;

            public NightValues() {
                // No-args constructor for Jackson
            }
        }
    }
}
