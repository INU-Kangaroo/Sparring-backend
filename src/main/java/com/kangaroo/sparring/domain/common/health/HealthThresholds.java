package com.kangaroo.sparring.domain.common.health;

public final class HealthThresholds {

    private HealthThresholds() {
    }

    // 혈당
    public static final int BLOOD_SUGAR_FASTING_NORMAL_MAX = 99;
    public static final int BLOOD_SUGAR_POST_MEAL_NORMAL_MAX = 139;
    public static final int BLOOD_SUGAR_MIN = 20;
    public static final int BLOOD_SUGAR_MAX = 600;

    // 혈압
    public static final int BLOOD_PRESSURE_SYSTOLIC_NORMAL_MAX = 119;
    public static final int BLOOD_PRESSURE_DIASTOLIC_NORMAL_MAX = 79;
    public static final int BLOOD_PRESSURE_HYPERTENSION_SYSTOLIC = 140;
    public static final int BLOOD_PRESSURE_HYPERTENSION_DIASTOLIC = 90;
    public static final int BLOOD_PRESSURE_SYSTOLIC_MIN = 50;
    public static final int BLOOD_PRESSURE_SYSTOLIC_MAX = 300;
    public static final int BLOOD_PRESSURE_DIASTOLIC_MIN = 30;
    public static final int BLOOD_PRESSURE_DIASTOLIC_MAX = 200;

    // 심박수
    public static final int HEART_RATE_MIN = 30;
    public static final int HEART_RATE_MAX = 250;
}
