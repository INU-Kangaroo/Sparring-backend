package com.kangaroo.sparring.domain.exercise.catalog.service;

import com.kangaroo.sparring.domain.exercise.catalog.entity.Exercise;
import com.kangaroo.sparring.domain.exercise.catalog.repository.ExerciseRepository;
import com.kangaroo.sparring.domain.exercise.catalog.type.ExerciseCategory;
import com.kangaroo.sparring.domain.exercise.catalog.type.ExerciseImpactLevel;
import com.kangaroo.sparring.domain.exercise.catalog.type.ExerciseLocation;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.recommendation.type.ExerciseIntensity;
import com.kangaroo.sparring.domain.survey.type.BloodPressureStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service("exerciseCatalogCandidateService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseCandidateService {

    private static final int MAX_CANDIDATES = 10;
    private static final EnumSet<BloodPressureStatus> HIGH_RISK_BLOOD_PRESSURE_STATUSES =
            EnumSet.of(BloodPressureStatus.STAGE1, BloodPressureStatus.STAGE2);

    private final ExerciseRepository exerciseRepository;

    /**
     * 안전 필터 + 태그 필터로 후보 운동 추출
     */
    public List<Exercise> findCandidates(
            HealthProfile healthProfile,
            List<BloodPressureLog> recentBloodPressures,
            ExerciseIntensity intensity,
            com.kangaroo.sparring.domain.recommendation.type.ExerciseLocation requestLocation
    ) {
        ExerciseLocation location = mapRequestLocation(requestLocation);
        boolean excludeHighImpact = shouldExcludeHighImpact(healthProfile, recentBloodPressures);
        boolean excludeHighIntensity = shouldExcludeHighIntensity(healthProfile, recentBloodPressures);

        // 안전 필터: 고강도 제외
        ExerciseIntensity safeIntensity = (excludeHighIntensity && intensity == ExerciseIntensity.HIGH)
                ? ExerciseIntensity.MEDIUM
                : intensity;

        List<Exercise> candidates = new ArrayList<>();

        for (ExerciseCategory category : ExerciseCategory.values()) {
            List<Exercise> filtered;
            if (excludeHighImpact) {
                filtered = exerciseRepository.findByCategoryAndIntensityAndLocationAndImpactLevel(
                        category, safeIntensity, location, ExerciseImpactLevel.LOW_IMPACT
                );
            } else {
                filtered = exerciseRepository.findByCategoryAndIntensityAndLocation(
                        category, safeIntensity, location
                );
            }
            candidates.addAll(filtered);
        }

        // 후보 부족 시 장소 조건 완화
        if (candidates.size() < 5) {
            candidates.addAll(exerciseRepository.findByIntensity(safeIntensity));
        }

        // 중복 제거 후 랜덤 셔플 → 최대 10개
        List<Exercise> unique = candidates.stream().distinct().collect(Collectors.toList());
        Collections.shuffle(unique);
        return unique.stream().limit(MAX_CANDIDATES).toList();
    }

    /**
     * 후보 목록을 프롬프트용 텍스트로 변환
     */
    public String buildCandidatesText(List<Exercise> candidates) {
        if (candidates.isEmpty()) {
            return "후보 운동 없음 (다양한 운동을 자유롭게 추천하세요)";
        }
        StringBuilder sb = new StringBuilder();
        for (Exercise e : candidates) {
            sb.append(String.format("- %s (카테고리: %s, MET: %.1f, 충격: %s)%n",
                    e.getExerciseName(),
                    e.getCategory().getDescription(),
                    e.getMetValue(),
                    e.getImpactLevel().getDescription()));
        }
        return sb.toString().trim();
    }

    // 고충격 운동 제외 여부: 관절 질환 키워드 or 혈압 160 이상
    private boolean shouldExcludeHighImpact(HealthProfile hp, List<BloodPressureLog> bpLogs) {
        if (hp == null) {
            return getLatestSystolic(bpLogs) >= 160;
        }
        if (hp.getMedications() != null) {
            String meds = hp.getMedications().toLowerCase();
            if (meds.contains("관절") || meds.contains("무릎") || meds.contains("허리")) {
                return true;
            }
        }
        return getLatestSystolic(bpLogs) >= 160;
    }

    // 고강도 운동 제외 여부: 혈압 수축기 160 이상
    private boolean shouldExcludeHighIntensity(HealthProfile hp, List<BloodPressureLog> bpLogs) {
        if (hp == null || hp.getBloodPressureStatus() == null) {
            return false;
        }
        if (HIGH_RISK_BLOOD_PRESSURE_STATUSES.contains(hp.getBloodPressureStatus())) {
            return getLatestSystolic(bpLogs) >= 160;
        }
        return false;
    }

    private int getLatestSystolic(List<BloodPressureLog> bpLogs) {
        if (bpLogs == null || bpLogs.isEmpty()) return 0;
        return bpLogs.get(0).getSystolic();
    }

    private ExerciseLocation mapRequestLocation(
            com.kangaroo.sparring.domain.recommendation.type.ExerciseLocation requestLocation
    ) {
        return switch (requestLocation) {
            case INDOOR, GYM -> ExerciseLocation.INDOOR;
            case OUTDOOR -> ExerciseLocation.OUTDOOR;
        };
    }
}
