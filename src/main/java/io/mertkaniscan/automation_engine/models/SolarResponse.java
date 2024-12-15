package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "solar_response")
public class SolarResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double lat;
    private double lon;
    private String date;
    private String tz;
    private String sunrise;
    private String sunset;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "irradiance_id")
    private Irradiance irradiance;

    @ManyToOne
    @JoinColumn(name = "fieldID", nullable = false)
    @JsonBackReference(value = "field-solarResponses")
    private Field field;

    @OneToOne
    @JoinColumn(name = "dayID", unique = true, nullable = false)
    private Day day;

    @Getter
    @Setter
    @Entity
    @Table(name = "irradiance")
    public static class Irradiance {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @OneToMany(cascade = CascadeType.ALL)
        @JoinColumn(name = "irradiance_id")
        //@JsonManagedReference("solarResponse-dailyIrradiance")
        private List<DailyIrradiance> daily;

        @OneToMany(cascade = CascadeType.ALL)
        @JoinColumn(name = "irradiance_id")
        //@JsonManagedReference("solarResponse-hourlyIrradiance")
        private List<HourlyIrradiance> hourly;

        @Getter
        @Setter
        @Entity
        @Table(name = "daily_irradiance")
        public static class DailyIrradiance {

            @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            private Long id;

            @Embedded
            @AttributeOverrides({
                    @AttributeOverride(name = "ghi", column = @Column(name = "clear_sky_ghi")),
                    @AttributeOverride(name = "dni", column = @Column(name = "clear_sky_dni")),
                    @AttributeOverride(name = "dhi", column = @Column(name = "clear_sky_dhi"))
            })
            @JsonProperty("clear_sky")
            private SkyIrradiance clearSky;

            @Embedded
            @AttributeOverrides({
                    @AttributeOverride(name = "ghi", column = @Column(name = "cloudy_sky_ghi")),
                    @AttributeOverride(name = "dni", column = @Column(name = "cloudy_sky_dni")),
                    @AttributeOverride(name = "dhi", column = @Column(name = "cloudy_sky_dhi"))
            })
            @JsonProperty("cloudy_sky")
            private SkyIrradiance cloudySky;
        }

        @Getter
        @Setter
        @Entity
        @Table(name = "hourly_irradiance")
        public static class HourlyIrradiance {
            @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            private Long id;

            private int hour;

            @Embedded
            @AttributeOverrides({
                    @AttributeOverride(name = "ghi", column = @Column(name = "clear_sky_ghi")),
                    @AttributeOverride(name = "dni", column = @Column(name = "clear_sky_dni")),
                    @AttributeOverride(name = "dhi", column = @Column(name = "clear_sky_dhi"))
            })
            @JsonProperty("clear_sky")
            private SkyIrradiance clearSky;

            @Embedded
            @AttributeOverrides({
                    @AttributeOverride(name = "ghi", column = @Column(name = "cloudy_sky_ghi")),
                    @AttributeOverride(name = "dni", column = @Column(name = "cloudy_sky_dni")),
                    @AttributeOverride(name = "dhi", column = @Column(name = "cloudy_sky_dhi"))
            })
            @JsonProperty("cloudy_sky")
            private SkyIrradiance cloudySky;
        }

        @Getter
        @Setter
        @Embeddable
        public static class SkyIrradiance {
            private double ghi;
            private double dni;
            private double dhi;
        }
    }
}
