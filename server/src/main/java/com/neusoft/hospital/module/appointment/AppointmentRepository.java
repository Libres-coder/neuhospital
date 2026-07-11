package com.neusoft.hospital.module.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(String patientId);

    @Query("select count(a) from Appointment a where a.patientId = :pid and a.status = 'no_show'")
    int countNoShow(@Param("pid") String patientId);

    /**
     * Counts active (non-cancelled, non-no-show) appointments that occupy the
     * same doctor + date + slot. Uses the (doctor_id, appointment_date) index,
     * so it scales with bookings-per-day, not with the whole table.
     */
    @Query("""
        select count(a) from Appointment a
        where a.doctorId = :doctorId
          and a.appointmentDate = :date
          and a.timeSlot = :slot
          and a.status not in ('cancelled', 'no_show')
        """)
    long countActiveOnSlot(@Param("doctorId") String doctorId,
                           @Param("date") String date,
                           @Param("slot") String slot);

    /**
     * Same predicate as above, but for one shot across many slots. Returns
     * rows of (time_slot, cnt). Used by the doctor schedule endpoint to
     * compute remaining capacity in a single round-trip.
     */
    @Query("""
        select a.timeSlot as slot, count(a) as cnt
        from Appointment a
        where a.doctorId = :doctorId
          and a.appointmentDate = :date
          and a.status not in ('cancelled', 'no_show')
        group by a.timeSlot
        """)
    List<SlotOccupancy> occupancyByDate(@Param("doctorId") String doctorId,
                                        @Param("date") String date);

    /** Projection used by [occupancyByDate]. */
    interface SlotOccupancy {
        String getSlot();
        long getCnt();
    }
}