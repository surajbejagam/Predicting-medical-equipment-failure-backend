package com.example.predict.repo;

import com.example.predict.model.Devices;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DevicesRepo extends MongoRepository<Devices,Integer> {
    Devices findByName(String device_Id);
    @Query(value = "{ 'name': { $regex: ?0, $options: 'i' }, 'country': ?1 }")
    Optional<Devices> findFirstByNameRegexAndCountry(String nameRegex, String country);
}
