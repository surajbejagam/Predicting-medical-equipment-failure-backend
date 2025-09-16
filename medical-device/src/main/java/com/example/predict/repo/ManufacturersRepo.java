package com.example.predict.repo;

import com.example.predict.model.Manufacturers;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
@Repository
public interface ManufacturersRepo extends MongoRepository<Manufacturers, Integer> {
     Manufacturers findByName(String manufacturerId) ;
     List<Manufacturers> findByIdIn(Collection<Object> ids);
}
