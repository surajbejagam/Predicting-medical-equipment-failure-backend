package com.example.predict.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "devices")
@Data
public class Devices {
    private String name;
    @Id
    private int id;
    private  int manufacturer_id;


}
