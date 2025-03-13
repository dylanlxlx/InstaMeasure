package com.dylanlxlx.instameasure.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dylanlxlx.instameasure.model.Measurement;

import java.util.ArrayList;
import java.util.List;

/**
 * 测量数据存储库。
 * 存储和管理测量历史记录。
 */
public class MeasurementRepository {
    private static volatile MeasurementRepository instance;

    // 用于测量的 LiveData
    private final MutableLiveData<List<Measurement>> measurements = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Measurement> currentMeasurement = new MutableLiveData<>();

    private MeasurementRepository() {
    }

    // 单例模式
    public static MeasurementRepository getInstance() {
        if (instance == null) {
            synchronized (MeasurementRepository.class) {
                if (instance == null) {
                    instance = new MeasurementRepository();
                }
            }
        }
        return instance;
    }

    /**
     * 保存新的测量
     * @param measurement 要保存的测量
     */
    public void saveMeasurement(Measurement measurement) {
        List<Measurement> measurementList = measurements.getValue();
        if (measurementList == null) {
            measurementList = new ArrayList<>();
        }

        measurementList.add(measurement);
        measurements.postValue(new ArrayList<>(measurementList)); // 创建新列表以触发观察者
        currentMeasurement.postValue(measurement);
    }

    /**
     * 删除测量数据
     * @param measurement 要删除的测量
     */
    public void deleteMeasurement(Measurement measurement) {
        List<Measurement> measurementList = measurements.getValue();
        if (measurementList == null) {
            return;
        }

        measurementList.remove(measurement);
        measurements.postValue(new ArrayList<>(measurementList));
    }

    /**
     * 设置当前测量数据
     * @param measurement 当前测量数据
     */
    public void setCurrentMeasurement(Measurement measurement) {
        currentMeasurement.postValue(measurement);
    }

    /**
     * 获取所有保存的测量结果
     * @return 测量列表的 LiveData
     */
    public LiveData<List<Measurement>> getMeasurements() {
        return measurements;
    }

    /**
     * 获取当前测量数据
     * @return 当前测量的 LiveData
     */
    public LiveData<Measurement> getCurrentMeasurement() {
        return currentMeasurement;
    }
}