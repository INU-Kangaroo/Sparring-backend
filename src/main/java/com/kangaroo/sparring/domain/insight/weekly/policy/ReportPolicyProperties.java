package com.kangaroo.sparring.domain.insight.weekly.policy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "report.policy")
public class ReportPolicyProperties {

    // 주간 목표
    private int bloodSugarWeeklyTarget = 21;
    private int bloodPressureWeeklyTarget = 14;
    private int exerciseActiveDaysTarget = 5;
    private int fullMealDaysTarget = 7;

    // 고위험 기준
    private int severeHighBloodSugar = 180;
    private int severeHighSystolic = 160;
    private int severeHighDiastolic = 100;

    // 리스크/안정성/추세 계산 계수
    private double severeRiskPenalty = 30.0;
    private double variabilityPenaltyFactor = 120.0;
    private double bloodSugarTrendSlopeFactor = 4.0;
    private double systolicTrendSlopeFactor = 5.0;
    private double diastolicTrendSlopeFactor = 7.0;

    // overall 가중치
    private double overallHealthWeight = 0.35;
    private double overallConsistencyWeight = 0.20;
    private double overallStabilityWeight = 0.15;
    private double overallTrendWeight = 0.10;
    private double overallLifestyleWeight = 0.20;
}
