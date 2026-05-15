package com.yy.catmed.data.repository

import com.yy.catmed.data.dao.*
import com.yy.catmed.data.entity.*

class AppRepository(
    private val medDao: MedicationDao,
    private val recordDao: MedicationRecordDao,
    private val weightDao: CatWeightDao,
    private val prescDao: PrescriptionDao,
    private val exerciseDao: ExerciseRecordDao
) {
    // ===== 药品 =====
    val allMedications = medDao.getAllMedications()
    val activeMedications = medDao.getActiveMedications()
    suspend fun getActiveMedicationsList() = medDao.getActiveMedicationsList()
    suspend fun saveMedication(med: Medication): Long = medDao.insert(med)
    suspend fun updateMedication(med: Medication) = medDao.update(med)
    suspend fun deleteMedication(med: Medication) = medDao.delete(med)

    // ===== 喂药记录 =====
    suspend fun saveMedicationRecord(record: MedicationRecord): Long = recordDao.insert(record)
    suspend fun getMedicationRecordsInRange(start: Long, end: Long) = recordDao.getRecordsInRange(start, end)
    suspend fun getRecordsForMedication(medId: Long, start: Long, end: Long) = recordDao.getRecordsForMedication(medId, start, end)

    // ===== 体重 =====
    suspend fun getLatestWeight(): CatWeight? = weightDao.getLatestWeight()
    suspend fun saveWeight(weight: CatWeight): Long = weightDao.insert(weight)

    // ===== 医嘱 =====
    suspend fun getActivePrescriptions() = prescDao.getActivePrescriptions()
    suspend fun savePrescription(p: Prescription): Long = prescDao.insert(p)
    suspend fun deactivatePrescription(p: Prescription) {
        prescDao.update(p.copy(isActive = false, endDate = System.currentTimeMillis()))
    }

    // ===== 运动 =====
    suspend fun saveExerciseRecord(record: ExerciseRecord): Long = exerciseDao.insert(record)
    suspend fun getExerciseRecordsInRange(start: Long, end: Long) = exerciseDao.getRecordsInRange(start, end)
}
