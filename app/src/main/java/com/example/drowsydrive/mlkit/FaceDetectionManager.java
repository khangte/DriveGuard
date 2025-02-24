package com.example.drowsydrive.mlkit;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;

/**
 * FaceDetectionManager 클래스
 * ML Kit의 FaceDetection API를 사용하여 얼굴을 감지하고, 졸음 및 하품 여부를 분석합니다.
 */
public class FaceDetectionManager {
    private static final String TAG = "FaceDetectionManager"; // 디버그 태그

    protected Canvas canvas;
    private final FaceDetector faceDetector; // ML Kit 얼굴 감지 클라이언트
    private long lastAnalyzedTime = 0; // 마지막 분석 시간 기록
    private static final long ANALYSIS_INTERVAL = 1000; // 분석 간격 (1초)

    private long eyesClosedStartTime = 0; // 눈 감기 시작 시간 기록
    private boolean eyesClosed = false; // 눈 감김 여부 상태
    private static final long EYES_CLOSED_DURATION_THRESHOLD = 1000; // 눈 감김 지속 시간 임계값 (2초)

    private boolean isYawning = false; // 하품 상태 여부
    private long yawnStartTime = 0; // 하품 시작 시간 기록
    private static final long YAWN_DURATION_THRESHOLD = 2500; // 하품 지속 시간 임계값 (2초)

    /**
     * FaceDetectionManager 생성자
     * 얼굴 감지 옵션을 설정하고 얼굴 인식 클라이언트를 초기화합니다.
     */
    public FaceDetectionManager() {
        // 얼굴 감지 옵션 설정: 성능 모드, 랜드마크 및 분류 모드 활성화
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST) // 빠른 처리 모드
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL) // 얼굴 랜드마크 감지
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // 얼굴 상태 분류
                .build();

        // ML Kit의 얼굴 감지 클라이언트 생성
        faceDetector = FaceDetection.getClient(options);
    }

    /**
     * FaceDetectionCallback 인터페이스
     * 상태가 지속될 경우 호출될 콜백 정의 (눈 감김 및 하품 감지 시).
     */
    public interface FaceDetectionCallback {
        void onEyesClosed(); // 눈 감김 지속 시 호출
        void onYawnDetected(); // 하품 감지 시 호출
        void onFaceDetected(Canvas canvas, Face face); // 얼굴 프레임 그리기
    }

    /**
     * 이미지를 분석하여 얼굴을 감지하는 메서드
     *
     * @param image    InputImage 객체
     * @param imageProxy ImageProxy 객체 (이미지 데이터)
     * @param callback FaceDetectionCallback 객체 (콜백 인터페이스 구현체)
     */
    public void analyzeImage(InputImage image, ImageProxy imageProxy, FaceDetectionCallback callback) {
        long currentTime = SystemClock.elapsedRealtime(); // 현재 시간 기록

        // 분석 간격 확인: 너무 자주 분석하지 않도록 제한
        if (currentTime - lastAnalyzedTime < ANALYSIS_INTERVAL) {
            imageProxy.close(); // 분석 불필요 시 ImageProxy 닫기
            return;
        }

        lastAnalyzedTime = currentTime; // 마지막 분석 시간 갱신

        // 얼굴 감지 수행
        faceDetector.process(image)
                .addOnSuccessListener(faces -> processFaces(faces, imageProxy, callback)) // 성공 시 얼굴 데이터 처리
                .addOnFailureListener(e -> {
                    Log.e(TAG, "얼굴 감지 실패", e); // 실패 로그 출력
                    imageProxy.close(); // 실패 시 ImageProxy 닫기
                });
    }

    // 얼굴 프레임을 그리는 함수 (android.graphics.Canvas)
    public void drawFaceFrame(Canvas canvas, Rect bounds) {
        if (canvas == null) {
            Log.e("FaceDetection", "Canvas is null!");
            return; // canvas가 null이면 리턴
        }

        // Paint 객체 생성 및 설정
        Paint paint = new Paint();
        paint.setColor(Color.RED);  // 프레임 색상 설정
        paint.setStrokeWidth(5f);   // 선 두께 설정
        paint.setStyle(Paint.Style.STROKE);  // 선 스타일을 "STROKE"로 설정 (채우지 않고 테두리만 그림)

        // 얼굴 경계에 프레임 그리기
        canvas.drawRect(bounds, paint);  // bounds에 맞춰 사각형을 그리고 paint로 스타일링
    }

    /**
     * 얼굴 감지 상태 초기화 메서드
     */
    private void resetDetectionState() {
        eyesClosed = false;
        eyesClosedStartTime = 0;
        isYawning = false;
        yawnStartTime = 0;
    }

    /**
     * 감지된 얼굴 데이터를 처리하는 메서드
     *
     * @param faces      감지된 얼굴 목록
     * @param imageProxy ImageProxy 객체
     * @param callback   FaceDetectionCallback 객체
     */
    private void processFaces(List<Face> faces, ImageProxy imageProxy, FaceDetectionCallback callback) {
        if (faces.isEmpty()) {
            Log.d(TAG, "얼굴이 감지되지 않았습니다.");
            resetDetectionState(); // 얼굴이 감지되지 않으면 상태 초기화
            imageProxy.close(); // ImageProxy 닫기
            return;
        }

        for (Face face : faces) {
            callback.onFaceDetected(canvas, face); // 얼굴 프레임 그리기 호출
            detectEyeClose(face, callback); // 눈 감김 상태 감지
            detectIsYawning(face, callback); // 하품 상태 감지
        }
        imageProxy.close(); // 이미지 처리 완료 후 ImageProxy 닫기
    }

    /**
     * 눈 감김 상태를 감지하는 메서드
     *
     * @param face    감지된 얼굴 데이터
     * @param callback FaceDetectionCallback 객체
     */
    private void detectEyeClose(Face face, FaceDetectionCallback callback) {
        if (face.getLeftEyeOpenProbability() != null
                && face.getRightEyeOpenProbability() != null) {
            float leftEyeOpenProb = face.getLeftEyeOpenProbability(); // 왼쪽 눈 열림 확률
            float rightEyeOpenProb = face.getRightEyeOpenProbability(); // 오른쪽 눈 열림 확률

            Log.d(TAG, "--------------------------");
            Log.d(TAG, "왼쪽 눈 열림 확률: " + leftEyeOpenProb);
            Log.d(TAG, "오른쪽 눈 열림 확률: " + rightEyeOpenProb);

            if (leftEyeOpenProb < 0.2 && rightEyeOpenProb < 0.2) { // 두 눈이 모두 감긴 경우
                if (!eyesClosed) {
                    eyesClosed = true;
                    eyesClosedStartTime = SystemClock.elapsedRealtime(); // 눈 감기 시작 시간 기록
                } else {
                    long closedEyesDuration
                            = SystemClock.elapsedRealtime() - eyesClosedStartTime;
                    if (closedEyesDuration >= EYES_CLOSED_DURATION_THRESHOLD) { // 눈 감김 지속 시간 초과
                        callback.onEyesClosed(); // 콜백 호출
                        Log.d(TAG, "졸음 감지!!");
                    }
                }
            } else { // 눈이 다시 열림
                eyesClosed = false;
                eyesClosedStartTime = 0; // 눈 감기 시작 시간 초기화
            }
        }
    }

    /**
     * 하품 상태를 감지하는 메서드
     *
     * @param face    감지된 얼굴 데이터
     * @param callback FaceDetectionCallback 객체
     */
    private void detectIsYawning(Face face, FaceDetectionCallback callback) {
        if (face.getSmilingProbability() != null
                && face.getLandmark(FaceLandmark.MOUTH_BOTTOM) != null
                && face.getLandmark(FaceLandmark.NOSE_BASE) != null) {

            float smilingProbability = face.getSmilingProbability(); // 웃음 확률
            FaceLandmark rightEyeLandmark = face.getLandmark(FaceLandmark.RIGHT_EYE);
            FaceLandmark noseBaseLandmark = face.getLandmark(FaceLandmark.NOSE_BASE);
            FaceLandmark mouthBottomLandmark = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);

            if (rightEyeLandmark != null && mouthBottomLandmark != null && noseBaseLandmark != null) {
                float lipToNoseDistance = Math.abs(mouthBottomLandmark.getPosition().y - noseBaseLandmark.getPosition().y);
                float lipToRightEyeDistance = Math.abs(mouthBottomLandmark.getPosition().y - rightEyeLandmark.getPosition().y);
                float relativeLipToNoseDistance = lipToNoseDistance / lipToRightEyeDistance;

                Log.d(TAG, "상대적인 아래 입술과 코 간 거리 비율: " + relativeLipToNoseDistance);
                Log.d(TAG, "웃음 확률: " + smilingProbability);

                if (relativeLipToNoseDistance > 0.80 && smilingProbability < 0.2) {
                    processYawning(callback);
                } else {
                    if (isYawning) {
                        Log.d(TAG, "하품이 중단됨!");
                    }
                    isYawning = false;
                    yawnStartTime = 0;
                }
            } else {
                Log.w(TAG, "랜드마크 데이터가 누락되었습니다.");
            }
        }
    }

    private void processYawning(FaceDetectionCallback callback) {
        if (!isYawning) {
            isYawning = true;
            yawnStartTime = SystemClock.elapsedRealtime();
            Log.d(TAG, "하품 시작됨!");
        } else {
            long yawnDuration = SystemClock.elapsedRealtime() - yawnStartTime;
            if (yawnDuration >= YAWN_DURATION_THRESHOLD) {
                Log.d(TAG, "하품 감지됨! (지속 시간: " + yawnDuration + ")");
                callback.onYawnDetected();
            }
        }
    }

}

