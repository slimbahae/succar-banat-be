package com.slimbahael.beauty_center.repository;

import com.slimbahael.beauty_center.model.Reservation;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ReservationRepository extends MongoRepository<Reservation, String> {

    List<Reservation> findByCustomerId(String customerId);

    List<Reservation> findByStaffId(String staffId);

    List<Reservation> findByReservationDateAndTimeSlot(Date reservationDate, String timeSlot);

    List<Reservation> findByStaffIdAndReservationDateAndTimeSlot(String staffId, Date reservationDate, String timeSlot);

    List<Reservation> findByStatus(String status);

    List<Reservation> findByReservationDateBetween(Date startDate, Date endDate);

    List<Reservation> findByStaffIdAndReservationDateBetween(String staffId, Date startDate, Date endDate);

    List<Reservation> findByReservationDate(@NotNull(message = "Date is required") Date date);
}